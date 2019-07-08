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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.avro.file.DataFileStream;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.nifi.annotation.behavior.DynamicProperty;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Marks;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.avro.AvroTypeUtil;
import org.apache.nifi.components.Validator;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.record.path.RecordPath;
import org.apache.nifi.record.path.validation.RecordPathValidator;
import org.apache.nifi.serialization.record.MapRecord;
import org.apache.nifi.serialization.record.Record;
import org.apache.nifi.serialization.record.RecordFieldType;
import org.apache.nifi.serialization.record.RecordSchema;

/**
 * @author GU Guoqiang
 *
 */
@InputRequirement(InputRequirement.Requirement.INPUT_REQUIRED)
@Marks(categories = { "Convert & Control/Convert" }, createdDate = "2018-12-10")
@Tags({ "Extract", "Attribute", "Record", "Avro" })
@CapabilityDescription("Provide the abblity of extracting the attributes by Record Path for Avro Record contents from the incoming flowfile. If don't set the dynamic property to set with Record path expression, will use the name 'ALL' with Record Path expression '/' for all by default")
@DynamicProperty(name = "Record Path property", //
        value = "The name of dynamic property with Record Path expression", //
        expressionLanguageScope = ExpressionLanguageScope.FLOWFILE_ATTRIBUTES, //
        description = "set the dynamic property with Record Path expression")
@WritesAttributes({ //
        @WritesAttribute(attribute = AbstractExtractToAttributesProcessor.ATTR_REASON, description = "The error message of extracting failure")//
})
public class ExtractAvroToAttributes extends AbstractExtractToAttributesProcessor {

    @Override
    protected Validator getPathValidator() {
        return new RecordPathValidator();
    }

    @Override
    protected String getDefaultAttributesPath() {
        return "/";
    }

    @Override
    protected void retrieveAttributes(ProcessContext context, ProcessSession session, FlowFile flowFile, InputStream rawIn, final Map<String, String> attributesFromRecords,
            final Map<String, String> attrPathSettings, final List<Pattern> includeFields, final List<Pattern> excludeFields) throws IOException {
        final Map<String, RecordPath> recordPaths = new HashMap<>();

        attrPathSettings.keySet().forEach(n -> recordPaths.put(n, RecordPath.compile(attrPathSettings.get(n))));

        try (final DataFileStream<GenericRecord> reader = new DataFileStream<>(new BufferedInputStream(rawIn), new GenericDatumReader<GenericRecord>())) {
            final RecordSchema recordSchema = AvroTypeUtil.createSchema(reader.getSchema());

            GenericRecord currRecord = null;
            if (reader.hasNext()) {
                currRecord = reader.next();
            }

            if (reader.hasNext()) {// has other records >1
                if (allowArray) {
                    processAttributes(attributesFromRecords, currRecord, recordSchema, recordPaths, 0, includeFields, excludeFields);
                    // processAttributes(attributesFromRecords, currRecord, recordSchema, recordPaths, -1, includeAttrs, excludeAttrs); // no index for first one

                    int recordCount = 1;
                    while (reader.hasNext()) {
                        currRecord = reader.next();
                        processAttributes(attributesFromRecords, currRecord, recordSchema, recordPaths, recordCount, includeFields, excludeFields);
                        recordCount++;
                    }
                } // else //ignore array
            } else if (currRecord != null) { // only one
                processAttributes(attributesFromRecords, currRecord, recordSchema, recordPaths, -1, includeFields, excludeFields);
            }
        } catch (Exception e) {
            final String msg = "Read Avro Record failure, because " + e.getMessage();
            getLogger().error(msg, e);
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException(msg, e);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void processAttributes(Map<String, String> attributes, GenericRecord avroRecord, final RecordSchema recordSchema, Map<String, RecordPath> recordPaths, int index,
            final List<Pattern> includeFields, final List<Pattern> excludeFields) {
        if (avroRecord == null || recordPaths.isEmpty()) {
            return;
        }
        final Map<String, Object> values = AvroTypeUtil.convertAvroRecordToMap(avroRecord, recordSchema);
        final Record record = new MapRecord(recordSchema, values);

        for (Entry<String, RecordPath> entry : recordPaths.entrySet()) {
            final RecordPath recordPath = entry.getValue();
            final String attrName = entry.getKey();

            final Map<String, List<Object>> results = new LinkedHashMap<>();
            final Map<String, List<Object>> scalarResults = new LinkedHashMap<>();
            final Map<String, List<Object>> scalarArrResults = new LinkedHashMap<>();
            final boolean[] filtered = new boolean[1];
            recordPath.evaluate(record).getSelectedFields().forEach(fv -> {
                final String fieldName = fv.getField().getFieldName();
                final RecordFieldType fieldType = fv.getField().getDataType().getFieldType();

                Object value = fv.getValue();
                if (value == null) {// not found field?
                    return; // ignore
                }
                // if map/record, still need check for the children, if just the value for path, can filter
                if (!fieldType.equals(RecordFieldType.MAP) && !fieldType.equals(RecordFieldType.RECORD) && ignoreField(fieldName, includeFields, excludeFields)) {
                    filtered[0] = true;
                    return;
                }

                if (isScalar(value)) {
                    List<Object> list = scalarResults.get(fieldName);
                    if (list == null) {
                        list = new ArrayList<>();
                        scalarResults.put(fieldName, list);
                    }
                    list.add(value);
                } else if (isScalarList(value)) {
                    List<Object> list = scalarArrResults.get(fieldName);
                    if (list == null) {
                        list = new ArrayList<>();
                        scalarArrResults.put(fieldName, list);
                    }
                    if (value instanceof Object[]) {
                        list.addAll(Arrays.asList((Object[]) value));
                    } else if (value instanceof List) {
                        list.addAll((List) value);
                    } else {
                        list.add(value);
                    }
                } else {
                    List<Object> list = results.get(fieldName);
                    if (list == null) {
                        list = new ArrayList<>();
                        results.put(fieldName, list);
                    }
                    list.add(value);
                }
            });

            setResults(attributes, attrName, index, includeFields, excludeFields, results, scalarResults, scalarArrResults, filtered[0]);

        }
    }

    protected void setAttributes(Map<String, String> attributes, final String attrPrefix, Object data, int index, final List<Pattern> includeFields, final List<Pattern> excludeFields) {
        if (data instanceof MapRecord) {
            MapRecord mapRecord = (MapRecord) data;
            mapRecord.getSchema().getFields().forEach(field -> {
                final String fieldName = field.getFieldName();
                final RecordFieldType fieldType = field.getDataType().getFieldType();

                // if map/record, still need check for the children
                if (!fieldType.equals(RecordFieldType.MAP) && !fieldType.equals(RecordFieldType.RECORD) && ignoreField(fieldName, includeFields, excludeFields)) {
                    return;
                }
                final String mapAttrPrefix = getAttributeName(attrPrefix, fieldName, index);

                final Object value = mapRecord.getValue(field);

                if (RecordFieldType.MAP.equals(fieldType) || RecordFieldType.RECORD.equals(fieldType) || RecordFieldType.ARRAY.equals(fieldType) && !isScalarList(value)) {
                    if (recurseChildren) {
                        setAttributes(attributes, mapAttrPrefix, value, -1, includeFields, excludeFields); // no need index yet
                    } // else //ignore
                } else { // other normal types
                    setAttributes(attributes, mapAttrPrefix, value, -1, includeFields, excludeFields);
                }
            });
        } else {
            super.setAttributes(attributes, attrPrefix, data, index, includeFields, excludeFields);
        }
    }

    @Override
    protected boolean isScalar(Object value) {
        return super.isScalar(value) && !(value instanceof MapRecord);
    }

}
