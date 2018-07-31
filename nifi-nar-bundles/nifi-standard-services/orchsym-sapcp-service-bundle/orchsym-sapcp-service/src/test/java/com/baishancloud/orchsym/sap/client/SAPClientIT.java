package com.baishancloud.orchsym.sap.client;

import java.util.UUID;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.baishancloud.orchsym.sap.AbsSAPIT;
import com.baishancloud.orchsym.sap.SAPDataManager;
import com.baishancloud.orchsym.sap.option.ESAPServerType;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoFunctionTemplate;
import com.sap.conn.jco.JCoListMetaData;
import com.sap.conn.jco.JCoMetaData;
import com.sap.conn.jco.JCoRecordMetaData;
import com.sap.conn.jco.JCoRuntimeException;

/**
 * @author GU Guoqiang
 *
 */
@Ignore
public class SAPClientIT extends AbsSAPIT {

    private JCoDestination destination;

    @Before
    public void init() throws Exception {
        final String identifier = UUID.randomUUID().toString();
        final ESAPServerType serverType = ESAPServerType.AS;

        SAPDataManager.getInstance().updateClientProp(identifier, serverType, getASProp());

        destination = SAPDataManager.getInstance().getDestination(identifier, serverType);
    }

    @After
    public void after() {
        destination = null;
    }

    @Test
    public void printAttributes() throws JCoException {
        destination.ping();
        System.out.println("+++++++++++++++++++++++++++++++++++");
        System.out.println(destination.getAttributes());
        System.out.println("+++++++++++++++++++++++++++++++++++");
    }

    @Test(expected = JCoRuntimeException.class)
    public void callOperator_lowercase() throws JCoException {
        JCoFunction function = destination.getRepository().getFunction("ZBSY_FUN_OP");
        Assert.assertNotNull(function);

        function.getImportParameterList().setValue("p1", 12);
        function.getImportParameterList().setValue("p2", 4);
        function.getImportParameterList().setValue("OPERATOR", "/");
    }

    @Test
    public void callOperator() throws JCoException {
        JCoFunction function = destination.getRepository().getFunction("ZBSY_FUN_OP");
        Assert.assertNotNull(function);

        function.getImportParameterList().setValue("P1", 12);
        function.getImportParameterList().setValue("P2", 4);
        function.getImportParameterList().setValue("OPERATOR", "/");

        function.execute(destination);

        final int result = function.getExportParameterList().getInt("RESULT");
        System.out.println("---------------------------");
        System.out.println("Result:" + result);
        System.out.println("---------------------------");
    }

    @Test
    public void printTable() throws JCoException {
        JCoFunction function = destination.getRepository().getFunction("RFC_READ_TABLE");
        Assert.assertNotNull(function);

        System.out.println("**************All*****************>>>>");
        final JCoFunctionTemplate functionTemplate = function.getFunctionTemplate();
        System.out.println(functionTemplate);

        System.out.println("**************Import*****************>>>>");
        final JCoListMetaData importParameterList = functionTemplate.getImportParameterList();
        // System.out.println(importParameterList);
        printMetadata(importParameterList);

        System.out.println("**************Export*****************>>>>");
        final JCoListMetaData exportParameterList = functionTemplate.getExportParameterList();
        // System.out.println(exportParameterList);
        printMetadata(importParameterList);

        System.out.println("*************Table******************>>>>");
        final JCoListMetaData tableParameterList = functionTemplate.getTableParameterList();
        // System.out.println(tableParameterList);
        printMetadata(tableParameterList);

        System.out.println("<<<<*******************************");
    }

    private void printMetadata(JCoMetaData metadata) {
        for (int i = 0; i < metadata.getFieldCount(); i++) {
            final String name = metadata.getName(i);
            System.out.println(name + " : " + metadata.getTypeAsString(i));
            if (metadata.getType(i) == JCoMetaData.TYPE_TABLE) {
                System.out.println("-------------Table：" + name + "-----------------");
                final JCoRecordMetaData recordMetaData = metadata.getRecordMetaData(i);
                printMetadata(recordMetaData);
            }
        }
    }
}
