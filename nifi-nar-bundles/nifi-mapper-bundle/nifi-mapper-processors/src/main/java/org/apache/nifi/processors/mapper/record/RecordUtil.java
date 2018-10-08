package org.apache.nifi.processors.mapper.record;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.components.PropertyValue;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processors.mapper.exp.ExpVar;
import org.apache.nifi.processors.mapper.exp.ExpVarTable;
import org.apache.nifi.processors.mapper.exp.MapperExpField;
import org.apache.nifi.processors.mapper.exp.MapperTable;
import org.apache.nifi.processors.mapper.exp.MapperTableType;
import org.apache.nifi.processors.mapper.exp.VarTableType;
import org.apache.nifi.processors.mapper.var.VarUtil;
import org.apache.nifi.record.path.RecordPath;
import org.apache.nifi.record.path.RecordPathResult;
import org.apache.nifi.record.path.util.RecordPathCache;
import org.apache.nifi.serialization.record.DataType;
import org.apache.nifi.serialization.record.Record;
import org.apache.nifi.serialization.record.RecordField;
import org.apache.nifi.serialization.record.RecordFieldType;
import org.apache.nifi.serialization.record.RecordSchema;
import org.apache.nifi.serialization.record.type.ArrayDataType;
import org.apache.nifi.serialization.record.type.ChoiceDataType;
import org.apache.nifi.serialization.record.type.MapDataType;
import org.apache.nifi.serialization.record.type.RecordDataType;
import org.apache.nifi.serialization.record.util.DataTypeUtils;

/**
 * map the pat of record with value.
 * 
 * @author GU Guoqiang
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class RecordUtil {

    public static Optional<RecordSchema> getChildSchema(final DataType dataType) {
        if (dataType instanceof RecordDataType) {
            RecordSchema childSchema = ((RecordDataType) dataType).getChildSchema();
            return Optional.of(childSchema);
        } else if (dataType instanceof ArrayDataType) {
            DataType typeOfArray = ((ArrayDataType) dataType).getElementType();
            return getChildSchema(typeOfArray);
        } else if (dataType instanceof MapDataType) {
            DataType typeOfMap = ((MapDataType) dataType).getValueType();
            return getChildSchema(typeOfMap);
        } else if (dataType instanceof ChoiceDataType) {
            // final List<DataType> possibleSubTypes = ((ChoiceDataType) dataType).getPossibleSubTypes();
            // TODO ???
        } else {
            // others without schema
        }

        return Optional.empty();
    }

    public static void calcRecordVarValues(final ProcessContext context, final Map<String, String> expValuesMap, final Map<String, String> shortExpValuesMap, final Record record,
            final RecordPathCache varRecordPaths, final ExpVarTable varTable, boolean withInnerVar) {
        if (record == null || varRecordPaths == null || varTable == null) {
            return;
        }
        final boolean isGlobal = varTable.getType().equals(VarTableType.GLOBAL);
        final String varPrefix = varTable.getVarPrefix();

        final Map<String, String> innerVarValuesMap = new HashMap<>(expValuesMap);
        if (shortExpValuesMap != null)
            innerVarValuesMap.putAll(shortExpValuesMap);

        for (ExpVar var : varTable.getVars()) {
            final String innerVarName = var.getName();
            final String outerVarName = varPrefix + '.' + innerVarName;
            final String exp = var.getExp();

            if (isGlobal && VarUtil.hasRP(exp)) {
                continue;// can't support record path in global.
            }

            if (VarUtil.hasEL(exp)) {
                PropertyValue expPropValue = context.newPropertyValue(exp);
                expPropValue = expPropValue.evaluateAttributeExpressions(innerVarValuesMap);
                String value = expPropValue.getValue();
                if (value != null) {
                    expValuesMap.put(outerVarName, value);
                    if (withInnerVar) {
                        expValuesMap.put(innerVarName, value);
                    }

                    innerVarValuesMap.put(outerVarName, value);
                    innerVarValuesMap.put(innerVarName, value);

                }
            } else if (VarUtil.hasRP(exp)) {// record path
                final RecordPath path = varRecordPaths.getCompiled(exp);
                final RecordPathResult result = path.evaluate(record);

                result.getSelectedFields().filter(f -> f.getValue() != null).forEach(f -> {
                    String value = f.getValue().toString();
                    expValuesMap.put(outerVarName, value);
                    if (withInnerVar) {
                        expValuesMap.put(innerVarName, value);
                    }
                    // support inner and outer for table var only
                    innerVarValuesMap.put(outerVarName, value);
                    innerVarValuesMap.put(innerVarName, value);
                });
            } else { // literal value
                expValuesMap.put(outerVarName, exp);
                if (withInnerVar) {
                    expValuesMap.put(innerVarName, exp);
                }
                innerVarValuesMap.put(outerVarName, exp);
                innerVarValuesMap.put(innerVarName, exp);
            }
        }

    }

    public static void calcRecordLongValues(final Map<String, String> expValuesMap, final Record record, final String prefix) {
        if (record == null) {
            return;
        }
        /*
         * will be like input.main.id=1 or output.out.id=10
         */
        record.getSchema().getFields().stream().filter(f -> isSimpleType(record, f.getDataType(), f.getFieldName())).forEach(f -> {
            expValuesMap.put(prefix + '.' + f.getFieldName(), record.getAsString(f.getFieldName()));
        });

    }

    public static void calcRecordShortValues(final Map<String, String> expValuesMap, final Record record) {
        if (record == null) {
            return;
        }
        // only the field name with value, like id=1,name=abc
        record.getSchema().getFields().stream().filter(f -> isSimpleType(record, f.getDataType(), f.getFieldName())).forEach(f -> {
            expValuesMap.put(f.getFieldName(), record.getAsString(f.getFieldName()));
        });

    }

    private static boolean isSimpleType(final Record record, final DataType fieldDataType, final String fieldName) {
        final RecordFieldType fieldType = fieldDataType.getFieldType();
        if (fieldType == RecordFieldType.CHOICE) {
            final Object value = record.getValue(fieldName);
            final DataType realDataType = DataTypeUtils.chooseDataType(value, (ChoiceDataType) fieldDataType);
            // find real dataType
            return isSimpleType(record, realDataType, fieldName);
        } else {
            // non-array, non-record, non-map
            return fieldType != RecordFieldType.ARRAY && fieldType != RecordFieldType.RECORD && fieldType != RecordFieldType.MAP;
        }

    }

    /**
     * Only deal with the leaves of Record/Map, list of array
     */
    private static void calcRecordValues(final Map<String, String> resultMap, String parentPath, final Record parentRecord, final RecordField field) {
        calcRecordValues(resultMap, parentPath, parentRecord, field.getDataType(), field.getFieldName());
    }

    private static void calcRecordValues(final Map<String, String> resultMap, String parentPath, final Record parentRecord, final DataType dataType, final String fieldName) {
        final Object value = parentRecord.getValue(fieldName);
        if (value == null) {
            return;
        }

        final String path = parentPath + '/' + fieldName;

        final RecordFieldType fieldType = dataType.getFieldType();
        if (fieldType == RecordFieldType.RECORD) {
            final Optional<RecordSchema> childSchemaOp = getChildSchema(dataType);
            if (childSchemaOp.isPresent()) {
                final RecordSchema childSchema = childSchemaOp.get();
                final Record childRecord = parentRecord.getAsRecord(fieldName, childSchema);

                childSchema.getFields().forEach(f -> calcRecordValues(resultMap, path, childRecord, f));
            }
        } else if (fieldType == RecordFieldType.MAP && value instanceof Map) {
            calcMapValues(resultMap, path, (Map<String, Object>) value);

        } else if (fieldType == RecordFieldType.ARRAY) {
            Object[] array = null;
            if (value instanceof List) {
                array = ((List) value).toArray();
            } else if (value instanceof Object[]) {
                array = (Object[]) value;
            }
            if (array != null && array.length > 0)
                calcArrayValues(resultMap, path, array);

        } else if (fieldType == RecordFieldType.CHOICE) {
            final DataType chooseDataType = DataTypeUtils.chooseDataType(value, (ChoiceDataType) dataType);
            // just use new dataType to re-calc
            calcRecordValues(resultMap, parentPath, parentRecord, chooseDataType, fieldName);
        } else { // simply type
            resultMap.put(path, parentRecord.getAsString(fieldName));
        }
    }

    private static void calcMapValues(final Map<String, String> resultMap, String parentPath, final Map<String, Object> mapValues) {
        for (Entry<String, Object> entry : mapValues.entrySet()) {
            final String entryPath = parentPath + "[\'" + entry.getKey() + "\']";
            final Object entryValue = entry.getValue();
            if (entryValue == null) {
                continue;
            }
            if (entryValue instanceof Record) { // record
                final Record record = (Record) entryValue;
                record.getSchema().getFields().forEach(f -> calcRecordValues(resultMap, entryPath, record, f));
            } else if (entryValue instanceof Map) { // map
                calcMapValues(resultMap, entryPath, (Map<String, Object>) entryValue);
            } else if (entryValue instanceof Object[]) { // array
                calcArrayValues(resultMap, entryPath, (Object[]) entryValue);
            } else if (entryValue instanceof List) { // list(array)
                calcArrayValues(resultMap, entryPath, ((List) entryValue).toArray());
            } else {
                resultMap.put(entryPath, entryValue.toString());
            }
        }
    }

    private static void calcArrayValues(final Map<String, String> resultMap, String parentPath, final Object[] arrayValues) {
        if (arrayValues == null || arrayValues.length == 0) {
            return;
        }
        for (int i = 0; i < arrayValues.length; i++) {
            final String arayPath = parentPath + "[" + i + "]";
            final Object value = arrayValues[i];
            if (value == null) {
                continue;
            }
            if (value instanceof Record) { // record
                final Record record = (Record) value;
                record.getSchema().getFields().forEach(f -> calcRecordValues(resultMap, arayPath, record, f));
            } else if (value instanceof Map) { // map
                calcMapValues(resultMap, arayPath, (Map<String, Object>) value);
            } else if (value instanceof Object[]) { // array
                calcArrayValues(resultMap, arayPath, (Object[]) value);
            } else if (value instanceof List) { // list(array)
                calcArrayValues(resultMap, arayPath, ((List) value).toArray());
            } else {
                resultMap.put(arayPath, value.toString());
            }

        }
    }

    /**
     * 
     * Calc the values of vars for input table from record and the vars of table too.
     */
    public static Map<String, String> calcInputVars(final ProcessContext context, final Record readerRecord, final MapperTable inputTable, final String inputTableName,
            final RecordPathCache inputRecordPaths) {
        final Map<String, String> expValuesMap = new HashMap<>();
        /*
         * FIXME, only support fixed "main" input flow currently, should only work for flat schema and the first level of simple field. if tree schema, better use input var way always.
         */
        calcRecordLongValues(expValuesMap, readerRecord, MapperTableType.INPUT.getPrefix(inputTableName));

        // the inner vars of input table self.
        final Map<String, String> inputFieldsValuesMap = new LinkedHashMap<>();
        calcRecordShortValues(inputFieldsValuesMap, readerRecord);

        // input var
        if (inputTable != null) {
            calcRecordVarValues(context, expValuesMap, inputFieldsValuesMap, readerRecord, inputRecordPaths, inputTable.getExpVarTable(), false);
        }

        return expValuesMap;
    }

    /**
     * 
     * Calc the values of path for expressions, the expression don't support record path, should only use expression language.
     */
    public static Map<String, String> calcOutputExpressionsValues(final ProcessContext context, final FlowFile inputFlowFile, final MapperTable outputTable, final Map<String, String> expValuesMap) {
        if (outputTable == null) {
            return Collections.emptyMap();
        }

        final Map<String, String> pathValuesMap = new LinkedHashMap<>();
        outputTable.getExpressions().stream().filter(f -> StringUtils.isNotBlank(f.getPath())).forEach(f -> {
            final String expression = f.getExp();

            if (VarUtil.hasEL(expression)) { // only process EL, shouldn't set the record path
                String value = context.newPropertyValue(expression).evaluateAttributeExpressions(inputFlowFile, expValuesMap).getValue();

                // try default value,
                final String defaultValue = f.getDefaultLiteralValue();
                if (StringUtils.isBlank(value) && defaultValue != null) { // set the default value, even ""
                    value = defaultValue;
                }

                final String path = RecordPathsWriter.checkPath(f.getPath());
                pathValuesMap.put(path, value);
            }
        });
        return pathValuesMap;
    }

    /**
     * 
     * Only calc the expression language for output, ignore the record path, even the inner var depend on record path also
     */
    public static Map<String, String> calcOutputVars(final ProcessContext context, final Record writeRecord, final MapperTable outputTable, final RecordPathsMap outputRecordPaths,
            final Map<String, String> expValuesMap, final Map<String, ExpVar> unCalcExpVars) {
        if (outputTable == null) {
            return Collections.emptyMap();
        }
        final Map<String, String> outputValuesMap = new HashMap<>(expValuesMap);

        // add all output data for first level
        calcRecordLongValues(outputValuesMap, writeRecord, MapperTableType.OUTPUT.getPrefix(outputTable.getName()));

        // the inner vars of output table self.
        final Map<String, String> outputFieldsValuesMap = new LinkedHashMap<>();
        calcRecordShortValues(outputFieldsValuesMap, writeRecord);
        outputValuesMap.putAll(outputFieldsValuesMap); // add the inner vars for output also

        // process the left output var for record path
        if (!unCalcExpVars.isEmpty()) {
            ExpVarTable rpVarsTable = new ExpVarTable(outputTable.getName(), VarTableType.matchTableType(outputTable.getType()), unCalcExpVars.values().toArray(new ExpVar[0]));
            calcRecordVarValues(context, outputValuesMap, outputFieldsValuesMap, writeRecord, outputRecordPaths, rpVarsTable, true);
        }
        return outputValuesMap;
    }

    /**
     * Only init the default value of expression for output table. currently, shouldn't no expressions for input table.
     */
    public static void initTableExpressionsDefaultValue(final ProcessContext context, Collection<MapperTable> outputTables) {
        if (outputTables == null || outputTables.isEmpty()) {
            return;
        }
        for (MapperTable table : outputTables) {
            final List<MapperExpField> expressions = table.getExpressions();
            for (MapperExpField field : expressions) {
                final String defaultValue = field.getDefaultValue();
                if (defaultValue != null) { // set
                    // also support the processor vars, but not for flowfile
                    String newDefaultValue = context.newPropertyValue(defaultValue).evaluateAttributeExpressions().getValue();
                    field.setDefaultLiteralValue(newDefaultValue); // record
                }
            }
        }
    }
}
