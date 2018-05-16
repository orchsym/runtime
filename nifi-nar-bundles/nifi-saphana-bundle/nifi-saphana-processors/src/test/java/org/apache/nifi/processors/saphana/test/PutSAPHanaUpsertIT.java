package org.apache.nifi.processors.saphana.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.apache.nifi.dbcp.DBCPService;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processors.saphana.GetSAPHana;
import org.apache.nifi.processors.saphana.PutSAPHana;
import org.apache.nifi.processors.saphana.util.ConnectionFactory;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Test;

/***
 * baishanTable表源数据
 *     A,B
 *     1,"a"
 *     2,"b"
 *     3,"c"
 *     4,"d"
 *     5,"ee"
 * */
@SuppressWarnings("deprecation")
public class PutSAPHanaUpsertIT {

    @Test
    public void testOnTriggerProcessContextProcessSession() throws InitializationException, FileNotFoundException {
        updateTable();
        checkTableRowCount();
        recoverTable();
    }
    
    /**
     * 更新数据
     * */
    public void updateTable() throws InitializationException, FileNotFoundException {
        final DBCPService dbcp = new ConnectionFactory();
        final Map<String, String> dbcpProperties = new HashMap<>();
        final TestRunner runner = TestRunners.newTestRunner(PutSAPHana.class);
        runner.addControllerService("dbcp", dbcp, dbcpProperties);
        runner.enableControllerService(dbcp);
        runner.setProperty(PutSAPHana.DBCP_SERVICE, "dbcp");
        runner.setProperty(PutSAPHana.OPERATION, "UPSERT");
        runner.setProperty(PutSAPHana.TABLE_NAME, String.valueOf("baishanTable"));
        runner.setProperty(PutSAPHana.BATCHSIZE, String.valueOf("2"));
        runner.setProperty(PutSAPHana.KEYCOLUMN, String.valueOf("A"));
        runner.setIncomingConnection(true);
        ProcessContext processContext = runner.getProcessContext();
        PutSAPHana processor = (PutSAPHana) runner.getProcessor();
        processor.updateScheduledTrue();
        ProcessSession session = runner.getProcessSessionFactory().createSession();
        FlowFile flowFile1 = session.create();
        flowFile1 = session.importFrom(new FileInputStream(new File("src/test/resources/avro.txt")), flowFile1);
        runner.enqueue(flowFile1);
        processor.onTrigger(processContext, runner.getProcessSessionFactory());
    }

    /**
     * 检查更新数据是否正确
     * */
    public void checkTableRowCount() throws InitializationException {
        final DBCPService dbcp = new ConnectionFactory();
        final Map<String, String> dbcpProperties = new HashMap<>();
        final TestRunner runner = TestRunners.newTestRunner(GetSAPHana.class);
        runner.addControllerService("dbcp", dbcp, dbcpProperties);
        runner.enableControllerService(dbcp);
        runner.setProperty(GetSAPHana.DBCP_SERVICE, "dbcp");
        runner.setProperty(GetSAPHana.SQL_SELECT_QUERY, String.valueOf("select * from baishanTable"));
        runner.setIncomingConnection(false);
        ProcessContext processContext = runner.getProcessContext();
        GetSAPHana processor = (GetSAPHana) runner.getProcessor();
        processor.updateScheduledTrue();
        processor.onTrigger(processContext, runner.getProcessSessionFactory());
        final List<MockFlowFile> flowFiles = runner.getFlowFilesForRelationship(GetSAPHana.REL_SUCCESS);
        long rowCount = 0;
        for (final MockFlowFile flowFile : flowFiles) {
            rowCount = Long.parseLong(flowFile.getAttribute(GetSAPHana.RESULT_ROW_COUNT));
        }
        Assert.assertEquals(6, rowCount);
    }

    /**
     * 还原数据
     * */
    public void recoverTable() throws InitializationException {
        delete6();
        update5();
        checkRecovery();
    }
    
    /**
     * 检查还原数据
     * */
    private void checkRecovery() throws InitializationException {
        final DBCPService dbcp = new ConnectionFactory();
        final Map<String, String> dbcpProperties = new HashMap<>();
        final TestRunner runner = TestRunners.newTestRunner(GetSAPHana.class);
        runner.addControllerService("dbcp", dbcp, dbcpProperties);
        runner.enableControllerService(dbcp);
        runner.setProperty(GetSAPHana.DBCP_SERVICE, "dbcp");
        runner.setProperty(GetSAPHana.SQL_SELECT_QUERY, String.valueOf("select * from baishanTable"));
        runner.setIncomingConnection(false);
        ProcessContext processContext = runner.getProcessContext();
        GetSAPHana processor = (GetSAPHana) runner.getProcessor();
        processor.updateScheduledTrue();
        processor.onTrigger(processContext, runner.getProcessSessionFactory());
        final List<MockFlowFile> flowFiles = runner.getFlowFilesForRelationship(GetSAPHana.REL_SUCCESS);
        long rowCount = 0;
        for (final MockFlowFile flowFile : flowFiles) {
            rowCount = Long.parseLong(flowFile.getAttribute(GetSAPHana.RESULT_ROW_COUNT));
        }
        Assert.assertEquals(5, rowCount);

    }

    private void update5() throws InitializationException {
        final DBCPService dbcp = new ConnectionFactory();
        final Map<String, String> dbcpProperties = new HashMap<>();
        final TestRunner runner = TestRunners.newTestRunner(GetSAPHana.class);
        runner.addControllerService("dbcp", dbcp, dbcpProperties);
        runner.enableControllerService(dbcp);
        runner.setProperty(GetSAPHana.DBCP_SERVICE, "dbcp");
        runner.setProperty(GetSAPHana.SQL_SELECT_QUERY, String.valueOf("update baishanTable set B='ee' where A=5"));
        runner.setIncomingConnection(false);
        ProcessContext processContext = runner.getProcessContext();
        GetSAPHana processor = (GetSAPHana) runner.getProcessor();
        processor.updateScheduledTrue();
        processor.onTrigger(processContext, runner.getProcessSessionFactory());
    }

    private void delete6() throws InitializationException {
        final DBCPService dbcp = new ConnectionFactory();
        final Map<String, String> dbcpProperties = new HashMap<>();
        final TestRunner runner = TestRunners.newTestRunner(GetSAPHana.class);
        runner.addControllerService("dbcp", dbcp, dbcpProperties);
        runner.enableControllerService(dbcp);
        runner.setProperty(GetSAPHana.DBCP_SERVICE, "dbcp");
        runner.setProperty(GetSAPHana.SQL_SELECT_QUERY, String.valueOf("delete from baishanTable where A=6"));
        runner.setIncomingConnection(false);
        ProcessContext processContext = runner.getProcessContext();
        GetSAPHana processor = (GetSAPHana) runner.getProcessor();
        processor.updateScheduledTrue();
        processor.onTrigger(processContext, runner.getProcessSessionFactory());
    }
}