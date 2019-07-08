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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericData.Record;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunners;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.kitesdk.data.spi.DataModelUtil;
import org.kitesdk.data.spi.JsonUtil;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author GU Guoqiang
 *
 */
public class ExtractAvroToAttributesRunnerTest extends AbstractAttributesRunnerTest {

    private static Schema testDataSchema;
    private static Record testDataRecord;
    private static byte[] testDataRecordBytes;

    public static InputStream streamFor(Record record) throws IOException {
        return streamFor(Arrays.asList(record));
    }

    public static InputStream streamFor(List<Record> records) throws IOException {
        return new ByteArrayInputStream(bytesFor(records));
    }

    @SuppressWarnings({ "unchecked", "resource" })
    public static byte[] bytesFor(List<Record> records) throws IOException {
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

    @BeforeClass
    public static void init() throws Exception {
        testDataContent = loadContents("test.json");

        testDataSchema = new Schema.Parser().parse(loadContents("test.avsc"));

        // same as JSONFileReader
        final GenericData model = DataModelUtil.getDataModelForType(Record.class);
        final JsonNode jsonNode = JsonUtil.parse(testDataContent);
        testDataRecord = (Record) JsonUtil.convertToAvro(model, jsonNode, testDataSchema);

        testDataRecordBytes = bytesFor(Arrays.asList(testDataRecord));
    }

    @AfterClass
    public static void cleanup() {
        testDataContent = null;
        testDataSchema = null;
        testDataRecord = null;
        testDataRecordBytes = null;
    }

    @Before
    public void before() {
        runner = TestRunners.newTestRunner(new ExtractAvroToAttributes());

    }

    protected MockFlowFile runForSuccessFlowFile() {
        runner.enqueue(testDataRecordBytes);
        return super.runForSuccessFlowFile();
    }

    protected void setProp_sub_arr_custom_all() {
        runner.setProperty("data", "/");
    }

    protected void setProp_sub_arr_no_name() {
        runner.setProperty("ABC", "/");
    }

    protected void setProp_arr_dynamic() {
        runner.setProperty("info", "/details");
        runner.setProperty("links", "/links");
        runner.setProperty("the_name", "/name");
        runner.setProperty("url", "/url");
    }

    protected void setProp_sub_arr_dynamic() {
        runner.setProperty("info", "/details");
        runner.setProperty("links", "/links");
        runner.setProperty("the_name", "/name");
        runner.setProperty("url_2", "/links[1]");
    }

    protected void setProp_arr_index() {
        runner.setProperty("links", "/links[0,2]");
        runner.setProperty("the_name", "/name");
        runner.setProperty("url_index1", "/links[1]");
    }

    protected void setProp_simple_arr() {
        runner.setProperty("the_tags", "/details/tags");
    }
}
