package com.baishancloud.orchsym.sap.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.conn.jco.server.JCoServer;
import com.sap.conn.jco.server.JCoServerContextInfo;
import com.sap.conn.jco.server.JCoServerErrorListener;
import com.sap.conn.jco.server.JCoServerExceptionListener;
import com.sap.conn.jco.server.JCoServerState;
import com.sap.conn.jco.server.JCoServerStateChangedListener;

/**
 * @author GU Guoqiang
 *
 */
public class SAPServerAdapter implements JCoServerErrorListener, JCoServerExceptionListener, JCoServerStateChangedListener {

    private static final Logger logger = LoggerFactory.getLogger(SAPServerAdapter.class);

    /*
     * (non-Javadoc)
     * 
     * @see com.sap.conn.jco.server.JCoServerExceptionListener#serverExceptionOccurred(com.sap.conn.jco.server.JCoServer, java.lang.String, com.sap.conn.jco.server.JCoServerContextInfo,
     * java.lang.Exception)
     */
    @Override
    public void serverExceptionOccurred(JCoServer jcoServer, String connectionId, JCoServerContextInfo serverCtx, Exception exception) {
        // logger.error(">>> Error occured on " + jcoServer.getProgramID() + " connection " + connectionId, exception);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sap.conn.jco.server.JCoServerErrorListener#serverErrorOccurred(com.sap.conn.jco.server.JCoServer, java.lang.String, com.sap.conn.jco.server.JCoServerContextInfo, java.lang.Error)
     */
    @Override
    public void serverErrorOccurred(JCoServer jcoServer, String connectionId, JCoServerContextInfo serverCtx, Error error) {
        // logger.error(">>> Error occured on " + jcoServer.getProgramID() + " connection " + connectionId, error);

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sap.conn.jco.server.JCoServerStateChangedListener#serverStateChangeOccurred(com.sap.conn.jco.server.JCoServer, com.sap.conn.jco.server.JCoServerState,
     * com.sap.conn.jco.server.JCoServerState)
     */
    @Override
    public void serverStateChangeOccurred(JCoServer server, JCoServerState oldState, JCoServerState newState) {
        // logger.info(">>> Server state changed from " + oldState.toString() + " to " + newState.toString() + " on server with program id " + server.getProgramID());

    }

}
