package com.baishancloud.orchsym.sap.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collection;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.baishancloud.orchsym.sap.metadata.param.ESAPMetaType;
import com.baishancloud.orchsym.sap.metadata.param.ESAPParamType;
import com.sap.conn.jco.JCoListMetaData;
import com.sap.conn.jco.JCoMetaData;
import com.sap.conn.jco.rt.AbstractMetaData;

/**
 * @author GU Guoqiang
 *
 */
public abstract class AbsImportExportTestJCoMetaUtil extends AbsTestJCoMetaUtil {
    private ESAPParamType paramType;
    private int nucLength;
    private int ucLength;
    private int pricesion;
    private int fixLength;
    private int fixByteLength;
    private int fixJavaLength;

    public AbsImportExportTestJCoMetaUtil(ESAPParamType paramType, int ucLength) {
        this(paramType, ucLength, ucLength);
    }

    public AbsImportExportTestJCoMetaUtil(ESAPParamType paramType, int nucLength, int ucLength) {
        this(paramType, nucLength, ucLength, 0);
    }

    public AbsImportExportTestJCoMetaUtil(ESAPParamType paramType, int nucLength, int ucLength, int pricesion) {
        this(paramType, nucLength, ucLength, pricesion, -1, -1, -1);
    }

    public AbsImportExportTestJCoMetaUtil(ESAPParamType paramType, int nucLength, int ucLength, int pricesion, int fixLength, int fixByteLength, int fixJavaLength) {
        super();
        this.paramType = paramType;
        this.nucLength = nucLength;
        this.ucLength = ucLength;
        this.pricesion = pricesion;
        this.fixLength = fixLength;
        this.fixByteLength = fixByteLength;
        this.fixJavaLength = fixJavaLength;
    }

    protected String getParamName() {
        return "Name";
    }

    protected abstract ESAPMetaType getMetaType();

    @Test
    public void test_createSimpleMetaData() {
        final ESAPMetaType metaType = getMetaType();

        JCoListMetaData metadata = JCoMetaUtil.createSimpleMetaData(paramType, Arrays.asList(//
                createParam(getParamName(), metaType, ucLength, nucLength, pricesion, null, null))); // need set the uclength, nuclength can be set also.
        assertNotNull(metadata);
        assertEquals(paramType.getMetaName(), metadata.getName());
        assertEquals(1, metadata.getFieldCount());

        assertEquals(getParamName().toUpperCase(), metadata.getName(0));

        assertEquals(metaType.getJCoType(), metadata.getType(0));
        assertEquals(metaType.getName(), metadata.getTypeAsString(0));

        assertEquals(metaType.getLength(nucLength), metadata.getLength(0));
        if (fixLength > -1) { // set
            assertEquals(fixLength, metadata.getLength(0));
        } else {
            assertEquals(nucLength, metadata.getLength(0));
        }
        assertEquals(metaType.getByteLength(nucLength), metadata.getByteLength(0));
        if (fixByteLength > -1) {
            assertEquals(fixByteLength, metadata.getByteLength(0));
            assertEquals(fixByteLength, metadata.getUnicodeByteLength(0));
        } else {
            assertEquals(nucLength, metadata.getByteLength(0));
            assertEquals(ucLength, metadata.getUnicodeByteLength(0));
        }

        assertEquals(pricesion, metadata.getDecimals(0));

        if (metadata instanceof AbstractMetaData) {
            final AbstractMetaData abstractMetaData = (AbstractMetaData) metadata;
            assertEquals(metaType.getJavaLength(nucLength), abstractMetaData.getJavaBufferLength(0));
            if (fixJavaLength > -1) {
                assertEquals(fixJavaLength, abstractMetaData.getJavaBufferLength(0));
            }else {
                assertEquals(nucLength, abstractMetaData.getJavaBufferLength(0));
            }
        }
    }
}
