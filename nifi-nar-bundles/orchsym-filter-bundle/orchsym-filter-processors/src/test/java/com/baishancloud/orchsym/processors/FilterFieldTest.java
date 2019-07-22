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
package com.baishancloud.orchsym.processors;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericData.Record;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.nifi.util.TestRunners;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kitesdk.data.spi.DataModelUtil;
import org.kitesdk.data.spi.JsonUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minidev.json.parser.ParseException;

public class FilterFieldTest {

    private Schema dataAvroSchema;
    private String dataContents;
    private byte[] dataRecordBytes;

    @Before
    public void init() throws IOException {
        dataAvroSchema = new Schema.Parser().parse(TestRunners.loadContents("filter/test.avsc"));
        dataContents = TestRunners.loadContents("filter/test.json");

        final GenericData model = DataModelUtil.getDataModelForType(Record.class);
        final JsonNode jsonNode = JsonUtil.parse(dataContents);
        Record testDataRecord = (Record) JsonUtil.convertToAvro(model, jsonNode, dataAvroSchema);

        dataRecordBytes = bytesFor(Arrays.asList(testDataRecord));

    }

    @SuppressWarnings({ "unchecked", "resource" })
    public byte[] bytesFor(List<Record> records) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        final Schema schema = records.get(0).getSchema();

        DataFileWriter<Record> writer = null;
        try {
            writer = new DataFileWriter<>(GenericData.get().createDatumWriter(schema));
            writer.setCodec(CodecFactory.snappyCodec());
            writer = writer.create(schema, out);

            for (Record record : records) {
                writer.append(record);
            }
            writer.flush();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }

        return out.toByteArray();
    }

    public JsonArray convertAvroToJson(final InputStream in) throws IOException, ParseException {
        final GenericData genericData = GenericData.get();
        JsonArray array = new JsonArray();

        try (final DataFileStream<GenericRecord> reader = new DataFileStream<>(in, new GenericDatumReader<GenericRecord>())) {
            GenericRecord currRecord = null;
            while (reader.hasNext()) {
                currRecord = reader.next(currRecord);
                JsonElement json = new JsonParser().parse(genericData.toString(currRecord));
                array.add(json);
            }
        }
        return array;

    }

    // @Test //seems can't work for change schema
    public void testProcessor() throws IOException, ParseException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        processsAvro(new ByteArrayInputStream(dataRecordBytes), baos, Arrays.asList("age", "details/tel", "details/tags"), Collections.emptyList());

        final JsonArray arr = convertAvroToJson(new ByteArrayInputStream(baos.toByteArray()));

        Assert.assertEquals(1, arr.size());
        JsonObject json = (JsonObject) arr.get(0);
        Assert.assertEquals("数聚蜂巢", json.get("name").getAsString());
        Assert.assertEquals("https://www.baishan.com/tech/orchsym", json.get("site").getAsString());
        Assert.assertFalse(json.has("age"));

        Assert.assertTrue(json.has("details"));
        final JsonObject detailsJson = json.get("details").getAsJsonObject();
        Assert.assertFalse(detailsJson.has("tel"));
        Assert.assertFalse(detailsJson.has("tags"));
        Assert.assertEquals(100000, detailsJson.get("post").getAsInt());
        Assert.assertTrue(detailsJson.has("address"));
        final JsonObject addressJson = detailsJson.get("address").getAsJsonObject();
        Assert.assertTrue(addressJson.has("street"));
        Assert.assertTrue(addressJson.has("city"));
        Assert.assertTrue(addressJson.has("country"));

        Assert.assertTrue(addressJson.has("links"));
    }

    private int processsAvro(final InputStream input, final OutputStream output, List<String> includeList, List<String> excludeList) throws IOException {
        DatumReader<GenericRecord> datumReader = new SpecificDatumReader<GenericRecord>();
        try (DataFileStream<GenericRecord> reader = new DataFileStream<GenericRecord>(input, datumReader)) {
            final Schema oldSchema = reader.getSchema();

            List<FieldMatcher> includeMatchers = createMatchers(includeList);
            List<FieldMatcher> excludeMatchers = createMatchers(excludeList);

            final Schema newSchema = filterFields(oldSchema, includeMatchers, excludeMatchers);

            final DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(newSchema);
            try (final DataFileWriter<GenericRecord> dataFileWriter = new DataFileWriter<>(datumWriter)) {
                dataFileWriter.create(newSchema, output);

                int recordCount = 0;
                GenericRecord currRecord = null;
                while (reader.hasNext()) {
                    currRecord = reader.next(currRecord);
                    recordCount++;
                    dataFileWriter.append(currRecord); // only write with new schema
                }
                return recordCount;
            }
        }
    }

    private Schema filterFields(final Schema schema, List<FieldMatcher> includeMatchers, List<FieldMatcher> excludeMatchers) {
        if (includeMatchers.isEmpty() && excludeMatchers.isEmpty()) { // keep original
            return null;
        }

        if (schema.getType() != Schema.Type.RECORD || schema.getFields().isEmpty()) { // only support record
            return null;
        }

        List<Field> fields = schema.getFields();
        List<Field> newFields = new ArrayList<>();

        for (Field f : fields) {
            Schema fieldSchema = f.schema();
            boolean fitered = true;
            if (includeMatchers.size() > 0) {
                final FieldMatcher matchLevel1 = matchLevel1(f.name(), includeMatchers);
                if (null != matchLevel1) {
                    if (matchLevel1.children.size() > 0) { // has child

                        List<FieldMatcher> childrenExcludes = excludeMatchers.stream().filter(m -> m.pattern.pattern().equals(matchLevel1.pattern.pattern())).map(fm -> fm.children)
                                .flatMap(pList -> pList.stream()).collect(Collectors.toList());
                        final Schema newFieldSchema = filterFields(fieldSchema, matchLevel1.children, childrenExcludes);
                        if (null != newFieldSchema) {
                            if (fieldSchema.getFields().size() > 0) {
                                fieldSchema = newFieldSchema;
                                fitered = false;
                            }
                        }
                    } else { // only one level
                        if (excludeMatchers.isEmpty() || null == matchLevel1(f.name(), excludeMatchers)) {
                            fitered = false;
                        }
                    }

                }

            } else if (excludeMatchers.size() > 0) {
                final FieldMatcher matchLevel1 = matchLevel1(f.name(), excludeMatchers);
                if (null == matchLevel1) {
                    fitered = false;
                }
            }
            if (!fitered) {
                Schema.Field newField = new Schema.Field(f.name(), fieldSchema, f.doc(), f.defaultVal());
                newFields.add(newField);
            }
        }
        return Schema.createRecord(schema.getName(), schema.getDoc(), schema.getNamespace(), false, newFields);
    }

    private FieldMatcher matchLevel1(String name, List<FieldMatcher> matchers) {
        for (FieldMatcher fm : matchers) {
            if (fm.pattern.matcher(name).find()) {
                return fm;
            }
        }
        return null;
    }

    private static final String SEP_CHILD = "/";

    private List<FieldMatcher> createMatchers(List<String> list) {
        List<FieldMatcher> matchers = new ArrayList<>();
        list.forEach(f -> {
            findMatcher(matchers, f);
        });

        return matchers;
    }

    private FieldMatcher findMatcher(List<FieldMatcher> matchers, String path) {
        String first = path;
        String children = null;
        if (path.contains(SEP_CHILD)) { // has child
            final int sepIndex = path.indexOf(SEP_CHILD);
            first = path.substring(0, sepIndex);
            children = path.substring(sepIndex + 1);
        }

        final String level1 = first;
        final Optional<FieldMatcher> found = matchers.stream().filter(fm -> fm.pattern.pattern().equals(level1)).findFirst();
        FieldMatcher fm = null;
        if (found.isPresent()) {
            fm = found.get();
        } else {
            fm = new FieldMatcher();
            fm.pattern = Pattern.compile(first, Pattern.CASE_INSENSITIVE);
            matchers.add(fm);
        }

        // children
        if (null != children) {
            findMatcher(fm.children, children);
        }
        return fm;
    }

    static class FieldMatcher {
        Pattern pattern;
        List<FieldMatcher> children = new ArrayList<>();
    }
}
