package com.baishancloud.orchsym.sap.metadata.param;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.baishancloud.orchsym.sap.metadata.AbsTestJCoMetaUtil;

/**
 * @author GU Guoqiang
 *
 */
public class TestSAPParamRoot extends AbsTestJCoMetaUtil {
    @Test
    public void test_writer_parser() throws IOException {
        SAPParamRoot root = new SAPParamRoot();
        SAPParam structParam = new SAPParam();
        structParam.setName("istruct");
        structParam.setType(ESAPMetaType.STRUCTURE.getLowerName());
        final SAPTabParam sTable = createTableParam("STRUCT");
        structParam.setTabMeta(sTable);

        final List<SAPParam> importParams = createParams("iname");
        importParams.add(structParam);
        root.setImportParams(importParams);

        //
        final List<SAPParam> exportParams = createParams("ename");
        root.setExportParams(exportParams);

        //
        final SAPTabParam importTable = createTableParam("table1");
        importTable.setType(ESAPTabType.INPUT.getLowerName());

        final SAPTabParam exportTable = createTableParam("table2");
        exportTable.setType(ESAPTabType.IN_OUT.getLowerName());
        root.setTableParams(Arrays.asList(importTable, exportTable));

        //
        SAPParamRoot.Writer writer = new SAPParamRoot.Writer();
        SAPParamRoot.Parser parser = new SAPParamRoot.Parser();

        final String json = writer.write(root);
        // System.out.println(json);

        final SAPParamRoot parseRoot = parser.parse(json);
        assertNotNull(parseRoot);

        // import
        final List<SAPParam> importParams2 = parseRoot.getImportParams();
        assertNotNull(importParams2);
        assertEquals(3, importParams2.size());
        doTest_Param(importParams2.get(0), "iname1", "char", 50, 30, 0);
        doTest_Param(importParams2.get(1), "iname2", "float", 4, 4, 2); // use unicode length also

        // import structure
        final SAPParam importStructParam = importParams2.get(2);
        assertEquals("istruct", importStructParam.getName());
        assertEquals("structure", importStructParam.getType());
        assertEquals(54, importStructParam.getLength()); // 50+4
        assertEquals(34, importStructParam.getNucLength()); // 30+4
        final SAPTabParam importTabMeta = importStructParam.getTabMeta();
        assertNotNull(importTabMeta);
        assertEquals("STRUCT", importTabMeta.getName());
        assertNull(importTabMeta.getTabType());
        final List<SAPParam> structParams = importTabMeta.getParams();
        assertEquals(2, structParams.size());
        doTest_Param(structParams.get(0), "STRUCT_param1", "char", 50, 30, 0);
        doTest_Param(structParams.get(1), "STRUCT_param2", "float", 4, 4, 2);

        // export
        final List<SAPParam> exportParams2 = parseRoot.getExportParams();
        assertNotNull(exportParams2);
        assertEquals(2, exportParams2.size());
        doTest_Param(exportParams2.get(0), "ename1", "char", 50, 30, 0);
        doTest_Param(exportParams2.get(1), "ename2", "float", 4, 4, 2); // use unicode length also

        // table
        final List<SAPTabParam> tableParams = parseRoot.getTableParams();
        assertNotNull(tableParams);
        assertEquals(2, exportParams2.size());
        final SAPTabParam table1Param = tableParams.get(0);
        assertEquals("table1", table1Param.getName());
        assertEquals("input", table1Param.getType());
        assertEquals(54, table1Param.getLength()); // 50+4
        assertEquals(34, table1Param.getNucLength()); // 30+4

        List<SAPParam> tableChildrenParams1 = table1Param.getParams();
        assertNotNull(tableChildrenParams1);
        assertEquals(2, tableChildrenParams1.size());
        doTest_Param(tableChildrenParams1.get(0), "table1_param1", "char", 50, 30, 0);
        doTest_Param(tableChildrenParams1.get(1), "table1_param2", "float", 4, 4, 2);
    }

    private List<SAPParam> createParams(String baseName) {
        List<SAPParam> params = new ArrayList<>();
        params.add(createParam(baseName + 1, ESAPMetaType.CHAR, 50, 30, 0));
        params.add(createParam(baseName + 2, ESAPMetaType.FLOAT, 4, 0, 2));

        return params;
    }

    private SAPTabParam createTableParam(String tableName) {
        SAPTabParam table = new SAPTabParam();
        table.setName(tableName);
        table.setParams(createParams(tableName + "_param"));

        return table;
    }

    private void doTest_Param(SAPParam param, String name, String type, int length, int nucLength, int precision) {
        doTest_Param(param, name, type, length, nucLength, precision, null, null);
    }

    private void doTest_Param(SAPParam param, String name, String type, int length, int nucLength, int precision, String defaultValue, String desc) {
        assertEquals(name, param.getName());
        assertEquals(type, param.getType());
        assertEquals(type, param.getMetaType().getLowerName());
        assertEquals(length, param.getLength());
        assertEquals(nucLength, param.getNucLength());
        assertEquals(precision, param.getPrecision());
        if (defaultValue == null) {
            assertNull(param.getDefaultValue());
        } else {
            assertEquals(defaultValue, param.getDefaultValue());
        }
        if (desc == null) {
            assertNull(param.getDesc());
        } else {
            assertEquals(desc, param.getDesc());
        }
        assertNull(param.getTabMeta());
    }

    @Test
    public void test_load() throws IOException {
        File jsoFile = new File("src/test/resources/param/custom.json");
        StringBuffer lines = new StringBuffer();
        try (BufferedReader reader = new BufferedReader(new FileReader(jsoFile))) {
            String line = reader.readLine();
            while (line != null) {
                lines.append(line);
                line = reader.readLine();
            }
        }
        final SAPParamRoot parseRoot = new SAPParamRoot.Parser().parse(lines.toString());
        assertNotNull(parseRoot);
        final List<SAPParam> importParams = parseRoot.getImportParams();
        assertNotNull(importParams);
        assertEquals(3, importParams.size());
        doTest_Param(importParams.get(0), "name", "char", 50, 30, 0, "<NONE>", "import name");
        doTest_Param(importParams.get(1), "price", "float", 0, 0, 2, "0", null);
        
        final SAPParam structParam = importParams.get(2);
        assertEquals("address", structParam.getName());
        assertEquals("structure", structParam.getType());
        final SAPTabParam structMeta = structParam.getTabMeta();
        assertNotNull(structMeta);
        assertEquals("ZADDR", structMeta.getName());
        final List<SAPParam> structParams = structMeta.getParams();
        assertEquals(5, structParams.size());
        
        final List<SAPParam> exportParams = parseRoot.getExportParams();
        assertNotNull(exportParams);
        assertEquals(2, exportParams.size());
        
        final List<SAPTabParam> tableParams = parseRoot.getTableParams();
        assertNotNull(tableParams);
        assertEquals(2, tableParams.size());
        final SAPTabParam table1 = tableParams.get(0);
        assertEquals("staff", table1.getName());
        assertEquals("input", table1.getType());
        assertEquals(3, table1.getParams().size());
    }
}
