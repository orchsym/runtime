package com.baishancloud.orchsym.sap.server;

import com.sap.conn.jco.AbapException;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoParameterList;
import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerFunctionHandler;

/**
 * @author GU Guoqiang
 *
 */
public class SAPServerFunctionHandler implements JCoServerFunctionHandler {

    /*
     * (non-Javadoc)
     * 
     * @see com.sap.conn.jco.server.JCoServerFunctionHandler#handleRequest(com.sap.conn.jco.server.JCoServerContext, com.sap.conn.jco.JCoFunction)
     */
    @Override
    public void handleRequest(JCoServerContext serverCtx, JCoFunction function) throws AbapException {
        System.out.println("----------------------------------------------------------------");
        System.out.println("call              : " + function.getName());
        System.out.println("ConnectionId      : " + serverCtx.getConnectionID());
        System.out.println("SessionId         : " + serverCtx.getSessionID());
        System.out.println("TID               : " + serverCtx.getTID());
        System.out.println("repository name   : " + serverCtx.getRepository().getName());
        System.out.println("is in transaction : " + serverCtx.isInTransaction());
        System.out.println("is stateful       : " + serverCtx.isStatefulSession());
        System.out.println("----------------------------------------------------------------");
        System.out.println("gwhost: " + serverCtx.getServer().getGatewayHost());
        System.out.println("gwserv: " + serverCtx.getServer().getGatewayService());
        System.out.println("progid: " + serverCtx.getServer().getProgramID());
        System.out.println("----------------------------------------------------------------");
        System.out.println("attributes  : ");
        System.out.println(serverCtx.getConnectionAttributes().toString());
        System.out.println("----------------------------------------------------------------");
        System.out.println("CPIC conversation ID: " + serverCtx.getConnectionAttributes().getCPICConversationID());
        System.out.println("----------------------------------------------------------------");

        final JCoParameterList importParameterList = function.getImportParameterList();
        System.out.println("import: " + importParameterList);
        
        final JCoParameterList exportParameterList = function.getExportParameterList();
        System.out.println("export: " + exportParameterList);
    }

}
