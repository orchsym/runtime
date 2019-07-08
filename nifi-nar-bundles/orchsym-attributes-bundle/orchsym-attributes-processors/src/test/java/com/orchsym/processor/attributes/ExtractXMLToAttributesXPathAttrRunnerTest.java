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
public class ExtractXMLToAttributesXPathAttrRunnerTest extends ExtractXMLToAttributesXPathRootRunnerTest {

    @BeforeClass
    public static void init() throws Exception {
        testDataContent = AbstractAttributesRunnerTest.loadContents("test-attr.xml");
    }

    @Test
    public void test_sub_arr_custom_all_with_attrs() throws Exception {
        super.test_sub_arr_custom_all_with_attrs();

        final List<MockFlowFile> flowFiles = runner.getFlowFilesForRelationship(ExtractXMLToAttributes.REL_SUCCESS);
        assertThat(flowFiles.size(), equalTo(1));

        final MockFlowFile successFlowFile = flowFiles.get(0);

        successFlowFile.assertAttributeEquals("data.details.tags.@index.0", "1");
        successFlowFile.assertAttributeEquals("data.details.tags.@name.0", "data");
        successFlowFile.assertAttributeEquals("data.details.tags.@index.1", "2");
        successFlowFile.assertAttributeEquals("data.details.tags.@name.1", "integration");
        successFlowFile.assertAttributeEquals("data.details.tags.@index.2", "3");
        successFlowFile.assertAttributeEquals("data.details.tags.@name.2", "api");

        successFlowFile.assertAttributeEquals("data.links.@index.0", "1");
        successFlowFile.assertAttributeEquals("data.links.name.@flag.0", "x");
        successFlowFile.assertAttributeEquals("data.links.@index.1", "2");
        successFlowFile.assertAttributeEquals("data.links.name.@flag.1", "y");
        successFlowFile.assertAttributeEquals("data.links.@index.2", "3");
        successFlowFile.assertAttributeEquals("data.links.name.@flag.2", "z");

    }

    @Test
    public void test_sub_arr_no_name() throws Exception {
        super.test_sub_arr_no_name();
    }

    @Test
    public void test_sub_arr_no_name_with_attr() throws Exception {
        super.test_sub_arr_no_name_with_attr();

        final List<MockFlowFile> flowFiles = runner.getFlowFilesForRelationship(ExtractXMLToAttributes.REL_SUCCESS);
        assertThat(flowFiles.size(), equalTo(1));

        final MockFlowFile successFlowFile = flowFiles.get(0);

        successFlowFile.assertAttributeEquals("details.tags.@index.0", "1");
        successFlowFile.assertAttributeEquals("details.tags.@name.0", "data");
        successFlowFile.assertAttributeEquals("details.tags.@index.1", "2");
        successFlowFile.assertAttributeEquals("details.tags.@name.1", "integration");
        successFlowFile.assertAttributeEquals("details.tags.@index.2", "3");
        successFlowFile.assertAttributeEquals("details.tags.@name.2", "api");

        successFlowFile.assertAttributeEquals("links.@index.0", "1");
        successFlowFile.assertAttributeEquals("links.name.@flag.0", "x");
        successFlowFile.assertAttributeEquals("links.@index.1", "2");
        successFlowFile.assertAttributeEquals("links.name.@flag.1", "y");
        successFlowFile.assertAttributeEquals("links.@index.2", "3");
        successFlowFile.assertAttributeEquals("links.name.@flag.2", "z");
    }

    @Test
    public void test_sub_arr_no_name_with_attr_filter() throws Exception {
        runner.setProperty(ExtractXMLToAttributes.RECURSE_CHILDREN, "true");
        runner.setProperty(ExtractXMLToAttributes.ALLOW_ARRAY, "true");
        runner.setProperty(ExtractXMLToAttributes.ALLOW_XML_ATTRIBUTES, "true");
        runner.setProperty(ExtractXMLToAttributes.EXCLUDE_FIELDS, "site,url,index");
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

        // successFlowFile.assertAttributeEquals("details.tags.@index.0", "1");
        successFlowFile.assertAttributeEquals("details.tags.@name.0", "data");
        // successFlowFile.assertAttributeEquals("details.tags.@index.1", "2");
        successFlowFile.assertAttributeEquals("details.tags.@name.1", "integration");
        // successFlowFile.assertAttributeEquals("details.tags.@index.2", "3");
        successFlowFile.assertAttributeEquals("details.tags.@name.2", "api");

        // successFlowFile.assertAttributeEquals("links.@index.0", "1");
        successFlowFile.assertAttributeEquals("links.name.@flag.0", "x");
        // successFlowFile.assertAttributeEquals("links.@index.1", "2");
        successFlowFile.assertAttributeEquals("links.name.@flag.1", "y");
        // successFlowFile.assertAttributeEquals("links.@index.2", "3");
        successFlowFile.assertAttributeEquals("links.name.@flag.2", "z");
    }

    @Test
    public void test_simple_arr_relative_path() throws Exception {
        runner.setProperty(AbstractExtractToAttributesProcessor.ALLOW_ARRAY, "true");
        runner.setProperty(AbstractExtractToAttributesProcessor.CONTAIN_DYNAMIC_PROPERTY_NAME, "false");
        runner.setProperty("the_tags", "//tags");

        final MockFlowFile successFlowFile = runForSuccessFlowFile();

        successFlowFile.assertAttributeEquals("the_tags.0", "Data");
        successFlowFile.assertAttributeEquals("the_tags.1", "Integration");
        successFlowFile.assertAttributeEquals("the_tags.2", "API");

        successFlowFile.assertAttributeNotExists("@index.0");
    }

    @Test
    public void test_simple_arr_relative_path_with_attr() throws Exception {
        runner.setProperty(AbstractExtractToAttributesProcessor.ALLOW_ARRAY, "true");
        runner.setProperty(ExtractXMLToAttributes.ALLOW_XML_ATTRIBUTES, "true");
        runner.setProperty(AbstractExtractToAttributesProcessor.CONTAIN_DYNAMIC_PROPERTY_NAME, "false");
        runner.setProperty("the_tags", "//tags");

        final MockFlowFile successFlowFile = runForSuccessFlowFile();

        successFlowFile.assertAttributeEquals("the_tags.0", "Data");
        successFlowFile.assertAttributeEquals("the_tags.1", "Integration");
        successFlowFile.assertAttributeEquals("the_tags.2", "API");

        successFlowFile.assertAttributeEquals("the_tags.0.@index", "1");
        successFlowFile.assertAttributeEquals("the_tags.0.@name", "data");
        successFlowFile.assertAttributeEquals("the_tags.1.@index", "2");
        successFlowFile.assertAttributeEquals("the_tags.1.@name", "integration");
        successFlowFile.assertAttributeEquals("the_tags.2.@index", "3");
        successFlowFile.assertAttributeEquals("the_tags.2.@name", "api");
    }

    @Test
    public void test_tags_attrs_relative_path() throws Exception {
        runner.setProperty(AbstractExtractToAttributesProcessor.ALLOW_ARRAY, "true");
        runner.setProperty(ExtractXMLToAttributes.ALLOW_XML_ATTRIBUTES, "true");
        runner.setProperty(AbstractExtractToAttributesProcessor.CONTAIN_DYNAMIC_PROPERTY_NAME, "false");
        runner.setProperty("my_name", "//@name");

        final MockFlowFile successFlowFile = runForSuccessFlowFile();

        successFlowFile.assertAttributeEquals("my_name.0", "data");
        successFlowFile.assertAttributeEquals("my_name.1", "integration");
        successFlowFile.assertAttributeEquals("my_name.2", "api");
    }

    @Test
    public void test_attr_relative_path_all_attr() throws Exception {
        runner.setProperty(AbstractExtractToAttributesProcessor.ALLOW_ARRAY, "true");
        runner.setProperty(ExtractXMLToAttributes.ALLOW_XML_ATTRIBUTES, "true");
        runner.setProperty(AbstractExtractToAttributesProcessor.CONTAIN_DYNAMIC_PROPERTY_NAME, "false");
        runner.setProperty("the_attr", "//tags[@*]");

        final MockFlowFile successFlowFile = runForSuccessFlowFile();

        successFlowFile.assertAttributeEquals("tags.0", "Data");
        successFlowFile.assertAttributeEquals("tags.@index.0", "1");
        successFlowFile.assertAttributeEquals("tags.@name.0", "data");

        successFlowFile.assertAttributeEquals("tags.1", "Integration");
        successFlowFile.assertAttributeEquals("tags.@index.1", "2");
        successFlowFile.assertAttributeEquals("tags.@name.1", "integration");

        successFlowFile.assertAttributeEquals("tags.2", "API");
        successFlowFile.assertAttributeEquals("tags.@index.2", "3");
        successFlowFile.assertAttributeEquals("tags.@name.2", "api");
    }

    @Test
    public void test_attr_relative_path_all_attr_2() throws Exception {
        runner.setProperty(AbstractExtractToAttributesProcessor.ALLOW_ARRAY, "true");
        runner.setProperty(ExtractXMLToAttributes.ALLOW_XML_ATTRIBUTES, "true");
        // runner.setProperty(AbstractExtractToAttributesProcessor.CONTAIN_DYNAMIC_PROPERTY_NAME, "false");
        runner.setProperty("the_attr", "//tags[@*]");

        final MockFlowFile successFlowFile = runForSuccessFlowFile();

        successFlowFile.assertAttributeEquals("the_attr.tags.0", "Data");
        successFlowFile.assertAttributeEquals("the_attr.tags.@index.0", "1");
        successFlowFile.assertAttributeEquals("the_attr.tags.@name.0", "data");

        successFlowFile.assertAttributeEquals("the_attr.tags.1", "Integration");
        successFlowFile.assertAttributeEquals("the_attr.tags.@index.1", "2");
        successFlowFile.assertAttributeEquals("the_attr.tags.@name.1", "integration");

        successFlowFile.assertAttributeEquals("the_attr.tags.2", "API");
        successFlowFile.assertAttributeEquals("the_attr.tags.@index.2", "3");
        successFlowFile.assertAttributeEquals("the_attr.tags.@name.2", "api");
    }

    @Test
    public void test_links_relative_path() throws Exception {
        runner.setProperty(AbstractExtractToAttributesProcessor.ALLOW_ARRAY, "true");
        runner.setProperty(AbstractExtractToAttributesProcessor.CONTAIN_DYNAMIC_PROPERTY_NAME, "false");
        runner.setProperty("the_tags", "//links");

        final MockFlowFile successFlowFile = runForSuccessFlowFile();

        successFlowFile.assertAttributeEquals("name.0", "白山云");
        successFlowFile.assertAttributeEquals("name.1", "白山云");
        successFlowFile.assertAttributeEquals("name.2", "Orchsym");
        successFlowFile.assertAttributeEquals("url.0", "http://www.baishan.com");
        successFlowFile.assertAttributeEquals("url.1", "http://www.baishancloud.com");
        successFlowFile.assertAttributeEquals("url.2", "https://www.baishan.com/tech/orchsym/");
    }

    @Test
    public void test_links_relative_path_with_attr() throws Exception {
        runner.setProperty(AbstractExtractToAttributesProcessor.ALLOW_ARRAY, "true");
        runner.setProperty(ExtractXMLToAttributes.ALLOW_XML_ATTRIBUTES, "true");
        runner.setProperty(AbstractExtractToAttributesProcessor.CONTAIN_DYNAMIC_PROPERTY_NAME, "false");
        runner.setProperty("the_tags", "//links");

        final MockFlowFile successFlowFile = runForSuccessFlowFile();

        successFlowFile.assertAttributeEquals("name.0", "白山云");
        successFlowFile.assertAttributeEquals("name.1", "白山云");
        successFlowFile.assertAttributeEquals("name.2", "Orchsym");
        successFlowFile.assertAttributeEquals("url.0", "http://www.baishan.com");
        successFlowFile.assertAttributeEquals("url.1", "http://www.baishancloud.com");
        successFlowFile.assertAttributeEquals("url.2", "https://www.baishan.com/tech/orchsym/");

        successFlowFile.assertAttributeEquals("@index.0", "1");
        successFlowFile.assertAttributeEquals("@index.1", "2");
        successFlowFile.assertAttributeEquals("@index.2", "3");
        successFlowFile.assertAttributeEquals("name.@flag.0", "x");
        successFlowFile.assertAttributeEquals("name.@flag.1", "y");
        successFlowFile.assertAttributeEquals("name.@flag.2", "z");
    }

    @Test
    public void test_attr_relative_path() throws Exception {
        runner.setProperty(AbstractExtractToAttributesProcessor.ALLOW_ARRAY, "true");
        runner.setProperty(ExtractXMLToAttributes.ALLOW_XML_ATTRIBUTES, "true");
        runner.setProperty(AbstractExtractToAttributesProcessor.CONTAIN_DYNAMIC_PROPERTY_NAME, "false");
        runner.setProperty("the_flag", "//@flag");

        final MockFlowFile successFlowFile = runForSuccessFlowFile();

        successFlowFile.assertAttributeEquals("the_flag.0", "x");
        successFlowFile.assertAttributeEquals("the_flag.1", "y");
        successFlowFile.assertAttributeEquals("the_flag.2", "z");
    }

    @Test
    public void test_attr_relative_path_2() throws Exception {
        runner.setProperty(AbstractExtractToAttributesProcessor.ALLOW_ARRAY, "true");
        // runner.setProperty(ExtractXMLToAttributes.ALLOW_XML_ATTRIBUTES, "true");
        runner.setProperty(AbstractExtractToAttributesProcessor.CONTAIN_DYNAMIC_PROPERTY_NAME, "false");
        runner.setProperty("the_flag", "//@flag");

        final MockFlowFile successFlowFile = runForSuccessFlowFile();

        successFlowFile.assertAttributeEquals("the_flag.0", "x");
        successFlowFile.assertAttributeEquals("the_flag.1", "y");
        successFlowFile.assertAttributeEquals("the_flag.2", "z");
    }

    @Test
    public void test_attr_relative_path_3() throws Exception {
        // runner.setProperty(AbstractExtractToAttributesProcessor.ALLOW_ARRAY, "true");
        // runner.setProperty(ExtractXMLToAttributes.ALLOW_XML_ATTRIBUTES, "true");
        // runner.setProperty(AbstractExtractToAttributesProcessor.CONTAIN_DYNAMIC_PROPERTY_NAME, "false");
        runner.setProperty("the_flag", "//@flag");

        final MockFlowFile successFlowFile = runForSuccessFlowFile();

        successFlowFile.assertAttributeNotExists("the_flag.0");
        successFlowFile.assertAttributeNotExists("the_flag.1");
        successFlowFile.assertAttributeNotExists("the_flag.2");
    }

    @Test
    public void test_attr_relative_path_with_exp() throws Exception {
        runner.setProperty(AbstractExtractToAttributesProcessor.ALLOW_ARRAY, "true");
        runner.setProperty(ExtractXMLToAttributes.ALLOW_XML_ATTRIBUTES, "true");
        runner.setProperty(ExtractXMLToAttributes.XML_ATTRIBUTE_MARK, "#&");
        runner.setProperty(AbstractExtractToAttributesProcessor.CONTAIN_DYNAMIC_PROPERTY_NAME, "false");
        runner.setProperty("last_name", "//links[last()]/name");
        runner.setProperty("last_url", "//links[last()]/url");

        final MockFlowFile successFlowFile = runForSuccessFlowFile();

        successFlowFile.assertAttributeEquals("last_name", "Orchsym");
        successFlowFile.assertAttributeEquals("last_name.#&flag", "z");
        successFlowFile.assertAttributeEquals("last_url", "https://www.baishan.com/tech/orchsym/");
    }

    @Test
    public void test_attr_relative_path_with_exp_1() throws Exception {
        // runner.setProperty(AbstractExtractToAttributesProcessor.ALLOW_ARRAY, "true");
        runner.setProperty(ExtractXMLToAttributes.ALLOW_XML_ATTRIBUTES, "true");
        runner.setProperty(ExtractXMLToAttributes.XML_ATTRIBUTE_MARK, "#&");
        runner.setProperty(AbstractExtractToAttributesProcessor.CONTAIN_DYNAMIC_PROPERTY_NAME, "false");
        runner.setProperty("last_name", "//links[last()]/name");
        runner.setProperty("last_url", "//links[last()]/url");

        final MockFlowFile successFlowFile = runForSuccessFlowFile();

        successFlowFile.assertAttributeEquals("last_name", "Orchsym");
        successFlowFile.assertAttributeEquals("last_name.#&flag", "z");
        successFlowFile.assertAttributeEquals("last_url", "https://www.baishan.com/tech/orchsym/");
    }

    @Test
    public void test_attr_relative_path_with_exp_2() throws Exception {
        // runner.setProperty(AbstractExtractToAttributesProcessor.ALLOW_ARRAY, "true");
        // runner.setProperty(ExtractXMLToAttributes.ALLOW_XML_ATTRIBUTES, "true");
        // runner.setProperty(ExtractXMLToAttributes.XML_ATTRIBUTE_MARK, "#&");
        runner.setProperty(AbstractExtractToAttributesProcessor.CONTAIN_DYNAMIC_PROPERTY_NAME, "false");
        runner.setProperty("last_name", "//links[last()]/name");
        runner.setProperty("last_url", "//links[last()]/url");
        runner.setProperty("url_1", "//links[@index=1]/url");
        runner.setProperty("name_z", "//links/name[@flag='z']");

        final MockFlowFile successFlowFile = runForSuccessFlowFile();

        successFlowFile.assertAttributeEquals("last_name", "Orchsym");
        // successFlowFile.assertAttributeEquals("#&flag", "z");
        successFlowFile.assertAttributeEquals("last_url", "https://www.baishan.com/tech/orchsym/");
        successFlowFile.assertAttributeEquals("url_1", "http://www.baishan.com");
        successFlowFile.assertAttributeEquals("name_z", "Orchsym");
    }
}
