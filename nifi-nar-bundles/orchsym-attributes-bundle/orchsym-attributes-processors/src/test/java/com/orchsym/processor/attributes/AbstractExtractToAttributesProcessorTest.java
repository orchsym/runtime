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

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author GU Guoqiang
 *
 */
public class AbstractExtractToAttributesProcessorTest {
    private AbstractExtractToAttributesProcessor processor;

    @BeforeEach
    public void before() {
        processor = new AbstractExtractToAttributesProcessor() {
        };
    }

    @AfterEach
    public void after() {
        processor = null;
    }

    @Test
    public void test_match_noPatterns() {
        assertFalse(processor.match(Collections.emptyList(), ""));
        assertFalse(processor.match(Collections.emptyList(), "ABC"));
        assertFalse(processor.match(null, "ABC"));
        assertFalse(processor.match(Collections.emptyList(), null));
    }

    @ParameterizedTest(name = "{index} ==> pattern=''{0}'', field={1}, case-sensitive= {2}")
    @CsvSource({ //
            "abc, abc, true, true", //
            "abc, ABC, false, true", //
            "abc, ABC, true, false", //

            "123, 123, true, true", //

            "A-123, ABC, true, false", //
            "A-123, a-123, false, true", //
            "A_123, a_123, false, true", //
            "A_123, a_123, true, false", //
    })
    public void test_match_literal(String pattern, String field, boolean caseSensitive, boolean matched) {
        final List<Pattern> list = Arrays.asList(pattern).stream().map(s -> Pattern.compile(s, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE)).collect(Collectors.toList());
        if (matched) {
            assertTrue(processor.match(list, field));
        } else {
            assertFalse(processor.match(list, field));
        }
    }

    @ParameterizedTest
    @CsvSource({ //
            "abc, false", //
            "ABC, true", //
            "a-123, false", //
            "a_123, false", //
            "A-123, true", //
            "B_xyz, true", //
            "b_xyz, false", //
    })
    public void test_match_literal_batch(String field, boolean matched) {
        final List<Pattern> list = Arrays.asList("ABC", "A-123", "B_xyz").stream().map(s -> Pattern.compile(s)).collect(Collectors.toList());
        if (matched) {
            assertTrue(processor.match(list, field));
        } else {
            assertFalse(processor.match(list, field));
        }
    }

    @ParameterizedTest
    @CsvSource({ //
            "XYZ, true", //
            "xyz, false", //
            "abc, false", //
            "ABC, true", //
            "AB, false", //
            "ABCD, true", //
            "-ABC, true", //
            "a-123, false", //
            "A_123, true", //
            "A-123, true", //
            "A-12345, true", //
            "B_xyz, true", //
            "b_xyz, false", //
            "B_xyz123, true", //
    })
    public void test_patterns(String field, boolean matched) {
        final List<Pattern> list = Arrays.asList("XYZ", ".*ABC.*", "A[-_]\\d+", "B_\\w+").stream().map(s -> Pattern.compile(s)).collect(Collectors.toList());
        if (matched) {
            assertTrue(processor.match(list, field));

            assertFalse(processor.ignoreField(field, list, null));
            assertTrue(processor.ignoreField(field, null, list));
        } else {
            assertFalse(processor.match(list, field));

            assertTrue(processor.ignoreField(field, list, null));
            assertFalse(processor.ignoreField(field, null, list));
        }
    }

    @Test
    public void test_ignoreField_no() {
        assertTrue(processor.ignoreField(null, null, null));
        assertTrue(processor.ignoreField("", null, null));
        assertTrue(processor.ignoreField("  ", null, null));

        assertFalse(processor.ignoreField("ABC", null, null));
        assertFalse(processor.ignoreField("ABC", Collections.emptyList(), Collections.emptyList()));
    }

    public void test_ignoreField_includes(String field, boolean ignore) {
        // use the same as "test_match_pattern"
    }

    public void test_ignoreField_excludes(String field, boolean ignore) {
        // use the same as "test_match_pattern"
    }

    @ParameterizedTest
    @CsvSource({ //
            "XYZ, false", // in includes
            "xyz, true", // in excludes
            "abc, true", // not in includes
            "ABC, false", // in includes
            "AB, true", // not in includes
            "ABCD, true", // in excludes
            "-ABC, false", // in includes
            "a-123, true", // not in includes
            "A_123, false", // in includes
            "A-123, false", // in includes
            "A-12345, false", // in includes
            "B_xyz, false", // in includes
            "b_xyz, true", // in excludes
            "B_xyz123, false", // in includes
    })
    public void test_ignoreField_all(String field, boolean ignore) {
        final List<Pattern> includes = Arrays.asList("XYZ", ".*ABC.*", "A[-_]\\d+", "B_\\w+").stream().map(s -> Pattern.compile(s)).collect(Collectors.toList());
        final List<Pattern> excludes = Arrays.asList("xyz", ".*D$", "b_\\w+").stream().map(s -> Pattern.compile(s)).collect(Collectors.toList());

        final boolean ignoreField = processor.ignoreField(field, includes, excludes);
        if (ignore) {
            assertTrue(ignoreField);
        } else {
            assertFalse(ignoreField);
        }
    }

    @Test
    public void test_setFieldAttribute_normal() {
        Map<String, String> attributes = new HashMap<>();

        processor.setFieldAttribute(attributes, "test.", "abc", "123", -1);
        processor.setFieldAttribute(attributes, "test.", "xyz", null, -1);
        processor.setFieldAttribute(attributes, "test.", "xyz", "   ", 1);
        processor.setFieldAttribute(attributes, " test.", "xyz ", null, 2);
        processor.setFieldAttribute(attributes, "test@", "xyz", null, 3);
        processor.setFieldAttribute(attributes, "test.", "zzz", "ZZzzz...  ", -1);
        processor.setFieldAttribute(attributes, "test.aaa", null, "none", -1);

        processor.setFieldAttribute(attributes, "orchsym.", "abc", "xyz", 0);
        processor.setFieldAttribute(attributes, "orchsym.", "abc", 123, 8);
        processor.setFieldAttribute(attributes, "orchsym.", "abc", 1.1, 10);
        processor.setFieldAttribute(attributes, "orchsym.", "abc", Boolean.TRUE, 20);
        processor.setFieldAttribute(attributes, "orchsym.", "abc", new byte[] { (byte) 'x', (byte) 'y', (byte) 'z' }, 30);
        processor.setFieldAttribute(attributes, "orchsym.", "abc", new char[] { (char) '1', (char) '2', (char) '3' }, 40);

        assertThat(attributes, hasEntry("test.abc", "123"));
        assertThat(attributes, hasEntry("test.xyz", ""));
        assertThat(attributes, hasEntry("test.xyz.1", "   "));
        assertThat(attributes, hasEntry("test.xyz.2", ""));
        assertThat(attributes, hasEntry("test@.xyz.3", ""));
        assertThat(attributes, hasEntry("test.zzz", "ZZzzz...  "));
        assertThat(attributes, hasEntry("test.aaa", "none"));

        assertThat(attributes, hasEntry("orchsym.abc.0", "xyz"));
        assertThat(attributes, hasEntry("orchsym.abc.8", "123"));
        assertThat(attributes, hasEntry("orchsym.abc.10", "1.1"));
        assertThat(attributes, hasEntry("orchsym.abc.20", "true"));
        assertThat(attributes, hasEntry("orchsym.abc.30", "xyz"));
        assertThat(attributes, hasEntry("orchsym.abc.40", "123"));
    }

    @ParameterizedTest(name = "[{index}] allow array: {0}")
    @ValueSource(strings = { "false", "true" })
    public void test_setAttributes_Array(boolean allowArray) {
        doTest_setAttributes_List(allowArray, true);
    }

    @ParameterizedTest(name = "[{index}] allow array: {0}")
    @ValueSource(strings = { "false", "true" })
    public void test_setAttributes_List(boolean allowArray) {
        doTest_setAttributes_List(allowArray, false);
    }

    private void doTest_setAttributes_List(boolean allowArray, boolean array) {
        List<Object> data = new ArrayList<>();
        data.add("abc");
        data.add(123);
        data.add(true);
        data.add("zzz".getBytes());

        Map<String, String> attributes = new HashMap<>();

        if (allowArray) {
            processor.allowArray = true;
        }
        if (array) {
            processor.setAttributes(attributes, "test", data.toArray(), -1, null, null);
            processor.setAttributes(attributes, "test", data.toArray(), 3, null, null);
        } else {
            processor.setAttributes(attributes, "test", data, -1, null, null);
            processor.setAttributes(attributes, "test", data, 3, null, null);
        }
        if (allowArray) {
            assertThat(attributes, hasEntry("test.0", "abc"));
            assertThat(attributes, hasEntry("test.1", "123"));
            assertThat(attributes, hasEntry("test.2", "true"));
            assertThat(attributes, hasEntry("test.3", "zzz"));

            assertThat(attributes, hasEntry("test.3.0", "abc"));
            assertThat(attributes, hasEntry("test.3.1", "123"));
            assertThat(attributes, hasEntry("test.3.2", "true"));
            assertThat(attributes, hasEntry("test.3.3", "zzz"));
        } else {
            assertThat(attributes.keySet(), empty());
        }
    }

    @Test
    public void test_setAttributes_ArrayWithMap() {
        doTest_setAttributes_ListWithMap(true);
    }

    @Test
    public void test_setAttributes_ListWithMap() {
        doTest_setAttributes_ListWithMap(false);
    }

    private void doTest_setAttributes_ListWithMap(boolean array) {
        List<Object> data = new ArrayList<>();
        data.add("abc");
        data.add(123);
        data.add(true);
        Map<String, Object> mapValues = new HashMap<>();
        mapValues.put("city", "BJ");
        mapValues.put("street", "WJ");
        mapValues.put("post", 100000);
        data.add(mapValues);

        if (array) {
            doTest_setAttributes_ListMap(data.toArray());
        } else {
            doTest_setAttributes_ListMap(data);
        }
    }

    private void doTest_setAttributes_ListMap(Object data) {
        Map<String, String> attributes = new HashMap<>();

        processor.allowArray = true;
        processor.setAttributes(attributes, "test", data, -1, null, null);
        processor.setAttributes(attributes, "test", data, 5, null, null);

        assertThat(attributes, hasEntry("test.0", "abc"));
        assertThat(attributes, hasEntry("test.1", "123"));
        assertThat(attributes, hasEntry("test.2", "true"));

        assertThat(attributes, hasEntry("test.5.0", "abc"));
        assertThat(attributes, hasEntry("test.5.1", "123"));
        assertThat(attributes, hasEntry("test.5.2", "true"));

        assertThat(attributes, hasEntry("test.city.3", "BJ"));
        assertThat(attributes, hasEntry("test.street.3", "WJ"));
        assertThat(attributes, hasEntry("test.post.3", "100000"));

        assertThat(attributes, hasEntry("test.5.city.3", "BJ"));
        assertThat(attributes, hasEntry("test.5.street.3", "WJ"));
        assertThat(attributes, hasEntry("test.5.post.3", "100000"));
    }
}
