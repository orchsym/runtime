package com.baishancloud.orchsym.processors.dubbo.param;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.internal.ExactComparisonCriteria;

import com.baishancloud.orchsym.processors.dubbo.Constants;
import com.baishancloud.orchsym.processors.dubbo.TestConstants;
import com.baishancloud.orchsym.processors.dubbo.it.Person;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * 
 * @author GU Guoqiang
 *
 */
public class CustomParamTest {
    final static String RES_CUSTOM = TestConstants.TEST_RES + '/' + "custom_param";

    final static ObjectMapper objectMapper = new ObjectMapper();

    protected List<CustomParam> loadParams(String dataFileName) throws IOException {
        final File dataJsonFile = new File(RES_CUSTOM, dataFileName + ".json");
        String contents = FileUtils.readFileToString(dataJsonFile, StandardCharsets.UTF_8);

        final List<CustomParam> params = new CustomParam.Parser().parse(contents);
        return params;
    }

    @Test
    public void test_parse_simple() throws IOException {
        // int
        JsonNode intNode = objectMapper.readTree("88");
        assertTrue(intNode instanceof IntNode);
        assertThat(((IntNode) intNode).asInt(), is(88));

        //
        JsonNode boolNode = objectMapper.readTree("true");
        assertTrue(boolNode instanceof BooleanNode);
        assertThat(((BooleanNode) boolNode).asBoolean(), is(true));

        // string
        JsonNode emptyNode = objectMapper.readTree("");
        assertNull(emptyNode);
    }

    @Test(expected = JsonParseException.class)
    public void test_parse_string() throws IOException {
        // string
        JsonNode emptyNode = objectMapper.readTree("");
        assertNull(emptyNode);

        JsonNode strNode = objectMapper.readTree("abc");
        assertTrue(strNode instanceof TextNode);
        assertThat(((TextNode) strNode).asText(), is("abc"));
    }

    @Test
    public void test_parse_one() throws IOException {
        String json = "{\"desc\":\"age arg\",\"class\":\"int\",\"value\":88}";

        doTest_one(json);
    }

    @Test
    public void test_parse_oneArray() throws IOException {
        String json = "[{\"desc\":\"age arg\",\"class\":\"int\",\"value\":88}]";

        doTest_one(json);
    }

    private void doTest_one(String json) throws IOException {
        List<CustomParam> params = new CustomParam.Parser().parse(json);
        assertEquals(1, params.size());

        final CustomParam param = params.get(0);
        assertThat(param.getClassName(), is("int"));
        assertThat(param.getValue(), is(88));
        assertThat(param.getDesc(), is("age arg"));
    }

    /*
     * Object with children
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void test_parse_mapWithChildren() throws IOException {
        final Map map = objectMapper.readValue(new File(RES_CUSTOM, "map-children.json"), Map.class);
        final String json = objectMapper.writeValueAsString(map);

        List<CustomParam> params = new CustomParam.Parser().parse(json);
        assertEquals(1, params.size());

        final CustomParam param = params.get(0);
        assertThat(param.getClassName(), is("x.y.Bean"));
        assertThat(param.getDesc(), is("Bean"));

        final Object value = param.getValue();
        assertTrue(value instanceof Map);

        Map<String, Object> valueMap = (Map<String, Object>) value;
        assertThat(valueMap.get("class"), is("x.y.P"));
        assertThat(valueMap.get("id"), is(123));
        assertThat(valueMap.get("name"), is("Hello"));
        assertThat(valueMap.get("age"), is(28));

        final Object childrenObj = valueMap.get("children");
        assertTrue(childrenObj instanceof List);
        List<Object> children = (List<Object>) childrenObj;

        assertThat(children.size(), is(2));

        // c1
        final Object c1 = children.get(0);
        assertTrue(c1 instanceof Map);
        Map<String, Object> child1 = (Map<String, Object>) c1;

        assertThat(child1.get("class"), is("x.y.Cat"));
        assertThat(child1.get("id"), is(1001));
        assertThat(child1.get("name"), is("Cat"));
        assertThat(child1.get("age"), is(3));

        //
        final Object cc1 = child1.get("children");
        assertTrue(cc1 instanceof List);
        assertThat(((List) cc1).size(), is(1));

        final Object cc12 = ((List) cc1).get(0);
        assertTrue(cc12 instanceof Map);
        Map<String, Object> cchild1 = (Map<String, Object>) cc12;

        assertThat(cchild1.get("class"), is("x.y.Cat"));
        assertThat(cchild1.get("id"), is(10011));
        assertThat(cchild1.get("name"), is("Cat2"));
        assertThat(cchild1.get("age"), is(0));

        final Object cc121 = cchild1.get("children");
        assertTrue(cc121 instanceof List);
        assertThat(((List) cc121).size(), is(0));

        // c2
        final Object c2 = children.get(1);
        assertTrue(c2 instanceof Map);
        Map<String, Object> child2 = (Map<String, Object>) c2;

        assertThat(child2.get("class"), is("x.y.Dog"));
        assertThat(child2.get("id"), is(1002));
        assertThat(child2.get("name"), is("Dog"));
        assertThat(child2.get("age"), is(5));

        final Object cc2 = child2.get("children");
        assertTrue(cc2 instanceof List);
        assertThat(((List) cc2).size(), is(0));
    }

    @Test
    public void test_parse_arrays() throws IOException {
        String json = "[{\"desc\":\"name arg\",\"class\":\"java.lang.String\",\"value\":\"Hello\"},{\"desc\":\"age arg\",\"class\":\"int\",\"value\":88}]";

        List<CustomParam> params = new CustomParam.Parser().parse(json);
        assertEquals(2, params.size());

        assertThat(params.get(0).getDesc(), is("name arg"));
        assertThat(params.get(0).getClassName(), is("java.lang.String"));
        assertThat(params.get(0).getValue(), is("Hello"));

        assertThat(params.get(1).getDesc(), is("age arg"));
        assertThat(params.get(1).getClassName(), is("int"));
        assertThat(params.get(1).getValue(), is(88));
    }

    @Test
    public void test_write_map() throws IOException {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", 1234);
        values.put("name", "Abc");
        values.put("additions", Arrays.asList(1, 2, "xyz"));

        CustomParam param = new CustomParam(Map.class.getName(), values);

        final String json = new CustomParam.Writer().write(param);
        String expected = "{\"class\":\"java.util.Map\",\"value\":{\"id\":1234,\"name\":\"Abc\",\"additions\":[1,2,\"xyz\"]}}";
        assertEquals(expected, json);
    }

    /**
     * Map<String, Object> call(Map<Short, Character> arg1, Map<Long, Byte> arg2, Map<Double, Boolean> arg3);
     *
     * Don't support the key is map via Jackson
     */
    @SuppressWarnings("rawtypes")
    @Test
    @Ignore
    public void test_write_mapKeyValue() throws IOException {
        Map<Map, Map> arg1 = new HashMap<>();
        arg1.put(createClassMap((short) 1), createClassMap('A'));
        arg1.put(createClassMap((short) 2), createClassMap('B'));
        arg1.put(createClassMap((short) 3), createClassMap('C'));
        CustomParam param1 = new CustomParam(Map.class.getName(), arg1);

        Map<Map, Map> arg2 = new HashMap<>();
        arg2.put(createClassMap(1L), createClassMap((byte) 'A'));
        arg2.put(createClassMap(2L), createClassMap((byte) 'B'));
        arg2.put(createClassMap(3L), createClassMap((byte) 'C'));
        CustomParam param2 = new CustomParam(Map.class.getName(), arg2);

        Map<Map, Map> arg3 = new HashMap<>();
        arg3.put(createClassMap(1d), createClassMap(true));
        arg3.put(createClassMap(2d), createClassMap(false));
        arg3.put(createClassMap(3d), createClassMap(true));
        CustomParam param3 = new CustomParam(Map.class.getName(), arg3);

        final String customSettings = new CustomParam.Writer().write(Arrays.asList(param1, param2, param3));
        List<CustomParam> parsedParameters = new CustomParam.Parser().parse(customSettings);
        doTest_mapKeyValue(parsedParameters);

        // just make sure the same contents
        List<CustomParam> loadedparameters = loadParams("primitive-object-map-kv");
        // assertEquals(contents, customSettings); //because the line, not equals
        doTest_mapKeyValue(loadedparameters);
    }

    private Map<String, Object> createClassMap(Object value) {
        Map<String, Object> result = new HashMap<>();
        result.put(Constants.FIELD_CLASS, value.getClass().getName());
        result.put(Constants.FIELD_VALUE, value);
        return result;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void doTest_mapKeyValue(List<CustomParam> parameters) {
        assertEquals(3, parameters.size());

        final Comparator<Map> comparator = new Comparator<Map>() {

            @Override
            public int compare(Map o1, Map o2) {
                return o1.get(Constants.FIELD_VALUE).toString().compareTo(o2.get(Constants.FIELD_VALUE).toString());
            }
        };

        // 1
        final CustomParam param1 = parameters.get(0);
        assertEquals(Map.class.getName(), param1.getClassName());
        assertTrue(param1.getValue() instanceof Map);
        Map<Map, Map> arg1 = (Map<Map, Map>) param1.getValue();
        final Object[] array1 = arg1.keySet().stream().sorted(comparator).toArray();

        assertEquals(Short.class.getName(), ((Map) array1[0]).get(Constants.FIELD_CLASS));
        assertEquals((short) 1, ((Map) array1[0]).get(Constants.FIELD_VALUE));
        assertEquals(Character.class.getName(), ((Map) arg1.get(array1[0])).get(Constants.FIELD_CLASS));
        assertEquals('A', ((Map) arg1.get(array1[0])).get(Constants.FIELD_VALUE));

        assertEquals(Short.class.getName(), ((Map) array1[1]).get(Constants.FIELD_CLASS));
        assertEquals((short) 2, ((Map) array1[1]).get(Constants.FIELD_VALUE));
        assertEquals(Character.class.getName(), ((Map) arg1.get(array1[1])).get(Constants.FIELD_CLASS));
        assertEquals('B', ((Map) arg1.get(array1[1])).get(Constants.FIELD_VALUE));

        assertEquals(Short.class.getName(), ((Map) array1[2]).get(Constants.FIELD_CLASS));
        assertEquals((short) 3, ((Map) array1[2]).get(Constants.FIELD_VALUE));
        assertEquals(Character.class.getName(), ((Map) arg1.get(array1[2])).get(Constants.FIELD_CLASS));
        assertEquals('C', ((Map) arg1.get(array1[2])).get(Constants.FIELD_VALUE));

        // 2
        final CustomParam param2 = parameters.get(1);
        assertEquals(Map.class.getName(), param2.getClassName());
        assertTrue(param2.getValue() instanceof Map);
        Map<Map, Map> arg2 = (Map<Map, Map>) param2.getValue();
        final Object[] array2 = arg2.keySet().stream().sorted(comparator).toArray();

        assertEquals(Long.class.getName(), ((Map) array2[0]).get(Constants.FIELD_CLASS));
        assertEquals(1L, ((Map) array2[0]).get(Constants.FIELD_VALUE));
        assertEquals(Byte.class.getName(), ((Map) arg2.get(array2[0])).get(Constants.FIELD_CLASS));
        assertEquals((byte) 'A', ((Map) arg2.get(array2[0])).get(Constants.FIELD_VALUE));

        assertEquals(Long.class.getName(), ((Map) array2[1]).get(Constants.FIELD_CLASS));
        assertEquals(2L, ((Map) array2[1]).get(Constants.FIELD_VALUE));
        assertEquals(Byte.class.getName(), ((Map) arg2.get(array2[1])).get(Constants.FIELD_CLASS));
        assertEquals((byte) 'B', ((Map) arg2.get(array2[1])).get(Constants.FIELD_VALUE));

        assertEquals(Long.class.getName(), ((Map) array2[2]).get(Constants.FIELD_CLASS));
        assertEquals(3L, ((Map) array2[2]).get(Constants.FIELD_VALUE));
        assertEquals(Byte.class.getName(), ((Map) arg2.get(array2[2])).get(Constants.FIELD_CLASS));
        assertEquals((byte) 'C', ((Map) arg2.get(array2[2])).get(Constants.FIELD_VALUE));

        // 3
        final CustomParam param3 = parameters.get(2);
        assertEquals(Map.class.getName(), param3.getClassName());
        assertTrue(param3.getValue() instanceof Map);
        Map<Map, Map> arg3 = (Map<Map, Map>) param3.getValue();
        final Object[] array3 = arg3.keySet().stream().sorted(comparator).toArray();

        assertEquals(Double.class.getName(), ((Map) array3[0]).get(Constants.FIELD_CLASS));
        assertEquals((double) 1, ((Map) array3[0]).get(Constants.FIELD_VALUE));
        assertEquals(Boolean.class.getName(), ((Map) arg3.get(array3[0])).get(Constants.FIELD_CLASS));
        assertEquals('A', ((Map) arg3.get(array3[0])).get(Constants.FIELD_VALUE));

        assertEquals(Double.class.getName(), ((Map) array3[1]).get(Constants.FIELD_CLASS));
        assertEquals((double) 2, ((Map) array3[1]).get(Constants.FIELD_VALUE));
        assertEquals(Boolean.class.getName(), ((Map) arg3.get(array3[1])).get(Constants.FIELD_CLASS));
        assertEquals('B', ((Map) arg3.get(array3[1])).get(Constants.FIELD_VALUE));

        assertEquals(Double.class.getName(), ((Map) array3[2]).get(Constants.FIELD_CLASS));
        assertEquals((double) 3, ((Map) array3[2]).get(Constants.FIELD_VALUE));
        assertEquals(Boolean.class.getName(), ((Map) arg3.get(array3[2])).get(Constants.FIELD_CLASS));
        assertEquals(true, ((Map) arg3.get(array3[2])).get(Constants.FIELD_VALUE));

    }

    static class Parent {
        int id, age;
        String name;
        Map<String, Object> additions;

    }

    private Map<String, Object> createPerson1() {
        Map<String, Object> masterValue = new LinkedHashMap<>();
        masterValue.put("class", Person.class.getName());
        masterValue.put("id", 123);
        masterValue.put("name", "Test");
        masterValue.put("age", 20);

        Map<String, Object> child1 = new LinkedHashMap<>();
        child1.put("class", Person.class.getName());
        child1.put("id", 456);
        child1.put("name", "ABC");
        child1.put("age", 5);

        masterValue.put("children", new Object[] { child1 });

        return masterValue;
    }

    private Map<String, Object> createPerson2() {
        Map<String, Object> masterValue = new LinkedHashMap<>();
        masterValue.put("class", Person.class.getName());
        masterValue.put("id", 124);
        masterValue.put("name", "Mock");
        masterValue.put("age", 28);

        Map<String, Object> child1 = new LinkedHashMap<>();
        child1.put("class", Person.class.getName());
        child1.put("id", 457);
        child1.put("name", "XYZ");
        child1.put("age", 12);

        masterValue.put("children", new Object[] { child1 });

        return masterValue;
    }

    @SuppressWarnings("rawtypes")
    private void doTest_pojo_MapValue(CustomParam param, String expectedFileName) throws IOException {
        final String json = new CustomParam.Writer().write(param);

        // load and convert the format.
        File expectedFile = new File(RES_CUSTOM, expectedFileName);
        final JavaType type = objectMapper.getTypeFactory().constructType(LinkedHashMap.class);
        final Map expectedMap = objectMapper.readValue(expectedFile, type);
        String expected = objectMapper.writeValueAsString(expectedMap);

        assertEquals(expected, json);
    }

    /**
     * call(PojoBean bean)
     */
    @Test
    public void test_write_pojo() throws IOException {
        Map<String, Object> masterValue = new LinkedHashMap<>();
        masterValue.put("class", Person.class.getName());
        masterValue.put("id", 123);
        masterValue.put("name", "Test");
        masterValue.put("age", 20);

        Map<String, Object> child1 = new LinkedHashMap<>();
        child1.put("class", Person.class.getName());
        child1.put("id", 456);
        child1.put("name", "ABC");
        child1.put("age", 5);

        masterValue.put("children", new Object[] { child1 });

        CustomParam param = new CustomParam(Person.class.getName(), masterValue);

        doTest_pojo_MapValue(param, "pojo.json");
    }

    /**
     * call(PojoBean[] beans)
     */
    @Test
    public void test_write_pojo_array() throws IOException {
        doTest_pojoArray(new Person[0].getClass(), "pojo-array.json");
    }

    /**
     * call(List<PojoBean> beans)
     */
    @Test
    public void test_write_pojo_list() throws IOException {
        doTest_pojoArray(List.class, "pojo-list.json");
    }

    private void doTest_pojoArray(Class<?> param2Class, String dataFileName) throws IOException {
        Map<String, Object> masterValue1 = createPerson1();
        Map<String, Object> masterValue2 = createPerson2();

        CustomParam param1 = new CustomParam(param2Class.getName(), Arrays.asList(masterValue1, masterValue2));
        CustomParam param2 = new CustomParam(boolean.class.getName(), false);
        final String json = new CustomParam.Writer().write(Arrays.asList(param1, param2));

        // load and convert the format.
        File expectedFile = new File(RES_CUSTOM, dataFileName);
        final List expectedList = objectMapper.readValue(expectedFile, List.class);
        String expected = objectMapper.writeValueAsString(expectedList);

        assertEquals(expected, json);
    }

    /**
     * call(Map<String,PojoBean> beansMap)
     */
    @Test
    public void test_write_pojo_map() throws IOException {
        Map<String, Object> masterValue1 = createPerson1();
        Map<String, Object> masterValue2 = createPerson2();

        Map<String, Object> values = new LinkedHashMap<>();
        values.put("master1", masterValue1);
        values.put("master2", masterValue2);

        CustomParam param = new CustomParam(Map.class.getName(), values);

        doTest_pojo_MapValue(param, "pojo-map.json");
    }

    /**
     * call(char, byte, short, int, long, float, double, boolean)
     */
    @Test
    public void test_primitiveClass() throws IOException {
        final List<CustomParam> params = loadParams("primitive-class");

        assertEquals(8, params.size());

        final CustomParam charParam = params.get(0);
        assertEquals("char", charParam.getClassName());
        assertTrue(charParam.getValue() instanceof Character);
        assertEquals('A', charParam.getValue());

        final CustomParam byteParam = params.get(1);
        assertEquals("byte", byteParam.getClassName());
        assertTrue(byteParam.getValue() instanceof Byte);
        assertEquals((byte) 'B', byteParam.getValue());

        final CustomParam shortParam = params.get(2);
        assertEquals("short", shortParam.getClassName());
        assertTrue(shortParam.getValue() instanceof Short);
        assertEquals((short) 'C', shortParam.getValue());

        final CustomParam intParam = params.get(3);
        assertEquals("int", intParam.getClassName());
        assertTrue(intParam.getValue() instanceof Integer);
        assertEquals((int) 'D', intParam.getValue());

        final CustomParam longParam = params.get(4);
        assertEquals("long", longParam.getClassName());
        assertTrue(longParam.getValue() instanceof Long);
        assertEquals((long) 'E', longParam.getValue());

        final CustomParam floatParam = params.get(5);
        assertEquals("float", floatParam.getClassName());
        assertTrue(floatParam.getValue() instanceof Float);
        assertEquals(1.1f, floatParam.getValue());

        final CustomParam doubleParam = params.get(6);
        assertEquals("double", doubleParam.getClassName());
        assertTrue(doubleParam.getValue() instanceof Double);
        assertEquals(2.2, doubleParam.getValue());

        final CustomParam booleanParam = params.get(7);
        assertEquals("boolean", booleanParam.getClassName());
        assertTrue(booleanParam.getValue() instanceof Boolean);
        assertEquals(true, booleanParam.getValue());
    }

    /**
     * call(Character, Byte, Short, Integer, Long, Float, Double, Boolean, String)
     */
    @Test
    public void test_primitive_object() throws IOException {
        final List<CustomParam> params = loadParams("primitive-object");

        assertEquals(9, params.size());

        final CustomParam charParam = params.get(0);
        assertEquals("java.lang.Character", charParam.getClassName());
        assertTrue(charParam.getValue() instanceof Character);
        assertEquals('A', charParam.getValue());
        assertEquals(new Character('A'), charParam.getValue());

        final CustomParam byteParam = params.get(1);
        assertEquals("java.lang.Byte", byteParam.getClassName());
        assertTrue(byteParam.getValue() instanceof Byte);
        assertEquals((byte) 'B', byteParam.getValue());
        assertEquals(new Byte((byte) 'B'), byteParam.getValue());

        final CustomParam shortParam = params.get(2);
        assertEquals("java.lang.Short", shortParam.getClassName());
        assertTrue(shortParam.getValue() instanceof Short);
        assertEquals((short) 'C', shortParam.getValue());
        assertEquals(new Short((short) 'C'), shortParam.getValue());

        final CustomParam intParam = params.get(3);
        assertEquals("java.lang.Integer", intParam.getClassName());
        assertTrue(intParam.getValue() instanceof Integer);
        assertEquals((int) 'D', intParam.getValue());

        final CustomParam longParam = params.get(4);
        assertEquals("java.lang.Long", longParam.getClassName());
        assertTrue(longParam.getValue() instanceof Long);
        assertEquals((long) 'E', longParam.getValue());

        final CustomParam floatParam = params.get(5);
        assertEquals("java.lang.Float", floatParam.getClassName());
        assertTrue(floatParam.getValue() instanceof Float);
        assertEquals(1.1f, floatParam.getValue());

        final CustomParam doubleParam = params.get(6);
        assertEquals("java.lang.Double", doubleParam.getClassName());
        assertTrue(doubleParam.getValue() instanceof Double);
        assertEquals(2.2, doubleParam.getValue());

        final CustomParam booleanParam = params.get(7);
        assertEquals("java.lang.Boolean", booleanParam.getClassName());
        assertTrue(booleanParam.getValue() instanceof Boolean);
        assertEquals(true, booleanParam.getValue());

        final CustomParam strParam = params.get(8);
        assertEquals("java.lang.String", strParam.getClassName());
        assertTrue(strParam.getValue() instanceof String);
        assertEquals("test", strParam.getValue());
    }

    /**
     * call(Character, Byte, Short, Integer, Long, Double, Boolean)
     */
    @Test
    public void test_primitive_types() throws IOException {
        final List<CustomParam> params = loadParams("primitive-types");

        assertEquals(9, params.size());

        final CustomParam charParam1 = params.get(0);
        assertEquals("java.lang.Character", charParam1.getClassName());
        assertTrue(charParam1.getValue() instanceof Character);
        assertEquals('A', charParam1.getValue());
        assertEquals(new Character('A'), charParam1.getValue());

        final CustomParam charParam2 = params.get(1);
        assertEquals("java.lang.Character", charParam2.getClassName());
        assertTrue(charParam2.getValue() instanceof Character);
        assertEquals('A', charParam2.getValue());

        final CustomParam byteParam1 = params.get(2);
        assertEquals("java.lang.Byte", byteParam1.getClassName());
        assertTrue(byteParam1.getValue() instanceof Byte);
        assertEquals((byte) 'B', byteParam1.getValue());

        final CustomParam byteParam2 = params.get(3);
        assertEquals("java.lang.Byte", byteParam2.getClassName());
        assertTrue(byteParam2.getValue() instanceof Byte);
        assertEquals((byte) 'B', byteParam2.getValue());

        final CustomParam shortParam = params.get(4);
        assertEquals("java.lang.Short", shortParam.getClassName());
        assertTrue(shortParam.getValue() instanceof Short);
        assertEquals((short) 'C', shortParam.getValue());
        assertEquals(new Short((short) 'C'), shortParam.getValue());

        final CustomParam longParam = params.get(5);
        assertEquals("java.lang.Long", longParam.getClassName());
        assertTrue(longParam.getValue() instanceof Long);
        assertEquals((long) 'E', longParam.getValue());

        final CustomParam doubleParam = params.get(6);
        assertEquals("java.lang.Double", doubleParam.getClassName());
        assertTrue(doubleParam.getValue() instanceof Double);
        assertEquals(2.2, doubleParam.getValue());

        final CustomParam booleanParam = params.get(7);
        assertEquals("java.lang.Boolean", booleanParam.getClassName());
        assertTrue(booleanParam.getValue() instanceof Boolean);
        assertEquals(false, booleanParam.getValue());

        final CustomParam strParam = params.get(8);
        assertEquals(String.class.getName(), strParam.getClassName());
        assertTrue(strParam.getValue() instanceof String);
        assertEquals("test", strParam.getValue());
    }

    /**
     * call(byte[], short[], int[], long[], float[], double[], boolean[])
     */
    @Test
    public void test_primitive_array() throws IOException {
        final List<CustomParam> params = loadParams("primitive-array");

        assertEquals(7, params.size());

        final CustomParam byteParam = params.get(0);
        assertEquals(byte[].class.getName(), byteParam.getClassName());
        assertTrue(byteParam.getValue() instanceof byte[]);
        assertArrayEquals(new byte[] { (byte) 'A', (byte) 'B', (byte) 'C' }, (byte[]) byteParam.getValue());

        final CustomParam shortParam = params.get(1);
        assertEquals(short[].class.getName(), shortParam.getClassName());
        assertTrue(shortParam.getValue() instanceof short[]);
        assertArrayEquals(new short[] { (short) 'A', (short) 'B', (short) 'C' }, (short[]) shortParam.getValue());

        final CustomParam intParam = params.get(2);
        assertEquals(int[].class.getName(), intParam.getClassName());
        assertTrue(intParam.getValue() instanceof int[]);
        assertArrayEquals(new int[] { (int) 'A', (int) 'B', (int) 'C' }, (int[]) intParam.getValue());

        final CustomParam longParam = params.get(3);
        assertEquals(long[].class.getName(), longParam.getClassName());
        assertTrue(longParam.getValue() instanceof long[]);
        assertArrayEquals(new long[] { (long) 'A', (long) 'B', (long) 'C' }, (long[]) longParam.getValue());

        final CustomParam floatParam = params.get(4);
        assertEquals(float[].class.getName(), floatParam.getClassName());
        assertTrue(floatParam.getValue() instanceof float[]);
        new ExactComparisonCriteria().arrayEquals(null, new float[] { 1.1f, 2.2f, 3.3f }, (float[]) floatParam.getValue());

        final CustomParam doubleParam = params.get(5);
        assertEquals(double[].class.getName(), doubleParam.getClassName());
        assertTrue(doubleParam.getValue() instanceof double[]);
        new ExactComparisonCriteria().arrayEquals(null, new double[] { 1.1, 2.2, 3.3 }, (double[]) doubleParam.getValue());

        final CustomParam booleanParam = params.get(6);
        assertEquals(boolean[].class.getName(), booleanParam.getClassName());
        assertTrue(booleanParam.getValue() instanceof boolean[]);
        assertArrayEquals(new boolean[] { true, false, true }, (boolean[]) booleanParam.getValue());
    }

    @Test
    @Ignore
    public void test_isValuePresent_list() {
        fail("not impl yet!");
    }

    @Test
    @Ignore
    public void test_isValuePresent_map() {
        fail("not impl yet!");
    }

    @Test
    @Ignore
    public void test_isValuePresent_common() {
        fail("not impl yet!");
    }

    @Test
    @Ignore
    public void test_evalValue_list() {
        fail("not impl yet!");
    }

    @Test
    @Ignore
    public void test_evalValue_map() {
        fail("not impl yet!");
    }

    @Test
    @Ignore
    public void test_evalValue_common() {
        fail("not impl yet!");
    }
}
