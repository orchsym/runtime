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

import java.io.IOException;
import java.util.List;

import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;

/**
 * @author GU Guoqiang
 *
 */
public abstract class AbstractAttributesRunnerTest {
    protected TestRunner runner;

    protected static String testDataContent;

    public static String loadContents(String dataFileName) throws IOException {
        return TestRunners.loadContents(dataFileName);
    }

    @AfterClass
    public static void cleanup() {
        testDataContent = null;
    }

    @After
    public void after() {
        runner = null;
    }

    protected MockFlowFile runForSuccessFlowFile() {
        runner.run();

        runner.assertTransferCount(AbstractExtractToAttributesProcessor.REL_SUCCESS, 1);
        runner.assertTransferCount(AbstractExtractToAttributesProcessor.REL_FAILURE, 0);

        final List<MockFlowFile> flowFiles = runner.getFlowFilesForRelationship(AbstractExtractToAttributesProcessor.REL_SUCCESS);
        assertThat(flowFiles.size(), equalTo(1));

        final MockFlowFile successFlowFile = flowFiles.get(0);
        return successFlowFile;
    }

    @Test
    public void test_default_for_1st_fields_with_filter() throws Exception {
        runner.setProperty(AbstractExtractToAttributesProcessor.INCLUDE_FIELDS, "(.*)a(.*)");

        final MockFlowFile successFlowFile = runForSuccessFlowFile();

        successFlowFile.assertAttributeEquals("ALL.name", "数聚蜂巢");
        successFlowFile.assertAttributeEquals("ALL.age", "3");
    }

    @Test
    public void test_sub_arr_custom_all() throws Exception {
        runner.setProperty(AbstractExtractToAttributesProcessor.RECURSE_CHILDREN, "true");
        runner.setProperty(AbstractExtractToAttributesProcessor.ALLOW_ARRAY, "true");
        runner.setProperty(AbstractExtractToAttributesProcessor.EXCLUDE_FIELDS, "site,url");
        setProp_sub_arr_custom_all();

        final MockFlowFile successFlowFile = runForSuccessFlowFile();

        successFlowFile.assertAttributeEquals("data.name", "数聚蜂巢");
        successFlowFile.assertAttributeEquals("data.age", "3");
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

    protected abstract void setProp_sub_arr_custom_all();

    @Test
    public void test_sub_arr_no_name() throws Exception {
        runner.setProperty(AbstractExtractToAttributesProcessor.RECURSE_CHILDREN, "true");
        runner.setProperty(AbstractExtractToAttributesProcessor.ALLOW_ARRAY, "true");
        runner.setProperty(AbstractExtractToAttributesProcessor.EXCLUDE_FIELDS, "site,url");
        runner.setProperty(AbstractExtractToAttributesProcessor.CONTAIN_DYNAMIC_PROPERTY_NAME, "false");
        setProp_sub_arr_no_name();

        final MockFlowFile successFlowFile = runForSuccessFlowFile();

        successFlowFile.assertAttributeEquals("name", "数聚蜂巢");
        successFlowFile.assertAttributeEquals("age", "3");
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

    protected abstract void setProp_sub_arr_no_name();

    @Test
    public void test_arr_dynamic() throws Exception {
        runner.setProperty(AbstractExtractToAttributesProcessor.ALLOW_ARRAY, "true");
        runner.setProperty(AbstractExtractToAttributesProcessor.EXCLUDE_FIELDS, "url");
        runner.setProperty(AbstractExtractToAttributesProcessor.CONTAIN_DYNAMIC_PROPERTY_NAME, "false");
        setProp_arr_dynamic();

        final MockFlowFile successFlowFile = runForSuccessFlowFile();

        successFlowFile.assertAttributeEquals("the_name", "数聚蜂巢");
        successFlowFile.assertAttributeEquals("url", "");
        successFlowFile.assertAttributeEquals("post", "100000");
        successFlowFile.assertAttributeEquals("tel", "010-12345678");
        successFlowFile.assertAttributeEquals("tags.0", "Data");
        successFlowFile.assertAttributeEquals("tags.1", "Integration");
        successFlowFile.assertAttributeEquals("tags.2", "API");
        successFlowFile.assertAttributeEquals("name.0", "白山云");
        successFlowFile.assertAttributeEquals("name.1", "白山云");
        successFlowFile.assertAttributeEquals("name.2", "Orchsym");
    }

    protected abstract void setProp_arr_dynamic();

    @Test
    public void test_arr_dynamic_forbid_arr() throws Exception {
        // runner.setProperty(AbstractExtractToAttributesProcessor.ALLOW_ARRAY, "true");
        runner.setProperty(AbstractExtractToAttributesProcessor.EXCLUDE_FIELDS, "url");
        runner.setProperty(AbstractExtractToAttributesProcessor.CONTAIN_DYNAMIC_PROPERTY_NAME, "false");
        setProp_arr_dynamic();

        final MockFlowFile successFlowFile = runForSuccessFlowFile();

        successFlowFile.assertAttributeEquals("the_name", "数聚蜂巢");
        successFlowFile.assertAttributeEquals("url", "");
        successFlowFile.assertAttributeEquals("post", "100000");
        successFlowFile.assertAttributeEquals("tel", "010-12345678");
    }

    @Test
    public void test_sub_arr_dynamic() throws Exception {
        runner.setProperty(AbstractExtractToAttributesProcessor.RECURSE_CHILDREN, "true");
        runner.setProperty(AbstractExtractToAttributesProcessor.ALLOW_ARRAY, "true");
        runner.setProperty(AbstractExtractToAttributesProcessor.EXCLUDE_FIELDS, "url");
        runner.setProperty(AbstractExtractToAttributesProcessor.CONTAIN_DYNAMIC_PROPERTY_NAME, "false");
        setProp_sub_arr_dynamic();

        final MockFlowFile successFlowFile = runForSuccessFlowFile();

        successFlowFile.assertAttributeEquals("the_name", "数聚蜂巢");
        successFlowFile.assertAttributeEquals("address.city", "北京");
        successFlowFile.assertAttributeEquals("address.country", "中国");
        successFlowFile.assertAttributeEquals("address.street", "望京");
        successFlowFile.assertAttributeEquals("post", "100000");
        successFlowFile.assertAttributeEquals("tel", "010-12345678");
        successFlowFile.assertAttributeEquals("tags.0", "Data");
        successFlowFile.assertAttributeEquals("tags.1", "Integration");
        successFlowFile.assertAttributeEquals("tags.2", "API");
        successFlowFile.assertAttributeEquals("name.0", "白山云");
        successFlowFile.assertAttributeEquals("name.1", "白山云");
        successFlowFile.assertAttributeEquals("name.2", "Orchsym");
    }

    protected abstract void setProp_sub_arr_dynamic();

    @Test
    public void test_sub_arr_dynamic_filter_arr() throws Exception {
        runner.setProperty(AbstractExtractToAttributesProcessor.RECURSE_CHILDREN, "true");
        runner.setProperty(AbstractExtractToAttributesProcessor.ALLOW_ARRAY, "true");
        runner.setProperty(AbstractExtractToAttributesProcessor.EXCLUDE_FIELDS, "url,tags");
        runner.setProperty(AbstractExtractToAttributesProcessor.CONTAIN_DYNAMIC_PROPERTY_NAME, "false");
        setProp_sub_arr_dynamic();

        final MockFlowFile successFlowFile = runForSuccessFlowFile();

        successFlowFile.assertAttributeEquals("the_name", "数聚蜂巢");
        successFlowFile.assertAttributeEquals("address.city", "北京");
        successFlowFile.assertAttributeEquals("address.country", "中国");
        successFlowFile.assertAttributeEquals("address.street", "望京");
        successFlowFile.assertAttributeEquals("post", "100000");
        successFlowFile.assertAttributeEquals("tel", "010-12345678");
        successFlowFile.assertAttributeEquals("name.0", "白山云");
        successFlowFile.assertAttributeEquals("name.1", "白山云");
        successFlowFile.assertAttributeEquals("name.2", "Orchsym");

    }

    @Test
    public void test_arr_index() throws Exception {
        runner.setProperty(AbstractExtractToAttributesProcessor.ALLOW_ARRAY, "true");
        runner.setProperty(AbstractExtractToAttributesProcessor.EXCLUDE_FIELDS, "name");
        runner.setProperty(AbstractExtractToAttributesProcessor.CONTAIN_DYNAMIC_PROPERTY_NAME, "false");
        setProp_arr_index();

        final MockFlowFile successFlowFile = runForSuccessFlowFile();

        successFlowFile.assertAttributeEquals("url", "http://www.baishancloud.com");
        successFlowFile.assertAttributeEquals("url.0", "http://www.baishan.com");
        successFlowFile.assertAttributeEquals("url.1", "https://www.baishan.com/tech/orchsym/");
    }

    protected abstract void setProp_arr_index();

    @Test
    public void test_arr_index_forbid_arr() throws Exception {
        // runner.setProperty(AbstractExtractToAttributesProcessor.ALLOW_ARRAY, "true"); //don't effect the result, because the Record Path is object
        runner.setProperty(AbstractExtractToAttributesProcessor.EXCLUDE_FIELDS, "name");
        runner.setProperty(AbstractExtractToAttributesProcessor.CONTAIN_DYNAMIC_PROPERTY_NAME, "false");
        setProp_arr_index();

        final MockFlowFile successFlowFile = runForSuccessFlowFile();

        successFlowFile.assertAttributeEquals("url", "http://www.baishancloud.com");
        // successFlowFile.assertAttributeEquals("url.0", "http://www.baishan.com");
        // successFlowFile.assertAttributeEquals("url.1", "https://www.baishan.com/tech/orchsym/");
    }

    @Test
    public void test_simple_arr() throws Exception {
        runner.setProperty(AbstractExtractToAttributesProcessor.ALLOW_ARRAY, "true");
        runner.setProperty(AbstractExtractToAttributesProcessor.CONTAIN_DYNAMIC_PROPERTY_NAME, "false");
        setProp_simple_arr();

        final MockFlowFile successFlowFile = runForSuccessFlowFile();

        successFlowFile.assertAttributeEquals("the_tags.0", "Data");
        successFlowFile.assertAttributeEquals("the_tags.1", "Integration");
        successFlowFile.assertAttributeEquals("the_tags.2", "API");
    }

    protected abstract void setProp_simple_arr();

    @Test
    public void test_simple_arr_forbid_arr() throws Exception {
        runner.setProperty(AbstractExtractToAttributesProcessor.CONTAIN_DYNAMIC_PROPERTY_NAME, "false");
        setProp_simple_arr();

        final MockFlowFile successFlowFile = runForSuccessFlowFile();

        successFlowFile.assertAttributeNotExists("the_tags.0");
        successFlowFile.assertAttributeNotExists("the_tags.1");
        successFlowFile.assertAttributeNotExists("the_tags.2");
    }

    @Test
    public void test_simaple_arr_filter() throws Exception {
        runner.setProperty(AbstractExtractToAttributesProcessor.EXCLUDE_FIELDS, "tags");
        runner.setProperty(AbstractExtractToAttributesProcessor.CONTAIN_DYNAMIC_PROPERTY_NAME, "false");
        setProp_simple_arr();

        final MockFlowFile successFlowFile = runForSuccessFlowFile();

        successFlowFile.assertAttributeNotExists("the_tags.0");
        successFlowFile.assertAttributeNotExists("the_tags.1");
        successFlowFile.assertAttributeNotExists("the_tags.2");
    }
}
