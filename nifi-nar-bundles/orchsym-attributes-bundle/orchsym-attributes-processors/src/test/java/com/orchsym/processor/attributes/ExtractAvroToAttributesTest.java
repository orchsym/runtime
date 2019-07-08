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
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.avro.generic.GenericRecord;
import org.apache.nifi.avro.AvroTypeUtil;
import org.apache.nifi.record.path.RecordPath;
import org.apache.nifi.serialization.SimpleRecordSchema;
import org.apache.nifi.serialization.record.MapRecord;
import org.apache.nifi.serialization.record.RecordField;
import org.apache.nifi.serialization.record.RecordFieldType;
import org.apache.nifi.serialization.record.RecordSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author GU Guoqiang
 *
 */
public class ExtractAvroToAttributesTest {
    private ExtractAvroToAttributes processor;

    @BeforeEach
    public void before() {
        processor = new ExtractAvroToAttributes();
    }

    @AfterEach
    public void after() {
        processor = null;
    }

    private MapRecord createRecord() {
        final RecordSchema recordSchema = new SimpleRecordSchema(Arrays.asList(//
                new RecordField("city", RecordFieldType.STRING.getDataType()), //
                new RecordField("street", RecordFieldType.STRING.getDataType()), //
                new RecordField("post", RecordFieldType.INT.getDataType())//
        ));
        Map<String, Object> mapValues = new HashMap<>();
        mapValues.put("city", "BJ");
        mapValues.put("street", "WJ");
        mapValues.put("post", 100000);

        return new MapRecord(recordSchema, mapValues);
    }

    private MapRecord create2LevelRecord() {
        MapRecord childrenRecord = createRecord();
        List<RecordField> fields = new ArrayList<>();
        fields.add(new RecordField("str", RecordFieldType.STRING.getDataType()));
        fields.add(new RecordField("int", RecordFieldType.INT.getDataType()));
        fields.add(new RecordField("bool", RecordFieldType.BOOLEAN.getDataType()));
        fields.add(new RecordField("children", RecordFieldType.RECORD.getRecordDataType(childrenRecord.getSchema()), false));
        RecordSchema schema = new SimpleRecordSchema(fields);

        Map<String, Object> mapValues = new HashMap<>();
        mapValues.put("str", "abc");
        mapValues.put("int", 123);
        mapValues.put("bool", Boolean.TRUE);
        mapValues.put("children", childrenRecord);

        MapRecord record = new MapRecord(schema, mapValues);
        return record;
    }

    private MapRecord create3LevelRecord() {
        final MapRecord detailsRecord = create2LevelRecord();

        List<RecordField> fields = new ArrayList<>();
        fields.add(new RecordField("id", RecordFieldType.INT.getDataType()));
        fields.add(new RecordField("name", RecordFieldType.STRING.getDataType()));
        fields.add(new RecordField("details", RecordFieldType.RECORD.getRecordDataType(detailsRecord.getSchema()), false));

        Map<String, Object> mapValues = new HashMap<>();
        mapValues.put("id", "101");
        mapValues.put("name", "test");
        mapValues.put("details", detailsRecord);

        final SimpleRecordSchema recordSchema = new SimpleRecordSchema(fields);

        return new MapRecord(recordSchema, mapValues);
    }

    @Test
    public void test_setAttributes_Map() {
        doTest_setAttributes_Record(create_setAttributes_Map());
    }

    private Map<String, Object> create_setAttributes_Map() {
        Map<String, Object> mapValues = new HashMap<>();
        mapValues.put("str", " abc ");
        mapValues.put("int", 123);
        mapValues.put("bool", Boolean.TRUE);
        mapValues.put("bytes", "abc".getBytes(StandardCharsets.UTF_8));
        mapValues.put("chars", "123".toCharArray());

        return mapValues;
    }

    @Test
    public void test_setAttributes_Record() {
        List<RecordField> fields = new ArrayList<>();
        fields.add(new RecordField("str", RecordFieldType.STRING.getDataType()));
        fields.add(new RecordField("int", RecordFieldType.INT.getDataType()));
        fields.add(new RecordField("bool", RecordFieldType.BOOLEAN.getDataType()));
        fields.add(new RecordField("bytes", RecordFieldType.STRING.getDataType()));
        fields.add(new RecordField("chars", RecordFieldType.STRING.getDataType()));
        RecordSchema schema = new SimpleRecordSchema(fields);

        MapRecord record = new MapRecord(schema, create_setAttributes_Map());

        doTest_setAttributes_Record(record);

    }

    private void doTest_setAttributes_Record(Object value) {
        Map<String, String> attributes = new HashMap<>();

        processor.setAttributes(attributes, "orchsym.", value, -1, null, null);
        processor.setAttributes(attributes, "orchsym.", value, 3, null, null);

        assertThat(attributes, hasEntry("orchsym.str", " abc "));
        assertThat(attributes, hasEntry("orchsym.int", "123"));
        assertThat(attributes, hasEntry("orchsym.bool", "true"));
        assertThat(attributes, hasEntry("orchsym.bytes", "abc"));
        assertThat(attributes, hasEntry("orchsym.chars", "123"));

        assertThat(attributes, hasEntry("orchsym.str.3", " abc "));
        assertThat(attributes, hasEntry("orchsym.int.3", "123"));
        assertThat(attributes, hasEntry("orchsym.bool.3", "true"));
        assertThat(attributes, hasEntry("orchsym.bytes.3", "abc"));
        assertThat(attributes, hasEntry("orchsym.chars.3", "123"));
    }

    @ParameterizedTest(name = "[{index}] with children: {0}")
    @ValueSource(strings = { "false", "true" })
    public void test_setAttributes_RecordWithChildren(boolean withChildren) {
        MapRecord record = create2LevelRecord();

        Map<String, String> attributes = new HashMap<>();

        if (withChildren) {
            processor.recurseChildren = true;
        }
        processor.setAttributes(attributes, "orchsym.", record, -1, null, null);
        processor.setAttributes(attributes, "test.", record, 3, null, null);

        assertThat(attributes, hasEntry("orchsym.str", "abc"));
        assertThat(attributes, hasEntry("orchsym.int", "123"));
        assertThat(attributes, hasEntry("orchsym.bool", "true"));

        assertThat(attributes, hasEntry("test.str.3", "abc"));
        assertThat(attributes, hasEntry("test.int.3", "123"));
        assertThat(attributes, hasEntry("test.bool.3", "true"));

        if (withChildren) {
            assertThat(attributes.keySet(), hasSize(12));
            assertThat(attributes, hasEntry("orchsym.children.city", "BJ"));
            assertThat(attributes, hasEntry("orchsym.children.street", "WJ"));
            assertThat(attributes, hasEntry("orchsym.children.post", "100000"));

            assertThat(attributes, hasEntry("test.children.3.city", "BJ"));
            assertThat(attributes, hasEntry("test.children.3.street", "WJ"));
            assertThat(attributes, hasEntry("test.children.3.post", "100000"));
        } else {
            assertThat(attributes.keySet(), hasSize(6));
            assertThat(attributes, not(hasKey("orchsym.children.city")));
            assertThat(attributes, not(hasKey("orchsym.children.street")));
            assertThat(attributes, not(hasKey("orchsym.children.post")));
        }
    }

    @Test
    public void test_setAttributes_ArrayWithRecord() {
        doTest_setAttributes_ListWithRecord(true);
    }

    @Test
    public void test_setAttributes_ListWithRecord() {
        doTest_setAttributes_ListWithRecord(false);
    }

    private void doTest_setAttributes_ListWithRecord(boolean array) {
        List<RecordField> fields = new ArrayList<>();
        fields.add(new RecordField("city", RecordFieldType.STRING.getDataType()));
        fields.add(new RecordField("street", RecordFieldType.STRING.getDataType()));
        fields.add(new RecordField("post", RecordFieldType.INT.getDataType()));
        RecordSchema schema = new SimpleRecordSchema(fields);

        Map<String, Object> mapValues = new HashMap<>();
        mapValues.put("city", "BJ");
        mapValues.put("street", "WJ");
        mapValues.put("post", 100000);
        MapRecord record = new MapRecord(schema, mapValues);

        List<Object> data = new ArrayList<>();
        data.add("abc");
        data.add(123);
        data.add(true);
        data.add(record);

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

    @Test
    public void test_processAttributes_noPath() {
        Map<String, String> attributes = new HashMap<>();
        processor.processAttributes(attributes, null, null, null, -1, null, null);
        assertThat(attributes.keySet(), empty());
    }

    @Test
    public void test_processAttributes_pathToElement() throws IOException {
        final MapRecord record = create2LevelRecord();
        final RecordSchema recordSchema = record.getSchema();
        final GenericRecord avroRecord = AvroTypeUtil.createAvroRecord(record, AvroTypeUtil.extractAvroSchema(recordSchema));

        Map<String, String> attributes = new HashMap<>();

        processor.processAttributes(attributes, avroRecord, recordSchema, Collections.emptyMap(), -1, null, null);
        assertThat(attributes.keySet(), empty());

        Map<String, RecordPath> recordPaths = new HashMap<>();
        recordPaths.put("my.bool", RecordPath.compile("/bool"));
        recordPaths.put("my.addr.city", RecordPath.compile("/children/city"));
        recordPaths.put("my.addr.street", RecordPath.compile("/children/street"));
        recordPaths.put("my.addr.post", RecordPath.compile("/children/post"));

        processor.processAttributes(attributes, avroRecord, recordSchema, recordPaths, -1, null, null);

        assertThat(attributes.keySet(), hasSize(4));
        assertThat(attributes, hasEntry("my.bool", "true"));
        assertThat(attributes, hasEntry("my.addr.city", "BJ"));
        assertThat(attributes, hasEntry("my.addr.street", "WJ"));
        assertThat(attributes, hasEntry("my.addr.post", "100000"));
    }

    @Test
    public void test_processAttributes_pathToRecord() throws IOException {
        final MapRecord record = create2LevelRecord();
        final RecordSchema recordSchema = record.getSchema();
        final GenericRecord avroRecord = AvroTypeUtil.createAvroRecord(record, AvroTypeUtil.extractAvroSchema(recordSchema));

        Map<String, String> attributes = new HashMap<>();

        processor.processAttributes(attributes, avroRecord, recordSchema, Collections.emptyMap(), -1, null, null);
        assertThat(attributes.keySet(), empty());

        Map<String, RecordPath> recordPaths = new HashMap<>();
        recordPaths.put("my.str", RecordPath.compile("/str"));
        recordPaths.put("my.addr", RecordPath.compile("/children"));

        processor.processAttributes(attributes, avroRecord, recordSchema, recordPaths, -1, null, null);

        assertThat(attributes.keySet(), hasSize(4));
        assertThat(attributes, hasEntry("my.str", "abc"));
        assertThat(attributes, hasEntry("my.addr.city", "BJ"));
        assertThat(attributes, hasEntry("my.addr.street", "WJ"));
        assertThat(attributes, hasEntry("my.addr.post", "100000"));
    }

    @Test
    public void test_processAttributes_pathToRecord_index() throws IOException {
        final MapRecord record = create2LevelRecord();
        final RecordSchema recordSchema = record.getSchema();
        final GenericRecord avroRecord = AvroTypeUtil.createAvroRecord(record, AvroTypeUtil.extractAvroSchema(recordSchema));

        Map<String, String> attributes = new HashMap<>();

        processor.processAttributes(attributes, avroRecord, recordSchema, Collections.emptyMap(), -1, null, null);
        assertThat(attributes.keySet(), empty());

        Map<String, RecordPath> recordPaths = new HashMap<>();
        recordPaths.put("my.str", RecordPath.compile("/str"));
        recordPaths.put("my.addr", RecordPath.compile("/children"));

        processor.processAttributes(attributes, avroRecord, recordSchema, recordPaths, 3, null, null);

        assertThat(attributes.keySet(), hasSize(4));
        assertThat(attributes, hasEntry("my.str.3", "abc"));
        assertThat(attributes, hasEntry("my.addr.3.city", "BJ"));
        assertThat(attributes, hasEntry("my.addr.3.street", "WJ"));
        assertThat(attributes, hasEntry("my.addr.3.post", "100000"));
    }

    @ParameterizedTest(name = "[{index}] with children: {0}")
    @ValueSource(strings = { "false", "true" })
    public void test_processAttributes_pathToRecordWithChildren(boolean withChildren) throws IOException {
        final MapRecord record = create3LevelRecord();
        final RecordSchema recordSchema = record.getSchema();
        final GenericRecord avroRecord = AvroTypeUtil.createAvroRecord(record, AvroTypeUtil.extractAvroSchema(recordSchema));

        Map<String, String> attributes = new HashMap<>();

        Map<String, RecordPath> recordPaths = new HashMap<>();
        recordPaths.put("my.id", RecordPath.compile("/id"));
        recordPaths.put("my.name", RecordPath.compile("/name"));
        recordPaths.put("my.details", RecordPath.compile("/details"));

        if (withChildren) {
            processor.recurseChildren = true;
        }
        processor.processAttributes(attributes, avroRecord, recordSchema, recordPaths, -1, null, null);
        processor.processAttributes(attributes, avroRecord, recordSchema, recordPaths, 5, null, null);

        assertThat(attributes, hasEntry("my.id", "101"));
        assertThat(attributes, hasEntry("my.name", "test"));
        assertThat(attributes, hasEntry("my.id.5", "101"));
        assertThat(attributes, hasEntry("my.name.5", "test"));

        assertThat(attributes, hasEntry("my.details.str", "abc"));
        assertThat(attributes, hasEntry("my.details.int", "123"));
        assertThat(attributes, hasEntry("my.details.bool", "true"));
        assertThat(attributes, hasEntry("my.details.5.str", "abc"));
        assertThat(attributes, hasEntry("my.details.5.int", "123"));
        assertThat(attributes, hasEntry("my.details.5.bool", "true"));

        if (withChildren) {
            assertThat(attributes.keySet(), hasSize(16));

            assertThat(attributes, hasEntry("my.details.children.city", "BJ"));
            assertThat(attributes, hasEntry("my.details.children.street", "WJ"));
            assertThat(attributes, hasEntry("my.details.children.post", "100000"));
            assertThat(attributes, hasEntry("my.details.5.children.city", "BJ"));
            assertThat(attributes, hasEntry("my.details.5.children.street", "WJ"));
            assertThat(attributes, hasEntry("my.details.5.children.post", "100000"));
        } else {
            assertThat(attributes.keySet(), hasSize(10));

        }
    }

    @Test
    public void test_processAttributes_pathToRoot() throws IOException {
        final MapRecord record = create3LevelRecord();
        final RecordSchema recordSchema = record.getSchema();
        final GenericRecord avroRecord = AvroTypeUtil.createAvroRecord(record, AvroTypeUtil.extractAvroSchema(recordSchema));

        Map<String, String> attributes = new HashMap<>();

        Map<String, RecordPath> recordPaths = new HashMap<>();
        recordPaths.put("my", RecordPath.compile("/"));

        processor.recurseChildren = true;
        processor.processAttributes(attributes, avroRecord, recordSchema, recordPaths, -1, null, null);
        processor.processAttributes(attributes, avroRecord, recordSchema, recordPaths, 5, null, null);

        assertThat(attributes, hasEntry("my.id", "101"));
        assertThat(attributes, hasEntry("my.name", "test"));
        assertThat(attributes, hasEntry("my.5.id", "101"));
        assertThat(attributes, hasEntry("my.5.name", "test"));

        assertThat(attributes, hasEntry("my.details.str", "abc"));
        assertThat(attributes, hasEntry("my.details.int", "123"));
        assertThat(attributes, hasEntry("my.details.bool", "true"));
        assertThat(attributes, hasEntry("my.5.details.str", "abc"));
        assertThat(attributes, hasEntry("my.5.details.int", "123"));
        assertThat(attributes, hasEntry("my.5.details.bool", "true"));

        assertThat(attributes.keySet(), hasSize(16));

        assertThat(attributes, hasEntry("my.details.children.city", "BJ"));
        assertThat(attributes, hasEntry("my.details.children.street", "WJ"));
        assertThat(attributes, hasEntry("my.details.children.post", "100000"));
        assertThat(attributes, hasEntry("my.5.details.children.city", "BJ"));
        assertThat(attributes, hasEntry("my.5.details.children.street", "WJ"));
        assertThat(attributes, hasEntry("my.5.details.children.post", "100000"));
    }

    @Test
    public void test_processAttributes_pathToRoot_excludes() throws IOException {
        final MapRecord record = create3LevelRecord();
        final RecordSchema recordSchema = record.getSchema();
        final GenericRecord avroRecord = AvroTypeUtil.createAvroRecord(record, AvroTypeUtil.extractAvroSchema(recordSchema));

        Map<String, String> attributes = new HashMap<>();

        Map<String, RecordPath> recordPaths = new HashMap<>();
        recordPaths.put("my", RecordPath.compile("/"));

        final List<Pattern> excludeFields = new ArrayList<>();
        excludeFields.add(Pattern.compile("^i.*"));
        excludeFields.add(Pattern.compile("post"));

        processor.recurseChildren = true;
        processor.processAttributes(attributes, avroRecord, recordSchema, recordPaths, -1, null, excludeFields);
        processor.processAttributes(attributes, avroRecord, recordSchema, recordPaths, 5, null, excludeFields);

        doTest_processAttributes_pathToRoot_excludes(attributes, "my.");
    }

    @Test
    public void test_processAttributes_pathToRoot_excludes_noAttrName() throws IOException {
        final MapRecord record = create3LevelRecord();
        final RecordSchema recordSchema = record.getSchema();
        final GenericRecord avroRecord = AvroTypeUtil.createAvroRecord(record, AvroTypeUtil.extractAvroSchema(recordSchema));

        Map<String, String> attributes = new HashMap<>();

        Map<String, RecordPath> recordPaths = new HashMap<>();
        recordPaths.put("my", RecordPath.compile("/"));

        final List<Pattern> excludeFields = new ArrayList<>();
        excludeFields.add(Pattern.compile("^i.*"));
        excludeFields.add(Pattern.compile("post"));

        processor.recurseChildren = true;
        processor.containPropName = false;

        processor.processAttributes(attributes, avroRecord, recordSchema, recordPaths, -1, null, excludeFields);
        processor.processAttributes(attributes, avroRecord, recordSchema, recordPaths, 5, null, excludeFields);

        doTest_processAttributes_pathToRoot_excludes(attributes, "");
    }

    private void doTest_processAttributes_pathToRoot_excludes(Map<String, String> attributes, String prefix) {
        assertThat(attributes, hasEntry(prefix + "name", "test"));
        assertThat(attributes, hasEntry(prefix + "5.name", "test"));

        assertThat(attributes, hasEntry(prefix + "details.str", "abc"));
        assertThat(attributes, hasEntry(prefix + "details.bool", "true"));
        assertThat(attributes, hasEntry(prefix + "5.details.str", "abc"));
        assertThat(attributes, hasEntry(prefix + "5.details.bool", "true"));

        assertThat(attributes.keySet(), hasSize(10));

        assertThat(attributes, hasEntry(prefix + "details.children.city", "BJ"));
        assertThat(attributes, hasEntry(prefix + "details.children.street", "WJ"));
        assertThat(attributes, hasEntry(prefix + "5.details.children.city", "BJ"));
        assertThat(attributes, hasEntry(prefix + "5.details.children.street", "WJ"));
    }

    @Test
    public void test_processAttributes_pathToRoot_includes() throws IOException {
        final MapRecord record = create3LevelRecord();
        final RecordSchema recordSchema = record.getSchema();
        final GenericRecord avroRecord = AvroTypeUtil.createAvroRecord(record, AvroTypeUtil.extractAvroSchema(recordSchema));

        Map<String, String> attributes = new HashMap<>();

        Map<String, RecordPath> recordPaths = new HashMap<>();
        recordPaths.put("my", RecordPath.compile("/"));

        final List<Pattern> includeFields = new ArrayList<>();
        includeFields.add(Pattern.compile("^i.*"));
        includeFields.add(Pattern.compile("city"));
        includeFields.add(Pattern.compile("street"));

        processor.recurseChildren = true;
        processor.processAttributes(attributes, avroRecord, recordSchema, recordPaths, -1, includeFields, null);
        processor.processAttributes(attributes, avroRecord, recordSchema, recordPaths, 5, includeFields, null);

        assertThat(attributes, hasEntry("my.id", "101"));
        assertThat(attributes, hasEntry("my.5.id", "101"));

        assertThat(attributes, hasEntry("my.details.int", "123"));
        assertThat(attributes, hasEntry("my.5.details.int", "123"));

        assertThat(attributes.keySet(), hasSize(8));

        assertThat(attributes, hasEntry("my.details.children.city", "BJ"));
        assertThat(attributes, hasEntry("my.details.children.street", "WJ"));
        assertThat(attributes, hasEntry("my.5.details.children.city", "BJ"));
        assertThat(attributes, hasEntry("my.5.details.children.street", "WJ"));
    }

    @Test
    public void test_processAttributes_includes() throws IOException {
        final MapRecord record = create3LevelRecord();
        final RecordSchema recordSchema = record.getSchema();
        final GenericRecord avroRecord = AvroTypeUtil.createAvroRecord(record, AvroTypeUtil.extractAvroSchema(recordSchema));

        Map<String, String> attributes = new HashMap<>();

        Map<String, RecordPath> recordPaths = new HashMap<>();
        recordPaths.put("my.id", RecordPath.compile("/id"));
        recordPaths.put("my.details", RecordPath.compile("/details"));

        final List<Pattern> includeFields = new ArrayList<>();
        includeFields.add(Pattern.compile("^i.*"));
        includeFields.add(Pattern.compile("city"));
        includeFields.add(Pattern.compile("street"));

        processor.recurseChildren = true;
        processor.processAttributes(attributes, avroRecord, recordSchema, recordPaths, -1, includeFields, null);
        processor.processAttributes(attributes, avroRecord, recordSchema, recordPaths, 5, includeFields, null);

        assertThat(attributes, hasEntry("my.id", "101"));
        assertThat(attributes, hasEntry("my.id.5", "101"));

        assertThat(attributes, hasEntry("my.details.int", "123"));
        assertThat(attributes, hasEntry("my.details.5.int", "123"));

        assertThat(attributes.keySet(), hasSize(8));

        assertThat(attributes, hasEntry("my.details.children.city", "BJ"));
        assertThat(attributes, hasEntry("my.details.children.street", "WJ"));
        assertThat(attributes, hasEntry("my.details.5.children.city", "BJ"));
        assertThat(attributes, hasEntry("my.details.5.children.street", "WJ"));
    }

    @Test
    public void test_processAttributes_includes_noAttrName() throws IOException {
        final MapRecord record = create3LevelRecord();
        final RecordSchema recordSchema = record.getSchema();
        final GenericRecord avroRecord = AvroTypeUtil.createAvroRecord(record, AvroTypeUtil.extractAvroSchema(recordSchema));

        Map<String, String> attributes = new HashMap<>();

        Map<String, RecordPath> recordPaths = new HashMap<>();
        recordPaths.put("my.id", RecordPath.compile("/id"));
        recordPaths.put("my.details", RecordPath.compile("/details"));

        final List<Pattern> includeFields = new ArrayList<>();
        includeFields.add(Pattern.compile("^i.*"));
        includeFields.add(Pattern.compile("city"));
        includeFields.add(Pattern.compile("street"));

        processor.recurseChildren = true;
        processor.containPropName = false;
        processor.processAttributes(attributes, avroRecord, recordSchema, recordPaths, -1, includeFields, null);
        processor.processAttributes(attributes, avroRecord, recordSchema, recordPaths, 5, includeFields, null);

        assertThat(attributes, hasEntry("my.id", "101"));
        assertThat(attributes, hasEntry("my.id.5", "101"));

        assertThat(attributes, hasEntry("int", "123"));
        assertThat(attributes, hasEntry("5.int", "123"));

        assertThat(attributes.keySet(), hasSize(8));

        assertThat(attributes, hasEntry("children.city", "BJ"));
        assertThat(attributes, hasEntry("children.street", "WJ"));
        assertThat(attributes, hasEntry("5.children.city", "BJ"));
        assertThat(attributes, hasEntry("5.children.street", "WJ"));
    }

    @Test
    public void test_processAttributes_excludes() throws IOException {
        final MapRecord record = create3LevelRecord();
        final RecordSchema recordSchema = record.getSchema();
        final GenericRecord avroRecord = AvroTypeUtil.createAvroRecord(record, AvroTypeUtil.extractAvroSchema(recordSchema));

        Map<String, String> attributes = new HashMap<>();

        Map<String, RecordPath> recordPaths = new HashMap<>();
        recordPaths.put("my.id", RecordPath.compile("/id"));
        recordPaths.put("my.details", RecordPath.compile("/details"));

        final List<Pattern> excludeFields = new ArrayList<>();
        excludeFields.add(Pattern.compile("^i.*"));
        excludeFields.add(Pattern.compile("city"));
        excludeFields.add(Pattern.compile("street"));

        processor.recurseChildren = true;
        processor.processAttributes(attributes, avroRecord, recordSchema, recordPaths, -1, null, excludeFields);
        processor.processAttributes(attributes, avroRecord, recordSchema, recordPaths, 5, null, excludeFields);

        assertThat(attributes, hasEntry("my.details.str", "abc"));
        assertThat(attributes, hasEntry("my.details.bool", "true"));
        assertThat(attributes, hasEntry("my.details.5.str", "abc"));
        assertThat(attributes, hasEntry("my.details.5.bool", "true"));

        assertThat(attributes.keySet(), hasSize(6));

        // assertThat(attributes, hasEntry("my.details.children.city", "BJ"));
        // assertThat(attributes, hasEntry("my.details.children.street", "WJ"));
        assertThat(attributes, hasEntry("my.details.children.post", "100000"));
        // assertThat(attributes, hasEntry("my.details.5.children.city", "BJ"));
        // assertThat(attributes, hasEntry("my.details.5.children.street", "WJ"));
        assertThat(attributes, hasEntry("my.details.5.children.post", "100000"));
    }

    @Test
    public void test_processAttributes_pathNotFound() throws IOException {
        final MapRecord record = create3LevelRecord();
        final RecordSchema recordSchema = record.getSchema();
        final GenericRecord avroRecord = AvroTypeUtil.createAvroRecord(record, AvroTypeUtil.extractAvroSchema(recordSchema));

        Map<String, String> attributes = new HashMap<>();

        Map<String, RecordPath> recordPaths = new HashMap<>();
        recordPaths.put("my.id", RecordPath.compile("/id"));
        recordPaths.put("my.name", RecordPath.compile("/name"));
        recordPaths.put("my.age", RecordPath.compile("/age"));
        recordPaths.put("my.country", RecordPath.compile("/details/children/country"));

        processor.recurseChildren = true;
        processor.processAttributes(attributes, avroRecord, recordSchema, recordPaths, -1, null, null);
        processor.processAttributes(attributes, avroRecord, recordSchema, recordPaths, 5, null, null);

        assertThat(attributes, hasEntry("my.id", "101"));
        assertThat(attributes, hasEntry("my.name", "test"));
        assertThat(attributes, hasEntry("my.id.5", "101"));
        assertThat(attributes, hasEntry("my.name.5", "test"));

        assertThat(attributes.keySet(), hasSize(8));

        // not existed, will be empty
        assertThat(attributes, hasEntry("my.age", ""));
        assertThat(attributes, hasEntry("my.country", ""));
        assertThat(attributes, hasEntry("my.age.5", ""));
        assertThat(attributes, hasEntry("my.country.5", ""));
    }
}
