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

import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunners;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * @author GU Guoqiang
 *
 */
public class ExtractJSONToAttributesRunnerTest extends AbstractAttributesRunnerTest {

    @BeforeClass
    public static void init() throws Exception {
        testDataContent = loadContents("test.json");
    }

    @Before
    public void before() {
        runner = TestRunners.newTestRunner(new ExtractJSONToAttributes());

    }

    protected MockFlowFile runForSuccessFlowFile() {
        runner.enqueue(testDataContent);
        return super.runForSuccessFlowFile();
    }

    protected void setProp_sub_arr_custom_all() {
        runner.setProperty("data", "$");
    }

    protected void setProp_sub_arr_no_name() {
        runner.setProperty("ABC", "$");
    }

    protected void setProp_arr_dynamic() {
        runner.setProperty("info", "$.details");
        runner.setProperty("links", "$.links");
        runner.setProperty("the_name", "$.name");
        runner.setProperty("url", "$.url");
    }

    protected void setProp_sub_arr_dynamic() {
        runner.setProperty("info", "$.details");
        runner.setProperty("links", "$.links");
        runner.setProperty("the_name", "$.name");
        runner.setProperty("url_2", "$.links[1]");
    }

    protected void setProp_arr_index() {
        runner.setProperty("links", "$.links[0,2]");
        runner.setProperty("the_name", "$.name");
        runner.setProperty("url_index1", "$.links[1]");
    }

    protected void setProp_simple_arr() {
        runner.setProperty("the_tags", "$.details.tags");
    }
}
