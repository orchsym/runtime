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
package com.orchsym.processor.jsonxml;

import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author Lu JB
 *
 */

public class ConvertXMLToJSONTest {
    private TestRunner testRunner;
    private ConvertXMLToJSON processor;

    @Before
    public void setup() {
        processor = new ConvertXMLToJSON();
        testRunner = TestRunners.newTestRunner(processor);
    }

    private final String xmlString = readFileToString("src/test/resources/employees2.xml");

    @Test
    public void test_Invalid_Xml() {
        testRunner.setValidateExpressionUsage(false);
        testRunner.setProperty(ConvertXMLToJSON.DESTINATION, ConvertXMLToJSON.DESTINATION_CONTENT);

        testRunner.enqueue("<item>123<".getBytes(StandardCharsets.UTF_8));
        testRunner.run();

        testRunner.assertAllFlowFilesTransferred(ConvertJSONToXML.REL_FAILURE, 1);
        System.out.println(testRunner.getLogger().getErrorMessages());
    }

    @Test
    public void testXml_To_Json_With_XPath_Expression() throws IOException {
        testRunner.setValidateExpressionUsage(false);
        testRunner.setProperty(ConvertXMLToJSON.DESTINATION, ConvertXMLToJSON.DESTINATION_CONTENT);
        testRunner.setProperty(ConvertXMLToJSON.XML_PATH_EXPRESSION, "/Employees/Employee[1]");
        testRunner.setProperty(ConvertXMLToJSON.XML_CONTENT_KEY_NAME, "content");

        testRunner.enqueue(xmlString.getBytes(StandardCharsets.UTF_8));
        testRunner.run();

        testRunner.assertAllFlowFilesTransferred(ConvertXMLToJSON.REL_SUCCESS, 1);
        final List<MockFlowFile> result = testRunner.getFlowFilesForRelationship(ConvertJSONToXML.REL_SUCCESS);
        result.get(0).assertContentEquals("{\"Employee\":{\"role\":{\"@department\":\"IT\",\"@extra\":\"manager\",\"content\":\"Java Developer\"},\"gender\":\"Male\",\"additional\":{\"petname\":\"Doggy\",\"petage\":2},\"name\":\"jiangbin\",\"@id\":1,\"device\":[\"computer\",\"ipad\"],\"age\":30}}".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testXml_To_Json_With_XPath_Expression_Age() throws IOException {
        testRunner.setValidateExpressionUsage(false);
        testRunner.setProperty(ConvertXMLToJSON.DESTINATION, ConvertXMLToJSON.DESTINATION_CONTENT);
        testRunner.setProperty(ConvertXMLToJSON.XML_PATH_EXPRESSION, "/Employees/Employee[2]/age");
        testRunner.setProperty(ConvertXMLToJSON.XML_CONTENT_KEY_NAME, "content");

        testRunner.enqueue(xmlString.getBytes(StandardCharsets.UTF_8));
        testRunner.run();

        testRunner.assertAllFlowFilesTransferred(ConvertXMLToJSON.REL_SUCCESS, 1);
        final List<MockFlowFile> result = testRunner.getFlowFilesForRelationship(ConvertJSONToXML.REL_SUCCESS);
        result.get(0).assertContentEquals("{\"age\":35}".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testXml_To_Json_With_XPath_Expression_Not_Found() throws IOException {
        testRunner.setValidateExpressionUsage(false);
        testRunner.setProperty(ConvertXMLToJSON.DESTINATION, ConvertXMLToJSON.DESTINATION_CONTENT);
        testRunner.setProperty(ConvertXMLToJSON.XML_PATH_EXPRESSION, "/Employees/Employee[11]/age");
        testRunner.setProperty(ConvertXMLToJSON.XML_CONTENT_KEY_NAME, "content");

        testRunner.enqueue(xmlString.getBytes(StandardCharsets.UTF_8));
        testRunner.run();

        testRunner.assertAllFlowFilesTransferred(ConvertJSONToXML.REL_SUCCESS, 1);
        final List<MockFlowFile> result = testRunner.getFlowFilesForRelationship(ConvertJSONToXML.REL_SUCCESS);
        result.get(0).assertContentEquals("{}".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testXml_To_Json_With_Invalid_XPath_Expression() {
        testRunner.setValidateExpressionUsage(false);
        testRunner.setProperty(ConvertXMLToJSON.DESTINATION, ConvertXMLToJSON.DESTINATION_CONTENT);
        testRunner.setProperty(ConvertXMLToJSON.XML_PATH_EXPRESSION, "###/Em");
        testRunner.setProperty(ConvertXMLToJSON.XML_CONTENT_KEY_NAME, "content");

        testRunner.enqueue(xmlString.getBytes(StandardCharsets.UTF_8));
        testRunner.run();

        testRunner.assertAllFlowFilesTransferred(ConvertJSONToXML.REL_FAILURE, 1);
        System.out.println(testRunner.getLogger().getErrorMessages());
    }

    @Test
    public void testXml_To_Json_With_Mark_KeyName() throws IOException {
        testRunner.setValidateExpressionUsage(false);
        testRunner.setProperty(ConvertXMLToJSON.DESTINATION, ConvertXMLToJSON.DESTINATION_CONTENT);
        testRunner.setProperty(ConvertXMLToJSON.XML_PATH_EXPRESSION, "/Employees/Employee");
        testRunner.setProperty(ConvertXMLToJSON.XML_CONTENT_KEY_NAME, "newKeyName");
        testRunner.setProperty(ConvertXMLToJSON.XML_ATTRIBUTE_MARK, "@");

        testRunner.enqueue("<Employees><Employee id=\"3\">lu</Employee></Employees>".getBytes(StandardCharsets.UTF_8));
        testRunner.run();

        testRunner.assertAllFlowFilesTransferred(ConvertJSONToXML.REL_SUCCESS, 1);
        final List<MockFlowFile> result = testRunner.getFlowFilesForRelationship(ConvertJSONToXML.REL_SUCCESS);
        result.get(0).assertContentEquals("{\"Employee\":{\"@id\":3,\"newKeyName\":\"lu\"}}".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testXml_To_Json_With_Default_KeyName() throws IOException {
        testRunner.setValidateExpressionUsage(false);
        testRunner.setProperty(ConvertXMLToJSON.DESTINATION, ConvertXMLToJSON.DESTINATION_CONTENT);
        testRunner.setProperty(ConvertXMLToJSON.XML_PATH_EXPRESSION, "/Employees/Employee");
        testRunner.setProperty(ConvertXMLToJSON.XML_ATTRIBUTE_MARK, "@");

        testRunner.enqueue("<Employees><Employee id=\"3\">lu</Employee></Employees>".getBytes(StandardCharsets.UTF_8));
        testRunner.run();

        testRunner.assertAllFlowFilesTransferred(ConvertJSONToXML.REL_SUCCESS, 1);
        final List<MockFlowFile> result = testRunner.getFlowFilesForRelationship(ConvertJSONToXML.REL_SUCCESS);
        result.get(0).assertContentEquals("{\"Employee\":{\"@id\":3,\"content\":\"lu\"}}".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testXml_To_Json_With_Default_KeyName_2() throws IOException {
        testRunner.setValidateExpressionUsage(false);
        testRunner.setProperty(ConvertXMLToJSON.DESTINATION, ConvertXMLToJSON.DESTINATION_CONTENT);
        testRunner.setProperty(ConvertXMLToJSON.XML_ATTRIBUTE_MARK, "@");

        testRunner.enqueue("<Employees><Employee id=\"3\">lu</Employee></Employees>".getBytes(StandardCharsets.UTF_8));
        testRunner.run();

        testRunner.assertAllFlowFilesTransferred(ConvertJSONToXML.REL_SUCCESS, 1);
        final List<MockFlowFile> result = testRunner.getFlowFilesForRelationship(ConvertJSONToXML.REL_SUCCESS);
        result.get(0).assertContentEquals("{\"Employees\":{\"Employee\":{\"@id\":3,\"content\":\"lu\"}}}".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testXml_To_Json_With_Default_KeyName_NO_Mark() throws IOException {
        testRunner.setValidateExpressionUsage(false);
        testRunner.setProperty(ConvertXMLToJSON.DESTINATION, ConvertXMLToJSON.DESTINATION_CONTENT);
        testRunner.setProperty(ConvertXMLToJSON.XML_PATH_EXPRESSION, "Employees/Employee");

        testRunner.enqueue("<Employees><Employee id=\"3\">lu</Employee></Employees>".getBytes(StandardCharsets.UTF_8));
        testRunner.run();

        testRunner.assertAllFlowFilesTransferred(ConvertJSONToXML.REL_SUCCESS, 1);
        final List<MockFlowFile> result = testRunner.getFlowFilesForRelationship(ConvertJSONToXML.REL_SUCCESS);
        result.get(0).assertContentEquals("{\"Employee\":{\"@id\":3,\"content\":\"lu\"}}".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testXml_To_Json_With_Default_KeyName2_NO_Mark() throws IOException {
        testRunner.setValidateExpressionUsage(false);
        testRunner.setProperty(ConvertXMLToJSON.DESTINATION, ConvertXMLToJSON.DESTINATION_CONTENT);

        testRunner.enqueue("<Employees><Employee id=\"3\">lu</Employee></Employees>".getBytes(StandardCharsets.UTF_8));
        testRunner.run();

        testRunner.assertAllFlowFilesTransferred(ConvertJSONToXML.REL_SUCCESS, 1);
        final List<MockFlowFile> result = testRunner.getFlowFilesForRelationship(ConvertJSONToXML.REL_SUCCESS);
        result.get(0).assertContentEquals("{\"Employees\":{\"Employee\":{\"@id\":3,\"content\":\"lu\"}}}".getBytes(StandardCharsets.UTF_8));
    }


    private String readFileToString(String file){
        try(FileInputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr)
        ) {
            StringBuilder sb = new StringBuilder();
            String tempStr = null;
            while ((tempStr = br.readLine()) != null){
                sb.append(tempStr);
            }
            return sb.toString();
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }
}
