package com.baishancloud.orchsym.processors.dubbo.convertor;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.avro.generic.GenericRecord;
import org.apache.nifi.avro.AvroTypeUtil;
import org.apache.nifi.serialization.record.MapRecord;

/**
 * When use AvroTypeUtil.convertAvroRecordToMap to convert, if record, will be MapRecord, not Map<String,Object>. So force converting to Map always.
 * 
 * @author GU Guoqiang
 *
 */
public class AvroMapConvertor {

    @SuppressWarnings("unchecked")
    public static Object convertToPureMap(Object value) {
        if (value instanceof Map) {
            return convertToPureMap((Map<String, Object>) value);
        } else if (value instanceof List) {
            return convertToPureMap((List<Object>) value);
        } else if (value instanceof Object[]) {
            return convertToPureMap((Object[]) value);
        } else if (value instanceof MapRecord) {
            final MapRecord record = (MapRecord) value;
            Map<String, Object> mapValue = record.getRawFieldNames().stream().collect(Collectors.toMap(Function.identity(), f -> convertToPureMap(record.getValue(f))));
            return mapValue;
        } else if (value instanceof GenericRecord) {
            return convertToPureMap((GenericRecord) value);
        }
        return value;
    }

    public static Map<String, Object> convertToPureMap(final GenericRecord record) {
        // Record will convert to MapRecord, so convert again.
        final Map<String, Object> valuesMap = AvroTypeUtil.convertAvroRecordToMap(record, AvroTypeUtil.createSchema(record.getSchema()));

        return convertToPureMap(valuesMap);
    }

    public static Map<String, Object> convertToPureMap(final Map<String, Object> valuesMap) {
        for (Entry<String, Object> entry : valuesMap.entrySet()) {
            valuesMap.put(entry.getKey(), convertToPureMap(entry.getValue()));
        }
        return valuesMap;
    }

    public static List<Object> convertToPureMap(List<Object> values) {
        for (int i = 0; i < values.size(); i++) {
            values.set(i, convertToPureMap(values.get(i)));
        }
        return values;
    }

    public static Object[] convertToPureMap(Object[] values) {
        for (int i = 0; i < values.length; i++) {
            values[i] = convertToPureMap(values[i]);
        }
        return values;
    }
}
