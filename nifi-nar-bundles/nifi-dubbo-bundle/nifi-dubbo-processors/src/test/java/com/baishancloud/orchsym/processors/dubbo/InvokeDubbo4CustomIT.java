/**
 * 
 */
package com.baishancloud.orchsym.processors.dubbo;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.nifi.util.MockFlowFile;
import org.junit.Ignore;
import org.junit.Test;

import com.baishancloud.orchsym.processors.dubbo.param.CustomParam;

/**
 * @author GU Guoqiang
 *
 */
public class InvokeDubbo4CustomIT extends AbsInvokeDubboIT {

    @Override
    protected String getBaseFolder() {
        return "custom_param";
    }

    @Test
    public void test_response_valid() throws IOException {
        runner.run();

        runner.assertTransferCount(InvokeDubbo.REL_FAILURE, 0);
        runner.assertTransferCount(InvokeDubbo.REL_SUCCESS, 1);
        final List<MockFlowFile> sucess = runner.getFlowFilesForRelationship(InvokeDubbo.REL_SUCCESS);
        assertEquals(1, sucess.size());
        sucess.get(0).assertContentEquals("{\"response\":\"Hello world\"}\n");
    }

    /**
     * String call(char arg1, byte arg2, short arg3, int arg4, long arg5, float arg6, double arg7, boolean arg8)
     */
    @Test
    public void test_response_call_primitive_api() throws IOException {
        runner.setProperty(InvokeDubbo.SERVICE_METHOD, "call");
        CustomParam param1 = new CustomParam(char.class.getName(), 'A');
        CustomParam param2 = new CustomParam(byte.class.getName(), (byte) 'A');
        CustomParam param3 = new CustomParam(short.class.getName(), (short) 11);
        CustomParam param4 = new CustomParam(int.class.getName(), 111);
        CustomParam param5 = new CustomParam(long.class.getName(), 222L);
        CustomParam param6 = new CustomParam(float.class.getName(), 1.1f);
        CustomParam param7 = new CustomParam(double.class.getName(), 2.2);
        CustomParam param8 = new CustomParam(boolean.class.getName(), true);

        final String customSettings = new CustomParam.Writer().write(Arrays.asList(param1, param2, param3, param4, param5, param6, param7, param8));
        runner.setProperty(InvokeDubbo.CUSTOM_PARAMETERS, customSettings);

        runner.run();

        runner.assertTransferCount(InvokeDubbo.REL_FAILURE, 0);
        runner.assertTransferCount(InvokeDubbo.REL_SUCCESS, 1);
        final List<MockFlowFile> sucess = runner.getFlowFilesForRelationship(InvokeDubbo.REL_SUCCESS);
        assertEquals(1, sucess.size());
        sucess.get(0).assertContentEquals("{\"response\":\"A,65,11,111,222,1.1,2.2,true,Hello\"}\n");
    }

    /**
     * String call(int arg1, double arg2, boolean arg3, String arg4)
     */
    @Test
    public void test_response_call_primitiveDefault() throws IOException {
        runner.setProperty(InvokeDubbo.SERVICE_METHOD, "call");

        final String customSettings = loadContents("primitive");
        runner.setProperty(InvokeDubbo.CUSTOM_PARAMETERS, customSettings);

        runner.run();

        runner.assertTransferCount(InvokeDubbo.REL_FAILURE, 0);
        runner.assertTransferCount(InvokeDubbo.REL_SUCCESS, 1);
        final List<MockFlowFile> sucess = runner.getFlowFilesForRelationship(InvokeDubbo.REL_SUCCESS);
        assertEquals(1, sucess.size());
        sucess.get(0).assertContentEquals("{\"response\":\"11,11.11,true,test\"}\n");
    }

    /**
     * String call(char arg1, byte arg2, short arg3, int arg4, long arg5, float arg6, double arg7, boolean arg8)
     */
    @Test
    public void test_response_call_primitiveClass() throws IOException {
        runner.setProperty(InvokeDubbo.SERVICE_METHOD, "call");

        final String customSettings = loadContents("primitive-class");
        runner.setProperty(InvokeDubbo.CUSTOM_PARAMETERS, customSettings);

        runner.run();

        runner.assertTransferCount(InvokeDubbo.REL_FAILURE, 0);
        runner.assertTransferCount(InvokeDubbo.REL_SUCCESS, 1);
        final List<MockFlowFile> sucess = runner.getFlowFilesForRelationship(InvokeDubbo.REL_SUCCESS);
        assertEquals(1, sucess.size());
        sucess.get(0).assertContentEquals("{\"response\":\"A,66,67,68,69,1.1,2.2,true,Hello\"}\n");
    }

    /**
     * String call(byte[] arg1, short[] arg2, int[] arg3, long[] arg4, float[] arg5, double[] arg6, boolean[] arg7)
     */
    @Test
    public void test_response_call_primitiveArray() throws IOException {
        runner.setProperty(InvokeDubbo.SERVICE_METHOD, "call");

        final String customSettings = loadContents("primitive-array");
        runner.setProperty(InvokeDubbo.CUSTOM_PARAMETERS, customSettings);

        runner.run();

        runner.assertTransferCount(InvokeDubbo.REL_FAILURE, 0);
        runner.assertTransferCount(InvokeDubbo.REL_SUCCESS, 1);
        final List<MockFlowFile> sucess = runner.getFlowFilesForRelationship(InvokeDubbo.REL_SUCCESS);
        assertEquals(1, sucess.size());
        sucess.get(0).assertContentEquals("{\"response\":\"65,66,67;65,66,67;65,66,67;65,66,67;1.1,2.2,3.3;1.1,2.2,3.3;true,false,true\"}\n");
    }

    /**
     * String call(char[] arg1)
     * 
     * Currently, don't support the char[], when send data, will be converted to String.
     */
    @Test(expected = AssertionError.class)
    public void test_response_call_primitiveArray_char() throws IOException {
        runner.setProperty(InvokeDubbo.SERVICE_METHOD, "call");

        final String customSettings = loadContents("primitive-array-char");
        runner.setProperty(InvokeDubbo.CUSTOM_PARAMETERS, customSettings);

        runner.run();
    }

    /**
     * String call(Character arg1, Byte arg2, Short arg3, Integer arg4, Long arg5, Float arg6, Double arg7, Boolean arg8, String arg9)
     */
    @Test
    public void test_response_call_primitive_object() throws IOException {
        runner.setProperty(InvokeDubbo.SERVICE_METHOD, "call");

        final String customSettings = loadContents("primitive-object");
        runner.setProperty(InvokeDubbo.CUSTOM_PARAMETERS, customSettings);

        runner.run();

        runner.assertTransferCount(InvokeDubbo.REL_FAILURE, 0);
        runner.assertTransferCount(InvokeDubbo.REL_SUCCESS, 1);
        final List<MockFlowFile> sucess = runner.getFlowFilesForRelationship(InvokeDubbo.REL_SUCCESS);
        assertEquals(1, sucess.size());
        sucess.get(0).assertContentEquals("{\"response\":\"A,66,67,68,69,1.1,2.2,true,test\"}\n");
    }

    /**
     * List<String> call(Character[] arg1, Byte[] arg2, Short[] arg3, Integer[] arg4, Long[] arg5, Float[] arg6, Double[] arg7, Boolean[] arg8, String[] arg9)
     */
    @Test
    public void test_response_call_primitive_objectArray() throws IOException {
        runner.setProperty(InvokeDubbo.SERVICE_METHOD, "call");

        final String customSettings = loadContents("primitive-object-array");
        runner.setProperty(InvokeDubbo.CUSTOM_PARAMETERS, customSettings);

        runner.run();

        runner.assertTransferCount(InvokeDubbo.REL_FAILURE, 0);
        runner.assertTransferCount(InvokeDubbo.REL_SUCCESS, 1);
        final List<MockFlowFile> sucess = runner.getFlowFilesForRelationship(InvokeDubbo.REL_SUCCESS);
        assertEquals(1, sucess.size());
        sucess.get(0).assertContentEquals("{\"response\":[\"A,B,C\",\"65,66,67\",\"65,66,67\",\"65,66,67\",\"65,66,67\",\"1.1,2.2,3.3\",\"1.1,2.2,3.3\",\"true,false,true\",\"A,B,C\"]}\n");
    }

    /**
     * List<String> call(Character[] arg1, Byte[] arg2, Short[] arg3, Integer[] arg4, Long[] arg5, Float[] arg6, Double[] arg7, Boolean[] arg8, String[] arg9)
     */
    @Test
    public void test_response_call_primitive_objectArrayWithClass() throws IOException {
        runner.setProperty(InvokeDubbo.SERVICE_METHOD, "call");

        final String customSettings = loadContents("primitive-object-array-class");
        runner.setProperty(InvokeDubbo.CUSTOM_PARAMETERS, customSettings);

        runner.run();

        runner.assertTransferCount(InvokeDubbo.REL_FAILURE, 0);
        runner.assertTransferCount(InvokeDubbo.REL_SUCCESS, 1);
        final List<MockFlowFile> sucess = runner.getFlowFilesForRelationship(InvokeDubbo.REL_SUCCESS);
        assertEquals(1, sucess.size());
        sucess.get(0).assertContentEquals("{\"response\":[\"A,B,C\",\"65,66,67\",\"65,66,67\",\"65,66,67\",\"65,66,67\",\"1.1,2.2,3.3\",\"1.1,2.2,3.3\",\"true,false,true\",\"A,B,C\"]}\n");
    }

    /**
     * List<String> call(List<Character> arg1, List<Byte> arg2, List<Short> arg3, List<Integer> arg4, List<Long> arg5, List<Float> arg6, List<Double> arg7, List<Boolean> arg8, List<String> arg9)
     */
    @Test
    public void test_response_call_primitive_objectList() throws IOException {
        runner.setProperty(InvokeDubbo.SERVICE_METHOD, "call");

        final String customSettings = loadContents("primitive-object-list");
        runner.setProperty(InvokeDubbo.CUSTOM_PARAMETERS, customSettings);

        runner.run();

        runner.assertTransferCount(InvokeDubbo.REL_FAILURE, 0);
        runner.assertTransferCount(InvokeDubbo.REL_SUCCESS, 1);
        final List<MockFlowFile> sucess = runner.getFlowFilesForRelationship(InvokeDubbo.REL_SUCCESS);
        assertEquals(1, sucess.size());
        sucess.get(0).assertContentEquals("{\"response\":[\"A,B,C\",\"65,66,67\",\"65,66,67\",\"65,66,67\",\"65,66,67\",\"1.1,2.2,3.3\",\"1.1,2.2,3.3\",\"true,false,true\",\"A,B,C\"]}\n");
    }

    /**
     * Map<String, Object> call(Map<Short, Character> arg1, Map<Long, Byte> arg2, Map<Double, Boolean> arg3);
     * 
     * don't work for key as Map yet, need check for Jackson.
     */
    @SuppressWarnings("rawtypes")
    @Test
    @Ignore
    public void test_response_call_primitive_objectMap_api() throws IOException {
        runner.setProperty(InvokeDubbo.SERVICE_METHOD, "call");

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
        runner.setProperty(InvokeDubbo.CUSTOM_PARAMETERS, customSettings);

        runner.run();

        runner.assertTransferCount(InvokeDubbo.REL_FAILURE, 0);
        runner.assertTransferCount(InvokeDubbo.REL_SUCCESS, 1);
        final List<MockFlowFile> sucess = runner.getFlowFilesForRelationship(InvokeDubbo.REL_SUCCESS);
        assertEquals(1, sucess.size());
        sucess.get(0).assertContentEquals("");
    }

    private Map<String, Object> createClassMap(Object value) {
        Map<String, Object> result = new HashMap<>();
        result.put(Constants.FIELD_CLASS, value.getClass().getName());
        result.put(Constants.FIELD_VALUE, value);
        return result;
    }

    /**
     * Map<String, Object> call(Map<Short, Character> arg1, Map<Long, Byte> arg2, Map<Double, Boolean> arg3);
     *
     */
    @Test
    public void test_response_call_primitive_objectMap() throws IOException {
        runner.setProperty(InvokeDubbo.SERVICE_METHOD, "call");

        final String customSettings = loadContents("primitive-object-map");
        runner.setProperty(InvokeDubbo.CUSTOM_PARAMETERS, customSettings);

        runner.run();

        runner.assertTransferCount(InvokeDubbo.REL_FAILURE, 0);
        runner.assertTransferCount(InvokeDubbo.REL_SUCCESS, 1);
        final List<MockFlowFile> sucess = runner.getFlowFilesForRelationship(InvokeDubbo.REL_SUCCESS);
        assertEquals(1, sucess.size());
        sucess.get(0).assertContentEquals("{\"response\":{\"1\":65,\"2.0\":false}}\n");
    }

    /**
     * Map<String, Object> call(Map<String, Character> arg1, Map<String, Short> arg2);
     *
     */
    @Test
    public void test_response_call_primitive_objectMap_keyString() throws IOException {
        runner.setProperty(InvokeDubbo.SERVICE_METHOD, "call");

        final String customSettings = loadContents("primitive-object-map-kstring");
        runner.setProperty(InvokeDubbo.CUSTOM_PARAMETERS, customSettings);

        runner.run();

        runner.assertTransferCount(InvokeDubbo.REL_FAILURE, 0);
        runner.assertTransferCount(InvokeDubbo.REL_SUCCESS, 1);
        final List<MockFlowFile> sucess = runner.getFlowFilesForRelationship(InvokeDubbo.REL_SUCCESS);
        assertEquals(1, sucess.size());
        sucess.get(0).assertContentEquals("{\"response\":{\"hello\":\"A\",\"world\":\"B\",\"A\":123,\"B\":456}}\n");
    }
}
