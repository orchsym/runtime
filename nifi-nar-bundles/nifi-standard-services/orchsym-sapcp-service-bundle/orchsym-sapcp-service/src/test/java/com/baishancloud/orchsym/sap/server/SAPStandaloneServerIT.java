package com.baishancloud.orchsym.sap.server;

import java.time.LocalTime;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.baishancloud.orchsym.sap.SAPDataManager;
import com.baishancloud.orchsym.sap.metadata.param.ESAPParamType;
import com.sap.conn.jco.AbapException;
import com.sap.conn.jco.JCo;
import com.sap.conn.jco.JCoCustomRepository;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoFunctionTemplate;
import com.sap.conn.jco.JCoListMetaData;
import com.sap.conn.jco.JCoMetaData;
import com.sap.conn.jco.JCoParameterList;
import com.sap.conn.jco.JCoTable;
import com.sap.conn.jco.server.DefaultServerHandlerFactory;
import com.sap.conn.jco.server.JCoServer;
import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerFunctionHandler;
import com.sap.conn.jco.server.JCoServerTIDHandler;

/**
 * @author GU Guoqiang
 *
 *         不能启动多个服务，单独启动任意一个服务都能运行良好。 standalone貌似与DefaultServerManager.computeGroupKey有关，一个程序标识，只能启动一个服务，而与serverName无关
 */
// @Ignore
public class SAPStandaloneServerIT extends AbsSAPServerIT {
    JCoServer server;

    @Before
    public void before() throws Exception {

        final Properties asPoolProp = getASwithPoolProp();

        SAPDataManager.getInstance().updateClientProp(identifier, serverType, asPoolProp);

        SAPDataManager.getInstance().updateServerProp(identifier, SAPServerConnectionPool.SERVER_NAME, getServerProp(asPoolProp));

        SAPDataManager.getInstance().getDestination(identifier, serverType); // same as connect to registry

        server = SAPDataManager.getInstance().getServer(identifier, SAPServerConnectionPool.SERVER_NAME);
    }

    @After
    public void after() {
        try {
            Thread.sleep(60 * 60 * 1000); // 1 hour
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (server != null) {
            server.stop();
            server.release();
        }
        // SAPDataManager.getInstance().unregister();
    }

    /**
     * 函数已经先在ABAP中签名预定义了，可定义函数的元数据信息：输入、输出、表等参数， 但没有函数的源代码实现
     * 
     */
    @Test
    @Ignore
    public void startOpFunServer_PreDefine() throws JCoException {
        /*
         * ABAP: baishan/ZBSY_STFC_OP_PD
         */
        final String funName = "ZBSY_RFUN_OP_PD"; // ABAP中预先定义了该函数
        startOpFunServer(funName);

        /*
         * 测试结果： 需要等待40s才反应，然后ABAP程序调用失败，并报错；服务器端能获得相应预先定义的参数信息并读取值，并设置输出值，且无任何错误。
         */

        // ABAP端报错：
        // Call function error SY-SUBRC = 1
        // CPIC-CALL: 'ThSAPCMRCV': cmRc=20 thRc=223#CPIC 程序连接被终止(读取错误)

        // Server端可被函数Handler正确读取输入参数，并可写入输出参数，并无报错，log中可以看到日志：
        // [JCoAPI] JCoServer after dispatch (ZBSY_RFUN_OP_PD) on handle [4]

        /*
         * 结论： 由于仅ABAP里报错，也许是元数据信息或数据信息对应不上？
         */
    }

    /**
     * 函数没有在ABAP中定义，也没有在该Server中动态定义。
     * 
     */
    @Test
    @Ignore
    public void startOpFunServer_NoDefine() throws JCoException {
        /*
         * ABAP: baishan/ZBSY_STFC_OP_ND
         */
        final String funName = "ZBSY_RFUN_OP_ND"; // ABAP中没有相应函数定义
        startOpFunServer(funName);

        /*
         * 测试结果： 需要等待40s才反应，均报未找到函数定义的错误。
         */

        // ABAP端报错：
        // Call function error SY-SUBRC = 1
        // 'ZBSY_RFUN_OP_ND' could not be found in the server repository.

        // Server端也可以看到报错日志，没有找到函数：
        // [JCoRFC] dispatchRequest caught an exception: 'ZBSY_RFUN_OP_ND' could not be found in the server repository.

        /*
         * 结论： 函数必须定义元数据信息（输入、输出、表参数）
         */

    }

    /**
     * 在服务器端通过JCo API动态定义了相同的函数元数据。
     * 
     */
    @Test
    @Ignore
    public void startOpFunServer_DynamicServerDefine() throws JCoException {
        /*
         * ABAP: baishan/ZBSY_STFC_OP_DSD
         */
        final String funName = "ZBSY_RFUN_OP_DSD"; // 纯由Server端通过JCo动态定义函数

        startOpDynamicFunServer(funName);

        /*
         * 测试结果： ABAP程序几乎瞬间反应，ABAP端始终错误，但服务器端无任何错误。
         */

        // ABAP端报错：
        // 如果没有设置事务handler，报如下错误：
        // Call function error SY-SUBRC = 2
        // Error when opening an RFC connection (CPIC-CALL: 'ThSAPOCMINIT' : cmRc=2 thRc=679#事务程序未注册)
        // 如果设置事务handler后，报与预先定义函数一样的错误（貌似类型不匹配导致）：
        // Call function error SY-SUBRC = 1
        // CPIC-CALL: 'ThSAPCMRCV': cmRc=20 thRc=223#CPIC 程序连接被终止(读取错误)

        // Server端无任何错误：

        /*
         * 结论：事务必须设置？？
         */

    }

    /**
     * 函数已经先在ABAP中签名预定义了，可定义函数的元数据信息：输入、输出、表等参数， 但没有函数的源代码实现
     * 
     * 同时在服务器端通过JCo API动态定义了相同的函数元数据。
     * 
     */
    @Test
    @Ignore
    public void startOpFunServer_BothDefine() throws JCoException {
        /*
         * ABAP: baishan/ZBSY_STFC_OP_PD
         */
        final String funName = "ZBSY_RFUN_OP_PD"; // 重用ABAP中预先定义了该函数

        startOpDynamicFunServer(funName);

        /*
         * 测试结果：同ABAP预先定义结果一样。
         */

    }

    /**
     * ABAP预先定义好函数，但未实现函数源代码
     */
    @Test
    // @Ignore
    public void startCharOpFunServer_PreDefine() throws JCoException {
        /*
         * ABAP: baishan/ZBSY_STFC_CHAR_OP_PD
         */
        final String funName = "ZBSY_RFUN_CHAR_OP_PD";

        JCoServerFunctionHandler stfcConnectionHandler = new SAPServerFunctionHandler() {

            @Override
            public void handleRequest(JCoServerContext serverCtx, JCoFunction function) throws AbapException {
                // super.handleRequest(serverCtx, function);

                final JCoParameterList importParameterList = function.getImportParameterList();
                final JCoParameterList exportParameterList = function.getExportParameterList();
                if (function.getName().equals(funName)) {
                    try {
                        int p1 = importParameterList.getInt("P1");
                        int p2 = importParameterList.getInt("P2");
                        String op = importParameterList.getString("OPERATOR");
                        int result = 0;
                        if (op.equals("+")) {
                            result = p1 + p2;
                        } else if (op.equals("-")) {
                            result = p1 - p2;
                        } else if (op.equals("*")) {
                            result = p1 * p2;
                        } else if (op.equals("/")) {
                            result = p1 / p2;
                        } else {
                            throw new IllegalArgumentException("Wrong operator:" + op);
                        }
                        exportParameterList.setValue("RESULT", result);
                    } catch (Exception e) {
                        exportParameterList.setValue("RESULT", 0);
                        exportParameterList.setValue("MSG", e.getMessage());

                    }
                }

            }

        };

        startServer(funName, stfcConnectionHandler);

        /*
         * 测试结果：ABAP返回类型为INT4匹配，运行成功
         */

    }

    /**
     * 通过JCo动态创建函数，类型必须匹配
     */
    @Test
    @Ignore
    public void startCharOpFunServer_DynamicServerDefine() throws JCoException {
        /*
         * ABAP: baishan/ZBSY_STFC_CHAR_OP_DSD
         */
        final String funName = "ZBSY_RFUN_CHAR_OP_DSD";

        JCoListMetaData importMeta = JCo.createListMetaData("IMPORT");
        importMeta.add("P1", JCoMetaData.TYPE_INT, 0, 0, 0, null, null, ESAPParamType.IMPORT.getJcoType(), null, null);
        importMeta.add("P2", JCoMetaData.TYPE_INT, 0, 0, 0, null, null, ESAPParamType.IMPORT.getJcoType(), null, null);
        importMeta.add("OPERATOR", JCoMetaData.TYPE_CHAR, 1, 1, 0, null, null, ESAPParamType.IMPORT.getJcoType(), null, null);
        importMeta.lock();

        JCoListMetaData exportMeta = JCo.createListMetaData("EXPORT");
        exportMeta.add("RESULT", JCoMetaData.TYPE_INT2, 10, 10, 0, null, null, ESAPParamType.EXPORT.getJcoType(), null, null);
        exportMeta.add("MSG", JCoMetaData.TYPE_CHAR, 100, 100, 0, null, null, ESAPParamType.EXPORT.getJcoType(), null, null);
        exportMeta.lock();

        JCoFunctionTemplate fT = JCo.createFunctionTemplate(funName, importMeta, exportMeta, null, null, null);

        JCoCustomRepository repository = JCo.createCustomRepository("JUnitCharOpRepository" + System.currentTimeMillis());
        repository.addFunctionTemplateToCache(fT);
        String repDest = server.getRepositoryDestination();
        if (repDest != null) {
            repository.setDestination(JCoDestinationManager.getDestination(repDest));
        }
        server.setRepository(repository);

        JCoServerFunctionHandler stfcConnectionHandler = new SAPServerFunctionHandler() {

            @Override
            public void handleRequest(JCoServerContext serverCtx, JCoFunction function) throws AbapException {
                // super.handleRequest(serverCtx, function);

                final JCoParameterList importParameterList = function.getImportParameterList();
                final JCoParameterList exportParameterList = function.getExportParameterList();
                if (function.getName().equals(funName)) {
                    try {
                        int p1 = importParameterList.getInt("P1");
                        int p2 = importParameterList.getInt("P2");
                        String op = importParameterList.getString("OPERATOR");
                        int result = 0;
                        if (op.equals("+")) {
                            result = p1 + p2;
                        } else if (op.equals("-")) {
                            result = p1 - p2;
                        } else if (op.equals("*")) {
                            result = p1 * p2;
                        } else if (op.equals("/")) {
                            result = p1 / p2;
                        } else {
                            throw new IllegalArgumentException("Wrong operator:" + op);
                        }
                        exportParameterList.setValue("RESULT", String.valueOf(result));
                    } catch (Exception e) {
                        exportParameterList.setValue("RESULT", String.valueOf(0));
                        exportParameterList.setValue("MSG", e.getMessage());

                    }
                }

            }

        };

        startServer(funName, stfcConnectionHandler);

        /*
         * 测试结果：如果输入输出类型匹配，则可运行成功，否则报错
         */

    }

    private void startOpFunServer(final String funName) {
        JCoServerFunctionHandler stfcConnectionHandler = new SAPServerFunctionHandler() {

            @Override
            public void handleRequest(JCoServerContext serverCtx, JCoFunction function) throws AbapException {
                super.handleRequest(serverCtx, function);

                final JCoParameterList importParameterList = function.getImportParameterList();
                final JCoParameterList exportParameterList = function.getExportParameterList();
                if (function.getName().equals(funName)) {
                    try {
                        int p1 = importParameterList.getInt("P1");
                        int p2 = importParameterList.getInt("P2");
                        String op = importParameterList.getString("OPERATOR");
                        int result = 0;
                        if (op.equals("+")) {
                            result = p1 + p2;
                        } else if (op.equals("-")) {
                            result = p1 - p2;
                        } else if (op.equals("*")) {
                            result = p1 * p2;
                        } else if (op.equals("/")) {
                            result = p1 / p2;
                        } else {
                            throw new IllegalArgumentException("Wrong operator:" + op);
                        }
                        exportParameterList.setValue("RESULT", String.valueOf(result));
                    } catch (Exception e) {
                        exportParameterList.setValue("RESULT", String.valueOf(0));
                        exportParameterList.setValue("MSG", e.getMessage());

                    }
                }

            }

        };

        startServer(funName, stfcConnectionHandler);
    }

    private void startOpDynamicFunServer(final String funName) throws JCoException {
        JCoListMetaData importMeta = JCo.createListMetaData("IMPORT");
        importMeta.add("P1", JCoMetaData.TYPE_INT, 0, 0, 0, null, null, ESAPParamType.IMPORT.getJcoType(), null, null);
        importMeta.add("P2", JCoMetaData.TYPE_INT, 0, 0, 0, null, null, ESAPParamType.IMPORT.getJcoType(), null, null);
        importMeta.add("OPERATOR", JCoMetaData.TYPE_STRING, 1, 1, 0, null, null, ESAPParamType.IMPORT.getJcoType(), null, null);
        importMeta.lock();

        JCoListMetaData exportMeta = JCo.createListMetaData("EXPORT");
        exportMeta.add("RESULT", JCoMetaData.TYPE_INT2, 0, 0, 0, null, null, ESAPParamType.EXPORT.getJcoType(), null, null);
        exportMeta.add("MSG", JCoMetaData.TYPE_STRING, 100, 100, 0, null, null, ESAPParamType.EXPORT.getJcoType(), null, null);
        exportMeta.lock();

        JCoFunctionTemplate fT = JCo.createFunctionTemplate(funName, importMeta, exportMeta, null, null, null);

        JCoCustomRepository repository = JCo.createCustomRepository("JUnitOpRepository" + System.currentTimeMillis());
        repository.addFunctionTemplateToCache(fT);
        String repDest = server.getRepositoryDestination();
        if (repDest != null) {
            repository.setDestination(JCoDestinationManager.getDestination(repDest));
        }
        server.setRepository(repository);

        startOpFunServer(funName);

    }

    /**
     * 预先在ABAP中已定义了函数
     */
    @Test
    @Ignore
    public void startEchoFunServer_PreDefine() throws JCoException {
        //
        /*
         * ABAP: baishan/ZBSY_STFC_ECHO_PD
         */
        final String funName = "ZBSY_RFUN_ECHO_PD";
        JCoServerFunctionHandler stfcConnectionHandler = new SAPServerFunctionHandler() {

            @Override
            public void handleRequest(JCoServerContext serverCtx, JCoFunction function) throws AbapException {
                // super.handleRequest(serverCtx, function);

                final JCoParameterList importParameterList = function.getImportParameterList();
                String reqtext = importParameterList.getString("REQTEXT"); // 区分大小写
                System.out.println("REQTEXT: " + reqtext);

                JCoParameterList exportParameterList = function.getExportParameterList();
                exportParameterList.setValue("RESTEXT", "SAP,SAP,这里是数据蜂巢.收到请回答。" + LocalTime.now());
                exportParameterList.setValue("ECHOTEXT", reqtext);
            }

        };
        startServer(funName, stfcConnectionHandler);

        /*
         * 测试结果：暂未进行测试
         */
    }

    /**
     * 无需预先在ABAP中进行函数签名（元数据定义），仅通过该Server，通过Java来动态定义函数的元数据信息。
     * 
     * Server端的动态函数定义应该都大写，因为签名函数在Java中区分大小写，但ABAP是不区分大小写。
     */
    @Test
    @Ignore
    public void startEchoFunServer_DynamicServerDefine() throws JCoException {
        //
        /*
         * ABAP: baishan/ZBSY_STFC_ECHO_DSD
         */
        final String funName = "ZBSY_RFUN_ECHO_DSD";// 由服务器端JCo API动态生成函数定义
        JCoServerFunctionHandler stfcConnectionHandler = new SAPServerFunctionHandler() {

            @Override
            public void handleRequest(JCoServerContext serverCtx, JCoFunction function) throws AbapException {
                // super.handleRequest(serverCtx, function);

                final JCoParameterList importParameterList = function.getImportParameterList();
                String reqtext = importParameterList.getString("REQTEXT"); // 区分大小写
                System.out.println("REQTEXT: " + reqtext);

                JCoParameterList exportParameterList = function.getExportParameterList();
                exportParameterList.setValue("RESTEXT", "SAP,SAP,这里是数据蜂巢.收到请回答。" + LocalTime.now());
                exportParameterList.setValue("ECHOTEXT", reqtext);
            }

        };
        startEchoServer(createEchoFunTemplate(funName), stfcConnectionHandler);

        /*
         * 测试结果：成功执行获得结果。
         */
    }

    /**
     * 预先在ABAP中定义了带表的函数
     * 
     * 不知道如何定义表
     */
    @Test
    @Ignore
    public void startEchoTableServer_PreDefine() throws JCoException {
        /*
         * ABAP: baishan/ZBSY_STFC_ECHO_TAB_PD
         */
        final String funName = "ZBSY_RFUN_ECHO_TAB_PD";
        JCoServerFunctionHandler stfcConnectionHandler = new SAPServerFunctionHandler() {

            @Override
            public void handleRequest(JCoServerContext serverCtx, JCoFunction function) throws AbapException {
                // super.handleRequest(serverCtx, function);

                final JCoParameterList importParameterList = function.getImportParameterList();
                String reqtext = importParameterList.getString("REQTEXT"); // 区分大小写
                System.out.println("REQTEXT: " + reqtext);

                final JCoParameterList tableParameterList = function.getTableParameterList();
                if (tableParameterList != null) {
                    System.out.println("TABLE: " + tableParameterList);
                    final JCoTable itTable = tableParameterList.getTable("IT_FIELDS");
                    final JCoTable etTable = tableParameterList.getTable("ET_FIELDS");
                    if (itTable != null) {
                        for (int i = 0; i < itTable.getNumRows(); i++) {
                            itTable.setRow(i);
                            etTable.appendRow();

                            String fieldName = itTable.getString("FIELDNAME");
                            String fieldText = itTable.getString("FIELDTEXT");

                            System.out.println(fieldName + " | " + fieldText);

                            etTable.setValue("FIELDNAME", itTable.getString("FIELDNAME"));
                            etTable.setValue("OFFSET", itTable.getInt("OFFSET"));
                            etTable.setValue("LENGTH", itTable.getInt("LENGTH"));// 兼容String
                            // etTable.setValue("LENGTH", itTable.getString("LENGTH"));
                            etTable.setValue("TYPE", itTable.getChar("TYPE")); // 兼容String
                            // etTable.setValue("TYPE", itTable.getString("TYPE"));
                            etTable.setValue("FIELDTEXT", itTable.getString("FIELDTEXT"));
                        }
                    }

                    System.out.println("--------------------");
                }
                final JCoParameterList changingParameterList = function.getChangingParameterList();
                if (changingParameterList != null) {
                    System.out.println("Changing: " + changingParameterList);
                }

                JCoParameterList exportParameterList = function.getExportParameterList();
                exportParameterList.setValue("RESTEXT", "SAP,SAP,这里是数据蜂巢.收到请回答");
                exportParameterList.setValue("ECHOTEXT", reqtext);
            }

        };
        startServer(funName, stfcConnectionHandler);
    }

    /**
     * 无需预先在ABAP中进行函数签名（元数据定义），仅通过该Server，通过Java来动态定义函数的元数据信息。
     * 
     * Server端的动态函数定义应该都大写，因为签名函数在Java中区分大小写，但ABAP是不区分大小写。
     * 
     * 带表的输入参数测试
     */
    @Test
    @Ignore
    public void startEchoTableServer_DynamicServerDefine() throws JCoException {
        /*
         * ABAP: baishan/ZBSY_STFC_ECHO_TAB_DSD
         */
        final String funName = "ZBSY_RFUN_ECHO_TAB_DSD"; // 由服务器端JCo API动态生成带表输入的函数定义
        JCoServerFunctionHandler stfcConnectionHandler = new SAPServerFunctionHandler() {

            @Override
            public void handleRequest(JCoServerContext serverCtx, JCoFunction function) throws AbapException {
                // super.handleRequest(serverCtx, function);

                final JCoParameterList importParameterList = function.getImportParameterList();
                String reqtext = importParameterList.getString("REQTEXT"); // 区分大小写
                System.out.println("REQTEXT: " + reqtext);

                final JCoParameterList tableParameterList = function.getTableParameterList();
                if (tableParameterList != null) {
                    System.out.println("TABLE: " + tableParameterList);

                    System.out.println("--------------------");
                }

                JCoParameterList exportParameterList = function.getExportParameterList();
                exportParameterList.setValue("RESTEXT", "SAP,SAP,这里是数据蜂巢.收到请回答");
                exportParameterList.setValue("ECHOTEXT", reqtext);
            }

        };
        startEchoServer(createEchoTableFunTemplate(funName), stfcConnectionHandler);
    }

    private void startEchoServer(JCoFunctionTemplate funTemplate, JCoServerFunctionHandler stfcConnectionHandler) throws JCoException {
        JCoCustomRepository repository = JCo.createCustomRepository("JUnitEchoRepository" + System.currentTimeMillis());
        repository.addFunctionTemplateToCache(funTemplate);
        String repDest = server.getRepositoryDestination();
        if (repDest != null) {
            repository.setDestination(JCoDestinationManager.getDestination(repDest));
        }
        server.setRepository(repository);

        startServer(funTemplate.getName(), stfcConnectionHandler);
    }

    /**
     * 无需ABAP进行函数签名（元数据定义），仅通过Java来动态定义函数的元数据信息
     */
    @Test
    @Ignore
    public void startMultiEchoFunsServer_DynamicServerDefine() throws JCoException {
        // 签名函数区分大小写，需精确匹配
        /*
         * ABAP: zddic/ZBSY_STFC_M_ECHO_DSD
         */
        final JCoFunctionTemplate funTemplate1 = createEchoFunTemplate("ZBSY_RMFUN_ECHO_DSD1");
        final JCoFunctionTemplate funTemplate2 = createEchoFunTemplate("ZBSY_RMFUN_ECHO_DSD2");
        final JCoFunctionTemplate funTemplate3 = createEchoFunTemplate("ZBSY_RMFUN_ECHO_DSD3");

        JCoCustomRepository repository = JCo.createCustomRepository("JunitMultiRepository" + System.currentTimeMillis());
        repository.addFunctionTemplateToCache(funTemplate1);
        repository.addFunctionTemplateToCache(funTemplate2);
        repository.addFunctionTemplateToCache(funTemplate3);
        String repDest = server.getRepositoryDestination();
        if (repDest != null) {
            repository.setDestination(JCoDestinationManager.getDestination(repDest));
        }
        server.setRepository(repository);

        JCoServerFunctionHandler stfcConnectionHandler = new SAPServerFunctionHandler() {

            @Override
            public void handleRequest(JCoServerContext serverCtx, JCoFunction function) throws AbapException {
                // super.handleRequest(serverCtx, function);

                final JCoParameterList importParameterList = function.getImportParameterList();
                String reqtext = importParameterList.getString("REQTEXT"); // 区分大小写
                System.out.println("REQTEXT: " + reqtext);

                JCoParameterList exportParameterList = function.getExportParameterList();
                exportParameterList.setValue("RESTEXT", "SAP,SAP,这里是数据蜂巢.收到请回答");
                exportParameterList.setValue("ECHOTEXT", reqtext);
            }

        };

        DefaultServerHandlerFactory.FunctionHandlerFactory factory = new DefaultServerHandlerFactory.FunctionHandlerFactory();
        // 注册函数
        factory.registerHandler(funTemplate1.getName(), stfcConnectionHandler);// 跟函数名相同，可重用函数handler
        factory.registerHandler(funTemplate2.getName(), stfcConnectionHandler);// 跟函数名相同，可重用函数handler
        factory.registerHandler(funTemplate3.getName(), stfcConnectionHandler);// 跟函数名相同，可重用函数handler

        startServer(factory);
    }

    private void startServer(String funName, JCoServerFunctionHandler handler) {
        DefaultServerHandlerFactory.FunctionHandlerFactory factory = new DefaultServerHandlerFactory.FunctionHandlerFactory();
        // 注册远程函数
        factory.registerHandler(funName, handler);

        startServer(factory);
    }

    private void startServer(DefaultServerHandlerFactory.FunctionHandlerFactory callHandler) {
        // 注册远程函数
        server.setCallHandlerFactory(callHandler);

        SAPServerAdapter listener = new SAPServerAdapter();
        // 错误监听
        server.addServerErrorListener(listener);
        server.addServerExceptionListener(listener);
        // 状态监听
        server.addServerStateChangedListener(listener);

        // 支持事务，Transaction
        JCoServerTIDHandler tidHandler = new SAPServerTIDHandler();
        server.setTIDHandler(tidHandler);

        server.start();
    }

    private JCoFunctionTemplate createEchoFunTemplate(String funName) {
        /*
         * 对应SAP测试源代码：ZBSY_STFC_ECHO
         */
        JCoListMetaData importMeta = JCo.createListMetaData("IMPORT");
        importMeta.add("REQTEXT", JCoMetaData.TYPE_CHAR, 50, 100, 0, null, null, ESAPParamType.IMPORT.getJcoType(), null, null);
        importMeta.lock();

        JCoListMetaData exportMeta = JCo.createListMetaData("EXPORT");
        exportMeta.add("RESTEXT", JCoMetaData.TYPE_CHAR, 50, 100, 0, null, null, ESAPParamType.EXPORT.getJcoType(), null, null);
        exportMeta.add("ECHOTEXT", JCoMetaData.TYPE_CHAR, 50, 100, 0, null, null, ESAPParamType.EXPORT.getJcoType(), null, null);
        exportMeta.add("MSG", JCoMetaData.TYPE_CHAR, 50, 100, 0, null, null, ESAPParamType.EXPORT.getJcoType(), null, null);
        exportMeta.lock();

        JCoFunctionTemplate fT = JCo.createFunctionTemplate(funName, importMeta, exportMeta, null, null, null);

        return fT;
    }

    private JCoFunctionTemplate createEchoTableFunTemplate(String funName) {
        /*
         * 对应SAP测试源代码：ZBSY_STFC_ECHO_T
         */
        JCoListMetaData importMeta = JCo.createListMetaData("IMPORT");
        importMeta.add("REQTEXT", JCoMetaData.TYPE_CHAR, 50, 100, 0, null, null, ESAPParamType.IMPORT.getJcoType(), null, null);
        importMeta.lock();

        JCoListMetaData exportMeta = JCo.createListMetaData("EXPORT");
        exportMeta.add("RESTEXT", JCoMetaData.TYPE_CHAR, 50, 100, 0, null, null, ESAPParamType.EXPORT.getJcoType(), null, null);
        exportMeta.add("ECHOTEXT", JCoMetaData.TYPE_CHAR, 50, 100, 0, null, null, ESAPParamType.EXPORT.getJcoType(), null, null);
        exportMeta.lock();

        JCoListMetaData tableMeta = JCo.createListMetaData("TABLE");
        tableMeta.add("IT_TAB", JCoMetaData.TYPE_TABLE, 1000, 500, 0, null, null, ESAPParamType.TABLE.getJcoType(), "I_TAB", null);
        tableMeta.lock();

        JCoFunctionTemplate fT = JCo.createFunctionTemplate(funName, importMeta, exportMeta, null, tableMeta, null);
        return fT;
    }
}
