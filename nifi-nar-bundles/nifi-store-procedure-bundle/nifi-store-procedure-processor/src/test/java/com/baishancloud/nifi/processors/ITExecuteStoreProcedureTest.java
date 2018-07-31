package com.baishancloud.nifi.processors;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.dbcp.DBCPService;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Integration test for execute store_procedure processor. Please set the dbcpService,MethodName,Params,fetchSize,etc before running the integrations.
 */
@Ignore("Comment this out for store procedure integration testing .")
public class ITExecuteStoreProcedureTest {
    private String driverClassName = "";
    private String url = "";
    private String user = "";
    private String password = "";

    @Before
    public void setUp() throws IOException {
        //indicates which database you are using
        driverClassName = "com.mysql.cj.jdbc.Driver";
        url = "jdbc:mysql://localhost:3306/new_database";
        user = "root";
        password = "1";
    }

    @Test
    public void testExecuteStoreProcedure() throws Exception {
        //test your storeProcedure with type of single in or many ins or even both ins an outs,etc.
        Map<String, String> map = new HashMap<String, String>();
        FlowFile flowFile = new MockFlowFile(1);
        ((MockFlowFile) flowFile).putAttributes(map);
        // Generate a test runner to mock a processor in a flow
        TestRunner runner = TestRunners.newTestRunner(new ExecuteStoreProcedure());
        GetDBCPServiceSimpleImpl dbcp = new GetDBCPServiceSimpleImpl();
        dbcp.setProperties(driverClassName, url, user, password);
        final Map<String, String> dbcpProperties = new HashMap<>();
        runner.addControllerService("dbcp", dbcp, dbcpProperties);
        runner.enableControllerService(dbcp);
        runner.setProperty(ExecuteStoreProcedure.DBCP_SERVICE, "dbcp");
        runner.setProperty(ExecuteStoreProcedure.METHOD_NAME, "addUserByNameAndDes");
        runner.setProperty(ExecuteStoreProcedure.PARAMS, "[{'param.name':'param1','param.type':'IN','param.value':'one','param.value.type':29},{'param.name':'param2','param.type':'IN','param.value':'girl','param.value.type':29}]");
        runner.setProperty(ExecuteStoreProcedure.QUERY_TIMEOUT, "1000 millis");
        runner.setProperty(ExecuteStoreProcedure.FETCH_SIZE, "1000");

        // Add the content to the runner
        runner.enqueue(flowFile);
        runner.enqueue();

        // Run the enqueued content, it also takes an int = number of contents queued
        runner.run(1, true, true);


        // All results were processed with out failure
        runner.assertQueueEmpty();

        // If you need to read or do additional tests on results you can access the content
        List<MockFlowFile> results = runner.getFlowFilesForRelationship(ExecuteStoreProcedure.REL_SUCCESS);
        //assertTrue("1 match", results.size() == 1);
        if (results.size() > 0) {
            MockFlowFile result = results.get(0);
            String resultValue = new String(runner.getContentAsByteArray(result));
        }


    }

    class GetDBCPServiceSimpleImpl extends AbstractControllerService implements DBCPService {
        private String driverClassName = "";
        private String url = "";
        private String user = "";
        private String password = "";

        public void setProperties(String driverClassName, String url, String user, String password) {
            this.driverClassName = driverClassName;
            this.url = url;
            this.user = user;
            this.password = password;
        }

        @Override
        public String getIdentifier() {
            return "dbcp";
        }

        @Override
        public Connection getConnection() throws ProcessException {
            try {
                Class.forName(driverClassName);
                final Connection con = DriverManager.getConnection(url, user, password);
                return con;
            } catch (final Exception e) {
                throw new ProcessException("getConnection failed: " + e);
            }
        }
    }
}
