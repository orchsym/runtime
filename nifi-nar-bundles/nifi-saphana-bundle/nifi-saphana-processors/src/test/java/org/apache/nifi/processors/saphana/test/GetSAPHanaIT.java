package org.apache.nifi.processors.saphana.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.apache.nifi.dbcp.DBCPService;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processors.saphana.GetSAPHana;
import org.apache.nifi.processors.saphana.util.ConnectionFactory;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Test;

@SuppressWarnings("deprecation")
public class GetSAPHanaIT {
    
    public synchronized static TestRunner executeSQL(String sql) throws InitializationException {
        final DBCPService dbcp = new ConnectionFactory();
        final Map<String, String> dbcpProperties = new HashMap<>();
        final TestRunner runner = TestRunners.newTestRunner(GetSAPHana.class);
        runner.addControllerService("dbcp", dbcp, dbcpProperties);
        runner.enableControllerService(dbcp);
        runner.setProperty(GetSAPHana.DBCP_SERVICE, "dbcp");
        runner.setProperty(GetSAPHana.SQL_SELECT_QUERY, String.valueOf(sql));
        runner.setIncomingConnection(false);
        ProcessContext processContext = runner.getProcessContext();
        GetSAPHana processor = (GetSAPHana) runner.getProcessor();
        processor.updateScheduledTrue();
        processor.onTrigger(processContext, runner.getProcessSessionFactory());
        return runner;
    }

    @Test
    public void testOnTriggerProcessContextProcessSession() throws InitializationException {
        final TestRunner runner = executeSQL("select * from baishanTable");
        final List<MockFlowFile> flowFiles = runner.getFlowFilesForRelationship(GetSAPHana.REL_SUCCESS);
        long totalFlowFilesSize = 0;
        long rowCount = 0;
        for (final MockFlowFile flowFile : flowFiles) {
            rowCount = Long.parseLong(flowFile.getAttribute(GetSAPHana.RESULT_ROW_COUNT));
            System.out.println(flowFile);
            totalFlowFilesSize += flowFile.getSize();
            System.out.println(new String(flowFile.toByteArray()));
        }
        System.out.println("rowCount：" + rowCount);
        System.out.println("totalFlowFilesSize：" + totalFlowFilesSize);
        Assert.assertEquals(5, rowCount);
    }

}
