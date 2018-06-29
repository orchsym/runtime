package org.apache.nifi.processors.mapper;

import java.util.Arrays;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;

/**
 * 
 * @author GU Guoqiang
 * 
 */
public class AvroCreator {
    public static Field createField(String name, Type type) {
        return createField(name, type, null);
    }

    public static Field createField(String name, Type type, Object defaultValue) {
        return createField(name, Schema.create(type), (Object) defaultValue);
    }

    public static Field createField(String name, Schema schema) {
        return createField(name, schema, null);
    }

    public static Field createField(String name, Schema schema, Object defaultValue) {
        return new Field(name, schema, null, (Object) defaultValue);
    }

    public static Schema createSchema(String name, Field... fields) {
        return createSchema(name, null, fields);
    }

    public static Schema createSchema(String name, String namespace, Field... fields) {
        final Schema schema = Schema.createRecord(name, null, namespace, false);
        if (fields != null)
            schema.setFields(Arrays.asList(fields));
        return schema;
    }
}
