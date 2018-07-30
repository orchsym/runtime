package com.baishancloud.orchsym.sap.server;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baishancloud.orchsym.sap.i18n.Messages;
import com.baishancloud.orchsym.sap.record.JCoRecordUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sap.conn.jco.AbapException;
import com.sap.conn.jco.JCo;
import com.sap.conn.jco.JCoCustomRepository;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoFunctionTemplate;
import com.sap.conn.jco.JCoRuntimeException;
import com.sap.conn.jco.server.DefaultServerHandlerFactory;
import com.sap.conn.jco.server.JCoServer;
import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerContextInfo;
import com.sap.conn.jco.server.JCoServerFunctionHandler;
import com.sap.conn.jco.server.JCoServerState;

/**
 * @author GU Guoqiang
 *
 */
public class SAPServerRunner implements SAPServerRunnable {
    private static final Logger logger = LoggerFactory.getLogger(SAPServerRunner.class);

    private final JCoServer jcoServer;
    private final JCoFunctionTemplate functionTempalte;

    private String[] importTables;
    private volatile SAPRequestCallback requestCallback;
    private volatile boolean stopped;

    private JCoCustomRepository repository;

    public SAPServerRunner(JCoServer jcoServer, JCoFunctionTemplate functionTempalte) {
        this.jcoServer = jcoServer;
        this.functionTempalte = functionTempalte;
    }

    public void setImportTables(String[] importTables) {
        this.importTables = importTables;
    }

    public void registryRequest(SAPRequestCallback callback) {
        if (callback != null && this.requestCallback != null) {
            if (this.requestCallback.getIdentifier().equals(callback.getIdentifier())) {
                return; // same one
            } else {
                throw new IllegalArgumentException(Messages.getString("SAPServerRunner.uniqueMessage")); //$NON-NLS-1$
            }
        }
        this.requestCallback = callback;
    }

    public void unregistryRequest(String identifier) {
        if (requestCallback != null && requestCallback.getIdentifier().equals(identifier)) { // same one
            requestCallback = null;
        }
    }

    protected void handleRequest(JCoServerContext serverCtx, JCoFunction function) throws AbapException {
        if (!function.getName().equals(functionTempalte.getName())) { // must same name
            return;
        }
        if (requestCallback == null) { // nothing to do
            return;
        }
        final boolean ignoreEmptyValues = requestCallback.ignoreEmptyValue();

        final Map<String, Object> importResults = new LinkedHashMap<>();
        // get import
        importResults.putAll(JCoRecordUtil.convertToMap(function.getImportParameterList(), ignoreEmptyValues));
        // get import tables
        importResults.putAll(JCoRecordUtil.convertTablesToMap(function, ignoreEmptyValues, importTables));

        requestCallback.process(importResults); // write import data with flow

        // will block to wait for the response
        final String responseData = requestCallback.waitResponse();

        if (StringUtils.isNotBlank(responseData)) {
            try {
                final ObjectMapper objectMapper = new ObjectMapper();
                final JsonNode jsonNode = objectMapper.readTree(responseData);
                if (jsonNode.isArray()) {
                    ArrayNode arrNode = (ArrayNode) jsonNode;
                    Iterator<JsonNode> elements = arrNode.elements();
                    while (elements.hasNext()) {
                        setResponseValues(function, elements.next());
                    }
                } else if (jsonNode.isObject()) {
                    setResponseValues(function, jsonNode);
                }
            } catch (IOException e) {
                throw new AbapException("Invalid value, " + e.getMessage());
            }

        }
    }

    private void setResponseValues(JCoFunction function, JsonNode jsonNode) throws IOException {
        if (jsonNode.isObject()) {
            final Map map = new ObjectMapper().readValue(jsonNode.toString(), Map.class);
            // set the response values
            JCoRecordUtil.setParams(function.getExportParameterList(), map);
            JCoRecordUtil.setParams(function.getTableParameterList(), map);
        }
    }

    protected SAPServerAdapter createSAPServerListener() {
        return new SAPServerAdapter() {

            @Override
            public void serverExceptionOccurred(JCoServer jcoServer, String connectionId, JCoServerContextInfo serverCtx, Exception exception) {
                super.serverExceptionOccurred(jcoServer, connectionId, serverCtx, exception);
                logger.error(">>> Error occured on " + jcoServer.getProgramID() + " connection " + connectionId, exception);
            }

            @Override
            public void serverErrorOccurred(JCoServer jcoServer, String connectionId, JCoServerContextInfo serverCtx, Error error) {
                super.serverErrorOccurred(jcoServer, connectionId, serverCtx, error);
                logger.error(">>> Error occured on " + jcoServer.getProgramID() + " connection " + connectionId, error);
            }

        };
    }

    @Override
    public Boolean call() throws Exception {
        if (functionTempalte == null) {
            return false;
        }
        repository = JCo.createCustomRepository("OrchsymRepository" + System.currentTimeMillis());
        repository.addFunctionTemplateToCache(functionTempalte);
        String repDest = jcoServer.getRepositoryDestination();
        if (repDest != null) {
            repository.setDestination(JCoDestinationManager.getDestination(repDest));
        }
        jcoServer.setRepository(repository);

        DefaultServerHandlerFactory.FunctionHandlerFactory factory = new DefaultServerHandlerFactory.FunctionHandlerFactory();
        factory.registerHandler(functionTempalte.getName(), new JCoServerFunctionHandler() {

            @Override
            public void handleRequest(JCoServerContext serverCtx, JCoFunction function) throws AbapException {
                SAPServerRunner.this.handleRequest(serverCtx, function);
            }

        });
        jcoServer.setCallHandlerFactory(factory);

        final SAPServerAdapter listener = createSAPServerListener();
        jcoServer.addServerErrorListener(listener);
        jcoServer.addServerExceptionListener(listener);
        jcoServer.addServerStateChangedListener(listener);

        jcoServer.start();

        stopped = false;

        // block server
        while (jcoServer.getState() == JCoServerState.ALIVE) {
            if (stopped) {
                break;
            }
            try {
                Thread.sleep(500); // wait
            } catch (InterruptedException e) {
                //
            }
        }

        return true;
    }

    public boolean isRunning() {
        return false == this.stopped;
    }

    public void stop() {
        this.stopped = true;
        try {
            if (jcoServer != null) {
                jcoServer.stop();
                jcoServer.release();
                repository.clear();
            }
        } catch (JCoRuntimeException e) {
            logger.error(e.getMessage(), e);
        }
    }

}