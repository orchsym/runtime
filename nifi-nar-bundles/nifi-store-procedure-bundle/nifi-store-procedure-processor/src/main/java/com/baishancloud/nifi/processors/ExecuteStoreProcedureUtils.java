package com.baishancloud.nifi.processors;

import groovy.lang.Closure;
import groovy.sql.*;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExecuteStoreProcedureUtils extends Sql{

    public ExecuteStoreProcedureUtils(DataSource dataSource) {
        super(dataSource);
    }

    public ExecuteStoreProcedureUtils(Connection connection) {
        super(connection);
    }

    public ExecuteStoreProcedureUtils(Sql parent) {
        super(parent);
    }


   @Override
   protected void setParameters(List<Object> params, PreparedStatement statement) throws SQLException {
        configure(statement);
        super.setParameters(params,statement);

   }



}
