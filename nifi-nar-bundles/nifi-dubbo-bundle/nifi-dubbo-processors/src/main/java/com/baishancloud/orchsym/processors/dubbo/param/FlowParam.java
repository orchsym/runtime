package com.baishancloud.orchsym.processors.dubbo.param;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.generic.GenericRecord;
import org.apache.nifi.util.Tuple;

import com.baishancloud.orchsym.processors.dubbo.Constants;
import com.baishancloud.orchsym.processors.dubbo.convertor.AvroMapConvertor;
import com.baishancloud.orchsym.processors.dubbo.convertor.SimpleConvertor;

/**
 * @author GU Guoqiang
 *
 */
@SuppressWarnings("unchecked")
public class FlowParam {

    private static final Map<Type, String> MAP_SIMPLE_AVRO_JAVA_PRIMITIVE = new HashMap<>();
    static {
        MAP_SIMPLE_AVRO_JAVA_PRIMITIVE.put(Type.BOOLEAN, boolean.class.getName());
        MAP_SIMPLE_AVRO_JAVA_PRIMITIVE.put(Type.BYTES, byte.class.getName()); // String, but match byte still.
        MAP_SIMPLE_AVRO_JAVA_PRIMITIVE.put(Type.INT, int.class.getName());
        MAP_SIMPLE_AVRO_JAVA_PRIMITIVE.put(Type.LONG, long.class.getName());
        MAP_SIMPLE_AVRO_JAVA_PRIMITIVE.put(Type.FLOAT, float.class.getName());
        MAP_SIMPLE_AVRO_JAVA_PRIMITIVE.put(Type.DOUBLE, double.class.getName());

        // deal with the String is like java primitive
        MAP_SIMPLE_AVRO_JAVA_PRIMITIVE.put(Type.STRING, String.class.getName());
        MAP_SIMPLE_AVRO_JAVA_PRIMITIVE.put(Type.ENUM, String.class.getName()); // ???, need check later
        MAP_SIMPLE_AVRO_JAVA_PRIMITIVE.put(Type.FIXED, String.class.getName()); // ???, need check later

        // FIXME, for complex type of avro, shouldn't support.
        // MAP_SIMPLE_AVRO_JAVA_PRIMITIVE.put(Type.RECORD, Date.class.getName());
        // MAP_SIMPLE_AVRO_JAVA_PRIMITIVE.put(Type.ARRAY, Object[].class.getName());
        // MAP_SIMPLE_AVRO_JAVA_PRIMITIVE.put(Type.MAP, Map.class.getName());
        // MAP_SIMPLE_AVRO_JAVA_PRIMITIVE.put(Type.UNION, Object.class.getName());
        // MAP_SIMPLE_AVRO_JAVA_PRIMITIVE.put(Type.NULL, Object.class.getName());

        // FIXME, If POJO or Java Object, should set the class field explicitly
    }
    private static final Map<Type, String> MAP_SIMPLE_AVRO_ARRAY = new HashMap<>();
    static {
        MAP_SIMPLE_AVRO_ARRAY.put(Type.BOOLEAN, SimpleConvertor.MAP_PRIMITIVE_ARRAY.get(boolean[].class)); // [Z
        MAP_SIMPLE_AVRO_ARRAY.put(Type.BYTES, SimpleConvertor.MAP_PRIMITIVE_ARRAY.get(byte[].class)); // [B
        MAP_SIMPLE_AVRO_ARRAY.put(Type.INT, SimpleConvertor.MAP_PRIMITIVE_ARRAY.get(int[].class)); // [I
        MAP_SIMPLE_AVRO_ARRAY.put(Type.LONG, SimpleConvertor.MAP_PRIMITIVE_ARRAY.get(long[].class)); // [J
        MAP_SIMPLE_AVRO_ARRAY.put(Type.FLOAT, SimpleConvertor.MAP_PRIMITIVE_ARRAY.get(float[].class)); // [F
        MAP_SIMPLE_AVRO_ARRAY.put(Type.DOUBLE, SimpleConvertor.MAP_PRIMITIVE_ARRAY.get(double[].class));// [D

        // deal with the String like primitive
        MAP_SIMPLE_AVRO_ARRAY.put(Type.STRING, SimpleConvertor.MAP_PRIMITIVE_ARRAY.get(String[].class));// [Ljava.lang.String;

    }

    /**
     * Only need extract the first level of java types, if POJO in deep, need figure out the class field in record explicitly.
     */
    public static Tuple<String[], Object[]> retrieve(final GenericRecord record) {
        final Schema schema = record.getSchema();
        final List<Field> fields = schema.getFields();
        final int size = fields.size();

        final Map<String, Object> valuesMap = AvroMapConvertor.convertToPureMap(record);

        String[] types = new String[size];
        Object[] values = new Object[size];
        for (int i = 0; i < size; i++) {
            Field field = fields.get(i);

            String fieldName = field.name();
            Schema fieldSchema = field.schema();
            Type avroType = fieldSchema.getType();

            Object fieldJavaValue = valuesMap.get(fieldName);

            String javaType = MAP_SIMPLE_AVRO_JAVA_PRIMITIVE.get(avroType);
            if (javaType != null) { // simply types, call(int, float, boolean)
                types[i] = javaType;
                // FIXME, will return the primitive object, didn't use primitive value.
                /*
                 * 1. call(int, long, ...), primitive, only support the matched arvo types.
                 */
                values[i] = chooseFieldValue(avroType, fieldJavaValue);
            } else if (Type.ARRAY == avroType && fieldJavaValue instanceof Object[]) {
                Tuple<String, Object> arrTuple = retieveArrayField(field, (Object[]) fieldJavaValue);
                types[i] = arrTuple.getKey();
                values[i] = arrTuple.getValue();
            } else if (Type.RECORD == avroType && fieldJavaValue instanceof Map) {
                Tuple<String, Object> recordTuple = retieveRecordField(field, (Map<String, Object>) fieldJavaValue);
                types[i] = recordTuple.getKey();
                values[i] = recordTuple.getValue();
            } else {
                // throw new IllegalArgumentException("Don't support the field type " + avroType);
                // ignore?
            }

        }
        return new Tuple<String[], Object[]>(types, values);
    }

    private static Byte getByteValue(Object fieldJavaValue) {
        if (fieldJavaValue instanceof Object[]) {
            Object[] bytes = (Object[]) fieldJavaValue;
            if (bytes.length == 1 && bytes[0] instanceof Byte) { // just valid the byte type
                // find the real byte value.
                return (Byte) bytes[0];
            }
        }
        return null;
    }

    private static Object chooseFieldValue(Type avroType, Object fieldJavaValue) {
        // process for byte
        if (avroType == Type.BYTES) {
            return getByteValue(fieldJavaValue);
        }
        // nothing to do
        return fieldJavaValue;
    }

    /**
     * the values of arrays are Object[] always. so try to reset to primitiver arrays.
     */
    private static Object resetArrayPrimitiveValues(String javaType, final Type elementType, Object[] fieldArrayValues) {
        Object[] array = (Object[]) fieldArrayValues;

        if (elementType == Type.BYTES) { // byte
            for (int i = 0; i < array.length; i++) {
                final Object chooseFieldValue = chooseFieldValue(elementType, array[i]);
                array[i] = chooseFieldValue;
            }
        }
        return SimpleConvertor.convertSimpleArray(javaType, array);
    }

    static Tuple<String, Object> retieveArrayField(final Field field, final Object[] fieldArrayValues) {
        String arrType = null;
        Object arrValues = null;

        final Type elementType = field.schema().getElementType().getType();
        final String elementJavaType = MAP_SIMPLE_AVRO_JAVA_PRIMITIVE.get(elementType);
        if (elementJavaType != null) { // simply types
            arrType = MAP_SIMPLE_AVRO_ARRAY.get(elementType);
            // FIXME, Because the Arrays are Object[] always, so change back to primitive arrays
            /*
             * 1. call(byte[], int[], ...), primitive-array
             */
            arrValues = resetArrayPrimitiveValues(arrType, elementType, fieldArrayValues);
        } else {
            // TODO, find the type from values.
        }
        return new Tuple<String, Object>(arrType, arrValues);
    }

    static Tuple<String, Object> retieveRecordField(final Field field, final Map<String, Object> fieldMap) {
        String recordType = null;
        Object recordValues = null;

        final Schema fieldSchema = field.schema();
        final String fieldName = field.name();

        final Field classField = fieldSchema.getField(Constants.FIELD_CLASS);
        final Field objectField = fieldSchema.getField(fieldName); // same name of field in record

        if (fieldSchema.getName().equals(fieldName) && classField != null) {// should be one Object record
            final Object className = fieldMap.get(Constants.FIELD_CLASS);
            recordType = className != null ? className.toString() : String.class.getName();

            if (objectField != null) { // have the same field
                final Type objectType = objectField.schema().getType();
                final Object fieldValue = fieldMap.get(fieldName);
                if (objectField != null && fieldValue != null) {
                    if (Type.ARRAY == objectType && fieldValue instanceof Object[]) { // Object Array
                        final Schema elementSchema = objectField.schema().getElementType();
                        final Type elementType = elementSchema.getType();

                        final Object[] arrValues = (Object[]) fieldValue;

                        if (elementType == Type.RECORD) {// object list or array
                            final Field elemField = elementSchema.getField(fieldName);
                            if (elemField != null) { // normal java objects
                                /*
                                 * arrayObjects_X
                                 * 
                                 * 1. call(Character[], Byte[], ...), primitive-object-array.
                                 * 
                                 * 2. call(List<Character>, List<Byte>, ...), primitive-object-list
                                 */
                                recordValues = resetArraySimpleObjectValues(elemField, arrValues);
                            } else { // object map
                                final Field keyField = elementSchema.getField(Constants.FIELD_KEY);
                                final Field valueField = elementSchema.getField(Constants.FIELD_VALUE);
                                // Because the key/value of Map must be Object, so should be avro record type always.
                                if (keyField != null && valueField != null && keyField.schema().getType() == Type.RECORD && valueField.schema().getType() == Type.RECORD) {
                                    /*
                                     * map_X
                                     * 
                                     * 1. call(Map<String,Boolean>, Map<Float,Boolean>), primitive-object-map.
                                     * 
                                     * 2. call(Map<Person,Dog>), pojo-map
                                     * 
                                     * 3. call(Map<Person,Dog[]>), pojo-map-value-array
                                     * 
                                     * 4. call(Map<Person,List<Dog>>), pojo-map-value-list
                                     * 
                                     */
                                    recordValues = retieveMapObjectValues(fieldName, keyField, valueField, arrValues);

                                } // else { //pojo, nothing to do

                            }
                        } else if (MAP_SIMPLE_AVRO_ARRAY.values().contains(recordType)) { // array of primitive types
                            /*
                             * 1. call(byte[], int[]), primitive-array-class, try class field way
                             */
                            recordValues = resetArrayPrimitiveValues(recordType, elementType, arrValues);
                        } else {
                            // ??
                        }
                    } else if (fieldValue instanceof Map) {
                        // ??
                    } else { // simple Object, and try to convert the type.
                        /*
                         * simpleObject_X
                         * 
                         * 1. call(Character, Byte, ...), primitive-object
                         * 
                         * 2. call(int, long, ...), primitive-class, like primitive object to have class field
                         * 
                         * 3. call(char, short), primitive-java, because no avro type to be matched, so do it like primitive object.
                         * 
                         * 4. call(LocalDate, LocalTime, LocalDateTime, Date), object-dates, deal with like primitive object.
                         * 
                         */
                        Object timePattern = fieldMap.get(Constants.FIELD_TIME_PATTERN);
                        recordValues = chooseSimpleObjectValue(objectType, recordType, fieldValue, timePattern == null ? null : timePattern.toString());
                    }
                }
            } else {
                // shouldn't be here ?
            }

        } else {
            // Normal Object?
        }

        if (recordValues == null) {
            /*
             * pojo_X
             * 
             * 1. call(Person, Dog), pojo
             * 
             * 2. call(Person[], Dog[]), pojo-array
             * 
             * 3. call(List<Person>, List<Dog>), pojo-list
             * 
             */
            recordValues = resetPojoFieldsValues(fieldMap);
        }
        return new Tuple<String, Object>(recordType, recordValues);
    }

    /**
     * all primitive value should do autoboxing to object.
     * 
     */
    static Object chooseSimpleObjectValue(final Type avroType, final String recordType, final Object fieldValue, final String timePattern) {

        final Object choosedFieldValue = chooseFieldValue(avroType, fieldValue);
        return SimpleConvertor.convertSingle(recordType, choosedFieldValue, timePattern);
    }

    static Character convertChar(final Object choosedFieldValue) {
        if (choosedFieldValue instanceof Number) { // use the value, even Byte.
            return (char) ((Number) choosedFieldValue).longValue();
        } else {
            final String fieldValueStr = choosedFieldValue == null ? "" : choosedFieldValue.toString();
            // deal with string
            return new Character(fieldValueStr.isEmpty() ? '\0' : fieldValueStr.charAt(0));
        }
    }

    static Object[] resetArraySimpleObjectValues(final Field elemField, final Object[] fieldArrayValues) {
        for (Object obj : fieldArrayValues) {
            if (obj instanceof Map) {
                Map<String, Object> mapValue = (Map<String, Object>) obj;
                resetMapSimpleObjectValues(elemField, mapValue);
            }
        }

        return fieldArrayValues;
    }

    static Map<String, Object> resetMapSimpleObjectValues(final Field elemField, final Map<String, Object> fieldMapValues) {
        final Type avroType = elemField.schema().getType();
        final String fieldName = elemField.name();

        final Object fieldType = fieldMapValues.get(Constants.FIELD_CLASS);
        final Object fieldValue = fieldMapValues.get(fieldName);
        if (fieldType != null && fieldValue != null) {
            final Object timePattern = fieldMapValues.get(Constants.FIELD_TIME_PATTERN);
            // if not simple object, like pojo, will be no change.
            final Object typeValue = chooseSimpleObjectValue(avroType, fieldType.toString(), fieldValue, timePattern == null ? null : timePattern.toString());
            fieldMapValues.put(fieldName, typeValue);

        } // else{ // nothing to do without class field

        return fieldMapValues;
    }

    /**
     * return for Object Map
     */
    static Map<Object, Object> retieveMapObjectValues(final String fieldName, final Field keyField, final Field valueField, final Object[] fieldArrayValues) {
        final Field keyValueField = keyField.schema().getField(fieldName);
        final Field valueValueField = valueField.schema().getField(fieldName);

        Map<Object, Object> results = new HashMap<>();
        for (Object obj : fieldArrayValues) {
            if (obj instanceof Map) {
                Map<String, Object> mapValue = (Map<String, Object>) obj;

                // key
                final Object keyObject = mapValue.get(Constants.FIELD_KEY);
                final Map<String, Object> keyMap = (Map<String, Object>) keyObject;

                Object mapKeyValues = null;
                if (keyValueField != null) {
                    final Object value = keyMap.get(fieldName);
                    if (keyValueField.schema().getType() == Type.RECORD) {
                        //
                    } else if (keyValueField.schema().getType() == Type.ARRAY) {
                        final Object arrClassObj = keyMap.get(Constants.FIELD_CLASS);
                        final String arrClassName = arrClassObj != null ? arrClassObj.toString() : null;
                        if (SimpleConvertor.isArrayClass(arrClassName)) { // array
                            /*
                             * Map<Person[],...>
                             */
                            mapKeyValues = value; // keep the array still.
                        } else { // list
                            /*
                             * Map<List<Person>,...>
                             */
                            mapKeyValues = resetListFieldsValues((Object[]) value);
                        }
                    } else {// simple object
                        /*
                         * Map<String,...>
                         */
                        mapKeyValues = resetMapSimpleObjectValues(keyValueField, keyMap);
                    }
                } else { // pojo
                    /*
                     * Map<Person,...>
                     */
                    mapKeyValues = resetPojoFieldsValues(keyMap);
                }

                // value
                final Object valueObject = mapValue.get(Constants.FIELD_VALUE);
                final Map<String, Object> valueMap = (Map<String, Object>) valueObject;

                Object mapValueValues = null;
                if (valueValueField != null) {
                    final Object value = valueMap.get(fieldName);
                    if (valueValueField.schema().getType() == Type.RECORD) {

                    } else if (valueValueField.schema().getType() == Type.ARRAY) {
                        final Object arrClassObj = valueMap.get(Constants.FIELD_CLASS);
                        final String arrClassName = arrClassObj != null ? arrClassObj.toString() : null;
                        if (SimpleConvertor.isArrayClass(arrClassName)) { // array
                            /*
                             * Map<Person[],...>
                             */
                            mapValueValues = value; // keep the array still.
                        } else { // list
                            /*
                             * Map<List<Person>,...>
                             */
                            mapValueValues = resetListFieldsValues((Object[]) value);
                        }
                    } else {// simple object
                        /*
                         * Map<...,Boolean>
                         */
                        mapValueValues = resetMapSimpleObjectValues(valueValueField, valueMap);
                    }
                } else { // pojo
                    /*
                     * Map<...,Dog>
                     */
                    mapValueValues = resetPojoFieldsValues(valueMap);
                }

                results.put(mapKeyValues, mapValueValues);
            }
        }

        return results;

    }

    static List<Object> resetListFieldsValues(Object[] array) {
        List<Object> list = new ArrayList<>(array.length);
        for (int i = 0; i < array.length; i++) {
            if (array[i] instanceof Map) {
                // pojo or simple object?
                list.add(resetPojoFieldsValues((Map<String, Object>) array[i]));
            }
        }
        return list;
    }

    static Map<String, Object> resetPojoFieldsValues(Map<String, Object> pojoMap) {
        // TODO, need convert the java types?
        return pojoMap;
    }

}
