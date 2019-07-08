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

import java.util.List;

import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunners;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author GU Guoqiang
 * 
 *         Because need ROOT element "Data" for XML, so the result have "ALL.Data.xxx" or such
 */
public class ExtractXMLToAttributesXQueryRunnerTest extends AbstractAttributesRunnerTest {

    @BeforeClass
    public static void init() throws Exception {
        testDataContent = loadContents("test.xml");
    }

    @Before
    public void before() {
        runner = TestRunners.newTestRunner(new ExtractXMLToAttributes());
        runner.setProperty(ExtractXMLToAttributes.XML_PATH_TYPE, ExtractXMLToAttributes.XQUERY);

    }

    protected MockFlowFile runForSuccessFlowFile() {
        runner.enqueue(testDataContent);
        return super.runForSuccessFlowFile();
    }

    @Test
    public void test_default_for_1st_fields_with_filter() throws Exception {
        runner.setProperty(AbstractExtractToAttributesProcessor.INCLUDE_FIELDS, "(.*)a(.*)");
        runner.setProperty("ALL", "for $elem in root() return $elem");
        final MockFlowFile successFlowFile = runForSuccessFlowFile();

        successFlowFile.assertAttributeEquals("ALL.name", "数聚蜂巢");
        successFlowFile.assertAttributeEquals("ALL.age", "3");
    }

    @Test
    public void test_default_for_1st_fields_with_attr() throws Exception {
        runner.setProperty(ExtractXMLToAttributes.ALLOW_XML_ATTRIBUTES, "true");
        // runner.setProperty(AbstractExtractToAttributesProcessor.INCLUDE_FIELDS, "(.*)a(.*)");
        runner.setProperty("ALL", "for $elem in root() return $elem");
        final MockFlowFile successFlowFile = runForSuccessFlowFile();

        successFlowFile.assertAttributeEquals("ALL.name", "数聚蜂巢");
        successFlowFile.assertAttributeEquals("ALL.age", "3");
        successFlowFile.assertAttributeEquals("ALL.@xmlns", "https://www.baishan.com");
    }

    @Test
    public void test_default_for_1st_fields_with_filter_with_attr() throws Exception {
        runner.setProperty(ExtractXMLToAttributes.ALLOW_XML_ATTRIBUTES, "true");
        runner.setProperty(AbstractExtractToAttributesProcessor.INCLUDE_FIELDS, "(.*)a(.*)");
        runner.setProperty("ALL", "for $elem in root() return $elem");
        final MockFlowFile successFlowFile = runForSuccessFlowFile();

        successFlowFile.assertAttributeEquals("ALL.name", "数聚蜂巢");
        successFlowFile.assertAttributeEquals("ALL.age", "3");
        // successFlowFile.assertAttributeEquals("ALL.@xmlns", "https://www.baishan.com"); //filtered
    }

    @Test
    public void test_sub_arr_custom_all_with_attr() throws Exception {
        runner.setProperty(ExtractXMLToAttributes.ALLOW_XML_ATTRIBUTES, "true");
        super.test_sub_arr_custom_all();

        final List<MockFlowFile> flowFiles = runner.getFlowFilesForRelationship(AbstractExtractToAttributesProcessor.REL_SUCCESS);
        final MockFlowFile successFlowFile = flowFiles.get(0);

        successFlowFile.assertAttributeEquals("data.@xmlns", "https://www.baishan.com");
    }

    protected void setProp_sub_arr_custom_all() {
        runner.setProperty("data", "for $elem in root() return $elem");
    }

    @Test
    public void test_sub_arr_no_name_with_attr() throws Exception {
        runner.setProperty(ExtractXMLToAttributes.ALLOW_XML_ATTRIBUTES, "true");
        super.test_sub_arr_no_name();

        final List<MockFlowFile> flowFiles = runner.getFlowFilesForRelationship(AbstractExtractToAttributesProcessor.REL_SUCCESS);
        final MockFlowFile successFlowFile = flowFiles.get(0);

        successFlowFile.assertAttributeEquals("@xmlns", "https://www.baishan.com");
    }

    protected void setProp_sub_arr_no_name() {
        runner.setProperty("ABC", "for $elem in root() return $elem");
    }

    @Test
    @Ignore
    public void test_arr_dynamic() throws Exception {
        super.test_arr_dynamic();
    }

    protected void setProp_arr_dynamic() {
        // FIXME need find a way to set the expression for XQuery
        runner.setProperty("info", "/Data/details");
        runner.setProperty("links", "/Data/links");
        runner.setProperty("the_name", "/Data/name");
        runner.setProperty("url", "/Data/url");
    }

    @Test
    @Ignore
    public void test_sub_arr_dynamic() throws Exception {
        super.test_sub_arr_dynamic();
    }

    @Test
    @Ignore
    public void test_sub_arr_dynamic_filter_arr() throws Exception {
        super.test_sub_arr_dynamic_filter_arr();
    }

    @Test
    @Ignore
    public void test_arr_dynamic_forbid_arr() throws Exception {
        super.test_arr_dynamic_forbid_arr();
    }

    protected void setProp_sub_arr_dynamic() {
        // FIXME need find a way to set the expression for XQuery
        runner.setProperty("info", "/Data/details");
        runner.setProperty("links", "/Data/links");
        runner.setProperty("the_name", "/Data/name");
        runner.setProperty("url_2", "/Data/links[1]");
    }

    @Test
    @Ignore
    public void test_arr_index() throws Exception {
        super.test_arr_index();
    }

    @Test
    @Ignore
    public void test_arr_index_forbid_arr() throws Exception {
        super.test_arr_index_forbid_arr();
    }

    protected void setProp_arr_index() {
        // FIXME need find a way to set the expression for XQuery
        runner.setProperty("links", "/Data/links[1]|/Data/links[3]");
        runner.setProperty("the_name", "/Data/name");
        runner.setProperty("url_index1", "/Data/links[2]");
    }

    @Test
    @Ignore
    public void test_simple_arr() throws Exception {
        super.test_simple_arr();
    }

    protected void setProp_simple_arr() {
        // FIXME need find a way to set the expression for XQuery
        runner.setProperty("the_tags", "/Data/details/tags");
    }
}
