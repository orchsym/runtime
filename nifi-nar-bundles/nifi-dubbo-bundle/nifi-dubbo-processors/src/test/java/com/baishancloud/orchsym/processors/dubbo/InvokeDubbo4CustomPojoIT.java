package com.baishancloud.orchsym.processors.dubbo;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.nifi.util.MockFlowFile;
import org.junit.Test;

import com.baishancloud.orchsym.processors.dubbo.it.Person;
import com.baishancloud.orchsym.processors.dubbo.param.CustomParam;

/**
 * 
 * @author GU Guoqiang
 *
 *         NOTE: before start this Integration Test, need start one local Zookeeper and the inner InvokeDubboITProvider first.
 */
public class InvokeDubbo4CustomPojoIT extends AbsInvokeDubboIT {
    @Override
    protected String getBaseFolder() {
        return "custom_param";
    }

    @Test
    public void test_response_valid_true() throws IOException {
        doTest_valid(23, true);
    }

    @Test
    public void test_response_valid_false() throws IOException {
        doTest_valid(14, false);
    }

    private Map<String, Object> creatPerson(int id, String name, int age) {
        Map<String, Object> personValue = new LinkedHashMap<>();
        personValue.put(Constants.FIELD_CLASS, Person.class.getName());
        personValue.put("id", id);
        personValue.put("name", name);
        personValue.put("age", age);

        return personValue;
    }

    private void doTest_valid(int age, boolean valid) throws IOException {
        runner.setProperty(InvokeDubbo.SERVICE_METHOD, "valid");

        CustomParam param = new CustomParam(Person.class.getName(), creatPerson(1, "test", age));
        runner.setProperty(InvokeDubbo.CUSTOM_PARAMETERS, new CustomParam.Writer().write(param));

        runner.run();

        runner.assertTransferCount(InvokeDubbo.REL_FAILURE, 0);
        runner.assertTransferCount(InvokeDubbo.REL_SUCCESS, 1);
        final List<MockFlowFile> sucess = runner.getFlowFilesForRelationship(InvokeDubbo.REL_SUCCESS);
        assertEquals(1, sucess.size());
        sucess.get(0).assertContentEquals("{\"response\":" + valid + "}\n");

    }

    /**
     * 
     * boolean valid(Person person)
     */
    @Test
    public void test_response_valid_pojo() throws IOException {
        runner.setProperty(InvokeDubbo.SERVICE_METHOD, "valid");

        final String customSettings = loadContents("pojo");
        runner.setProperty(InvokeDubbo.CUSTOM_PARAMETERS, customSettings);

        runner.run();

        runner.assertTransferCount(InvokeDubbo.REL_FAILURE, 0);
        runner.assertTransferCount(InvokeDubbo.REL_SUCCESS, 1);
        final List<MockFlowFile> sucess = runner.getFlowFilesForRelationship(InvokeDubbo.REL_SUCCESS);
        assertEquals(1, sucess.size());
        sucess.get(0).assertContentEquals("{\"response\":true}\n");
    }

    /**
     * List<Person> filter(List<Person> persons, boolean withChild);
     */
    @Test
    public void test_response_filter_list() throws IOException {
        doTest_filterArray("pojo-list");
    }

    /**
     * 
     * Person[] filter(Person[] persons, boolean withChild);
     */
    @Test
    public void test_response_filter_array() throws IOException {
        doTest_filterArray("pojo-array");
    }

    private void doTest_filterArray(String filterDataFileName) throws IOException {
        runner.setProperty(InvokeDubbo.SERVICE_METHOD, "filter");

        final String customSettings = loadContents(filterDataFileName);
        runner.setProperty(InvokeDubbo.CUSTOM_PARAMETERS, customSettings);

        runner.run();

        runner.assertTransferCount(InvokeDubbo.REL_FAILURE, 0);
        runner.assertTransferCount(InvokeDubbo.REL_SUCCESS, 1);
        final List<MockFlowFile> sucess = runner.getFlowFilesForRelationship(InvokeDubbo.REL_SUCCESS);
        assertEquals(1, sucess.size());
        sucess.get(0).assertContentEquals("{\"response\":["//
                + "{\"additions\":null,\"children\":null,\"name\":\"Test\",\"id\":123,\"class\":\"com.baishancloud.orchsym.processors.dubbo.it.Person\",\"age\":20},"//
                + "{\"additions\":null,\"children\":null,\"name\":\"Mock\",\"id\":124,\"class\":\"com.baishancloud.orchsym.processors.dubbo.it.Person\",\"age\":28}]}\n");

    }

    /**
     * Map<Integer, Person> convert(Map<String, Person> persons)
     */
    @Test
    public void test_response_convert_map() throws IOException {
        runner.setProperty(InvokeDubbo.SERVICE_METHOD, "convert");

        final String customSettings = loadContents("pojo-map");
        runner.setProperty(InvokeDubbo.CUSTOM_PARAMETERS, customSettings);

        runner.run();

        runner.assertTransferCount(InvokeDubbo.REL_FAILURE, 0);
        runner.assertTransferCount(InvokeDubbo.REL_SUCCESS, 1);
        final List<MockFlowFile> sucess = runner.getFlowFilesForRelationship(InvokeDubbo.REL_SUCCESS);
        assertEquals(1, sucess.size());
        sucess.get(0).assertContentEquals("{\"response\":{" + //
                "\"123\":{\"additions\":null,\"children\":null,\"name\":\"Test\",\"id\":123,\"class\":\"com.baishancloud.orchsym.processors.dubbo.it.Person\",\"age\":20}," + //
                "\"124\":{\"additions\":null,\"children\":null,\"name\":\"Mock\",\"id\":124,\"class\":\"com.baishancloud.orchsym.processors.dubbo.it.Person\",\"age\":28}}}\n");
    }
}
