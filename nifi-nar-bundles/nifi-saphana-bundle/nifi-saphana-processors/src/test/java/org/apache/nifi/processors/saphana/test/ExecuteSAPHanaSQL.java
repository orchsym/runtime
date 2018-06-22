package org.apache.nifi.processors.saphana.test;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.nifi.dbcp.DBCPService;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processors.saphana.GetSAPHana;
import org.apache.nifi.processors.saphana.util.ConnectionFactory;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;

public class ExecuteSAPHanaSQL {
    
    protected Logger logger = Logger.getLogger(getClass().getName());
    
    protected TestRunner executeSQL(String sql) throws InitializationException {
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
}
