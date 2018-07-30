package com.baishancloud.orchsym.sap.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.BeforeClass;

import com.baishancloud.orchsym.sap.metadata.param.ESAPMetaType;
import com.baishancloud.orchsym.sap.metadata.param.SAPParam;
import com.sap.conn.jco.JCoListMetaData;
import com.sap.conn.jco.JCoMetaData;
import com.sap.conn.jco.rt.AbstractMetaData;

/**
 * @author GU Guoqiang
 *
 */
public class AbsTestJCoMetaUtil {

    @BeforeClass
    public static void init() {
        JCoMetaUtil.unifyUpperCase = true;
    }

    protected SAPParam createParam(String name, ESAPMetaType type, int length) {
        return createParam(name, type, length, length, 0, null, null);
    }

    protected SAPParam createParam(String name, ESAPMetaType type, int length, int precision) {
        return createParam(name, type, length, length, precision);
    }

    protected SAPParam createParam(String name, ESAPMetaType type, int length, int nucLength, int precision) {
        return createParam(name, type, length, nucLength, precision, null, null);
    }

    protected SAPParam createParam(String name, ESAPMetaType type, int ucLength, int nucLength, int precision, String defaultValue, String desc) {
        SAPParam param = new SAPParam();

        param.setName(name);
        param.setType(type.getLowerName());
        param.setLength(ucLength);
        param.setDefaultValue(defaultValue);
        param.setDesc(desc);
        param.setNucLength(nucLength);
        param.setPrecision(precision);

        return param;
    }

    protected void doTest_createSimpleMetaData(JCoMetaData metadata, int index, String name, int ucbyteLength, ESAPMetaType metaType) {
        doTest_createSimpleMetaData(metadata, index, name, ucbyteLength, ucbyteLength, metaType, 0);
    }

    protected void doTest_createSimpleMetaData(JCoMetaData metadata, int index, String name, int ucbyteLength, int nucbyteLength, ESAPMetaType metaType, int precision) {
        assertEquals(name, metadata.getName(index));
        assertEquals(metaType.getJCoType(), metadata.getType(index));
        assertEquals(metaType.getName(), metadata.getTypeAsString(index));
        assertEquals(precision, metadata.getDecimals(index));
        assertEquals(metaType.getLength(nucbyteLength), metadata.getLength(index));
        assertEquals(metaType.getByteLength(nucbyteLength), metadata.getByteLength(index));
        assertEquals(ucbyteLength, metadata.getUnicodeByteLength(index));
        if (metadata instanceof JCoListMetaData) {
            assertNull(((JCoListMetaData) metadata).getDefault(index));
        }
        assertNull(metadata.getDescription(index));

        if (metadata instanceof AbstractMetaData) {
            final AbstractMetaData abstractMetaData = (AbstractMetaData) metadata;
            assertEquals(metaType.getJavaLength(nucbyteLength), abstractMetaData.getJavaBufferLength(index));
        }
    }
}
