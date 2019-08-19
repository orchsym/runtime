/*
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Lu JB
 * @author weiwei.zhan
 *
 */

public class ConvertJSONToXMLTest {
    private TestRunner testRunner;
    private ConvertJSONToXML processor;

    @Before
    public void setup() {
        processor = new ConvertJSONToXML();
        testRunner = TestRunners.newTestRunner(processor);
    }

    @Test
    public void testJson_To_XML_Attributes() throws IOException {
        testRunner.setValidateExpressionUsage(false);
        testRunner.setProperty(ConvertJSONToXML.DESTINATION, ConvertJSONToXML.DESTINATION_CONTENT);
        testRunner.setProperty(ConvertJSONToXML.ENCODING, "UTF-8");
        testRunner.setProperty(ConvertJSONToXML.JSON_ATTRIBUTE_MARK, "@");
        testRunner.setProperty(ConvertJSONToXML.JSON_CONTENT_KEY_NAME, "content");

        testRunner.enqueue("{\"user\":{\"name\":\"lu\",\"book\":{\"@id\":\"123\",\"@price\":\"17$\",\"content\":\"English\"}}}".getBytes(StandardCharsets.UTF_8));
        testRunner.run();
        testRunner.assertAllFlowFilesTransferred(ConvertJSONToXML.REL_SUCCESS, 1);
        final List<MockFlowFile> result = testRunner.getFlowFilesForRelationship(ConvertJSONToXML.REL_SUCCESS);
        final String expectStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><user><book id=\"123\" price=\"17$\">English</book><name>lu</name></user>";
        result.get(0).assertContentEquals(expectStr.getBytes(StandardCharsets.UTF_8));
    }


    @Test
    public void testJson_To_XML_Invalid() {
        testRunner.setValidateExpressionUsage(false);
        testRunner.setProperty(ConvertJSONToXML.DESTINATION, ConvertJSONToXML.DESTINATION_CONTENT);
        testRunner.setProperty(ConvertJSONToXML.ENCODING, "UTF-8");

        testRunner.enqueue("{\"user\":{\"name\":\"lu\",\"book\":}".getBytes(StandardCharsets.UTF_8));
        testRunner.run();
        testRunner.assertAllFlowFilesTransferred(ConvertJSONToXML.REL_FAILURE, 1);
    }

    @Test
    public void testJson_To_XML_NameSpace() throws IOException{
        testRunner.setValidateExpressionUsage(false);
        testRunner.setProperty(ConvertJSONToXML.DESTINATION, ConvertJSONToXML.DESTINATION_CONTENT);
        testRunner.setProperty(ConvertJSONToXML.ENCODING, "UTF-8");
        testRunner.setProperty(ConvertJSONToXML.NAMESPACE, "testSpace");

        testRunner.enqueue("{\"user\":{\"name\":\"lu\",\"age\":30}}".getBytes(StandardCharsets.UTF_8));
        testRunner.run();
        testRunner.assertAllFlowFilesTransferred(ConvertJSONToXML.REL_SUCCESS, 1);
        final List<MockFlowFile> result = testRunner.getFlowFilesForRelationship(ConvertJSONToXML.REL_SUCCESS);
        final String expectStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><user xmlns=\"testSpace\"><name>lu</name><age>30</age></user>";
        result.get(0).assertContentEquals(expectStr.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testJson_To_XML_Complex() throws IOException{
        testRunner.setValidateExpressionUsage(false);
        testRunner.setProperty(ConvertJSONToXML.DESTINATION, ConvertJSONToXML.DESTINATION_CONTENT);
        testRunner.setProperty(ConvertJSONToXML.ENCODING, "UTF-8");
        testRunner.setProperty(ConvertJSONToXML.JSON_ATTRIBUTE_MARK, "@");
        testRunner.setProperty(ConvertJSONToXML.JSON_CONTENT_KEY_NAME, "content");

        testRunner.enqueue("{\"glossary\":{\"title\":\"example glossary\",\"GlossDiv\":{\"title\":\"S\",\"GlossList\":{\"GlossEntry\":{\"ID\":\"SGML\",\"SortAs\":\"SGML\",\"GlossTerm\":\"Standard Generalized Markup Language\",\"Acronym\":\"SGML\",\"Abbrev\":\"ISO 8879:1986\",\"GlossDef\":{\"para\":\"A meta-markup language, used to create markup languages such as DocBook.\",\"GlossSeeAlso\":[\"GML\",\"XML\"]},\"GlossSee\":\"markup\",\"info\":{\"@name\":\"Jon\",\"@age\":12,\"content\":\"this is content\"}}}}}}".getBytes(StandardCharsets.UTF_8));
        testRunner.run();
        testRunner.assertAllFlowFilesTransferred(ConvertJSONToXML.REL_SUCCESS, 1);
        final List<MockFlowFile> result = testRunner.getFlowFilesForRelationship(ConvertJSONToXML.REL_SUCCESS);
        final String expectStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><glossary><title>example glossary</title><GlossDiv><GlossList><GlossEntry><GlossTerm>Standard Generalized Markup Language</GlossTerm><GlossSee>markup</GlossSee><SortAs>SGML</SortAs><GlossDef><para>A meta-markup language, used to create markup languages such as DocBook.</para><GlossSeeAlso>GML</GlossSeeAlso><GlossSeeAlso>XML</GlossSeeAlso></GlossDef><ID>SGML</ID><Acronym>SGML</Acronym><Abbrev>ISO 8879:1986</Abbrev><info age=\"12\" name=\"Jon\">this is content</info></GlossEntry></GlossList><title>S</title></GlossDiv></glossary>";
        result.get(0).assertContentEquals(expectStr.getBytes(StandardCharsets.UTF_8));
    }

    private static final String jsonString = "{\"store\":{\"book\":[{\"category\":\"reference\",\"author\":\"Nigel Rees\",\"title\":\"Sayings of the Century\",\"price\":8.95},{\"category\":\"fiction\",\"author\":\"Evelyn Waugh\",\"title\":\"Sword of Honour\",\"price\":12.99}],\"bicycle\":{\"color\":\"red\",\"price\":19.95,\"info\":[\"info1\",\"info2\"]}}}";

    @Test
    public void testJson_To_XML_JsonPath() throws IOException {
        testRunner.setValidateExpressionUsage(false);
        testRunner.setProperty(ConvertJSONToXML.DESTINATION, ConvertJSONToXML.DESTINATION_CONTENT);
        testRunner.setProperty(ConvertJSONToXML.ENCODING, "UTF-8");
        testRunner.setProperty(ConvertJSONToXML.JSON_PATH_EXPRESSION, "$.store.book[1]");
        testRunner.setProperty(ConvertJSONToXML.ELEMENT_NAME, "Book");

        testRunner.enqueue(jsonString.getBytes(StandardCharsets.UTF_8));
        testRunner.run();
        testRunner.assertAllFlowFilesTransferred(ConvertJSONToXML.REL_SUCCESS, 1);
        final List<MockFlowFile> result = testRunner.getFlowFilesForRelationship(ConvertJSONToXML.REL_SUCCESS);
        final String expectStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Book><category>fiction</category><title>Sword of Honour</title><author>Evelyn Waugh</author><price>12.99</price></Book>";
        result.get(0).assertContentEquals(expectStr.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testJson_To_XML_JsonPath_Array() throws IOException {
        testRunner.setValidateExpressionUsage(false);
        testRunner.setProperty(ConvertJSONToXML.DESTINATION, ConvertJSONToXML.DESTINATION_CONTENT);
        testRunner.setProperty(ConvertJSONToXML.ENCODING, "UTF-8");
        testRunner.setProperty(ConvertJSONToXML.JSON_PATH_EXPRESSION, "$.store.book[*].author");
        testRunner.setProperty(ConvertJSONToXML.ELEMENT_NAME, "Author");

        testRunner.enqueue(jsonString.getBytes(StandardCharsets.UTF_8));
        testRunner.run();
        testRunner.assertAllFlowFilesTransferred(ConvertJSONToXML.REL_SUCCESS, 1);
        final List<MockFlowFile> result = testRunner.getFlowFilesForRelationship(ConvertJSONToXML.REL_SUCCESS);
        final String expectStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><list><Author>Nigel Rees</Author><Author>Evelyn Waugh</Author></list>";
        result.get(0).assertContentEquals(expectStr.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testJson_To_XML_JsonPath_Array_2() throws IOException {
        testRunner.setValidateExpressionUsage(false);
        testRunner.setProperty(ConvertJSONToXML.DESTINATION, ConvertJSONToXML.DESTINATION_CONTENT);
        testRunner.setProperty(ConvertJSONToXML.ENCODING, "UTF-8");
        testRunner.setProperty(ConvertJSONToXML.JSON_PATH_EXPRESSION, "$.store.bicycle");
        testRunner.setProperty(ConvertJSONToXML.ELEMENT_NAME, "Bic");

        testRunner.enqueue(jsonString.getBytes(StandardCharsets.UTF_8));
        testRunner.run();
        testRunner.assertAllFlowFilesTransferred(ConvertJSONToXML.REL_SUCCESS, 1);
        final List<MockFlowFile> result = testRunner.getFlowFilesForRelationship(ConvertJSONToXML.REL_SUCCESS);
        final String expectStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Bic><color>red</color><price>19.95</price><info>info1</info><info>info2</info></Bic>";
        result.get(0).assertContentEquals(expectStr.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testJson_To_XML_JsonPath_PathNotFoundException() {
        testRunner.setValidateExpressionUsage(false);
        testRunner.setProperty(ConvertJSONToXML.DESTINATION, ConvertJSONToXML.DESTINATION_CONTENT);
        testRunner.setProperty(ConvertJSONToXML.ENCODING, "UTF-8");
        testRunner.setProperty(ConvertJSONToXML.JSON_PATH_EXPRESSION, "$.st");
        testRunner.setProperty(ConvertJSONToXML.ELEMENT_NAME, "XXX");

        testRunner.enqueue(jsonString.getBytes(StandardCharsets.UTF_8));
        testRunner.run();
        testRunner.assertAllFlowFilesTransferred(ConvertJSONToXML.REL_FAILURE, 1);
    }

    @Test
    public void testJson_To_XML_JsonPath_Not_Found() throws IOException {
        testRunner.setValidateExpressionUsage(false);
        testRunner.setProperty(ConvertJSONToXML.DESTINATION, ConvertJSONToXML.DESTINATION_CONTENT);
        testRunner.setProperty(ConvertJSONToXML.ENCODING, "UTF-8");
        testRunner.setProperty(ConvertJSONToXML.JSON_PATH_EXPRESSION, "$.store.book[*]/author");
        testRunner.setProperty(ConvertJSONToXML.ELEMENT_NAME, "XXX");

        testRunner.enqueue(jsonString.getBytes(StandardCharsets.UTF_8));
        testRunner.run();
        final List<MockFlowFile> result = testRunner.getFlowFilesForRelationship(ConvertJSONToXML.REL_SUCCESS);
        final String expectStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><list><XXX/></list>";
        result.get(0).assertContentEquals(expectStr.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testJson_To_XML_Attribute_WithOut_Content() throws IOException {
        testRunner.setValidateExpressionUsage(false);
        testRunner.setProperty(ConvertJSONToXML.DESTINATION, ConvertJSONToXML.DESTINATION_CONTENT);
        testRunner.setProperty(ConvertJSONToXML.ENCODING, "UTF-8");
        testRunner.setProperty(ConvertJSONToXML.JSON_ATTRIBUTE_MARK, "@");
        testRunner.setProperty(ConvertJSONToXML.JSON_CONTENT_KEY_NAME, "content");

        testRunner.enqueue("{\"bookstore\":{\"book\":[{\"@category\":\"cooking\",\"year\":2005,\"author\":\"Giada De Laurentiis\",\"price\":30.0,\"title\":{\"@lang\":\"en\",\"content\":\"Everyday Italian\"}}]}}".getBytes(StandardCharsets.UTF_8));
        testRunner.run();
        final List<MockFlowFile> result = testRunner.getFlowFilesForRelationship(ConvertJSONToXML.REL_SUCCESS);
        final String expectStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><bookstore><book category=\"cooking\"><title lang=\"en\">Everyday Italian</title><year>2005</year><author>Giada De Laurentiis</author><price>30.0</price></book></bookstore>";
        result.get(0).assertContentEquals(expectStr.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testJson_To_XML_Byte_Array_Simple_Array() throws IOException {
        try(FileInputStream fis = new FileInputStream("src/test/resources/simple-array.json")){
            byte[] result = processor.convertJsonToXMLBytes(fis, null, null, "NS", null, "UTF-8", "TEST");
            assertThat(new String(result, StandardCharsets.UTF_8), equalTo("<?xml version=\"1.0\" encoding=\"UTF-8\"?><array><list>a</list><list>b</list><list>c</list></array>"));
        }
    }

    @Test
    public void testJson_To_XML_Byte_Array_Simple_Object() throws IOException {
        try(FileInputStream fis = new FileInputStream("src/test/resources/simple-object.json")){
            byte[] result = processor.convertJsonToXMLBytes(fis, null, null, "NS", null, "UTF-8", "TEST");
            assertThat(new String(result, StandardCharsets.UTF_8), equalTo("<?xml version=\"1.0\" encoding=\"UTF-8\"?><TEST xmlns=\"NS\"><education>HU</education><education>MTU</education><name><last_name>Zhan</last_name><first_name>Nicholas</first_name></name><tel>12345678901</tel><position>Developer</position><age>22</age></TEST>"));
        }
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

    @Test
    public void testJson_To_Xml_Simple_Array() throws IOException {
        testRunner.setValidateExpressionUsage(false);
        testRunner.setProperty(ConvertJSONToXML.NAMESPACE, "NS");
        testRunner.setProperty(ConvertJSONToXML.DESTINATION, ConvertJSONToXML.DESTINATION_CONTENT);
        testRunner.setProperty(ConvertJSONToXML.ENCODING, "UTF-8");

        testRunner.enqueue(readFileToString("src/test/resources/simple-array.json").getBytes(StandardCharsets.UTF_8));
        testRunner.run();

        testRunner.assertAllFlowFilesTransferred(ConvertJSONToXML.REL_SUCCESS, 1);
        final List<MockFlowFile> result = testRunner.getFlowFilesForRelationship(ConvertJSONToXML.REL_SUCCESS);
        result.get(0).assertContentEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><array><list>a</list><list>b</list><list>c</list></array>".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testJson_To_Xml_Simple_Object() throws IOException {
        testRunner.setValidateExpressionUsage(false);
        testRunner.setProperty(ConvertJSONToXML.NAMESPACE, "NS");
        testRunner.setProperty(ConvertJSONToXML.DESTINATION, ConvertJSONToXML.DESTINATION_CONTENT);
        testRunner.setProperty(ConvertJSONToXML.ENCODING, "UTF-8");
        testRunner.setProperty(ConvertJSONToXML.ELEMENT_NAME, "RootElement");

        testRunner.enqueue(readFileToString("src/test/resources/simple-object.json").getBytes(StandardCharsets.UTF_8));
        testRunner.run();

        testRunner.assertAllFlowFilesTransferred(ConvertJSONToXML.REL_SUCCESS, 1);
        final List<MockFlowFile> result = testRunner.getFlowFilesForRelationship(ConvertJSONToXML.REL_SUCCESS);
        final String expectStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><RootElement xmlns=\"NS\"><education>HU</education><education>MTU</education><name><last_name>Zhan</last_name><first_name>Nicholas</first_name></name><tel>12345678901</tel><position>Developer</position><age>22</age></RootElement>";
        result.get(0).assertContentEquals(expectStr.getBytes(StandardCharsets.UTF_8));
    }
}
