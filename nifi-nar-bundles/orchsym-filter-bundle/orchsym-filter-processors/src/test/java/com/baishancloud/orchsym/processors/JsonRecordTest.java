package com.baishancloud.orchsym.processors;

import org.apache.commons.io.IOUtils;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class JsonRecordTest {
    private TestRunner testRunner;
    private InputStream jsonInput;
    private String jsonSQL;
    @Before
    public void init() throws IOException {
        testRunner = TestRunners.newTestRunner(FilterRecord.class);
        jsonInput = IOUtils.toInputStream(TestRunners.loadContents("/recordTest.json"));
        jsonSQL = TestRunners.loadContents("/SQL.txt");;
    }
    @After
    public void clean(){
        testRunner=null;
        jsonInput=null;
    }
    protected MockFlowFile runForSuccessFlowFile() {
        testRunner.enqueue(jsonInput);
        testRunner.run();
        //断言REL_FILTER连线上有数据
        testRunner.assertTransferCount(FilterRecord.REL_FILTER, 1);
        final List<MockFlowFile> flowFiles = testRunner.getFlowFilesForRelationship(FilterRecord.REL_FILTER);
        final MockFlowFile successFlowFile = flowFiles.get(0);
        return successFlowFile;
    }
    @Test
    public void testJson_AND(){
        testRunner.setProperty(FilterRecord.CONTENT_TYPE, Constant.CONTENT_TYPE_JSON);
        testRunner.setProperty(FilterRecord.LOGICAL_TYPE, Constant.LOGICAL_TYPE_AND);
        testRunner.setProperty(FilterRecord.SETTINGS,jsonSQL);

        MockFlowFile incompatible = runForSuccessFlowFile();
        incompatible.assertContentEquals("[{\"isChina\":true,\"city\":\"GZ\",\"name\":\"张三\",\"age\":\"22\"}]");

        testRunner.assertTransferCount(FilterRecord.REL_REJECT, 1);
    }
    @Test
    public void testJson_OR(){
        testRunner.setProperty(FilterRecord.CONTENT_TYPE, Constant.CONTENT_TYPE_JSON);
        testRunner.setProperty(FilterRecord.LOGICAL_TYPE, Constant.LOGICAL_TYPE_OR);
        testRunner.setProperty(FilterRecord.SETTINGS,jsonSQL);

        MockFlowFile incompatible = runForSuccessFlowFile();
        incompatible.assertContentEquals("[{\"isChina\":true,\"city\":\"GZ\",\"name\":\"张三\",\"age\":\"22\"},{\"isChina\":true,\"city\":\"BJ\",\"name\":\"李四\",\"age\":\"23\"},{\"isChina\":false,\"city\":\"New York\",\"name\":\"Linda\",\"age\":\"20\"},{\"isChina\":false,\"city\":\"London\",\"name\":\"Tom\",\"age\":\"22\"}]");
        testRunner.assertTransferCount(FilterRecord.REL_REJECT, 0);
    }
    @Test
    public void testJson_NOT(){
        testRunner.setProperty(FilterRecord.CONTENT_TYPE, Constant.CONTENT_TYPE_JSON);
        testRunner.setProperty(FilterRecord.LOGICAL_TYPE, Constant.LOGICAL_TYPE_NOT);
        testRunner.setProperty(FilterRecord.SETTINGS,jsonSQL);

        MockFlowFile incompatible = runForSuccessFlowFile();
        incompatible.assertContentEquals("[{\"isChina\":true,\"city\":\"GZ\",\"name\":\"张三\",\"age\":\"22\"},{\"isChina\":true,\"city\":\"BJ\",\"name\":\"李四\",\"age\":\"23\"}]");
        testRunner.assertTransferCount(FilterRecord.REL_REJECT, 1);
    }
}
