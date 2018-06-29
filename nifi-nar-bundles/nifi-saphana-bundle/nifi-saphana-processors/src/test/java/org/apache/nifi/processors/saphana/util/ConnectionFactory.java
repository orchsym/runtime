package org.apache.nifi.processors.saphana.util;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.dbcp.DBCPService;
import org.apache.nifi.processor.exception.ProcessException;

public class ConnectionFactory extends AbstractControllerService implements DBCPService {

    @Override
    public String getIdentifier() {
        return "dbcp";
    }

    @Override
    public Connection getConnection() throws ProcessException {
        final String host = "172.18.28.246";
        final Integer port = 39017;
        final String username = "SYSTEM";
        final String password = "Baishancloud3";
        java.sql.Connection connection = null;
        try {
            java.lang.Class.forName("com.sap.db.jdbc.Driver");
            String url = "jdbc:sap://" + host + ":" + port;
            connection = java.sql.DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return connection;
    }

}
