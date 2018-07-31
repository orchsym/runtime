package com.baishancloud.orchsym.sap.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.baishancloud.orchsym.sap.metadata.param.ESAPMetaType;
import com.baishancloud.orchsym.sap.metadata.param.ESAPParamType;
import com.baishancloud.orchsym.sap.metadata.param.SAPParam;
import com.baishancloud.orchsym.sap.metadata.param.SAPTabParam;
import com.sap.conn.jco.JCoListMetaData;
import com.sap.conn.jco.JCoRecordMetaData;

/**
 * @author GU Guoqiang
 *
 */
@RunWith(Parameterized.class)
@SuppressWarnings("rawtypes")
public class StructureJCoMetaUtilIT extends AbsTestJCoMetaUtil {
    private ESAPParamType paramType;

    public StructureJCoMetaUtilIT(ESAPParamType paramType) {
        super();
        this.paramType = paramType;
    }

    @Parameters
    public static Collection setup() {
        return Arrays.asList(ESAPParamType.IMPORT, ESAPParamType.EXPORT);
    }

    @Test
    public void test_createSimpleMetaData() {
        List<SAPParam> paramsList = new ArrayList<>();

        paramsList.add(createParam("ID", ESAPMetaType.INT, 4));

        SAPParam nameParam = createParam("name", ESAPMetaType.STRUCTURE, 0);
        paramsList.add(nameParam);

        SAPTabParam structParam = new SAPTabParam();
        structParam.setName("Zname");
        nameParam.setTabMeta(structParam);

        List<SAPParam> structParamsList = new ArrayList<>();
        structParam.setParams(structParamsList);
        structParamsList.add(createParam("first_name", ESAPMetaType.CHAR, 50, 30, 0, null, null));
        structParamsList.add(createParam("middle_name", ESAPMetaType.CHAR, 30, 20, 0, null, null));
        structParamsList.add(createParam("last_name", ESAPMetaType.CHAR, 50, 30, 0, null, null));

        JCoListMetaData metadata = JCoMetaUtil.createSimpleMetaData(paramType, paramsList);
        assertNotNull(metadata);

        assertEquals(paramType.getMetaName(), metadata.getName());
        assertEquals(paramsList.size(), metadata.getFieldCount());

        doTest_createSimpleMetaData(metadata, 0, "ID", 4, ESAPMetaType.INT);

        assertEquals(ESAPMetaType.STRUCTURE.getJCoType(), metadata.getType(1));
        JCoRecordMetaData recordMetaData = metadata.getRecordMetaData(1);
        assertEquals("ZNAME", recordMetaData.getName());
        assertEquals(structParamsList.size(), recordMetaData.getFieldCount());

        doTest_createSimpleMetaData(recordMetaData, 0, "FIRST_NAME", 50, 30, ESAPMetaType.CHAR, 0);
        doTest_createSimpleMetaData(recordMetaData, 1, "MIDDLE_NAME", 30, 20, ESAPMetaType.CHAR, 0);
        doTest_createSimpleMetaData(recordMetaData, 2, "LAST_NAME", 50, 30, ESAPMetaType.CHAR, 0);
    }

}
