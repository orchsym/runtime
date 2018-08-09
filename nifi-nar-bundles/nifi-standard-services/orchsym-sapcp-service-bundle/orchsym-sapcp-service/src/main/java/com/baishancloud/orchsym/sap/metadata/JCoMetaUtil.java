package com.baishancloud.orchsym.sap.metadata;

import java.util.List;

import com.baishancloud.orchsym.sap.metadata.param.AbsParam;
import com.baishancloud.orchsym.sap.metadata.param.ESAPMetaType;
import com.baishancloud.orchsym.sap.metadata.param.ESAPParamType;
import com.baishancloud.orchsym.sap.metadata.param.ESAPTabType;
import com.baishancloud.orchsym.sap.metadata.param.SAPParam;
import com.baishancloud.orchsym.sap.metadata.param.SAPParamRoot;
import com.baishancloud.orchsym.sap.metadata.param.SAPTabParam;
import com.sap.conn.jco.JCo;
import com.sap.conn.jco.JCoFunctionTemplate;
import com.sap.conn.jco.JCoListMetaData;
import com.sap.conn.jco.JCoRecordMetaData;

/**
 * 
 * @author GU Guoqiang
 *
 */
public final class JCoMetaUtil {
    public static boolean unifyUpperCase = true;

    public static JCoFunctionTemplate createFunTemplate(String funName, final SAPParamRoot paramRoot) {
        if (funName == null || paramRoot == null) {
            return null;
        }

        final JCoListMetaData importMeta = createSimpleMetaData(ESAPParamType.IMPORT, paramRoot.getImportParams());

        final JCoListMetaData exportMeta = createSimpleMetaData(ESAPParamType.EXPORT, paramRoot.getExportParams());

        final JCoListMetaData tableMetaData = createTablesMetaData(paramRoot);

        if (unifyUpperCase) {
            funName = funName.toUpperCase();
        }
        JCoFunctionTemplate fT = JCo.createFunctionTemplate(funName, importMeta, exportMeta, null, tableMetaData, null);
        return fT;
    }

    static int getNucLength(AbsParam param) {
        int nucLength = param.getNucLength();
        if (nucLength <= 0) { // set default Non-Unicode
            nucLength = param.getLength();
        }
        return nucLength;
    }

    static JCoListMetaData createSimpleMetaData(ESAPParamType paramType, final List<SAPParam> importParams) {
        if (importParams == null || importParams.isEmpty()) {
            return null;
        }
        JCoListMetaData listMeta = JCo.createListMetaData(paramType.getMetaName());
        int jcoType = paramType.getJcoType();

        for (SAPParam param : importParams) {
            final ESAPMetaType metaType = param.getMetaType();
            final int jcoParamType = metaType.getJCoType();
            String name = param.getName();
            if (unifyUpperCase) {
                name = name.toUpperCase();
            }
            if (ESAPMetaType.EXCEPTION == metaType) {
                // ignore?
            } else if (ESAPMetaType.TABLE == metaType || ESAPMetaType.STRUCTURE == metaType) {
                final SAPTabParam table = param.getTabMeta();
                if (table != null) {
                    final JCoRecordMetaData tableRecordMetaData = createTableMetaData(table);
                    if (tableRecordMetaData != null) {
                        if (ESAPMetaType.TABLE == metaType) {
                            jcoType = ESAPParamType.TABLE.getJcoType();// 0 for table
                        } // else{ // structure use the same type of import/output

                        listMeta.add(name, jcoParamType, tableRecordMetaData, jcoType);
                    }

                }
            } else {
                listMeta.add(name, jcoParamType, getNucLength(param), param.getLength(), param.getPrecision(), param.getDefaultValue(), param.getDesc(), jcoType, null, null);
            }

        }
        listMeta.lock();

        return listMeta;
    }

    static JCoListMetaData createTablesMetaData(final SAPParamRoot paramRoot) {
        if (paramRoot == null) {
            return null;
        }

        final List<SAPTabParam> tableParams = paramRoot.getTableParams();
        if (tableParams == null || tableParams.isEmpty()) {
            return null;
        }

        JCoListMetaData tableMeta = JCo.createListMetaData("TABLE");//$NON-NLS-1$

        for (SAPTabParam table : tableParams) {
            String name = table.getName();
            if (unifyUpperCase) {
                name = name.toUpperCase();
            }
            // JCoRecordMetaData tableRecordMeta = createTableMetaData(table);
            // if (tableRecordMeta != null) {

            // tableMeta.add(name, ESAPMetaType.TABLE.getJCoType(), // 99
            // tableRecordMeta, ESAPParamType.TABLE.getJcoType()); // 0
            tableMeta.add(name, ESAPMetaType.TABLE.getJCoType(), // 99
                    table.getNucLength(), table.getLength(), 0, //
                    null, null, //
                    ESAPParamType.TABLE.getJcoType(), table.getTypeName(), null);
            // }
        }
        tableMeta.lock();

        return tableMeta;
    }

    static JCoRecordMetaData createTableMetaData(SAPTabParam table) {
        if (table == null) {
            return null;
        }
        final ESAPTabType type = table.getTabType();
        if (type == null) {// not table, should be structure
            //
        }

        final List<SAPParam> params = table.getParams();
        if (params == null || params.isEmpty()) { // no params
            return null;
        }

        String typeName = table.getTypeName();
        if (unifyUpperCase) {
            typeName = typeName.toUpperCase();
        }
        JCoRecordMetaData tableRecordMeta = JCo.createRecordMetaData(typeName, params.size());

        int ucoffset = 0;
        int nucoffset = 0;

        for (SAPParam param : params) {
            ESAPMetaType metaType = param.getMetaType();
            final int jcoParamType = metaType.getJCoType();
            String name = param.getName();
            if (unifyUpperCase) {
                name = name.toUpperCase();
            }
            if (ESAPMetaType.EXCEPTION == metaType) {
                // ignore?
            } else if (ESAPMetaType.TABLE == metaType || ESAPMetaType.STRUCTURE == metaType) {
                final SAPTabParam child = param.getTabMeta();
                if (child != null) {
                    final JCoRecordMetaData tableRecordMetaData = createTableMetaData(child);
                    if (tableRecordMetaData != null) {
                        tableRecordMeta.add(name, jcoParamType, // 99 for table, 17 for structure
                                tableRecordMetaData.getRecordLength(), nucoffset, tableRecordMetaData.getUnicodeRecordLength(), ucoffset, 0, param.getDesc(), tableRecordMetaData, null);
                        ucoffset += tableRecordMetaData.getUnicodeRecordLength();
                        nucoffset += tableRecordMetaData.getRecordLength();
                    }

                }
            } else {
                final int nucLength = getNucLength(param);
                tableRecordMeta.add(name, jcoParamType, nucLength, nucoffset, param.getLength(), ucoffset, param.getPrecision(), param.getDesc(), null, null);
                ucoffset += param.getLength();
                nucoffset += nucLength;
            }
        }

        tableRecordMeta.setRecordLength(table.getNucLength(), table.getLength());

        tableRecordMeta.lock();
        return tableRecordMeta;
    }

}
