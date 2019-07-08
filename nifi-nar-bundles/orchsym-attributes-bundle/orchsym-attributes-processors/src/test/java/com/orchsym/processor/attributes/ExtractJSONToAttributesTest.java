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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

/**
 * @author GU Guoqiang
 *
 */
public class ExtractJSONToAttributesTest {
    private ExtractJSONToAttributes processor;

    @BeforeEach
    public void before() {
        processor = new ExtractJSONToAttributes();
    }

    @AfterEach
    public void after() {
        processor = null;
    }

    private DocumentContext createJsonContext(String json) {
        DocumentContext ctx = JsonPath.using(ExtractJSONToAttributes.STRICT_PROVIDER_CONFIGURATION).parse(json);

        return ctx;
    }

    @Test
    public void test_processAttributes_empty() {
        Map<String, String> attributes = new HashMap<>();

        processor.processAttributes(attributes, null, null, -1, null, null);
        processor.processAttributes(attributes, createJsonContext("{}"), null, -1, null, null);
        processor.processAttributes(attributes, createJsonContext("{}"), Collections.emptyMap(), -1, null, null);

        assertThat(attributes.keySet(), empty());
    }

    @Test
    public void test_processAttributes_notFoundPath() {
        Map<String, String> attributes = new HashMap<>();

        Map<String, JsonPath> jsonPaths = new HashMap<>();
        jsonPaths.put("test.id", JsonPath.compile("$.id"));
        jsonPaths.put("test.addr.city", JsonPath.compile("$.addr.city"));

        processor.processAttributes(attributes, createJsonContext("{}"), jsonPaths, -1, null, null);
        processor.processAttributes(attributes, createJsonContext("{}"), jsonPaths, 3, null, null);

        assertThat(attributes.keySet(), hasSize(4));

        assertThat(attributes, hasEntry("test.id", ""));
        assertThat(attributes, hasEntry("test.addr.city", ""));
        assertThat(attributes, hasEntry("test.id.3", ""));
        assertThat(attributes, hasEntry("test.addr.city.3", ""));

        assertThat(attributes.keySet(), hasSize(4));
    }

    @Test
    public void test_processAttributes_object() {
        String json = "{"//
                + "\"id\":123,"//
                + "\"name\":\"test\","//
                + "\"addr\":"//
                + " {"//
                + "  \"city\":\"BJ\","//
                + "  \"street\":\"WJ\""//
                + " }" //
                + "}";

        final DocumentContext jsonContext = createJsonContext(json);

        Map<String, JsonPath> jsonPaths = new HashMap<>();
        jsonPaths.put("test.id", JsonPath.compile("$.id"));
        jsonPaths.put("test.name", JsonPath.compile("$.name"));
        jsonPaths.put("test.addr", JsonPath.compile("$.addr"));

        Map<String, String> attributes = new HashMap<>();

        processor.processAttributes(attributes, jsonContext, jsonPaths, -1, null, null);
        processor.processAttributes(attributes, jsonContext, jsonPaths, 5, null, null);

        assertThat(attributes, hasEntry("test.id", "123"));
        assertThat(attributes, hasEntry("test.name", "test"));
        assertThat(attributes, hasEntry("test.addr.city", "BJ"));
        assertThat(attributes, hasEntry("test.addr.street", "WJ"));

        assertThat(attributes, hasEntry("test.id.5", "123"));
        assertThat(attributes, hasEntry("test.name.5", "test"));
        assertThat(attributes, hasEntry("test.addr.5.city", "BJ"));
        assertThat(attributes, hasEntry("test.addr.5.street", "WJ"));

        assertThat(attributes.keySet(), hasSize(8));
    }

    @Test
    public void test_processAttributes_object_uncontain_prop() {
        String json = "{"//
                + "\"id\":123,"//
                + "\"name\":\"test\","//
                + "\"addr\":"//
                + " {"//
                + "  \"city\":\"BJ\","//
                + "  \"street\":\"WJ\""//
                + " }" //
                + "}";

        final DocumentContext jsonContext = createJsonContext(json);

        Map<String, JsonPath> jsonPaths = new HashMap<>();
        jsonPaths.put("test.id", JsonPath.compile("$.id"));
        jsonPaths.put("test.name", JsonPath.compile("$.name"));
        jsonPaths.put("test.addr", JsonPath.compile("$.addr"));

        processor.containPropName = false;

        Map<String, String> attributes = new HashMap<>();
        processor.processAttributes(attributes, jsonContext, jsonPaths, -1, null, null);
        processor.processAttributes(attributes, jsonContext, jsonPaths, 5, null, null);

        assertThat(attributes, hasEntry("test.id", "123"));
        assertThat(attributes, hasEntry("test.name", "test"));
        assertThat(attributes, hasEntry("city", "BJ"));
        assertThat(attributes, hasEntry("street", "WJ"));

        assertThat(attributes, hasEntry("test.id.5", "123"));
        assertThat(attributes, hasEntry("test.name.5", "test"));
        assertThat(attributes, hasEntry("5.city", "BJ"));
        assertThat(attributes, hasEntry("5.street", "WJ"));

        assertThat(attributes.keySet(), hasSize(8));
    }

    @ParameterizedTest(name = "[{index}] with array: {0}")
    @ValueSource(strings = { "false", "true" })
    public void test_processAttributes_array(boolean arr) {
        String json = "{"//
                + "\"id\":123,"//
                + "\"name\":\"test\","//
                + "\"list\":"//
                + " ["//
                + "  \"BJ\","//
                + "  \"WJ\""//
                + " ]" //
                + "}";

        final DocumentContext jsonContext = createJsonContext(json);

        Map<String, JsonPath> jsonPaths = new HashMap<>();
        jsonPaths.put("test.id", JsonPath.compile("$.id"));
        jsonPaths.put("test.name", JsonPath.compile("$.name"));
        jsonPaths.put("test.list", JsonPath.compile("$.list"));

        Map<String, String> attributes = new HashMap<>();

        if (arr) {
            processor.allowArray = true;
        }
        processor.processAttributes(attributes, jsonContext, jsonPaths, -1, null, null);
        processor.processAttributes(attributes, jsonContext, jsonPaths, 5, null, null);

        assertThat(attributes, hasEntry("test.id", "123"));
        assertThat(attributes, hasEntry("test.name", "test"));
        assertThat(attributes, hasEntry("test.id.5", "123"));
        assertThat(attributes, hasEntry("test.name.5", "test"));

        if (arr) {
            assertThat(attributes, hasEntry("test.list.0", "BJ"));
            assertThat(attributes, hasEntry("test.list.1", "WJ"));
            assertThat(attributes, hasEntry("test.list.5.0", "BJ"));
            assertThat(attributes, hasEntry("test.list.5.1", "WJ"));

            assertThat(attributes.keySet(), hasSize(8));
        } else {
            assertThat(attributes.keySet(), hasSize(4));
        }
    }

    @ParameterizedTest(name = "[{index}] with children: {0} and array: {1}")
    @CsvSource({ //
            "false, false", //
            "true, false", //
            "false, true", //
            "true, true", //
    })
    public void test_processAttributes(boolean children, boolean array) {
        String json = "{"//
                + "\"id\":123,"//
                + "\"name\":\"test\","//
                + "\"addr\":"//
                + " {"//
                + "  \"city\":\"BJ\","//
                + "  \"street\":\"WJ\","//
                + "  \"additions\":"//
                + "  {"//
                + "    \"num\":3,"//
                + "    \"ids\":[101,203]"//
                + "   }" //
                + " }" //
                + "}";

        final DocumentContext jsonContext = createJsonContext(json);

        Map<String, JsonPath> jsonPaths = new HashMap<>();
        jsonPaths.put("test.id", JsonPath.compile("$.id"));
        jsonPaths.put("test.name", JsonPath.compile("$.name"));
        jsonPaths.put("test.addr", JsonPath.compile("$.addr")); // children
        jsonPaths.put("my.id", JsonPath.compile("$.addr.additions.ids")); // array
        jsonPaths.put("a.id", JsonPath.compile("$..ids[0]")); // children +array

        Map<String, String> attributes = new HashMap<>();

        if (children) {
            processor.recurseChildren = true;
        }
        if (array) {
            processor.allowArray = true;
        }
        processor.processAttributes(attributes, jsonContext, jsonPaths, -1, null, null);
        processor.processAttributes(attributes, jsonContext, jsonPaths, 5, null, null);

        assertThat(attributes, hasEntry("test.id", "123"));
        assertThat(attributes, hasEntry("test.name", "test"));
        assertThat(attributes, hasEntry("test.addr.city", "BJ"));
        assertThat(attributes, hasEntry("test.addr.street", "WJ"));

        assertThat(attributes, hasEntry("test.id.5", "123"));
        assertThat(attributes, hasEntry("test.name.5", "test"));
        assertThat(attributes, hasEntry("test.addr.5.city", "BJ"));
        assertThat(attributes, hasEntry("test.addr.5.street", "WJ"));

        // not array
        assertThat(attributes, hasEntry("a.id", "101"));
        assertThat(attributes, hasEntry("a.id.5", "101"));

        if (array) {
            assertThat(attributes, hasEntry("my.id.0", "101"));
            assertThat(attributes, hasEntry("my.id.1", "203"));
            assertThat(attributes, hasEntry("my.id.5.0", "101"));
            assertThat(attributes, hasEntry("my.id.5.1", "203"));
        }

        if (children) {
            assertThat(attributes, hasEntry("test.addr.additions.num", "3"));
            assertThat(attributes, hasEntry("test.addr.5.additions.num", "3"));
        }

        if (array && children) {
            assertThat(attributes, hasEntry("test.addr.additions.ids.0", "101"));
            assertThat(attributes, hasEntry("test.addr.additions.ids.1", "203"));

            assertThat(attributes, hasEntry("test.addr.5.additions.ids.0", "101"));
            assertThat(attributes, hasEntry("test.addr.5.additions.ids.1", "203"));

            assertThat(attributes.keySet(), hasSize(20));
        } else if (array) {
            assertThat(attributes.keySet(), hasSize(14));
        } else if (children) {
            assertThat(attributes.keySet(), hasSize(12));
        } else { // no array, no children
            assertThat(attributes.keySet(), hasSize(10));
        }
    }

    @Test
    public void test_processAttributes_includes() {
        String json = "{"//
                + "\"id\":123,"//
                + "\"name\":\"test\","//
                + "\"addr\":"//
                + " {"//
                + "  \"city\":\"BJ\","//
                + "  \"street\":\"WJ\","//
                + "  \"additions\":"//
                + "  {"//
                + "    \"num\":3,"//
                + "    \"ids\":[101,203]"//
                + "   }" //
                + " }" //
                + "}";

        final DocumentContext jsonContext = createJsonContext(json);

        Map<String, JsonPath> jsonPaths = new HashMap<>();
        jsonPaths.put("test.id", JsonPath.compile("$.id"));
        jsonPaths.put("test.name", JsonPath.compile("$.name"));
        jsonPaths.put("test.addr", JsonPath.compile("$.addr")); // children
        jsonPaths.put("my.id", JsonPath.compile("$.addr.additions.ids")); // array
        jsonPaths.put("a.id", JsonPath.compile("$..ids[0]")); // children +array

        List<Pattern> includeFields = new ArrayList<>();
        includeFields.add(Pattern.compile(".*(city|street).*"));
        includeFields.add(Pattern.compile("num"));

        Map<String, String> attributes = new HashMap<>();

        processor.recurseChildren = true;
        processor.allowArray = true;

        processor.processAttributes(attributes, jsonContext, jsonPaths, -1, includeFields, null);
        processor.processAttributes(attributes, jsonContext, jsonPaths, 5, includeFields, null);

        assertThat(attributes, hasEntry("test.addr.city", "BJ"));
        assertThat(attributes, hasEntry("test.addr.street", "WJ"));

        assertThat(attributes, hasEntry("test.addr.5.city", "BJ"));
        assertThat(attributes, hasEntry("test.addr.5.street", "WJ"));

        assertThat(attributes, hasEntry("test.addr.additions.num", "3"));
        assertThat(attributes, hasEntry("test.addr.5.additions.num", "3"));

        assertThat(attributes.keySet(), hasSize(6));
    }

    @Test
    public void test_processAttributes_excludes() {
        String json = "{"//
                + "\"id\":123,"//
                + "\"name\":\"test\","//
                + "\"addr\":"//
                + " {"//
                + "  \"city\":\"BJ\","//
                + "  \"street\":\"WJ\","//
                + "  \"additions\":"//
                + "  {"//
                + "    \"num\":3,"//
                + "    \"ids\":[101,203]"//
                + "   }" //
                + " }" //
                + "}";

        final DocumentContext jsonContext = createJsonContext(json);

        Map<String, JsonPath> jsonPaths = new HashMap<>();
        jsonPaths.put("test.id", JsonPath.compile("$.id"));
        jsonPaths.put("test.name", JsonPath.compile("$.name"));
        jsonPaths.put("test.addr", JsonPath.compile("$.addr")); // children
        jsonPaths.put("my.id", JsonPath.compile("$.addr.additions.ids")); // array
        jsonPaths.put("a.id", JsonPath.compile("$..ids[0]")); // children +array

        List<Pattern> excludeFields = new ArrayList<>();
        excludeFields.add(Pattern.compile(".*(city|street).*"));
        excludeFields.add(Pattern.compile("num"));

        Map<String, String> attributes = new HashMap<>();

        processor.recurseChildren = true;
        processor.allowArray = true;

        processor.processAttributes(attributes, jsonContext, jsonPaths, -1, null, excludeFields);
        processor.processAttributes(attributes, jsonContext, jsonPaths, 5, null, excludeFields);

        assertThat(attributes, hasEntry("test.id", "123"));
        assertThat(attributes, hasEntry("test.name", "test"));
        assertThat(attributes, hasEntry("test.id.5", "123"));
        assertThat(attributes, hasEntry("test.name.5", "test"));

        assertThat(attributes, hasEntry("my.id.0", "101"));
        assertThat(attributes, hasEntry("my.id.1", "203"));
        assertThat(attributes, hasEntry("my.id.5.0", "101"));
        assertThat(attributes, hasEntry("my.id.5.1", "203"));

        assertThat(attributes, hasEntry("a.id", "101"));
        assertThat(attributes, hasEntry("a.id.5", "101"));

        assertThat(attributes, hasEntry("test.addr.additions.ids.0", "101"));
        assertThat(attributes, hasEntry("test.addr.additions.ids.1", "203"));

        assertThat(attributes, hasEntry("test.addr.5.additions.ids.0", "101"));
        assertThat(attributes, hasEntry("test.addr.5.additions.ids.1", "203"));

        assertThat(attributes.keySet(), hasSize(14));
    }

    @Test
    public void test_processAttributes_root() {
        String json = "{"//
                + "\"id\":123,"//
                + "\"name\":\"test\","//
                + "\"addr\":"//
                + " {"//
                + "  \"city\":\"BJ\","//
                + "  \"street\":\"WJ\","//
                + "  \"additions\":"//
                + "  {"//
                + "    \"num\":3,"//
                + "    \"ids\":[101,203]"//
                + "   }" //
                + " }" //
                + "}";

        final DocumentContext jsonContext = createJsonContext(json);

        Map<String, JsonPath> jsonPaths = new HashMap<>();
        jsonPaths.put("test", JsonPath.compile("$"));

        Map<String, String> attributes = new HashMap<>();

        processor.recurseChildren = true;
        processor.allowArray = true;
        processor.processAttributes(attributes, jsonContext, jsonPaths, -1, null, null);
        processor.processAttributes(attributes, jsonContext, jsonPaths, 5, null, null);

        assertThat(attributes, hasEntry("test.id", "123"));
        assertThat(attributes, hasEntry("test.name", "test"));
        assertThat(attributes, hasEntry("test.addr.city", "BJ"));
        assertThat(attributes, hasEntry("test.addr.street", "WJ"));

        assertThat(attributes, hasEntry("test.5.id", "123"));
        assertThat(attributes, hasEntry("test.5.name", "test"));
        assertThat(attributes, hasEntry("test.5.addr.city", "BJ"));
        assertThat(attributes, hasEntry("test.5.addr.street", "WJ"));

        assertThat(attributes, hasEntry("test.addr.additions.num", "3"));
        assertThat(attributes, hasEntry("test.5.addr.additions.num", "3"));

        assertThat(attributes, hasEntry("test.addr.additions.ids.0", "101"));
        assertThat(attributes, hasEntry("test.addr.additions.ids.1", "203"));
        assertThat(attributes, hasEntry("test.5.addr.additions.ids.0", "101"));
        assertThat(attributes, hasEntry("test.5.addr.additions.ids.1", "203"));

        assertThat(attributes.keySet(), hasSize(14));
    }

    @Test
    public void test_processAttributes_root_includes() {
        String json = "{"//
                + "\"id\":123,"//
                + "\"name\":\"test\","//
                + "\"addr\":"//
                + " {"//
                + "  \"city\":\"BJ\","//
                + "  \"street\":\"WJ\","//
                + "  \"additions\":"//
                + "  {"//
                + "    \"num\":3,"//
                + "    \"ids\":[101,203]"//
                + "   }" //
                + " }" //
                + "}";

        final DocumentContext jsonContext = createJsonContext(json);

        Map<String, JsonPath> jsonPaths = new HashMap<>();
        jsonPaths.put("test", JsonPath.compile("$"));

        List<Pattern> includeFields = new ArrayList<>();
        includeFields.add(Pattern.compile(".*(city|street).*"));
        includeFields.add(Pattern.compile("num"));

        Map<String, String> attributes = new HashMap<>();

        processor.recurseChildren = true;
        processor.allowArray = true;

        processor.processAttributes(attributes, jsonContext, jsonPaths, -1, includeFields, null);
        processor.processAttributes(attributes, jsonContext, jsonPaths, 5, includeFields, null);

        assertThat(attributes, hasEntry("test.addr.city", "BJ"));
        assertThat(attributes, hasEntry("test.addr.street", "WJ"));
        assertThat(attributes, hasEntry("test.5.addr.city", "BJ"));
        assertThat(attributes, hasEntry("test.5.addr.street", "WJ"));

        assertThat(attributes, hasEntry("test.addr.additions.num", "3"));
        assertThat(attributes, hasEntry("test.5.addr.additions.num", "3"));

        assertThat(attributes.keySet(), hasSize(6));
    }

    @Test
    public void test_processAttributes_root_excludes() {
        String json = "{"//
                + "\"id\":123,"//
                + "\"name\":\"test\","//
                + "\"addr\":"//
                + " {"//
                + "  \"city\":\"BJ\","//
                + "  \"street\":\"WJ\","//
                + "  \"additions\":"//
                + "  {"//
                + "    \"num\":3,"//
                + "    \"ids\":[101,203]"//
                + "   }" //
                + " }" //
                + "}";

        final DocumentContext jsonContext = createJsonContext(json);

        Map<String, JsonPath> jsonPaths = new HashMap<>();
        jsonPaths.put("test", JsonPath.compile("$"));

        List<Pattern> excludeFields = new ArrayList<>();
        excludeFields.add(Pattern.compile(".*(city|street).*"));
        excludeFields.add(Pattern.compile("num"));

        Map<String, String> attributes = new HashMap<>();

        processor.recurseChildren = true;
        processor.allowArray = true;
        processor.processAttributes(attributes, jsonContext, jsonPaths, -1, null, excludeFields);
        processor.processAttributes(attributes, jsonContext, jsonPaths, 5, null, excludeFields);

        assertThat(attributes, hasEntry("test.id", "123"));
        assertThat(attributes, hasEntry("test.name", "test"));
        assertThat(attributes, hasEntry("test.5.id", "123"));
        assertThat(attributes, hasEntry("test.5.name", "test"));

        assertThat(attributes, hasEntry("test.addr.additions.ids.0", "101"));
        assertThat(attributes, hasEntry("test.addr.additions.ids.1", "203"));
        assertThat(attributes, hasEntry("test.5.addr.additions.ids.0", "101"));
        assertThat(attributes, hasEntry("test.5.addr.additions.ids.1", "203"));

        assertThat(attributes.keySet(), hasSize(8));
    }
}
