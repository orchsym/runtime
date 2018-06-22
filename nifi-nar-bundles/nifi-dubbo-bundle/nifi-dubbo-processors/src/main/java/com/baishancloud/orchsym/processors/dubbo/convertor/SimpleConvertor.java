package com.baishancloud.orchsym.processors.dubbo.convertor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.baishancloud.orchsym.processors.dubbo.Constants;

/**
 * Try to convert the object to right value according to the java type
 * 
 * @author GU Guoqiang
 *
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class SimpleConvertor {

    public static final Map<Class<?>, String> MAP_PRIMITIVE_ARRAY;
    static {
        Map<Class<?>, String> map = new HashMap<>();

        map.put(char[].class, char[].class.getName()); // [C
        map.put(byte[].class, byte[].class.getName()); // [B
        map.put(short[].class, short[].class.getName()); // [S
        map.put(int[].class, int[].class.getName()); // [I
        map.put(long[].class, long[].class.getName()); // [J
        map.put(float[].class, float[].class.getName()); // [F
        map.put(double[].class, double[].class.getName());// [D
        map.put(boolean[].class, boolean[].class.getName()); // [Z

        // deal with the String like primitive
        map.put(String[].class, String[].class.getName());// [Ljava.lang.String;

        MAP_PRIMITIVE_ARRAY = Collections.unmodifiableMap(map);
    }
    public static final List<String> NAMES_PRIMITIVE_ARRAY;
    static {
        NAMES_PRIMITIVE_ARRAY = Collections.unmodifiableList(MAP_PRIMITIVE_ARRAY.values().stream().collect(Collectors.toList()));
    }
    public static final Map<Class<?>, Class<?>> MAP_PRIMITIVE_OBJ;
    static {
        Map<Class<?>, Class<?>> map = new HashMap<>();
        map.put(Character.class, char.class);
        map.put(Byte.class, byte.class);
        map.put(Short.class, short.class);
        map.put(Integer.class, int.class);
        map.put(Long.class, long.class);
        map.put(Float.class, float.class);
        map.put(Double.class, double.class);
        map.put(Boolean.class, boolean.class);

        MAP_PRIMITIVE_OBJ = Collections.unmodifiableMap(map);
    }
    public static final List<String> NAMES_PRIMITIVE_OBJ;
    static {
        NAMES_PRIMITIVE_OBJ = Collections.unmodifiableList(MAP_PRIMITIVE_OBJ.keySet().stream().map(c -> c.getName()).collect(Collectors.toList()));
    }

    public static boolean isArrayClass(String name) {
        return NAMES_PRIMITIVE_ARRAY.contains(name) || name.startsWith("[L") && name.endsWith(";"); // general array
    }

    /**
     * all primitive value should do autoboxing to object.
     * 
     */
    public static Object convertSingle(final String javaTypeName, final Object value, final String timePattern) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map && javaTypeName != null) {
            Map<String, Object> objectMap = (Map<String, Object>) value;
            if (objectMap.containsKey(Constants.FIELD_CLASS) && objectMap.containsKey(Constants.FIELD_VALUE)) {
                if (javaTypeName.equals(objectMap.get(Constants.FIELD_CLASS))) { // same type
                    final Object mapValue = objectMap.get(Constants.FIELD_VALUE);

                    final Object timePatternObj = objectMap.get(Constants.FIELD_TIME_PATTERN);
                    final String timePatternStr = timePatternObj != null ? timePatternObj.toString() : null;

                    return convertSimple(javaTypeName, mapValue, timePatternStr);
                }
            }
        }

        return convertSimple(javaTypeName, value, timePattern);
    }

    public static Object convertSingle(final Class<?> clazz, final Object value) {
        return convertSingle(clazz.getName(), value);
    }

    public static Object convertSingle(final String javaTypeName, final Object value) {
        return convertSingle(javaTypeName, value, null);
    }

    public static Object convertSimple(final String javaTypeName, final Object value, final String timePattern) {
        if (value == null) {
            return null;
        }

        Object result = null;
        final String fieldValueStr = value.toString();
        final String numberStr = StringUtils.isBlank(fieldValueStr) ? "0" : fieldValueStr;

        if (boolean.class.getName().equals(javaTypeName)// primitive type use object way also.
                || Boolean.class.getName().equals(javaTypeName)) {
            result = Boolean.parseBoolean(fieldValueStr);

        } else if (char.class.getName().equals(javaTypeName) // primitive type use object way also.
                || Character.class.getName().equals(javaTypeName)) {
            if (value instanceof Number) { // use the value, even Byte.
                result = (char) ((Number) value).longValue();
            } else {
                // deal with string
                result = new Character(fieldValueStr.isEmpty() ? '\0' : fieldValueStr.charAt(0));
            }

        } else if (byte.class.getName().equals(javaTypeName) // primitive type use object way also.
                || Byte.class.getName().equals(javaTypeName)) {
            if (value instanceof Number) {
                result = ((Number) value).byteValue();
            } else {
                result = Byte.parseByte(numberStr); // have done the chooseFieldValue for byte, so just do parseByte directly.
            }

        } else if (short.class.getName().equals(javaTypeName) // primitive type use object way also.
                || Short.class.getName().equals(javaTypeName)) {
            if (value instanceof Number) {
                result = ((Number) value).shortValue();
            } else {
                result = Short.parseShort(numberStr);
            }

        } else if (int.class.getName().equals(javaTypeName) // primitive type use object way also.
                || Integer.class.getName().equals(javaTypeName)) {
            if (value instanceof Number) {
                result = ((Number) value).intValue();
            } else {
                result = Integer.parseInt(numberStr);
            }

        } else if (long.class.getName().equals(javaTypeName) // primitive type use object way also.
                || Long.class.getName().equals(javaTypeName)) {
            if (value instanceof Number) {
                result = ((Number) value).longValue();
            } else {
                result = Long.parseLong(numberStr);
            }

        } else if (float.class.getName().equals(javaTypeName) // primitive type use object way also.
                || Float.class.getName().equals(javaTypeName)) {
            if (value instanceof Number) {
                result = ((Number) value).floatValue();
            } else {
                result = Float.parseFloat(numberStr);
            }

        } else if (double.class.getName().equals(javaTypeName)// primitive type use object way also.
                || Double.class.getName().equals(javaTypeName)) {
            if (value instanceof Number) {
                result = ((Number) value).doubleValue();
            } else {
                result = Double.parseDouble(numberStr);
            }

        } else if (LocalDate.class.getName().equals(javaTypeName)) { // use string type
            if (StringUtils.isNotBlank(timePattern)) { // set pattern
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timePattern);
                result = LocalDate.parse(fieldValueStr, formatter);
            } else {
                // default the pattern is like 2018-06-04, even 2018-6-4 is invalid.
                result = LocalDate.parse(fieldValueStr);
            }

        } else if (LocalTime.class.getName().equals(javaTypeName)) { // use string type
            if (StringUtils.isNotBlank(timePattern)) { // set pattern
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timePattern);
                result = LocalTime.parse(fieldValueStr, formatter);
            } else {
                // default the pattern is like '10:15' or '10:15:30'
                result = LocalTime.parse(fieldValueStr);
            }

        } else if (LocalDateTime.class.getName().equals(javaTypeName)) { // use string type
            if (StringUtils.isNotBlank(timePattern)) { // set pattern
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timePattern);
                result = LocalDateTime.parse(fieldValueStr, formatter);
            } else {
                // default the pattern is like 2018-06-04T10:15:30
                result = LocalDateTime.parse(fieldValueStr);
            }

        } else if (Date.class.getName().equals(javaTypeName)) { // use string type
            SimpleDateFormat format = null;
            if (StringUtils.isNotBlank(timePattern)) { // set pattern
                format = new SimpleDateFormat(timePattern);
            } else {
                format = new SimpleDateFormat(); // default pattern.
            }
            try {
                result = format.parse(fieldValueStr);
            } catch (ParseException e) {
                result = fieldValueStr; // no format
            }

        } else { // keep original
            result = value;
        }
        return result;
    }

    /**
     * Try to reset to primitiver arrays.
     */
    public static Object convertSimpleArray(String arrayType, Object[] array) {
        if (array == null || array.length == 0) {
            return new Object[0];
        }
        if (arrayType.equals(boolean[].class.getName())) {
            boolean[] booleanValues = new boolean[array.length];
            for (int i = 0; i < array.length; i++) {
                booleanValues[i] = (boolean) convertSingle(boolean.class, array[i]);
            }
            return booleanValues;
        } else if (arrayType.equals(char[].class.getName())) {
            char[] charValues = new char[array.length];
            for (int i = 0; i < array.length; i++) {
                charValues[i] = (char) convertSingle(char.class, array[i]);
            }
            return charValues;
        } else if (arrayType.equals(byte[].class.getName())) {
            byte[] byteValues = new byte[array.length];
            for (int i = 0; i < array.length; i++) {
                byteValues[i] = (byte) convertSingle(byte.class, array[i]);
            }
            return byteValues;
        } else if (arrayType.equals(short[].class.getName())) {
            short[] shortValues = new short[array.length];
            for (int i = 0; i < array.length; i++) {
                shortValues[i] = (short) convertSingle(short.class, array[i]);
            }
            return shortValues;
        } else if (arrayType.equals(int[].class.getName())) {
            int[] intValues = new int[array.length];
            for (int i = 0; i < array.length; i++) {
                intValues[i] = (int) convertSingle(int.class, array[i]);
            }
            return intValues;
        } else if (arrayType.equals(long[].class.getName())) {
            long[] longValues = new long[array.length];
            for (int i = 0; i < array.length; i++) {
                longValues[i] = (long) convertSingle(long.class, array[i]);
            }
            return longValues;
        } else if (arrayType.equals(float[].class.getName())) {
            float[] floatValues = new float[array.length];
            for (int i = 0; i < array.length; i++) {
                floatValues[i] = (float) convertSingle(float.class, array[i]);
            }
            return floatValues;
        } else if (arrayType.equals(double[].class.getName())) {
            double[] doubleValues = new double[array.length];
            for (int i = 0; i < array.length; i++) {
                doubleValues[i] = (double) convertSingle(double.class, array[i]);
            }
            return doubleValues;
        } else if (arrayType.equals(String[].class.getName())) {
            String[] strValues = new String[array.length];
            for (int i = 0; i < array.length; i++) {
                strValues[i] = convertSingle(String.class, array[i]).toString();
            }
            return strValues;
        }

        // nothing to do, use the array value directly.
        return array;
    }

    public static Object[] convertObjectArray(String arrayType, Object[] array) {
        if (array == null || array.length == 0) {
            return new Object[0];
        }
        if (arrayType.equals(Boolean[].class.getName())) {
            Boolean[] booleanValues = new Boolean[array.length];
            for (int i = 0; i < array.length; i++) {
                booleanValues[i] = (Boolean) convertSingle(Boolean.class, array[i]);
            }
            return booleanValues;
        } else if (arrayType.equals(Character[].class.getName())) {
            Character[] charValues = new Character[array.length];
            for (int i = 0; i < array.length; i++) {
                charValues[i] = (Character) convertSingle(Character.class, array[i]);
            }
            return charValues;
        } else if (arrayType.equals(Byte[].class.getName())) {
            Byte[] byteValues = new Byte[array.length];
            for (int i = 0; i < array.length; i++) {
                byteValues[i] = (Byte) convertSingle(Byte.class, array[i]);
            }
            return byteValues;
        } else if (arrayType.equals(Short[].class.getName())) {
            Short[] shortValues = new Short[array.length];
            for (int i = 0; i < array.length; i++) {
                shortValues[i] = (Short) convertSingle(Short.class, array[i]);
            }
            return shortValues;
        } else if (arrayType.equals(Integer[].class.getName())) {
            Integer[] intValues = new Integer[array.length];
            for (int i = 0; i < array.length; i++) {
                intValues[i] = (Integer) convertSingle(Integer.class, array[i]);
            }
            return intValues;
        } else if (arrayType.equals(Long[].class.getName())) {
            Long[] longValues = new Long[array.length];
            for (int i = 0; i < array.length; i++) {
                longValues[i] = (Long) convertSingle(Long.class, array[i]);
            }
            return longValues;
        } else if (arrayType.equals(Float[].class.getName())) {
            Float[] floatValues = new Float[array.length];
            for (int i = 0; i < array.length; i++) {
                floatValues[i] = (Float) convertSingle(Float.class, array[i]);
            }
            return floatValues;
        } else if (arrayType.equals(Double[].class.getName())) {
            Double[] doubleValues = new Double[array.length];
            for (int i = 0; i < array.length; i++) {
                doubleValues[i] = (Double) convertSingle(Double.class, array[i]);
            }
            return doubleValues;
        } else if (arrayType.equals(String[].class.getName())) {
            String[] strValues = new String[array.length];
            for (int i = 0; i < array.length; i++) {
                strValues[i] = convertSingle(String.class, array[i]).toString();
            }
            return strValues;
        }

        // nothing to do, use the array value directly.
        return array;
    }

    /**
     * List<Object>
     */
    public static boolean isList(String arrayType, Object value) {
        if (value == null) {
            return false;
        }
        if (List.class.getName().equals(arrayType) && value instanceof List && !((List) value).isEmpty()) {
            if (((List) value).get(0) instanceof Map) { // according to the first Class to create list/map
                Map<String, Object> objectMap = (Map<String, Object>) ((List) value).get(0);
                if (objectMap.containsKey(Constants.FIELD_CLASS) && objectMap.containsKey(Constants.FIELD_VALUE)) { // List
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * If set the class field for primitive object, can't use Map like POJO, must extract to create the list of Object directly, for example, List<Byte>.
     */
    public static List<Object> convertObjectList(List<Object> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        if (list.get(0) instanceof Map) { // according to the first Class to create list/map
            Map<String, Object> objectMap = (Map<String, Object>) list.get(0);
            if (objectMap.containsKey(Constants.FIELD_CLASS) && objectMap.containsKey(Constants.FIELD_VALUE)) { // List
                final String className = objectMap.get(Constants.FIELD_CLASS).toString();

                final Object[] workingArr = list.stream().map(o -> (Map<String, Object>) o).map(map -> map.get(Constants.FIELD_VALUE)).toArray();
                final Object[] resultArr = convertObjectArray("[L" + className + ";", workingArr);
                return Arrays.asList(resultArr);
            }
        }
        return list;
    }

    /**
     * Map<Object,Object>
     */
    public static boolean isMap(String arrayType, Object value) {
        if (value == null) {
            return false;
        }
        if (Map.class.getName().equals(arrayType) && value instanceof List && !((List) value).isEmpty()) {
            if (((List) value).get(0) instanceof Map) { // according to the first Class to create list/map
                Map<String, Object> objectMap = (Map<String, Object>) ((List) value).get(0);
                if (objectMap.containsKey(Constants.FIELD_KEY) && objectMap.containsKey(Constants.FIELD_VALUE)) {// Map
                    final Object keyObj = objectMap.get(Constants.FIELD_KEY);
                    final Object valueObj = objectMap.get(Constants.FIELD_VALUE);
                    if (keyObj instanceof Map && valueObj instanceof Map // because all are objects for Map<Object,Object>, so key and value are Map also.
                            && ((Map) keyObj).containsKey(Constants.FIELD_CLASS) && ((Map) keyObj).containsKey(Constants.FIELD_VALUE) // key is Map for Object
                            && ((Map) valueObj).containsKey(Constants.FIELD_CLASS) && ((Map) valueObj).containsKey(Constants.FIELD_VALUE)) { // value is Map for Object
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * each object of list with key/value fields, means, the key is also have class field.
     */
    public static Map<Object, Object> convertObjectMap(List<Object> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyMap();
        }

        return list.stream().filter(o -> o instanceof Map).map(o -> (Map<String, Object>) o).collect(Collectors.toMap(map -> {
            final Object keyObj = map.get(Constants.FIELD_KEY);
            if (keyObj instanceof Map) {
                final Object kClassName = ((Map) keyObj).get(Constants.FIELD_CLASS);
                final String kClass = kClassName != null ? kClassName.toString() : String.class.getName();
                return convertSingle(kClass, keyObj);
            }
            return keyObj; // orignal
        }, map -> {
            final Object valueObj = map.get(Constants.FIELD_VALUE);
            if (valueObj instanceof Map) {
                final Object kClassName = ((Map) valueObj).get(Constants.FIELD_CLASS);
                final String kClass = kClassName != null ? kClassName.toString() : String.class.getName();
                return convertSingle(kClass, valueObj);
            }
            return valueObj;
        }));

    }

    /**
     * Map<String,Object>, means the key must String, the value can be any Object with class field, even POJO.
     */
    public static Map<String, Object> convertObjectMap(Map<String, Object> map) {
        Map<String, Object> results = new LinkedHashMap<>();
        for (Entry<String, Object> entry : map.entrySet()) {
            final String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map && ((Map) value).containsKey(Constants.FIELD_CLASS)) {
                final Object kClassName = ((Map) value).get(Constants.FIELD_CLASS);
                final String kClass = kClassName != null ? kClassName.toString() : String.class.getName();
                value = convertSingle(kClass, value);
            }
            results.put(key, value);
        }
        return results;
    }
}
