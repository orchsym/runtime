package com.baishancloud.orchsym.sap.record;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.generic.GenericRecord;
import org.apache.nifi.avro.AvroTypeUtil;
import org.apache.nifi.serialization.record.DataType;
import org.apache.nifi.serialization.record.RecordField;
import org.apache.nifi.serialization.record.RecordFieldType;
import org.apache.nifi.serialization.record.RecordSchema;
import org.apache.nifi.serialization.record.type.ArrayDataType;
import org.apache.nifi.serialization.record.type.RecordDataType;

/**
 * @author GU Guoqiang
 *
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RecordValuesUtil {

    /**
     * Create the import parameters which are simple types.
     */
    public static Map<String, Object> createImportValues(GenericRecord readRecord) {
        if (readRecord == null) {
            return Collections.emptyMap();
        }
        final Map<String, Object> avroRecordMap = AvroTypeUtil.convertAvroRecordToMap(readRecord, AvroTypeUtil.createSchema(readRecord.getSchema()));

        List<String> simpleTypesNames = readRecord.getSchema().getFields().stream().filter(f -> {
            Type type = f.schema().getType();
            // simple types
            return type != Type.ARRAY && type != Type.MAP && type != Type.RECORD;
        }).map(f -> f.name()).collect(Collectors.toList());

        // remove non-simple fields
        new ArrayList<String>(avroRecordMap.keySet())// fix for ConcurrentModificationException
                .stream().filter(n -> !simpleTypesNames.contains(n))//
                .forEach(n -> avroRecordMap.remove(n));

        return avroRecordMap;
    }

    /**
     * Create the import table parameters which are map, record and list types.
     */
    public static Map<String, Object> createImportTableValues(GenericRecord readRecord) {
        if (readRecord == null) {
            return Collections.emptyMap();
        }

        final Map<String, Object> results = new HashMap<>();

        final List<Field> fields = readRecord.getSchema().getFields();

        // 1. Map, only one map for each field
        fields.stream().filter(f -> f.schema().getType() == Type.MAP).forEach(f -> {
            String fieldName = f.name();
            Object obj = readRecord.get(fieldName);
            if (obj instanceof Map) {
                results.put(fieldName, Arrays.asList((Map) obj));
            } // else {//
        });

        // 2. record
        fields.stream().filter(f -> f.schema().getType() == Type.RECORD).forEach(f -> {
            String fieldName = f.name();
            // Type mapValueType = f.schema().getElementType().getType();

            Object value = readRecord.get(fieldName);
            if (value instanceof GenericRecord) {
                GenericRecord record = (GenericRecord) value;
                RecordSchema recordSchema = AvroTypeUtil.createSchema(record.getSchema());

                // FIXME, don't support complex record with inner types(map,record, list), only support the 1 level
                Map<String, Object> map = AvroTypeUtil.convertAvroRecordToMap(record, recordSchema);
                results.put(fieldName, Arrays.asList(map));
            } // else {//
        });

        // 3. array
        fields.stream().filter(f -> f.schema().getType() == Type.ARRAY).forEach(f -> {
            String fieldName = f.name();
            // Type arrElemType = f.schema().getElementType().getType();
            Object value = readRecord.get(fieldName);

            if (value instanceof Collection) { // List or Array
                Collection recordsList = (Collection) value;
                List<Map<String, Object>> reulstList = new ArrayList<>();

                for (Object record : recordsList) {
                    if (record instanceof GenericRecord) { // only process list with record
                        final GenericRecord childRecord = (GenericRecord) record;
                        // FIXME, don't support complex record with inner types(map,record, list), only support the 1 level
                        Map<String, Object> paramRecords = createImportValues(childRecord);
                        reulstList.add(paramRecords);
                    } // else { //non-record in list
                }

                if (!reulstList.isEmpty())
                    results.put(fieldName, reulstList);

            } // else { //non-list

        });

        return results;
    }

    public static List<String> createExportParamNames(RecordSchema writeSchema) {
        return writeSchema.getFields().stream().filter(f -> {
            RecordFieldType fieldType = f.getDataType().getFieldType();
            // simple types
            return fieldType != RecordFieldType.ARRAY && fieldType != RecordFieldType.MAP && fieldType != RecordFieldType.RECORD;
        }).map(f -> f.getFieldName()).collect(Collectors.toList());
    }

    public static Map<String, List<String>> createExportTableParamNames(RecordSchema writeSchema) {
        final List<RecordField> fields = writeSchema.getFields();

        final Map<String, List<String>> results = new HashMap<>();

        // 1. map, no the map keys
        fields.stream().filter(f -> f.getDataType().getFieldType() == RecordFieldType.MAP).forEach(f -> {
            // f.getDataType()
        });

        // 2. record
        fields.stream().filter(f -> f.getDataType().getFieldType() == RecordFieldType.RECORD).forEach(f -> {
            DataType dataType = f.getDataType();
            if (dataType instanceof RecordDataType) {
                final RecordSchema childSchema = ((RecordDataType) dataType).getChildSchema();
                // FIXME, don't support complex record with inner types(map,record, list), only support the 1 level
                List<String> params = createExportParamNames(childSchema);
                if (!params.isEmpty()) {
                    results.put(f.getFieldName(), params);
                }
            }
        });

        // 3. array
        fields.stream().filter(f -> f.getDataType().getFieldType() == RecordFieldType.ARRAY).forEach(f -> {
            DataType dataType = f.getDataType();
            if (dataType instanceof ArrayDataType) {
                DataType elementDataType = ((ArrayDataType) dataType).getElementType();

                if (elementDataType instanceof RecordDataType) {
                    final RecordSchema childSchema = ((RecordDataType) elementDataType).getChildSchema();
                    // FIXME, don't support complex record with inner types(map,record, list), only support the 1 level
                    List<String> params = createExportParamNames(childSchema);
                    if (!params.isEmpty()) {
                        results.put(f.getFieldName(), params);
                    }
                } // else { //non-record in array
            }
        });

        return results;
    }

}
