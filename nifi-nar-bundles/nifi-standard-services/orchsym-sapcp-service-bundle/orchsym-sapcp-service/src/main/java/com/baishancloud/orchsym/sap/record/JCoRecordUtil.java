package com.baishancloud.orchsym.sap.record;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.avro.generic.GenericRecord;
import org.apache.nifi.serialization.record.MapRecord;

import com.sap.conn.jco.JCoField;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoMetaData;
import com.sap.conn.jco.JCoParameterList;
import com.sap.conn.jco.JCoRecord;
import com.sap.conn.jco.JCoStructure;
import com.sap.conn.jco.JCoTable;

/**
 * @author GU Guoqiang
 *
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class JCoRecordUtil {

    public static void setParams(JCoRecord jcoRecord, Object sourceObj) {
        setParams(jcoRecord, sourceObj, true);
    }

    public static void setParams(JCoRecord jcoRecord, Object source, boolean caseInsensitive) {
        if (source == null || jcoRecord == null || !isMapRecord(source)) {
            return;
        }
        for (JCoField field : jcoRecord) {
            final int type = field.getType();
            final String name = field.getName();
            final String upperName = name.toUpperCase();

            // find the same name of value
            Object value = null;
            if (source instanceof GenericRecord) {
                GenericRecord gRecord = (GenericRecord) source;
                value = gRecord.get(name);

                if (value == null && caseInsensitive) {
                    final Optional<Object> firstValue = gRecord.getSchema().getFields().stream()//
                            .filter(f -> f.name().toUpperCase().equals(upperName))//
                            .map(f -> gRecord.get(f.name()))//
                            .findFirst();
                    if (firstValue.isPresent())
                        value = firstValue.get();
                }
            } else if (source instanceof MapRecord) { // when convert avro to map
                MapRecord mRecord = (MapRecord) source;
                value = mRecord.getValue(name);

                if (value == null && caseInsensitive) {
                    final Optional<Object> firstValue = mRecord.getRawFieldNames().stream()//
                            .filter(f -> f.toUpperCase().equals(upperName))//
                            .map(f -> mRecord.getValue(f))//
                            .findFirst();
                    if (firstValue.isPresent())
                        value = firstValue.get();
                }
            } else if (source instanceof Map) {
                Map map = (Map) source;
                value = map.get(name);

                if (value == null && caseInsensitive) {
                    final Optional<Object> firstValue = map.keySet().stream()//
                            .filter(f -> f.toString().toUpperCase().equals(upperName))//
                            .map(f -> map.get(f))//
                            .findFirst();
                    if (firstValue.isPresent())
                        value = firstValue.get();
                }
            }

            // ignore null value to set
            if (value == null) {
                continue;
            }

            if (type == JCoMetaData.TYPE_STRUCTURE) {
                if (isMapRecord(value)) { // only for record and map
                    JCoStructure structure = field.getStructure();
                    setParams(structure, value);
                } // else //ignore other values
            } else if (type == JCoMetaData.TYPE_TABLE) {
                JCoTable table = field.getTable();
                if (isMapRecord(value)) { // only for record and map
                    table.appendRow();
                    setParams(table, value);
                } else if (value instanceof List) { // multi-values
                    for (Object line : (List) value) {
                        table.appendRow();
                        setParams(table, line);
                    }
                } else if (value instanceof Object[]) { // record is array
                    for (Object line : (Object[]) value) {
                        table.appendRow();
                        setParams(table, line);
                    }
                }

                // } else if (type == JCoMetaData.TYPE_ITAB) {
                // // ???
                // } else if (type == JCoMetaData.TYPE_EXCEPTION) {
                // // need add exceptions?
                // } else if (type == JCoMetaData.TYPE_INVALID) {// UNINITIALIZED
                // // ignore
            } else {
                field.setValue(value);
            }
        }
    }

    private static boolean isMapRecord(Object value) {
        return value instanceof GenericRecord || value instanceof MapRecord || value instanceof Map;
    }

    public static Map<String, Object> convertToMap(JCoRecord jcoRecord, boolean ignoreEmptyValues) {
        if (jcoRecord == null) {
            return Collections.emptyMap();
        }
        final Map<String, Object> results = new LinkedHashMap<>();

        for (JCoField field : jcoRecord) {
            int type = field.getType();
            String name = field.getName();
            String value = field.getString();
            if (value == null) {
                if (!ignoreEmptyValues) {
                    results.put(name, "");
                }
                continue;
            }
            if (type == JCoMetaData.TYPE_STRUCTURE) {
                Map<String, Object> structResults = convertToMap(jcoRecord.getStructure(name), ignoreEmptyValues);
                if (structResults.size() > 0 || !ignoreEmptyValues) {// even add empty
                    results.put(name, structResults);
                }
            } else if (type == JCoMetaData.TYPE_TABLE) {
                Map<String, Object> tableResults = convertToMap(jcoRecord.getTable(name), ignoreEmptyValues);
                if (tableResults.size() > 0 || !ignoreEmptyValues) {// even add empty
                    results.put(name, tableResults);
                }

                // } else if (type == JCoMetaData.TYPE_ITAB) {
                // // ???
                // } else if (type == JCoMetaData.TYPE_EXCEPTION) {
                // // need add exceptions?
                // } else if (type == JCoMetaData.TYPE_INVALID) {// UNINITIALIZED
                // // ignore
            } else {
                if (ignoreEmptyValues && value.toString().isEmpty()) {
                    continue;
                }
                results.put(name, value);
            }

        }
        return results;
    }

    public static List<Map<String, Object>> convertToList(JCoTable jcoTable, boolean ignoreEmptyValues) {
        if (jcoTable == null) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> resultsList = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < jcoTable.getNumRows(); rowIndex++, jcoTable.nextRow()) {
            jcoTable.setRow(rowIndex);

            final Map<String, Object> tableResults = convertToMap(jcoTable, ignoreEmptyValues);
            if (tableResults.size() > 0 || !ignoreEmptyValues) {// even add empty
                resultsList.add(tableResults);
            }
        }
        return resultsList;
    }

    public static Map<String, Object> convertTablesToMap(JCoFunction jcoFunction, boolean ignoreEmptyValues, String... tableNames) {
        if (jcoFunction == null) {
            return Collections.emptyMap();
        }
        final JCoParameterList tableParameterList = jcoFunction.getTableParameterList();
        if (tableParameterList == null) {
            return Collections.emptyMap(); // no export table
        }
        final List<String> exportTablesList = tableNames != null ? Arrays.asList(tableNames) : Collections.emptyList();

        final Map<String, Object> results = new LinkedHashMap<>();

        for (JCoField f : tableParameterList) {
            if (f.getType() == JCoMetaData.TYPE_TABLE) {
                final String tableName = f.getName();
                if (!exportTablesList.isEmpty() && !exportTablesList.contains(tableName.toUpperCase())) {
                    continue; // if set, and not in the list, will ignore
                }
                JCoTable table = f.getTable();
                List<Map<String, Object>> tableResults = JCoRecordUtil.convertToList(table, ignoreEmptyValues);
                if (tableResults.size() > 0 || !ignoreEmptyValues) {// even add empty
                    results.put(tableName, tableResults);
                }

            }
        }
        return results;
    }

}
