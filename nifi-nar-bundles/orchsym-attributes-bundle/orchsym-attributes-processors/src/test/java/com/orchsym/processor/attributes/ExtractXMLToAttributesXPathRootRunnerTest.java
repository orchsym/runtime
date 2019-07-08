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
package com.orchsym.processor.attributes;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author GU Guoqiang
 *
 */
public class ExtractXMLToAttributesXPathRootRunnerTest {

    protected TestRunner runner;

    protected static String testDataContent;

    @BeforeClass
    public static void init() throws Exception {
        testDataContent = AbstractAttributesRunnerTest.loadContents("test.xml");
    }

    @AfterClass
    public static void cleanup() {
        testDataContent = null;
    }

    @After
    public void after() {
        runner = null;
    }

    @Before
    public void before() {
        runner = TestRunners.newTestRunner(new ExtractXMLToAttributes());

    }

    protected MockFlowFile runForSuccessFlowFile() {
        runner.enqueue(testDataContent);
        runner.run();

        runner.assertTransferCount(ExtractXMLToAttributes.REL_SUCCESS, 1);
        runner.assertTransferCount(ExtractXMLToAttributes.REL_FAILURE, 0);

        final List<MockFlowFile> flowFiles = runner.getFlowFilesForRelationship(ExtractXMLToAttributes.REL_SUCCESS);
        assertThat(flowFiles.size(), equalTo(1));

        final MockFlowFile successFlowFile = flowFiles.get(0);
        return successFlowFile;
    }

    @Test
    public void test_default_for_1st_fields_with_filter() throws Exception {
        runner.setProperty(ExtractXMLToAttributes.INCLUDE_FIELDS, "(.*)a(.*)");

        final MockFlowFile successFlowFile = runForSuccessFlowFile();

        successFlowFile.assertAttributeEquals("ALL.name", "数聚蜂巢");
        successFlowFile.assertAttributeEquals("ALL.age", "3");
        // successFlowFile.assertAttributeEquals("ALL.@xmlns", "https://www.baishan.com"); //filtered
    }

    @Test
    public void test_default_for_1st_fields_with_attr() throws Exception {
        runner.setProperty(ExtractXMLToAttributes.ALLOW_XML_ATTRIBUTES, "true");
        runner.setProperty(ExtractXMLToAttributes.INCLUDE_FIELDS, "(.*)n(.*)");

        final MockFlowFile successFlowFile = runForSuccessFlowFile();

        successFlowFile.assertAttributeEquals("ALL.name", "数聚蜂巢");
        // successFlowFile.assertAttributeEquals("ALL.age", "3");
        successFlowFile.assertAttributeEquals("ALL.@xmlns", "https://www.baishan.com");
    }

    @Test
    public void test_sub_arr_custom_all() throws Exception {
        runner.setProperty(ExtractXMLToAttributes.RECURSE_CHILDREN, "true");
        runner.setProperty(ExtractXMLToAttributes.ALLOW_ARRAY, "true");
        // runner.setProperty(ExtractXMLToAttributes.ALLOW_XML_ATTRIBUTES, "true");
        runner.setProperty(ExtractXMLToAttributes.EXCLUDE_FIELDS, "site,url");
        runner.setProperty("data", "/*");

        final MockFlowFile successFlowFile = runForSuccessFlowFile();

        successFlowFile.assertAttributeEquals("data.name", "数聚蜂巢");
        successFlowFile.assertAttributeEquals("data.age", "3");
        // successFlowFile.assertAttributeEquals("data.@xmlns", "https://www.baishan.com");
        successFlowFile.assertAttributeEquals("data.details.address.city", "北京");
        successFlowFile.assertAttributeEquals("data.details.address.country", "中国");
        successFlowFile.assertAttributeEquals("data.details.address.street", "望京");
        successFlowFile.assertAttributeEquals("data.details.post", "100000");
        successFlowFile.assertAttributeEquals("data.details.tel", "010-12345678");
        successFlowFile.assertAttributeEquals("data.details.tags.0", "Data");
        successFlowFile.assertAttributeEquals("data.details.tags.1", "Integration");
        successFlowFile.assertAttributeEquals("data.details.tags.2", "API");
        successFlowFile.assertAttributeEquals("data.links.name.0", "白山云");
        successFlowFile.assertAttributeEquals("data.links.name.1", "白山云");
        successFlowFile.assertAttributeEquals("data.links.name.2", "Orchsym");
    }

    @Test
    public void test_sub_arr_custom_all_with_attrs() throws Exception {
        runner.setProperty(ExtractXMLToAttributes.RECURSE_CHILDREN, "true");
        runner.setProperty(ExtractXMLToAttributes.ALLOW_ARRAY, "true");
        runner.setProperty(ExtractXMLToAttributes.ALLOW_XML_ATTRIBUTES, "true");
        runner.setProperty(ExtractXMLToAttributes.EXCLUDE_FIELDS, "site,url");
        runner.setProperty("data", "/*");

        final MockFlowFile successFlowFile = runForSuccessFlowFile();

        successFlowFile.assertAttributeEquals("data.name", "数聚蜂巢");
        successFlowFile.assertAttributeEquals("data.age", "3");
        successFlowFile.assertAttributeEquals("data.@xmlns", "https://www.baishan.com");
        successFlowFile.assertAttributeEquals("data.details.address.city", "北京");
        successFlowFile.assertAttributeEquals("data.details.address.country", "中国");
        successFlowFile.assertAttributeEquals("data.details.address.street", "望京");
        successFlowFile.assertAttributeEquals("data.details.post", "100000");
        successFlowFile.assertAttributeEquals("data.details.tel", "010-12345678");
        successFlowFile.assertAttributeEquals("data.details.tags.0", "Data");
        successFlowFile.assertAttributeEquals("data.details.tags.1", "Integration");
        successFlowFile.assertAttributeEquals("data.details.tags.2", "API");
        successFlowFile.assertAttributeEquals("data.links.name.0", "白山云");
        successFlowFile.assertAttributeEquals("data.links.name.1", "白山云");
        successFlowFile.assertAttributeEquals("data.links.name.2", "Orchsym");
    }

    @Test
    public void test_sub_arr_no_name() throws Exception {
        runner.setProperty(ExtractXMLToAttributes.RECURSE_CHILDREN, "true");
        runner.setProperty(ExtractXMLToAttributes.ALLOW_ARRAY, "true");
        // runner.setProperty(ExtractXMLToAttributes.ALLOW_XML_ATTRIBUTES, "true");
        runner.setProperty(ExtractXMLToAttributes.EXCLUDE_FIELDS, "site,url");
        runner.setProperty(ExtractXMLToAttributes.CONTAIN_DYNAMIC_PROPERTY_NAME, "false");
        runner.setProperty("ABC", "/*");

        final MockFlowFile successFlowFile = runForSuccessFlowFile();

        successFlowFile.assertAttributeEquals("name", "数聚蜂巢");
        successFlowFile.assertAttributeEquals("age", "3");
        // successFlowFile.assertAttributeEquals("@xmlns", "https://www.baishan.com");
        successFlowFile.assertAttributeEquals("details.address.city", "北京");
        successFlowFile.assertAttributeEquals("details.address.country", "中国");
        successFlowFile.assertAttributeEquals("details.address.street", "望京");
        successFlowFile.assertAttributeEquals("details.post", "100000");
        successFlowFile.assertAttributeEquals("details.tel", "010-12345678");
        successFlowFile.assertAttributeEquals("details.tags.0", "Data");
        successFlowFile.assertAttributeEquals("details.tags.1", "Integration");
        successFlowFile.assertAttributeEquals("details.tags.2", "API");
        successFlowFile.assertAttributeEquals("links.name.0", "白山云");
        successFlowFile.assertAttributeEquals("links.name.1", "白山云");
        successFlowFile.assertAttributeEquals("links.name.2", "Orchsym");
    }

    @Test
    public void test_sub_arr_no_name_with_attr() throws Exception {
        runner.setProperty(ExtractXMLToAttributes.RECURSE_CHILDREN, "true");
        runner.setProperty(ExtractXMLToAttributes.ALLOW_ARRAY, "true");
        runner.setProperty(ExtractXMLToAttributes.ALLOW_XML_ATTRIBUTES, "true");
        runner.setProperty(ExtractXMLToAttributes.EXCLUDE_FIELDS, "site,url");
        runner.setProperty(ExtractXMLToAttributes.CONTAIN_DYNAMIC_PROPERTY_NAME, "false");
        runner.setProperty("ABC", "/*");

        final MockFlowFile successFlowFile = runForSuccessFlowFile();

        successFlowFile.assertAttributeEquals("name", "数聚蜂巢");
        successFlowFile.assertAttributeEquals("age", "3");
        successFlowFile.assertAttributeEquals("@xmlns", "https://www.baishan.com");
        successFlowFile.assertAttributeEquals("details.address.city", "北京");
        successFlowFile.assertAttributeEquals("details.address.country", "中国");
        successFlowFile.assertAttributeEquals("details.address.street", "望京");
        successFlowFile.assertAttributeEquals("details.post", "100000");
        successFlowFile.assertAttributeEquals("details.tel", "010-12345678");
        successFlowFile.assertAttributeEquals("details.tags.0", "Data");
        successFlowFile.assertAttributeEquals("details.tags.1", "Integration");
        successFlowFile.assertAttributeEquals("details.tags.2", "API");
        successFlowFile.assertAttributeEquals("links.name.0", "白山云");
        successFlowFile.assertAttributeEquals("links.name.1", "白山云");
        successFlowFile.assertAttributeEquals("links.name.2", "Orchsym");
    }
}
