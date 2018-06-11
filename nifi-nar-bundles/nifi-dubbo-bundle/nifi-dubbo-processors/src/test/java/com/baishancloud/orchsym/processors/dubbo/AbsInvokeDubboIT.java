package com.baishancloud.orchsym.processors.dubbo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.baishancloud.orchsym.processors.dubbo.InvokeDubbo.PARAM_CHOICE;
import com.baishancloud.orchsym.processors.dubbo.it.PersonService;
import com.baishancloud.orchsym.processors.dubbo.param.CustomParam;

/**
 * 
 * @author GU Guoqiang
 *
 *         NOTE: before start this Integration Test, need start one local Zookeeper and the inner InvokeDubboITProvider first.
 */
public abstract class AbsInvokeDubboIT {

    final static String address = "zookeeper://127.0.0.1:2181";
    final static String user = "user";
    final static String pass = "pass";
    final static String version = "1.2.3";

    TestRunner runner;

    protected abstract String getBaseFolder();

    protected String loadContents(String dataFileName) throws IOException {
        final File dataJsonFile = new File(TestConstants.TEST_RES, getBaseFolder() + "/" + dataFileName + TestConstants.EXT_JSON);
        if (!dataJsonFile.exists()) {
            throw new FileNotFoundException(dataJsonFile.getAbsolutePath());
        }
        String contents = FileUtils.readFileToString(dataJsonFile, StandardCharsets.UTF_8);
        return contents;
    }

    @BeforeClass
    public static void init() throws Exception {
    }

    @AfterClass
    public static void cleanup() {
    }

    @Before
    public void setup() throws IOException {
        runner = TestRunners.newTestRunner(new InvokeDubbo());

        runner.setProperty(InvokeDubbo.ADDRESSES, address);
        runner.setProperty(InvokeDubbo.SERVICE_INTERFACE, PersonService.class.getName());
        runner.setProperty(InvokeDubbo.SERVICE_METHOD, "sayHello"); // default
        runner.setProperty(InvokeDubbo.SERVICE_VERSION, version);
        runner.setProperty(InvokeDubbo.USERNAME, user);
        runner.setProperty(InvokeDubbo.PASSWORD, pass);

        runner.setProperty(InvokeDubbo.PARAMETERS_CHOICE, PARAM_CHOICE.CUSTOM.getName());

        CustomParam param = new CustomParam("java.lang.String", "world");
        runner.setProperty(InvokeDubbo.CUSTOM_PARAMETERS, new CustomParam.Writer().write(param));

        runner.setProperty(InvokeDubbo.TIMEOUT, "1s");
    }

}
