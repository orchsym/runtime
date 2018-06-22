package org.apache.nifi.processors.mapper.record;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.attribute.expression.language.PreparedQuery;
import org.apache.nifi.attribute.expression.language.Query;
import org.apache.nifi.components.PropertyValue;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processors.mapper.RecordMapper;
import org.apache.nifi.processors.mapper.exp.ExpVar;
import org.apache.nifi.processors.mapper.exp.ExpVarTable;
import org.apache.nifi.processors.mapper.exp.VarTableType;
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
            final List<DataType> possibleSubTypes = ((ChoiceDataType) dataType).getPossibleSubTypes();
            // TODO ???
        } else {
            // others without schema
        }

        return Optional.empty();
    }

    public static void calcRecordVarValues(final ProcessContext context, final Map<String, String> expValuesMap, final Record record, final RecordPathCache varRecordPaths,
            final ExpVarTable varTable) {
        if (record == null || varRecordPaths == null || varTable == null) {
            return;
        }

        final VarTableType varTableType = varTable.getType();
        // global without the table name
        final String varPrefix = (varTableType == VarTableType.GLOBAL) ? varTableType.getPrefix() : varTableType.getPrefix(varTable.getName());

        final Map<String, String> innerVarValuesMap = new HashMap<>(expValuesMap);

        for (ExpVar var : varTable.getVars()) {
            final String innerVarName = var.getName();
            final String outerVarName = varPrefix + '.' + innerVarName;
            final String exp = var.getExp();
            if (StringUtils.isEmpty(exp)) {
                continue;
            }
            final PreparedQuery query = Query.prepare(exp);
            if (query.isExpressionLanguagePresent()) { // expression
                PropertyValue expPropValue = context.newPropertyValue(exp);
                expPropValue = expPropValue.evaluateAttributeExpressions(innerVarValuesMap);
                String value = expPropValue.getValue();
                if (value != null) {
                    expValuesMap.put(outerVarName, value);

                    // support inner and outer for table var only
                    innerVarValuesMap.put(outerVarName, value);
                    innerVarValuesMap.put(innerVarName, value);
                }
            } else if (varTableType != VarTableType.GLOBAL) {// record path shouldn't be in global
                final RecordPath path = varRecordPaths.getCompiled(exp);
                final RecordPathResult result = path.evaluate(record);

                result.getSelectedFields().filter(f -> f.getValue() != null).forEach(f -> {
                    String value = f.getValue().toString();
                    expValuesMap.put(outerVarName, value);

                    // support inner and outer for table var only
                    innerVarValuesMap.put(outerVarName, value);
                    innerVarValuesMap.put(innerVarName, value);
                });
            }
        }

    }

    public static void calcRecordValues(final Map<String, String> expValuesMap, final Record record, final String prefix, final boolean shortPath) {
        if (record == null) {
            return;
        }
        // because don't support both expression and record path, so no use now
        // record.getSchema().getFields().stream().forEach(f -> calcRecordValues(resultMap, inputPrefix, record, f));

        // FIXME, in order to be compatible with old or simple flat schema, only deal with first level simple type.
        // like, "input.main.<field>", if use font record path way, will be like "input.main./<field>",
        record.getSchema().getFields().stream().filter(f -> isSimpleType(record, f.getDataType(), f.getFieldName())).forEach(f -> {
            expValuesMap.put(prefix + '.' + f.getFieldName(), record.getAsString(f.getFieldName()));

            /*
             * FIXME, normally, without prefix for output, means the key is field of current output table.
             */
            if (shortPath) {
                expValuesMap.put(f.getFieldName(), record.getAsString(f.getFieldName()));
            }
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
}
