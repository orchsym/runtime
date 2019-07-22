package com.baishancloud.orchsym.processors;

import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class AvroTest {
    private TestRunner testRunner;
    private InputStream avroInput;
    @Before
    public void init() throws FileNotFoundException {
        testRunner = TestRunners.newTestRunner(FilterField.class);
        avroInput = JSONTest.class.getResourceAsStream("/Test.avro");
    }
    @After
    public void clean(){
        testRunner=null;
        avroInput=null;
    }
    protected MockFlowFile runForSuccessFlowFile() {
        testRunner.enqueue(avroInput);
        testRunner.run();

        testRunner.assertAllFlowFilesTransferred(FilterField.REL_SUCCESS, 1);
        final List<MockFlowFile> flowFiles = testRunner.getFlowFilesForRelationship(FilterField.REL_SUCCESS);
        final MockFlowFile successFlowFile = flowFiles.get(0);
        return successFlowFile;
    }
    @Test
    public void testAvroExclude() throws IOException {
        testRunner.setProperty(FilterField.CONTENT_TYPE,Constant.CONTENT_TYPE_AVRO);
        testRunner.setProperty(FilterField.EXCLUDE, "links");
        testRunner.setProperty(FilterField.INCLUDE,"");
        
        MockFlowFile incompatible = runForSuccessFlowFile();
        byte[] avro =  incompatible.toByteArray();
        String json = Utils.avroToJson(avro);//输出的avro转json
        assert json.equals("{\"address\":{\"country\":\"中国\",\"city\":\"贵州贵阳\",\"street\":\"贵安新区\"},\"isNonProfit\":true,\"name\":\"baishancloud\",\"url\":\"https://orchsym-studio.baishancloud.com\"}");
    }
    @Test
    public void testAvroInclude() throws IOException {
        testRunner.setProperty(FilterField.CONTENT_TYPE,Constant.CONTENT_TYPE_AVRO);
        testRunner.setProperty(FilterField.EXCLUDE, "");
        testRunner.setProperty(FilterField.INCLUDE,"address");

        MockFlowFile incompatible = runForSuccessFlowFile();
        byte[] avro =  incompatible.toByteArray();
        String json = Utils.avroToJson(avro);//输出的avro转json
        assert json.equals("{\"address\":{\"country\":\"中国\",\"city\":\"贵州贵阳\",\"street\":\"贵安新区\"}}");
    }
    @Test
    public void testAvroIncludeAndExclude() throws IOException {
        testRunner.setProperty(FilterField.CONTENT_TYPE,Constant.CONTENT_TYPE_AVRO);
        testRunner.setProperty(FilterField.EXCLUDE, "address.street");
        testRunner.setProperty(FilterField.INCLUDE,"address");

        MockFlowFile incompatible = runForSuccessFlowFile();
        byte[] avro =  incompatible.toByteArray();
        String json = Utils.avroToJson(avro);//输出的avro转json
        assert json.equals("{\"address\":{\"country\":\"中国\",\"city\":\"贵州贵阳\"}}");
    }
    @Test
    public void testAvroNullInput() throws IOException {
        testRunner.setProperty(FilterField.CONTENT_TYPE,Constant.CONTENT_TYPE_AVRO);
        testRunner.setProperty(FilterField.EXCLUDE, "");
        testRunner.setProperty(FilterField.INCLUDE,"");

        MockFlowFile incompatible = runForSuccessFlowFile();
        byte[] avro =  incompatible.toByteArray();
        String json = Utils.avroToJson(avro);//输出的avro转json
        assert json.equals("{\"address\":{\"country\":\"中国\",\"city\":\"贵州贵阳\",\"street\":\"贵安新区\"},\"isNonProfit\":true,\"name\":\"baishancloud\",\"links\":[{\"name\":\"Google\",\"url\":\"http://www.google.com\"},{\"name\":\"Baidu\",\"url\":\"http://www.baidu.com\"},{\"name\":\"SoSo\",\"url\":\"http://www.SoSo.com\"}],\"url\":\"https://orchsym-studio.baishancloud.com\"}");
    }
}
