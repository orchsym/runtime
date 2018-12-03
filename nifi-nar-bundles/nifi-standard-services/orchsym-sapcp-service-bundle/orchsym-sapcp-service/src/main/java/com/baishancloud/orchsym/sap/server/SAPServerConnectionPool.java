package com.baishancloud.orchsym.sap.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Marks;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.controller.ControllerServiceInitializationContext;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.reporting.InitializationException;

import com.baishancloud.orchsym.sap.SAPConnectionPool;
import com.baishancloud.orchsym.sap.SAPDataManager;
import com.baishancloud.orchsym.sap.SAPException;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.ext.DestinationDataProvider;
import com.sap.conn.jco.ext.ServerDataProvider;
import com.sap.conn.jco.server.JCoServer;

/**
 * @author GU Guoqiang
 *
 */
@Marks(createdDate = "2018-07-30")
@Tags({ "SAP", "RFC", "ABAP", "TCP", "JCo", "server", "connection", "pooling"})
@CapabilityDescription("Provides SAP Connection Pooling Service. Connections can be asked from pool and returned after usage.")
public class SAPServerConnectionPool extends SAPConnectionPool implements SAPServerConnectionPoolService {
    static final String SERVER_NAME = "OServer";

    static final PropertyDescriptor SAP_CONN_COUNT = new PropertyDescriptor.Builder()//
            .name("connection-count") //$NON-NLS-1$
            .displayName("Connection Count")//$NON-NLS-1$
            .description("Maximum number of connections that can be connected for a SAP ABAP")//$NON-NLS-1$
            .required(false)//
            .defaultValue("2")//$NON-NLS-1$
            .addValidator(StandardValidators.NON_NEGATIVE_INTEGER_VALIDATOR)//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
            .build();

    static final PropertyDescriptor SAP_PROGID = new PropertyDescriptor.Builder()//
            .name("program-id") //$NON-NLS-1$
            .displayName("Program ID")//$NON-NLS-1$
            .description("The Program Id of RFC destination to registry in SAP TCP/IP connection")//$NON-NLS-1$
            .required(true)//
            .addValidator(StandardValidators.NON_BLANK_VALIDATOR)//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
            .build();

    private AtomicBoolean running = new AtomicBoolean();
    private final ReentrantLock lock = new ReentrantLock();
    private Map<String, SAPRequestCallback> callbackMap = new Hashtable<>();

    protected volatile Properties serverProperties;

    private volatile SAPServerRunner serverRunner;
    private volatile ExecutorService executorService;

    @Override
    protected void init(ControllerServiceInitializationContext config) throws InitializationException {
        super.init(config);

        final List<PropertyDescriptor> props = new ArrayList<>(this.properties);
        props.add(SAP_PROGID);
        // props.add(SAP_CONN_COUNT); //don't set, only support one

        properties = Collections.unmodifiableList(props);
    }

    @Override
    public void onConfigured(final ConfigurationContext context) throws InitializationException {
        super.onConfigured(context);
        executorService = Executors.newSingleThreadExecutor();

        final String programId = context.getProperty(SAP_PROGID).evaluateAttributeExpressions().getValue();
        // final Integer connectionCount = context.getProperty(SAP_CONN_COUNT).evaluateAttributeExpressions().asInteger();

        serverProperties = new Properties();
        // same IP as client
        serverProperties.setProperty(ServerDataProvider.JCO_GWHOST, clientProperties.getProperty(DestinationDataProvider.JCO_ASHOST));
        // 00 is SYSNR, also can find from parameters in SMGW
        serverProperties.setProperty(ServerDataProvider.JCO_GWSERV, "sapgw" //$NON-NLS-1$
                + clientProperties.getProperty(DestinationDataProvider.JCO_SYSNR));
        // from Program ID in SM59
        serverProperties.setProperty(ServerDataProvider.JCO_PROGID, programId);
        // server type of client
        serverProperties.setProperty(ServerDataProvider.JCO_REP_DEST, SAPDataManager.getDestinationName(getIdentifier(), serverType));
        serverProperties.setProperty(ServerDataProvider.JCO_CONNECTION_COUNT, "2"/* connectionCount.toString() */);

        // set Dynamic properties for server
        context.getProperties().entrySet().stream().filter(e -> e.getKey().isDynamic()).forEach(e -> {
            serverProperties.setProperty(e.getKey().getName(), e.getValue());
        });

        try {
            updateProperties();
            connect();
        } catch (SAPException e) {
            throw new InitializationException(e.getCause());
        }
    }

    @Override
    public void shutdown() {
        stop(); // also stop all
        removeProperties();

        super.shutdown();
    }

    private void updateProperties() {
        final String identifier = getIdentifier();

        SAPDataManager.getInstance().updateClientProp(identifier, serverType, clientProperties);
        SAPDataManager.getInstance().updateServerProp(identifier, SERVER_NAME, serverProperties);
    }

    private void removeProperties() {
        final String identifier = getIdentifier();
        if (serverType != null) {
            SAPDataManager.getInstance().updateClientProp(identifier, serverType, null);
        }
        SAPDataManager.getInstance().updateServerProp(identifier, SERVER_NAME, null);
    }

    @Override
    public void connect() throws SAPException {

        try {
            final JCoDestination destination = SAPDataManager.getInstance().getDestination(getIdentifier(), serverType);
            destination.ping();
        } catch (JCoException e) {
            throw new SAPException(e);
        }
    }

    private void stop() {
        lock.lock();

        if (serverRunner != null) {
            serverRunner.stop();
            serverRunner = null;
        }
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }

        try {
            Thread.sleep(100); // waiting to stop the server
        } catch (InterruptedException e) {
            //
        }
        lock.unlock();
    }

    private void restart() throws SAPException {
        updateProperties(); // reset, make sure the properties is right

        stop();
        start();
    }

    private void start() throws SAPException {
        lock.lock();
        running.set(true);
        connect();

        try {
            JCoServer jcoServer = SAPDataManager.getInstance().getServer(this.getIdentifier(), SERVER_NAME);
            executorService = Executors.newSingleThreadExecutor();
            serverRunner = new SAPServerRunner(jcoServer, callbackMap);
            executorService.submit(serverRunner);

            try {
                Thread.sleep(200); // waiting to stop the server
            } catch (InterruptedException e) {
                //
            }

            running.set(false);
        } catch (JCoException e) {
            throw new SAPException(e);
        } finally {
            lock.unlock();
        }

    }

    public void registryRequest(final SAPRequestCallback requestCallback) throws SAPServerException {
        callbackMap.put(requestCallback.getIdentifier(), requestCallback);

        try {
            restart(); // force to restart, if other processor started, maybe have problem.
        } catch (SAPException e) {
            throw new SAPServerException(e.getCause());
        }
    }

    @Override
    public void unregistryRequest(String identifier) {
        callbackMap.remove(identifier);
        stop();
    }

}
