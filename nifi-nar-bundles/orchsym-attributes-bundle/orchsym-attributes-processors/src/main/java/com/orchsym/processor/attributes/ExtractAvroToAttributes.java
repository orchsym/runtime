package com.orchsym.processor.attributes;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.avro.file.DataFileStream;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Marks;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.avro.AvroTypeUtil;
import org.apache.nifi.components.Validator;
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
@CapabilityDescription("Provide the abblity of extracting the attributes by Record Path for Avro Record contents from the incoming flowfile")
public class ExtractAvroToAttributes extends AbstractExtractToAttributesProcessor {

    @Override
    protected Validator getPathValidator() {
        return new RecordPathValidator();
    }

    @Override
    protected void retrieveAttributes(InputStream rawIn, final Map<String, String> attributesFromRecords, final List<Pattern> includeFields, final List<Pattern> excludeFields) throws IOException {
        final Map<String, RecordPath> recordPaths = new HashMap<>();
        attrPaths.keySet().forEach(n -> recordPaths.put(n, RecordPath.compile(attrPaths.get(n))));

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
        }
    }

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

            recordPath.evaluate(record).getSelectedFields().forEach(fv -> {
                final String fieldName = fv.getField().getFieldName();
                final RecordFieldType fieldType = fv.getField().getDataType().getFieldType();

                // if map/record, still need check for the children
                if (!fieldType.equals(RecordFieldType.MAP) && !fieldType.equals(RecordFieldType.RECORD) && ignoreField(fieldName, includeFields, excludeFields)) {
                    return;
                }

                Object value = fv.getValue();
                setAttributes(attributes, attrName, value, index, includeFields, excludeFields);
            });

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

                if (RecordFieldType.MAP.equals(fieldType) || RecordFieldType.RECORD.equals(fieldType) || RecordFieldType.ARRAY.equals(fieldType)) {
                    if (recurseChildren) {
                        setAttributes(attributes, mapAttrPrefix, mapRecord.getValue(field), -1, includeFields, excludeFields); // no need index yet
                    } // else //ignore
                } else { // other normal types
                    setAttributes(attributes, mapAttrPrefix, mapRecord.getValue(field), -1, includeFields, excludeFields);
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
