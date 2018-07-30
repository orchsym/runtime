package com.baishancloud.orchsym.sap.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.baishancloud.orchsym.sap.metadata.param.ESAPMetaType;
import com.baishancloud.orchsym.sap.metadata.param.ESAPParamType;
import com.baishancloud.orchsym.sap.metadata.param.SAPParam;
import com.baishancloud.orchsym.sap.metadata.param.SAPParamRoot;
import com.baishancloud.orchsym.sap.metadata.param.SAPTabParam;
import com.sap.conn.jco.JCoListMetaData;
import com.sap.conn.jco.JCoRecordMetaData;

/**
 * @author GU Guoqiang
 *
 */
public class JCoMetaUtilIT extends AbsTestJCoMetaUtil {

    @Test
    public void test_createSimpleMetaData_empty() {
        JCoListMetaData metadata = JCoMetaUtil.createSimpleMetaData(ESAPParamType.IMPORT, null);
        assertNull(metadata);

        List<SAPParam> params = new ArrayList<>();
        metadata = JCoMetaUtil.createSimpleMetaData(ESAPParamType.IMPORT, params);
        assertNull(metadata);
    }

    // @Test
    public void test_createSimpleMetaData_import_structureType() {

    }

    // @Test
    public void test_createSimpleMetaData_import_tableType() {

    }

    // @Test
    public void test_createSimpleMetaData_import_innerTableType() {

    }

    @Test
    public void test_createTablesMetaData_empty() {
        JCoListMetaData metadata = JCoMetaUtil.createTablesMetaData(null);
        assertNull(metadata);

        // null list
        SAPParamRoot root = mock(SAPParamRoot.class);
        metadata = JCoMetaUtil.createTablesMetaData(root);
        assertNull(metadata);

        // empty list
        List<SAPTabParam> tableParams = new ArrayList<>();
        when(root.getTableParams()).thenReturn(tableParams);
        metadata = JCoMetaUtil.createTablesMetaData(root);
        assertNull(metadata);
        verify(root, times(2)).getTableParams();
    }

    @Test
    public void test_createTableMetaData_empty() {
        JCoRecordMetaData metadata = JCoMetaUtil.createTableMetaData(null);
        assertNull(metadata);

        // null list
        SAPTabParam tableParam = mock(SAPTabParam.class);
        metadata = JCoMetaUtil.createTableMetaData(tableParam);
        assertNull(metadata);

        // empty list
        List<SAPParam> params = new ArrayList<>();
        when(tableParam.getParams()).thenReturn(params);
        metadata = JCoMetaUtil.createTableMetaData(tableParam);
        assertNull(metadata);
        verify(tableParam, times(2)).getParams();
    }

}
