/*
 * Licensed to the Orchsym Runtime under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 * 
 * this file to You under the Orchsym License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * https://github.com/orchsym/runtime/blob/master/orchsym/LICENSE
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baishancloud.orchsym.processors;

import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.SQLException;
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
        incompatible.assertContentEquals("{\"name\":\"baishancloud\",\"url\":\"https://orchsym-studio.baishancloud.com\",\"isNonProfit\":true,\"address\":{\"street\":\"贵安新区\",\"city\":\"贵州贵阳\",\"country\":\"中国\"}}");
    }
    @Test
    public void testJsonInclude(){
        testRunner.setProperty(FilterField.CONTENT_TYPE,Constant.CONTENT_TYPE_JSON);
        testRunner.setProperty(FilterField.EXCLUDE, "");
        testRunner.setProperty(FilterField.INCLUDE,"address");

        MockFlowFile incompatible = runForSuccessFlowFile();
        incompatible.assertContentEquals("{\"address\":{\"street\":\"贵安新区\",\"city\":\"贵州贵阳\",\"country\":\"中国\"}}");
    }
    @Test
    public void testJsonIncludeAndExclude(){
        testRunner.setProperty(FilterField.CONTENT_TYPE,Constant.CONTENT_TYPE_JSON);
        testRunner.setProperty(FilterField.EXCLUDE, "address.street");
        testRunner.setProperty(FilterField.INCLUDE,"address");


        MockFlowFile incompatible = runForSuccessFlowFile();
        incompatible.assertContentEquals("{\"address\":{\"city\":\"贵州贵阳\",\"country\":\"中国\"}}");
    }
    @Test
    public void testJsonNullInput(){
        testRunner.setProperty(FilterField.CONTENT_TYPE,Constant.CONTENT_TYPE_JSON);
        testRunner.setProperty(FilterField.EXCLUDE, "");
        testRunner.setProperty(FilterField.INCLUDE,"");

        MockFlowFile incompatible = runForSuccessFlowFile();
        incompatible.assertContentEquals("{\"name\":\"baishancloud\",\"url\":\"https://orchsym-studio.baishancloud.com\",\"isNonProfit\":true,\"address\":{\"street\":\"贵安新区\",\"city\":\"贵州贵阳\",\"country\":\"中国\"},\"links\":[{\"name\":\"Google\",\"url\":\"http://www.google.com\"},{\"name\":\"Baidu\",\"url\":\"http://www.baidu.com\"},{\"name\":\"SoSo\",\"url\":\"http://www.SoSo.com\"}]}");
    }
    @Test
    public void lzw() throws SQLException, ClassNotFoundException {
        String json = "[\n" +
                "  {\n" +
                "    \"name\":\"张三\",\n" +
                "    \"age\":\"22\",\n" +
                "    \"city\":\"GZ\",\n" +
                "    \"isChina\":true\n" +
                "  },\n" +
                "  {\n" +
                "    \"name\":\"李四\",\n" +
                "    \"age\":\"23\",\n" +
                "    \"city\":\"BJ\",\n" +
                "    \"isChina\":true\n" +
                "  },\n" +
                "  {\n" +
                "    \"name\":\"Linda\",\n" +
                "    \"age\":\"20\",\n" +
                "    \"city\":\"New York\",\n" +
                "    \"isChina\":false\n" +
                "  },\n" +
                "  {\n" +
                "    \"name\":\"Tom\",\n" +
                "    \"age\":\"22\",\n" +
                "    \"city\":\"London\",\n" +
                "    \"isChina\":false\n" +
                "  }\n" +
                "]";
        String jsonArray = "[\n" +
                "{\n" +
                "\"field\": \"name\",\n" +
                "\"function\": \"\",\n" +
                "\"opertator\": \"=\",\n" +
                "\"value\": \"张三\"\n" +
                "},\n" +
                "{\n" +
                "\"field\": \"age\",\n" +
                "\"function\": \"\",\n" +
                "\"opertator\": \"<\",\n" +
                "\"value\": \"23\"\n" +
                "}\n" +
                "]";
        System.out.println(Utils.JsonSQL(json,SQLUtils.generateInSQL(jsonArray,"OR")));
    }
}
