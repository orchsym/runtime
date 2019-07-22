package com.baishancloud.orchsym.processors;

import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

public class JSONTest {
    private TestRunner testRunner;
    private InputStream jsonInput;
    @Before
    public void init() throws FileNotFoundException {
        testRunner = TestRunners.newTestRunner(FilterField.class);
        jsonInput = JSONTest.class.getResourceAsStream("/Test.json");
    }
    @After
    public void clean(){
        testRunner=null;
        jsonInput=null;
    }
    protected MockFlowFile runForSuccessFlowFile() {
        testRunner.enqueue(jsonInput);
        testRunner.run();

        testRunner.assertAllFlowFilesTransferred(FilterField.REL_SUCCESS, 1);
        final List<MockFlowFile> flowFiles = testRunner.getFlowFilesForRelationship(FilterField.REL_SUCCESS);
        final MockFlowFile successFlowFile = flowFiles.get(0);
        return successFlowFile;
    }
    @Test
    public void testJsonExclude(){
        testRunner.setProperty(FilterField.CONTENT_TYPE,Constant.CONTENT_TYPE_JSON);
        testRunner.setProperty(FilterField.EXCLUDE, "links");
        testRunner.setProperty(FilterField.INCLUDE,"");

        MockFlowFile incompatible = runForSuccessFlowFile();
        incompatible.assertContentEquals("{\"address\":{\"country\":\"中国\",\"city\":\"贵州贵阳\",\"street\":\"贵安新区\"},\"isNonProfit\":true,\"name\":\"baishancloud\",\"url\":\"https://orchsym-studio.baishancloud.com\"}");
    }
    @Test
    public void testJsonInclude(){
        testRunner.setProperty(FilterField.CONTENT_TYPE,Constant.CONTENT_TYPE_JSON);
        testRunner.setProperty(FilterField.EXCLUDE, "");
        testRunner.setProperty(FilterField.INCLUDE,"address");

        MockFlowFile incompatible = runForSuccessFlowFile();
        incompatible.assertContentEquals("{\"address\":{\"country\":\"中国\",\"city\":\"贵州贵阳\",\"street\":\"贵安新区\"}}");
    }
    @Test
    public void testJsonIncludeAndExclude(){
        testRunner.setProperty(FilterField.CONTENT_TYPE,Constant.CONTENT_TYPE_JSON);
        testRunner.setProperty(FilterField.EXCLUDE, "address.street");
        testRunner.setProperty(FilterField.INCLUDE,"address");


        MockFlowFile incompatible = runForSuccessFlowFile();
        incompatible.assertContentEquals("{\"address\":{\"country\":\"中国\",\"city\":\"贵州贵阳\"}}");
    }
    @Test
    public void testJsonNullInput(){
        testRunner.setProperty(FilterField.CONTENT_TYPE,Constant.CONTENT_TYPE_JSON);
        testRunner.setProperty(FilterField.EXCLUDE, "");
        testRunner.setProperty(FilterField.INCLUDE,"");

        MockFlowFile incompatible = runForSuccessFlowFile();
        incompatible.assertContentEquals("{\"address\":{\"country\":\"中国\",\"city\":\"贵州贵阳\",\"street\":\"贵安新区\"},\"isNonProfit\":true,\"name\":\"baishancloud\",\"links\":[{\"name\":\"Google\",\"url\":\"http://www.google.com\"},{\"name\":\"Baidu\",\"url\":\"http://www.baidu.com\"},{\"name\":\"SoSo\",\"url\":\"http://www.SoSo.com\"}],\"url\":\"https://orchsym-studio.baishancloud.com\"}");
    }

}
