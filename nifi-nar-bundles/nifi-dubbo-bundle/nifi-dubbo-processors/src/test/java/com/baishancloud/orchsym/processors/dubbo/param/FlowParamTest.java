package com.baishancloud.orchsym.processors.dubbo.param;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.nifi.util.Tuple;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.internal.ExactComparisonCriteria;

import com.baishancloud.orchsym.processors.dubbo.Constants;
import com.baishancloud.orchsym.processors.dubbo.TestConstants;
import com.baishancloud.orchsym.processors.dubbo.it.Dog;
import com.baishancloud.orchsym.processors.dubbo.it.Person;

/**
 * 
 * 1) the value should do autoboxing/unboxing for type.
 * 
 * 2) if the generic for List/Map, no need figure out the types when the type is primitive, if Object(POJO also, and Object, like Integer) need the class field.
 * 
 * @author GU Guoqiang
 *
 */
@SuppressWarnings("unchecked")
public class FlowParamTest {
    final static String RES_FLOW = TestConstants.TEST_RES + '/' + "flow_param";

    protected Tuple<Schema, GenericRecord> loadRecord(String fileName) throws IOException {
        return loadRecord(fileName, fileName);
    }

    protected Tuple<Schema, GenericRecord> loadRecord(String avorFileName, String dataFileName) throws IOException {
        final File avroSchemaFile = new File(RES_FLOW, avorFileName + TestConstants.EXT_AVRO);
        if (!avroSchemaFile.exists()) {
            throw new FileNotFoundException("Can't find the avro schema file: " + avroSchemaFile.getAbsolutePath());
        }
        final File jsonDataFile = new File(RES_FLOW, dataFileName + TestConstants.EXT_JSON);
        if (!jsonDataFile.exists()) {
            throw new FileNotFoundException("Can't find the data file: " + jsonDataFile.getAbsolutePath());
        }

        final Schema avroSchema = new Schema.Parser().parse(avroSchemaFile);
        final DatumReader<GenericRecord> reader = new GenericDatumReader<GenericRecord>(avroSchema);

        final DataInputStream inputStream = new DataInputStream(new FileInputStream(jsonDataFile));
        final Decoder decoder = DecoderFactory.get().jsonDecoder(avroSchema, inputStream);
        GenericRecord record = reader.read(null, decoder); // only read one record to test

        return new Tuple<Schema, GenericRecord>(avroSchema, record);
    }

    /**
     * Test for the cases which the java primitive types can map to avro types directly
     * 
     * call(int arg1, long arg2, float arg3, double arg4, boolean arg5, String arg6, byte arg7)
     * 
     * String can be do like primitive, byte seems be different with others in Record.
     */
    @Test
    public void test_retrieve_primitive() throws IOException {
        doTest_primitive("primitive");
    }

    private void doTest_primitive(String filename) throws IOException {
        final Tuple<Schema, GenericRecord> data = loadRecord(filename);
        final GenericRecord record = data.getValue();

        final Tuple<String[], Object[]> tuple = FlowParam.retrieve(record);

        final String[] types = tuple.getKey();
        final Object[] values = tuple.getValue();

        assertArrayEquals(
                new String[] { int.class.getName(), long.class.getName(), float.class.getName(), double.class.getName(), boolean.class.getName(), String.class.getName(), byte.class.getName() },
                types);

        assertEquals(7, values.length);
        assertArrayEquals(new Object[] { 11, 1111L, 1.11f, 1.1111, true, "test", (byte) 'A' }, values);
    }

    /**
     * Test for the cases which the arrays of java primitive types which can map to avro types arrays
     * 
     * call(byte[], int[], long[], float[], double[], boolean[], String[])
     * 
     * String can be do like primitive
     */
    @Test
    public void test_retrieve_primitiveArray() throws IOException {
        doTest_primitiveArray("primitive-array");
    }

    private void doTest_primitiveArray(String fileName) throws IOException {
        final Tuple<Schema, GenericRecord> data = loadRecord(fileName);
        final GenericRecord record = data.getValue();
        final Tuple<String[], Object[]> tuple = FlowParam.retrieve(record);
        final String[] types = tuple.getKey();
        final Object[] values = tuple.getValue();

        assertArrayEquals(new String[] { byte[].class.getName(), int[].class.getName(), long[].class.getName(), float[].class.getName(), double[].class.getName(), boolean[].class.getName(),
                String[].class.getName() }, types);

        assertEquals(7, values.length);

        final Object bytesValue = values[0];
        assertTrue(bytesValue instanceof byte[]);
        assertFalse(bytesValue instanceof Byte[]);
        assertArrayEquals(new byte[] { (byte) 'A', (byte) 'B', (byte) 'C' }, (byte[]) bytesValue);

        final Object intValue = values[1];
        assertTrue(intValue instanceof int[]);
        assertFalse(intValue instanceof Integer[]);
        assertArrayEquals(new int[] { 1, 2, 3 }, (int[]) intValue);

        final Object longValue = values[2];
        assertTrue(longValue instanceof long[]);
        assertFalse(longValue instanceof Long[]);
        assertArrayEquals(new long[] { 9L, 8L, 7L }, (long[]) longValue);

        final Object floatValue = values[3];
        assertTrue(floatValue instanceof float[]);
        assertFalse(floatValue instanceof Float[]);
        new ExactComparisonCriteria().arrayEquals(null, new float[] { 1.1f, 2.2f, 3.3f }, (float[]) floatValue);

        final Object doubleValue = values[4];
        assertTrue(doubleValue instanceof double[]);
        assertFalse(doubleValue instanceof Double[]);
        new ExactComparisonCriteria().arrayEquals(null, new double[] { 9.9, 8.8, 7.7 }, (double[]) doubleValue);

        final Object boolValue = values[5];
        assertTrue(boolValue instanceof boolean[]);
        assertFalse(boolValue instanceof Boolean[]);
        assertArrayEquals(new boolean[] { true, false, true }, (boolean[]) boolValue);

        final Object strValue = values[6];
        assertTrue(strValue instanceof String[]);
        assertArrayEquals(new String[] { "A", "B", "C" }, (String[]) strValue);
    }

    /**
     * Test the primitive Objects which must add class field to figure out the java types.
     * 
     * call(Character, Byte, Short, Integer, Long, Float, Double, Boolean, String)
     * 
     * String, also can be done in Object way.
     */
    @Test
    public void test_retrieve_simpleObject_1_primitiveObject() throws IOException {
        final Tuple<Schema, GenericRecord> data = loadRecord("primitive-object");
        final GenericRecord record = data.getValue();

        final Tuple<String[], Object[]> tuple = FlowParam.retrieve(record);

        final String[] types = tuple.getKey();
        final Object[] values = tuple.getValue();

        assertArrayEquals(new String[] { Character.class.getName(), Byte.class.getName(), Short.class.getName(), Integer.class.getName(), Long.class.getName(), Float.class.getName(),
                Double.class.getName(), Boolean.class.getName(), String.class.getName() }, types);
        assertArrayEquals(new Object[] { 'X', (byte) 'A', (short) 123, 1234, 12345L, 1.11f, 1.1111, true, "ABC" }, values);
        assertArrayEquals(
                new Object[] { 'X', Byte.valueOf((byte) 'A'), Short.valueOf((short) 123), new Integer(1234), new Long(12345L), new Float(1.11f), new Double(1.1111), Boolean.TRUE, new String("ABC") },
                values);
    }

    /**
     * Test for the java primitive types, and figure out the class field, like Object way.
     * 
     * call(int arg1, long arg2, float arg3, double arg4, boolean arg5, String arg6, byte arg7)
     * 
     */
    @Test
    public void test_retrieve_simpleObject_2_primitiveWithClassField() throws IOException {
        doTest_primitive("primitive-class");
    }

    /**
     * Test the java primitive types which can't be mapped to avro types, so have to do it like Object with class field. And, use different avro type.
     * 
     * call(char, char, char, short, short)
     * 
     * same as "primitive-classs" way to use class field.
     */
    @Test
    public void test_retrieve_simpleObject_3_primitiveJavaTypes() throws IOException {
        final Tuple<Schema, GenericRecord> data = loadRecord("primitive-java");
        final GenericRecord record = data.getValue();

        final Tuple<String[], Object[]> tuple = FlowParam.retrieve(record);

        final String[] types = tuple.getKey();
        final Object[] values = tuple.getValue();

        assertArrayEquals(new String[] { char.class.getName(), char.class.getName(), char.class.getName(), short.class.getName(), short.class.getName(), short.class.getName() }, types);
        assertArrayEquals(new Object[] { 'A', 'B', 'C', (short) 'D', (short) 'E', (short) 'F' }, values);
    }

    /**
     * Test the java primitive types which can't be mapped to avro types, so have to do it like Object with class field.
     * 
     * call(LocalDate, LocalDate, LocalTime, LocalTime, LocalTime. LocalDateTime, LocalDateTime, Date, Date, Date)
     * 
     * The localXXX have fixed pattern, the Date can use default pattern, or add one "time_pattern" field to customize the pattern.
     * 
     */
    @Test
    public void test_retrieve_simpleObject_4_javaDates() throws IOException {
        final Tuple<Schema, GenericRecord> data = loadRecord("object-dates");
        final GenericRecord record = data.getValue();

        final Tuple<String[], Object[]> tuple = FlowParam.retrieve(record);

        final String[] types = tuple.getKey();
        final Object[] values = tuple.getValue();

        assertArrayEquals(new String[] { LocalDate.class.getName(), LocalDate.class.getName(), LocalTime.class.getName(), LocalTime.class.getName(), LocalTime.class.getName(),
                LocalDateTime.class.getName(), LocalDateTime.class.getName(), Date.class.getName(), Date.class.getName(), Date.class.getName() }, types);

        assertEquals(10, values.length);

        final LocalDate date20180604 = LocalDate.of(2018, 6, 4);

        assertEquals(date20180604, values[0]);
        assertEquals(date20180604, values[1]);
        assertEquals(LocalTime.of(13, 3), values[2]);
        assertEquals(LocalTime.of(11, 45, 30, 123456789), values[3]);
        assertEquals(LocalTime.of(11, 45, 30), values[4]);
        assertEquals(LocalDateTime.of(2018, 6, 4, 11, 45, 30), values[5]);
        assertEquals(LocalDateTime.of(2018, 6, 4, 11, 45, 30), values[6]);

        final String pattern = "yyyy/MM/dd";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        SimpleDateFormat dateFormatter = new SimpleDateFormat(pattern);
        assertEquals(date20180604.format(formatter), dateFormatter.format((Date) values[7]));
        assertEquals(date20180604.format(formatter), dateFormatter.format((Date) values[8]));
        assertEquals(date20180604.format(formatter), dateFormatter.format((Date) values[8]));

    }

    /**
     * Test the primitive types but figure out the class field like Object.
     * 
     * call(byte[], int[], long[], float[], double[], boolean[], String[])
     * 
     * Try the object way to deal with the primitive arrays.
     * 
     */
    @Test
    public void test_retrieve_primitiveArrayWithClassField() throws IOException {
        doTest_primitiveArray("primitive-array-class");
    }

    /**
     * 
     * call(Character[], Byte[], Short[], Integer[], Long[], Float[], Double[], Boolean[], String[])
     * 
     * same avro schema with object list
     */
    @Test
    public void test_retrieve_arrayObjects_1_primitiveObjectArray() throws IOException {
        final Tuple<Schema, GenericRecord> data = loadRecord("primitive-object-array");
        final GenericRecord record = data.getValue();

        final Tuple<String[], Object[]> tuple = FlowParam.retrieve(record);

        final String[] types = tuple.getKey();
        final Object[] values = tuple.getValue();

        assertArrayEquals(new String[] { Character[].class.getName(), Byte[].class.getName(), Short[].class.getName(), Integer[].class.getName(), Long[].class.getName(), Float[].class.getName(),
                Double[].class.getName(), Boolean[].class.getName(), String[].class.getName() }, types);

        doTest_primitiveObjectArray(values, record.getSchema());
    }

    private void doTest_primitiveObjectArray(final Object[] values, final Schema schema) {
        doTest_ObjectArrayMap(values, 0, schema, Character.class, new Character[] { 'A', 'B', 'C' });
        doTest_ObjectArrayMap(values, 1, schema, Byte.class, new Byte[] { (byte) 'A', (byte) 'B', (byte) 'C' });
        doTest_ObjectArrayMap(values, 2, schema, Short.class, new Short[] { 1, 2, 3 });
        doTest_ObjectArrayMap(values, 3, schema, Integer.class, new Integer[] { 1, 2, 3 });
        doTest_ObjectArrayMap(values, 4, schema, Long.class, new Long[] { 9L, 8L, 7L });
        doTest_ObjectArrayMap(values, 5, schema, Float.class, new Float[] { 1.1f, 2.2f, 3.3f });
        doTest_ObjectArrayMap(values, 6, schema, Double.class, new Double[] { 9.9, 8.8, 7.7 });
        doTest_ObjectArrayMap(values, 7, schema, Boolean.class, new Boolean[] { true, false, true });
        doTest_ObjectArrayMap(values, 8, schema, String.class, new String[] { "A", "B", "C" });
    }

    private void doTest_ObjectArrayMap(final Object[] fullValues, final int index, final Schema schema, Class<?> clazz, Object[] expected) {
        Object value = fullValues[index];
        String fieldName = schema.getFields().get(index).name();
        String className = clazz.getName();

        assertTrue(value instanceof Object[]);
        final Object[] values = (Object[]) value;
        assertEquals(3, values.length);
        assertEquals(3, expected.length);

        Map<String, Object> map1 = (Map<String, Object>) values[0];
        assertThat(map1.get(Constants.FIELD_CLASS), is(className));
        assertThat(map1.get(fieldName), is(expected[0]));

        Map<String, Object> map2 = (Map<String, Object>) values[1];
        assertThat(map2.get(Constants.FIELD_CLASS), is(className));
        assertThat(map2.get(fieldName), is(expected[1]));

        Map<String, Object> map3 = (Map<String, Object>) values[2];
        assertThat(map3.get(Constants.FIELD_CLASS), is(className));
        assertThat(map3.get(fieldName), is(expected[2]));
    }

    /**
     * call(List<Character>, List<Byte>, List<Short>, List<Integer>, List<Long>, List<Float>, List<Double>, List<Boolean>, List<String>)
     * 
     * like object array and pojo list,
     * 
     * same avro schema with object array
     */
    @Test
    public void test_retrieve_arrayObjects_2_primitiveObjectList() throws IOException {
        final Tuple<Schema, GenericRecord> data = loadRecord("primitive-object-array", "primitive-object-list");
        final GenericRecord record = data.getValue();

        final Tuple<String[], Object[]> tuple = FlowParam.retrieve(record);

        final String[] types = tuple.getKey();
        final Object[] values = tuple.getValue();

        // only the types are different;
        assertArrayEquals(new String[] { List.class.getName(), List.class.getName(), List.class.getName(), List.class.getName(), List.class.getName(), List.class.getName(), List.class.getName(),
                List.class.getName(), List.class.getName() }, types);

        doTest_primitiveObjectArray(values, record.getSchema());
    }

    /**
     * call(Map<String,Boolean>, Map<Float,Boolean>)
     */
    @Test
    public void test_retrieve_map_1_primitiveObjectMap() throws IOException {
        final Tuple<Schema, GenericRecord> data = loadRecord("primitive-object-map");
        final GenericRecord record = data.getValue();

        final Tuple<String[], Object[]> tuple = FlowParam.retrieve(record);

        final String[] types = tuple.getKey();
        final Object[] values = tuple.getValue();

        assertArrayEquals(new String[] { Map.class.getName(), Map.class.getName() }, types);

        assertEquals(2, values.length);

        final List<Field> fields = record.getSchema().getFields();

        // Map<String,Boolean>
        final String fieldName1 = fields.get(0).name();
        assertTrue(values[0] instanceof Map);
        Map<Map<String, Object>, Map<String, Object>> arg1Map = (Map<Map<String, Object>, Map<String, Object>>) values[0];
        List<String> arg1Values = arg1Map.keySet().stream().map(m -> m.get(fieldName1).toString()).sorted().collect(Collectors.toList());

        assertThat(arg1Values.get(0), is("A"));
        assertThat(arg1Values.get(1), is("B"));
        assertThat(arg1Values.get(2), is("C"));

        for (Entry<Map<String, Object>, Map<String, Object>> entry : arg1Map.entrySet()) {
            final Map<String, Object> key = entry.getKey();
            assertThat(key.get(Constants.FIELD_CLASS), is(String.class.getName()));
            final Object keyField = key.get(fieldName1);
            assertTrue(keyField instanceof String);

            final Map<String, Object> value = entry.getValue();
            assertThat(value.get(Constants.FIELD_CLASS), is(Boolean.class.getName()));
            final Object valueField = value.get(fieldName1);
            assertTrue(valueField instanceof Boolean);

            if (keyField.equals("A")) {
                assertThat(valueField, is(true));
            } else if (keyField.equals("B")) {
                assertThat(valueField, is(false));
            } else if (keyField.equals("C")) {
                assertThat(valueField, is(true));
            }
        }

        // Map<Float,Boolean>
        final String fieldName2 = fields.get(1).name();
        assertTrue(values[1] instanceof Map);
        Map<Map<String, Object>, Map<String, Object>> arg2Map = (Map<Map<String, Object>, Map<String, Object>>) values[1];
        List<Float> arg2Values = arg2Map.keySet().stream().map(m -> (Float) m.get(fieldName2)).sorted().collect(Collectors.toList());

        assertThat(arg2Values.get(0), is(1.1f));
        assertThat(arg2Values.get(1), is(2.2f));
        assertThat(arg2Values.get(2), is(3.3f));

        for (Entry<Map<String, Object>, Map<String, Object>> entry : arg2Map.entrySet()) {
            final Map<String, Object> key = entry.getKey();
            assertThat(key.get(Constants.FIELD_CLASS), is(Float.class.getName()));
            final Object keyField = key.get(fieldName2);
            assertTrue(keyField instanceof Float);

            final Map<String, Object> value = entry.getValue();
            assertThat(value.get(Constants.FIELD_CLASS), is(Boolean.class.getName()));
            final Object valueField = value.get(fieldName2);
            assertTrue(valueField instanceof Boolean);

            if (keyField.equals(1.1f)) {
                assertThat(valueField, is(true));
            } else if (keyField.equals(2.2f)) {
                assertThat(valueField, is(false));
            } else if (keyField.equals(3.3f)) {
                assertThat(valueField, is(true));
            }
        }

    }

    /**
     * call(Person, Dog)
     */
    @Test
    public void test_retrieve_pojo_1() throws IOException {
        // FIXME, the key of json for "person" and "dog", is not used at all, just one key for figure out the first and second fields, even can use "arg1" and "arg2" still, but for the data json,
        // should set it in meaning
        final Tuple<Schema, GenericRecord> data = loadRecord("pojo");
        final GenericRecord record = data.getValue();

        final Tuple<String[], Object[]> tuple = FlowParam.retrieve(record);

        final String[] types = tuple.getKey();
        final Object[] values = tuple.getValue();

        assertArrayEquals(new String[] { Person.class.getName(), Dog.class.getName() }, types);
        assertEquals(2, values.length);

        final Map<String, Object> person = (Map<String, Object>) values[0];
        assertThat(person.get(Constants.FIELD_CLASS), is(Person.class.getName()));
        assertThat(person.get("id"), is(1));
        assertThat(person.get("name"), is("Test"));
        assertThat(person.get("age"), is(26));

        final Map<String, Object> dog = (Map<String, Object>) values[1];
        assertThat(dog.get(Constants.FIELD_CLASS), is(Dog.class.getName()));
        assertThat(dog.get("id"), is(2));
        assertThat(dog.get("name"), is("dog"));
        assertThat(dog.get("color"), is(123));
    }

    /**
     * need set the class field for array type and pojo
     * 
     * call(Person[], Dog[])
     */
    @Test
    public void test_retrieve_pojo_2_Array() throws IOException {
        final Tuple<Schema, GenericRecord> data = loadRecord("pojo-array");
        final GenericRecord record = data.getValue();

        final Tuple<String[], Object[]> tuple = FlowParam.retrieve(record);

        final String[] expectedTypes = new String[] { Person[].class.getName(), Dog[].class.getName() };

        doTest_pojoArray(tuple, expectedTypes);
    }

    private void doTest_pojoArray(final Tuple<String[], Object[]> tuple, final String[] expectedTypes) {
        final String[] types = tuple.getKey();
        final Object[] values = tuple.getValue();

        assertArrayEquals(expectedTypes, types);
        assertEquals(2, values.length);

        final Map<String, Object> personsMap = (Map<String, Object>) values[0];
        assertEquals(2, personsMap.size());
        assertThat(personsMap.get(Constants.FIELD_CLASS), is(expectedTypes[0]));

        final Object persionsObj = personsMap.get("persons");
        assertTrue(persionsObj instanceof Object[]);
        Object[] persons = (Object[]) persionsObj;
        assertEquals(3, persons.length);

        assertTrue(persons[0] instanceof Map);
        Map<String, Object> person1 = (Map<String, Object>) persons[0];
        assertThat(person1.get(Constants.FIELD_CLASS), is(Person.class.getName()));
        assertThat(person1.get("id"), is(1));
        assertThat(person1.get("name"), is("Zhang"));
        assertThat(person1.get("age"), is(28));

        assertTrue(persons[1] instanceof Map);
        Map<String, Object> person2 = (Map<String, Object>) persons[1];
        assertThat(person2.get(Constants.FIELD_CLASS), is(Person.class.getName()));
        assertThat(person2.get("id"), is(2));
        assertThat(person2.get("name"), is("Wang"));
        assertThat(person2.get("age"), is(26));

        assertTrue(persons[2] instanceof Map);
        Map<String, Object> person3 = (Map<String, Object>) persons[2];
        assertThat(person3.get(Constants.FIELD_CLASS), is(Person.class.getName()));
        assertThat(person3.get("id"), is(3));
        assertThat(person3.get("name"), is("Li"));
        assertThat(person3.get("age"), is(22));

        //
        final Map<String, Object> dogsMap = (Map<String, Object>) values[1];
        assertEquals(2, dogsMap.size());
        assertThat(dogsMap.get(Constants.FIELD_CLASS), is(expectedTypes[1]));

        final Object dogsObj = dogsMap.get("dogs");
        assertTrue(dogsObj instanceof Object[]);
        Object[] dogs = (Object[]) dogsObj;
        assertEquals(2, dogs.length);

        assertTrue(dogs[0] instanceof Map);
        Map<String, Object> dog1 = (Map<String, Object>) dogs[0];
        assertThat(dog1.get(Constants.FIELD_CLASS), is(Dog.class.getName()));
        assertThat(dog1.get("id"), is(21));
        assertThat(dog1.get("name"), is("kemi"));
        assertThat(dog1.get("color"), is(123));

        assertTrue(dogs[1] instanceof Map);
        Map<String, Object> dog2 = (Map<String, Object>) dogs[1];
        assertThat(dog2.get(Constants.FIELD_CLASS), is(Dog.class.getName()));
        assertThat(dog2.get("id"), is(22));
        assertThat(dog2.get("name"), is("nick"));
        assertThat(dog2.get("color"), is(456));
    }

    /**
     * need set the class field for List type and pojo
     * 
     * call(List<Person>, List<Dog>)
     * 
     * like object array
     */
    @Test
    public void test_retrieve_pojo_3_List() throws IOException {
        // same avsc file like pojo array, but only the class field are differnt.
        final Tuple<Schema, GenericRecord> data = loadRecord("pojo-array", "pojo-list");
        final GenericRecord record = data.getValue();

        final Tuple<String[], Object[]> tuple = FlowParam.retrieve(record);

        // different types
        final String[] expectedTypes = new String[] { List.class.getName(), List.class.getName() };

        doTest_pojoArray(tuple, expectedTypes);
    }

    /**
     * call(Map<Person,Dog>)
     * 
     * the schema similar with java primitive Object Map
     */
    @Test
    public void test_retrieve_map_2_pojoMap() throws IOException {
        final Tuple<Schema, GenericRecord> data = loadRecord("pojo-map");
        final GenericRecord record = data.getValue();

        final Tuple<String[], Object[]> tuple = FlowParam.retrieve(record);

        final String[] types = tuple.getKey();
        final Object[] values = tuple.getValue();

        assertArrayEquals(new String[] { Map.class.getName() }, types);

        assertEquals(1, values.length);
        assertTrue(values[0] instanceof Map);
        Map<Map<String, Object>, Map<String, Object>> mapValues = (Map<Map<String, Object>, Map<String, Object>>) values[0];
        assertEquals(2, mapValues.size());

        for (Entry<Map<String, Object>, Map<String, Object>> entry : mapValues.entrySet()) {
            final Map<String, Object> person = entry.getKey();
            final Map<String, Object> dog = entry.getValue();
            if (person.get("id").equals(1)) {
                assertThat(person.get("name"), is("Zhang"));
                assertThat(person.get("age"), is(22));

                assertThat(dog.get("id"), is(21));
                assertThat(dog.get("name"), is("kemi"));
                assertThat(dog.get("color"), is(123));
            } else if (person.get("id").equals(2)) {
                assertThat(person.get("name"), is("Wang"));
                assertThat(person.get("age"), is(26));

                assertThat(dog.get("id"), is(22));
                assertThat(dog.get("name"), is("nick"));
                assertThat(dog.get("color"), is(456));
            }
        }
    }

    /**
     * call(Map<Person,Dog[]>)
     */
    @Test
    public void test_retrieve_map_3_pojoMap_valueArray() throws IOException {
        final Tuple<Schema, GenericRecord> data = loadRecord("pojo-map-value-array");
        final GenericRecord record = data.getValue();

        final Tuple<String[], Object[]> tuple = FlowParam.retrieve(record);

        final String[] types = tuple.getKey();
        final Object[] values = tuple.getValue();

        assertArrayEquals(new String[] { Map.class.getName() }, types);

        assertEquals(1, values.length);

        assertTrue(values[0] instanceof Map);
        Map<Map<String, Object>, Object[]> mapValues = (Map<Map<String, Object>, Object[]>) values[0];
        assertEquals(2, mapValues.size());

        for (Entry<Map<String, Object>, Object[]> entry : mapValues.entrySet()) {
            final Map<String, Object> person = entry.getKey();
            final Object[] dogs = entry.getValue();
            if (person.get("id").equals(1)) {
                assertThat(person.get("name"), is("Zhang"));
                assertThat(person.get("age"), is(22));

                assertEquals(2, dogs.length);

                assertTrue(dogs[0] instanceof Map);
                final Map<String, Object> dog1 = (Map<String, Object>) dogs[0];
                assertThat(dog1.get("id"), is(21));
                assertThat(dog1.get("name"), is("kemi"));
                assertThat(dog1.get("color"), is(123));

                assertTrue(dogs[1] instanceof Map);
                final Map<String, Object> dog2 = (Map<String, Object>) dogs[1];
                assertThat(dog2.get("id"), is(22));
                assertThat(dog2.get("name"), is("nick"));
                assertThat(dog2.get("color"), is(456));
            } else if (person.get("id").equals(2)) {
                assertThat(person.get("name"), is("Wang"));
                assertThat(person.get("age"), is(26));

                assertEquals(1, dogs.length);

                assertTrue(dogs[0] instanceof Map);
                final Map<String, Object> dog1 = (Map<String, Object>) dogs[0];
                assertThat(dog1.get("id"), is(22));
                assertThat(dog1.get("name"), is("nick"));
                assertThat(dog1.get("color"), is(456));
            }
        }
    }

    /**
     * call(Map<Person,List<Dog>>)
     * 
     * same avro schema, but different the class field
     */
    @Test
    public void test_retrieve_map_4_pojoMap_valueList() throws IOException {
        final Tuple<Schema, GenericRecord> data = loadRecord("pojo-map-value-array", "pojo-map-value-list");
        final GenericRecord record = data.getValue();

        final Tuple<String[], Object[]> tuple = FlowParam.retrieve(record);

        final String[] types = tuple.getKey();
        final Object[] values = tuple.getValue();

        assertArrayEquals(new String[] { Map.class.getName() }, types);

        assertEquals(1, values.length);

        assertTrue(values[0] instanceof Map);
        Map<Map<String, Object>, List<Object>> mapValues = (Map<Map<String, Object>, List<Object>>) values[0];
        assertEquals(2, mapValues.size());

        for (Entry<Map<String, Object>, List<Object>> entry : mapValues.entrySet()) {
            final Map<String, Object> person = entry.getKey();
            final List<Object> dogs = entry.getValue();
            if (person.get("id").equals(1)) {
                assertThat(person.get("name"), is("Zhang"));
                assertThat(person.get("age"), is(22));

                assertEquals(2, dogs.size());

                assertTrue(dogs.get(0) instanceof Map);
                final Map<String, Object> dog1 = (Map<String, Object>) dogs.get(0);
                assertThat(dog1.get("id"), is(21));
                assertThat(dog1.get("name"), is("kemi"));
                assertThat(dog1.get("color"), is(123));

                assertTrue(dogs.get(1) instanceof Map);
                final Map<String, Object> dog2 = (Map<String, Object>) dogs.get(1);
                assertThat(dog2.get("id"), is(22));
                assertThat(dog2.get("name"), is("nick"));
                assertThat(dog2.get("color"), is(456));
            } else if (person.get("id").equals(2)) {
                assertThat(person.get("name"), is("Wang"));
                assertThat(person.get("age"), is(26));

                assertEquals(1, dogs.size());

                assertTrue(dogs.get(0) instanceof Map);
                final Map<String, Object> dog1 = (Map<String, Object>) dogs.get(0);
                assertThat(dog1.get("id"), is(22));
                assertThat(dog1.get("name"), is("nick"));
                assertThat(dog1.get("color"), is(456));
            }
        }
    }

    /**
     * call(List<Map<Person,Dog>>)
     */
    @Test
    @Ignore
    public void test_retrieve_pojoListMap() throws IOException {
        final Tuple<Schema, GenericRecord> data = loadRecord("pojo-list-map");
        final GenericRecord record = data.getValue();

        final Tuple<String[], Object[]> tuple = FlowParam.retrieve(record);

        final String[] types = tuple.getKey();
        final Object[] values = tuple.getValue();

        assertArrayEquals(new String[] { List.class.getName() }, types);

        assertEquals(1, values.length);

        fail("Not impl yet!");
    }

    /**
     * call(Person,Dog)
     */
    @Test
    @Ignore
    public void test_retrieve_pojoChildren() {
        fail("Not impl yet!");
    }
}
