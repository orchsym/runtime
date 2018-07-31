package com.baishancloud.orchsym.sap.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.nifi.annotation.documentation.CapabilityDescription;
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
import com.baishancloud.orchsym.sap.i18n.Messages;
import com.baishancloud.orchsym.sap.metadata.JCoMetaUtil;
import com.baishancloud.orchsym.sap.metadata.param.ESAPTabType;
import com.baishancloud.orchsym.sap.metadata.param.SAPParamRoot;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunctionTemplate;
import com.sap.conn.jco.ext.DestinationDataProvider;
import com.sap.conn.jco.ext.ServerDataProvider;
import com.sap.conn.jco.server.JCoServer;

/**
 * @author GU Guoqiang
 *
 */
@Tags({ "SAP", "RFC", "ABAP", "TCP", "JCo", "server", "connection", "pooling", "Orchsym" })
@CapabilityDescription("Provides SAP Connection Pooling Service. Connections can be asked from pool and returned after usage.")
@SuppressWarnings("rawtypes")
public class SAPServerConnectionPool extends SAPConnectionPool implements SAPServerConnectionPoolService {

    static final PropertyDescriptor SAP_CONN_COUNT = new PropertyDescriptor.Builder()//
            .name("sap-connection-count") //$NON-NLS-1$
            .displayName(Messages.getString("SAPServerConnectionPool.ConnectionCount"))//$NON-NLS-1$
            .description(Messages.getString("SAPServerConnectionPool.ConnectionCount_Desc"))//$NON-NLS-1$
            .required(false)//
            .defaultValue("2")//$NON-NLS-1$
            .addValidator(StandardValidators.NON_NEGATIVE_INTEGER_VALIDATOR)//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
            .build();

    static final PropertyDescriptor SAP_PROGID = new PropertyDescriptor.Builder()//
            .name("sap-program-id") //$NON-NLS-1$
            .displayName(Messages.getString("SAPServerConnectionPool.ProgramId"))//$NON-NLS-1$
            .description(Messages.getString("SAPServerConnectionPool.ProgramId_Desc"))//$NON-NLS-1$
            .required(true)//
            .addValidator(StandardValidators.NON_BLANK_VALIDATOR)//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
            .build();

    static final PropertyDescriptor SAP_FUNCTION = new PropertyDescriptor.Builder().name("sap-function") //$NON-NLS-1$
            .displayName(Messages.getString("SAPServerConnectionPool.Function"))//$NON-NLS-1$
            .description(Messages.getString("SAPServerConnectionPool.Function_Desc"))//$NON-NLS-1$
            .required(true)//
            .addValidator(StandardValidators.NON_BLANK_VALIDATOR)//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
            .build();

    static final PropertyDescriptor SAP_FUN_METADATA = new PropertyDescriptor.Builder()//
            .name("sap-function-metadata")//$NON-NLS-1$
            .displayName(Messages.getString("SAPServerConnectionPool.FunMetadata"))//$NON-NLS-1$
            .description(Messages.getString("SAPServerConnectionPool.FunMetadata_Desc"))//$NON-NLS-1$
            .required(true)//
            .addValidator(StandardValidators.NON_BLANK_VALIDATOR)//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
            .build();

    protected volatile Properties serverProperties;

    private volatile String serverName;
    private volatile SAPServerRunner serverRunner;
    private volatile ExecutorService executorService;

    @Override
    protected void init(ControllerServiceInitializationContext config) throws InitializationException {
        super.init(config);

        final List<PropertyDescriptor> props = new ArrayList<>(this.properties);
        props.add(SAP_PROGID);
        // props.add(SAP_CONN_COUNT); //don't set, only support one
        props.add(SAP_FUNCTION);
        props.add(SAP_FUN_METADATA);

        properties = Collections.unmodifiableList(props);
    }

    @Override
    public void onConfigured(final ConfigurationContext context) throws InitializationException {
        super.onConfigured(context);
        executorService = Executors.newSingleThreadExecutor();

        String functionName = context.getProperty(SAP_FUNCTION).evaluateAttributeExpressions().getValue();

        String funMetadataJson = context.getProperty(SAP_FUN_METADATA).evaluateAttributeExpressions().getValue();
        SAPParamRoot paramRoot = null;
        try {
            paramRoot = new SAPParamRoot.Parser().parse(funMetadataJson);
        } catch (IOException e) {
            throw new InitializationException(e);
        }

        String[] importTables = null;
        if (paramRoot.getTableParams() != null)
            importTables = paramRoot.getTableParams().stream() //
                    .filter(t -> t.getType() != null && t.getTabType() != ESAPTabType.OUTPUT) // not structure and output table
                    .map(p -> p.getName())//
                    .toArray(String[]::new);

        //
        serverName = "SAPServer" + System.currentTimeMillis(); //$NON-NLS-1$

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
        serverProperties.setProperty(ServerDataProvider.JCO_REP_DEST, serverType.getValue());
        serverProperties.setProperty(ServerDataProvider.JCO_CONNECTION_COUNT, "1"/* connectionCount.toString() */);

        SAPDataManager.getInstance().updateServerProp(this.getIdentifier(), serverName, serverProperties);

        try {
            connect();
            
            JCoServer jcoServer = SAPDataManager.getInstance().getServer(this.getIdentifier(), serverName);

            final JCoFunctionTemplate functionTempalate = JCoMetaUtil.createFunTemplate(functionName, paramRoot);

            serverRunner = new SAPServerRunner(jcoServer, functionTempalate);
            serverRunner.setImportTables(importTables);

            executorService.submit(serverRunner);
        } catch (JCoException | SAPException e) {
            throw new InitializationException(e);
        }
    }

    @Override
    public void shutdown() {
        if (serverRunner != null) {
            serverRunner.stop();
            serverRunner.registryRequest(null); // clear callback
        }
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }

        super.shutdown();
        if (serverName != null)
            SAPDataManager.getInstance().updateServerProp(this.getIdentifier(), serverName, null);
    }

    public void registryRequest(final SAPRequestCallback requestCallback) throws SAPServerException {
        if (serverRunner == null || !serverRunner.isRunning())
            throw new SAPServerException(Messages.getString("SAPConnectionPool.WrongServer")); //$NON-NLS-1$
        serverRunner.registryRequest(requestCallback);
    }

    @Override
    public void unregistryRequest(String identifier) {
        if (serverRunner == null || !serverRunner.isRunning())
            throw new SAPServerException(Messages.getString("SAPConnectionPool.WrongServer")); //$NON-NLS-1$
        serverRunner.unregistryRequest(identifier);

    }

}
