package com.baishancloud.orchsym.processors.dubbo;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import org.apache.nifi.util.MockFlowFile;
import org.junit.Assert;
import org.junit.Test;

import com.baishancloud.orchsym.processors.dubbo.param.CustomParam;

/**
 * 
 * @author GU Guoqiang
 *
 *         NOTE: before start this Integration Test, need start one local Zookeeper and the inner InvokeDubboITProvider first.
 */
public class InvokeDubbo4WrongSettingsIT extends AbsInvokeDubboIT {
    @Override
    protected String getBaseFolder() {
        return "";
    }

    /**
     * When init the consumer in OnScheduled, will throw the IllegalStateException for Dubbo, but when use mock to test, will throw AssertionError
     */
    @Test(expected = AssertionError.class)
    public void test_wrong_address() throws IOException {
        runner.setProperty(InvokeDubbo.ADDRESSES, "abc"); // reset wrong value

        runner.run();
    }

    /**
     * When init the consumer in OnScheduled, will throw the IllegalStateException for Dubbo, but when use mock to test, will throw AssertionError
     */
    @Test(expected = AssertionError.class)
    public void test_wrong_interface() throws IOException {
        runner.setProperty(InvokeDubbo.SERVICE_INTERFACE, Serializable.class.getName());

        runner.run();
    }

    /**
     * When init the consumer in OnScheduled, will throw the IllegalStateException for Dubbo, but when use mock to test, will throw AssertionError
     */
    @Test(expected = AssertionError.class)
    public void test_empty_version() throws IOException {
        runner.setProperty(InvokeDubbo.SERVICE_VERSION, "");

        runner.run();
    }

    /**
     * When init the consumer in OnScheduled, will throw the IllegalStateException for Dubbo, but when use mock to test, will throw AssertionError
     */
    @Test(expected = AssertionError.class)
    public void test_wrong_version() throws IOException {
        runner.setProperty(InvokeDubbo.SERVICE_VERSION, "3.2.1");

        runner.run();
    }

    @Test
    public void test_all_version() throws IOException {
        // because set verson in the provider.xml. so can work for *
        runner.setProperty(InvokeDubbo.SERVICE_VERSION, "*");

        runner.run();

        runner.assertTransferCount(InvokeDubbo.REL_FAILURE, 0);
        runner.assertTransferCount(InvokeDubbo.REL_SUCCESS, 1);
        final List<MockFlowFile> successRelation = runner.getFlowFilesForRelationship(InvokeDubbo.REL_SUCCESS);
        Assert.assertEquals(1, successRelation.size());
        successRelation.get(0).assertContentEquals("{\"response\":\"Hello world\"}\n");
    }

    @Test
    public void test_empty_user() throws IOException {
        // even empty user, can read data still ???
        runner.setProperty(InvokeDubbo.USERNAME, "");

        runner.run();

        runner.assertTransferCount(InvokeDubbo.REL_FAILURE, 0);
        runner.assertTransferCount(InvokeDubbo.REL_SUCCESS, 1);
    }

    @Test
    public void test_wrong_user() throws IOException {
        // even wrong user, can read data still ???
        runner.setProperty(InvokeDubbo.USERNAME, "xyz");

        runner.run();

        runner.assertTransferCount(InvokeDubbo.REL_FAILURE, 0);
        runner.assertTransferCount(InvokeDubbo.REL_SUCCESS, 1);
    }

    @Test
    public void test_wrong_pass() throws IOException {
        // even wrong password, can read data still ???
        runner.setProperty(InvokeDubbo.PASSWORD, "123");
        runner.run();

        runner.assertTransferCount(InvokeDubbo.REL_FAILURE, 0);
        runner.assertTransferCount(InvokeDubbo.REL_SUCCESS, 1);
    }

    /**
     * Because wrong method, should have com.alibaba.dubbo.remoting.RemotingException. but use mock to test.
     */
    @Test(expected = AssertionError.class)
    public void test_wrong_method() throws IOException {
        runner.setProperty(InvokeDubbo.SERVICE_METHOD, "sayHi");

        runner.run();
    }

    /**
     * Because wrong method, should have com.alibaba.dubbo.remoting.RemotingException. but use mock to test.
     */
    @Test(expected = AssertionError.class)
    public void test_no_custom() throws IOException {
        runner.setProperty(InvokeDubbo.CUSTOM_PARAMETERS, "");

        runner.run();
    }

    /**
     * Because wrong method, should have com.alibaba.dubbo.remoting.RemotingException. but use mock to test.
     */
    @Test(expected = AssertionError.class)
    public void test_diff_type() throws IOException {
        CustomParam param = new CustomParam("long", "1");
        runner.setProperty(InvokeDubbo.CUSTOM_PARAMETERS, new CustomParam.Writer().write(param));

        runner.run();
    }
}
