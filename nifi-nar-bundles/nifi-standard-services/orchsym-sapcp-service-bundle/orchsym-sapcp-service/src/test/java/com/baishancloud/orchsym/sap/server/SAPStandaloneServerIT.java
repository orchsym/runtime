package com.baishancloud.orchsym.sap.server;

import java.util.Arrays;
import java.util.Properties;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.baishancloud.orchsym.sap.SAPDataManager;
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
import com.sap.conn.jco.JCoRuntimeException;
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
@Ignore
public class SAPStandaloneServerIT extends AbsSAPServerIT {
    static String identifier;
    JCoServer server;

    @BeforeClass
    public static void init() throws Exception {
        identifier = UUID.randomUUID().toString();

    }

    @Before
    public void before() throws Exception {
        final String serverName = "SAPServer" + System.currentTimeMillis(); // 与standalone无关

        final Properties asPoolProp = getASwithPoolProp();

        SAPDataManager.getInstance().updateClientProp(identifier, serverType, asPoolProp);

        SAPDataManager.getInstance().updateServerProp(identifier, serverName, getServerProp(asPoolProp));

        SAPDataManager.getInstance().getDestination(identifier, serverType); // same as connect to registry

        server = SAPDataManager.getInstance().getServer(identifier, serverName);
    }

    @After
    public void after() {
        // SAPDataManager.getInstance().unregister();
    }

    /**
     * 简单调用, 函数需要先在ABAP中签名（定义函数的元数据信息：输入、输出、表等参数）
     * 
     * don't work to call, can't find the function always.
     */
    @Test
    @Ignore
    public void startSimpleServer() throws JCoException {
        final String funName1 = "ZBSY_RFUN_OP";
        final String funName2 = "STFC_CONNECTION";

        JCoServerFunctionHandler stfcConnectionHandler = new SAPServerFunctionHandler() {

            @Override
            public void handleRequest(JCoServerContext serverCtx, JCoFunction function) throws AbapException {
                super.handleRequest(serverCtx, function);

                final JCoParameterList importParameterList = function.getImportParameterList();
                JCoParameterList exportParameterList = function.getExportParameterList();
                System.out.println("req text: " + importParameterList);
                if (function.getName().equals(funName1)) {
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
                if (function.getName().equals(funName2)) {
                    String reqtext = importParameterList.getString("REQUTEXT"); // 区分大小写
                    System.out.println("REQUTEXT: " + reqtext);

                    exportParameterList.setValue("RESPTEXT", "哈喽SAP，这里是Java服务器.");
                    exportParameterList.setValue("ECHOTEXT", reqtext);
                }

            }

        };
        DefaultServerHandlerFactory.FunctionHandlerFactory factory = new DefaultServerHandlerFactory.FunctionHandlerFactory();
        // 注册远程函数
        factory.registerHandler(funName1, stfcConnectionHandler);
        factory.registerHandler(funName2, stfcConnectionHandler);
        server.setCallHandlerFactory(factory);

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

    /**
     * 无需ABAP进行函数签名（元数据定义），仅通过Java来动态定义函数的元数据信息
     */
    @Test
    public void startEchoServer() throws JCoException {
        // 签名函数区分大小写，需精确匹配
        startServer("ZBSY_RFUN_ECHO"); // ABAP: baishan/ZBSY_STFC_ECHO
    }

    /**
     * 启动另一服务，与Echo不能同时启动，单独启动任意一个都可以运行良好。
     * 
     */
    @Test
    public void startDynamicServer() throws JCoException {
        // 签名函数区分大小写，需精确匹配
        startServer("ZBSY_RFUN_DYNAMIC", "ZBSY_RFUN_DYNAMIC2", "ZBSY_RFUN_DYNAMIC3");// ABAP: zddic/ZBSY_STFC_DYNAMIC
    }

    private void startServer(final String... funNames) throws JCoRuntimeException, JCoException {
        if (funNames == null || funNames.length == 0) {
            return;
        }

        // 创建函数元数据
        JCoCustomRepository repository = JCo.createCustomRepository("MyCustomRepository" + System.currentTimeMillis());
        for (String fun : funNames) {
            repository.addFunctionTemplateToCache(createFunTemplate(fun));
        }
        String repDest = server.getRepositoryDestination();
        if (repDest != null) {
            repository.setDestination(JCoDestinationManager.getDestination(repDest));
        }
        server.setRepository(repository);

        JCoServerFunctionHandler stfcConnectionHandler = new SAPServerFunctionHandler() {

            @Override
            public void handleRequest(JCoServerContext serverCtx, JCoFunction function) throws AbapException {
                super.handleRequest(serverCtx, function);

                System.out.println("++++>>>> Functions :" + Arrays.asList(funNames));

                final JCoParameterList importParameterList = function.getImportParameterList();
                String reqtext = importParameterList.getString("REQTEXT"); // 区分大小写
                System.out.println("REQTEXT: " + reqtext);

                JCoParameterList exportParameterList = function.getExportParameterList();
                exportParameterList.setValue("RESTEXT", "哈喽SAP，这里是Java服务器.");
                exportParameterList.setValue("ECHOTEXT", reqtext);
            }

        };
        DefaultServerHandlerFactory.FunctionHandlerFactory factory = new DefaultServerHandlerFactory.FunctionHandlerFactory();
        // 注册函数
        for (String fun : funNames) {
            factory.registerHandler(fun, stfcConnectionHandler);// 跟函数名相同，可重用函数handler
        }
        server.setCallHandlerFactory(factory);

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

        try {
            Thread.sleep(60 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        server.stop();
        server.release();
        repository.clear();
    }

    private JCoFunctionTemplate createFunTemplate(String funName) {
        JCoListMetaData importMeta = JCo.createListMetaData("IMPORT");
        importMeta.add("REQTEXT", JCoMetaData.TYPE_CHAR, 50, 100, 0, null, null, JCoListMetaData.IMPORT_PARAMETER, null, null);
        importMeta.lock();

        JCoListMetaData exportMeta = JCo.createListMetaData("EXPORT");
        exportMeta.add("RESTEXT", JCoMetaData.TYPE_CHAR, 50, 100, 0, null, null, JCoListMetaData.EXPORT_PARAMETER, null, null);
        exportMeta.add("ECHOTEXT", JCoMetaData.TYPE_CHAR, 50, 100, 0, null, null, JCoListMetaData.EXPORT_PARAMETER, null, null);
        exportMeta.lock();

        JCoFunctionTemplate fT = JCo.createFunctionTemplate(funName, importMeta, exportMeta, null, null, null);
        return fT;
    }

}
