package org.apache.nifi.processors.saphana;

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
public class PutSAPHanaMultipleRowsIT {
    
    private final String TABLE_NAME = "baishanTest";
    private final String SOURCE_INPUT_FILE = "C:/runtime/saphana/baishanTestAVRO.txt";
    private final int SOURCE_INPUT_FILE_ROW_COUNT = 10;


    @Test
    public void testOnTriggerProcessContextProcessSession() throws InitializationException, FileNotFoundException {
        createTable();
        long begintime = System.currentTimeMillis();
        updateTable();
        long endtime = System.currentTimeMillis();
        System.out.println((endtime-begintime)/1000);
        checkTableRowCount();
        dropTable();
    }
    
    private void createTable()  throws InitializationException{
        // TODO Auto-generated method stub
        String sql = "create table "+TABLE_NAME+"(id integer,name varchar(20),birthday datetime)";
        executeSQL(sql);
    }
    
    private void dropTable()  throws InitializationException{
        // TODO Auto-generated method stub
        String sql = "drop table "+TABLE_NAME;
        executeSQL(sql);
    }

    private void executeSQL(String sql) throws InitializationException {
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
        runner.setProperty(PutSAPHana.TABLE_NAME, String.valueOf(TABLE_NAME));
        runner.setProperty(PutSAPHana.BATCHSIZE, String.valueOf("10"));
        runner.setProperty(PutSAPHana.KEYCOLUMN, String.valueOf("id"));
        runner.setIncomingConnection(true);
        ProcessContext processContext = runner.getProcessContext();
        PutSAPHana processor = (PutSAPHana) runner.getProcessor();
        processor.updateScheduledTrue();
        ProcessSession session = runner.getProcessSessionFactory().createSession();
        FlowFile flowFile1 = session.create();
        flowFile1 = session.importFrom(new FileInputStream(new File(SOURCE_INPUT_FILE)), flowFile1);
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
        runner.setProperty(GetSAPHana.SQL_SELECT_QUERY, String.valueOf("select * from "+TABLE_NAME));
        runner.setIncomingConnection(false);
        ProcessContext processContext = runner.getProcessContext();
        GetSAPHana processor = (GetSAPHana) runner.getProcessor();
        processor.updateScheduledTrue();
        processor.onTrigger(processContext, runner.getProcessSessionFactory());
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
        Assert.assertEquals(SOURCE_INPUT_FILE_ROW_COUNT, rowCount);
    }

}