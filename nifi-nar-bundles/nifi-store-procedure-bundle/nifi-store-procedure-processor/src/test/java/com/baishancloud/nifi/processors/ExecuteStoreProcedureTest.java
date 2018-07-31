package com.baishancloud.nifi.processors;

import com.fasterxml.jackson.databind.ObjectMapper;
import groovy.lang.Closure;
import groovy.sql.GroovyRowResult;
import groovy.sql.Sql;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExecuteStoreProcedureTest {

    final static String MYSQL_DB_LOCATION = "//localhost:3306/new_database";
    final static String MSSQL_DB_LOCATION = "//127.0.0.1:1433";

    @org.junit.Test
    public void testGroovyCallProcedure() throws Exception {
        List<Object> paramList = new ArrayList<>();
        String sqlQuery = "";
        Integer out = 0;

        //test call storeProcedure with neither in and out
        sqlQuery = "{call addUser()}";
        testGroovyCallFunctions(null, sqlQuery);


        //test call storeProcedure with in
        paramList.add("three");
        paramList.add("girl");
        paramList.add(Sql.INTEGER);
        paramList.add(Sql.INTEGER);
        sqlQuery = "{call addUserByNameAndDesAndManyOuts(?,?,?,?)}";
        testGroovyCallFunctions(paramList, sqlQuery);

        //test call storeProcedure with in and out
        paramList.add("three");
        paramList.add("girl");
        paramList.add(Sql.INTEGER);
        sqlQuery = "{call addUserByNameAndDesAndOut(?,?,?)}";
        testGroovyCallFunctions(paramList, sqlQuery);

        //test call storeProcedure with inout params
        paramList.add("three");
        paramList.add(Sql.inout(Sql.in(Sql.VARCHAR.getType(), "girl")));
        sqlQuery = "{call testInOut(?,?)}";
        testGroovyCallFunctions(paramList, sqlQuery);
    }

    private void testGroovyCallFunctions(List<Object> paramList, String sqlQuery) throws Exception {
        Sql sql = Sql.newInstance("jdbc:mysql://localhost:3306/new_database",
                "root", "1", "com.mysql.jdbc.Driver");

        List<List<GroovyRowResult>> lists = sql.callWithAllRows(sqlQuery, paramList, new Closure(this) {
                    @Override
                    public Object call(Object... args) {
                        Map map1 = new HashMap();
                        for (int i = 0; i < args.length; i++) {
                            System.out.println(args[0]);
                            map1.put("sql.args." + i, args[i]);
                        }
                        return map1;
                    }
                }
        );
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println(objectMapper.writeValueAsString(lists));
        sql.close();
    }

    @org.junit.Test
    public void testParams() throws Exception {
        Pattern SQL_TYPE_ATTRIBUTE_PATTERN = Pattern.compile("sql\\.args\\.(\\d+)\\.type");
        Matcher matcher = SQL_TYPE_ATTRIBUTE_PATTERN.matcher("sql.args.2.type");
        if (matcher.matches()) {
            int parameterIndex = Integer.parseInt(matcher.group(1));
            System.out.println(parameterIndex);

        }
    }


}
