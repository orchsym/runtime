package org.apache.nifi.processors.mapper.record;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.nifi.avro.AvroTypeUtil;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processors.mapper.exp.MapperTable;
import org.apache.nifi.record.path.FieldValue;
import org.apache.nifi.record.path.RecordPath;
import org.apache.nifi.record.path.RecordPathResult;
import org.apache.nifi.record.path.paths.ArrayIndexPath;
import org.apache.nifi.record.path.paths.RecordPathSegment;
import org.apache.nifi.serialization.record.DataType;
import org.apache.nifi.serialization.record.MapRecord;
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
 * write the record via record path.
 * 
 * @author GU Guoqiang
 */
public class RecordPathsWriter {
    public static final String OP_SLASH = "/";
    private boolean coerceTypes = false;
    private boolean dropUnknownFields = true;

    public void setTypeChecked(boolean coerceTypes) {
        this.coerceTypes = coerceTypes;
    }

    public boolean isTypeChecked() {
        return coerceTypes;
    }

    public boolean isDropUnknownFields() {
        return dropUnknownFields;
    }

    public void setDropUnknownFields(boolean dropUnknownFields) {
        this.dropUnknownFields = dropUnknownFields;
    }

    public Optional<Record> write(final ProcessContext context, final Record readerRecord, final MapperTable outputTable, final RecordPathsMap recordPathsMap,
            Map<String, Object> additionalPathValuesMap) {
        if (additionalPathValuesMap == null) {
            additionalPathValuesMap = Collections.emptyMap();
        }

        final Map<String, Object> expPathValuesMap = new LinkedHashMap<>(additionalPathValuesMap);
        final List<String> processedPathsMap = new ArrayList<>(additionalPathValuesMap.keySet());

        // process the record path for output expression field.
        outputTable.getExpressions().stream().filter(f -> !processedPathsMap.contains(checkPath(f.getPath()))).forEach(f -> {
            final String path = checkPath(f.getPath());
            processedPathsMap.add(path);

            final String expression = f.getExp();
            /*
             * FIXME maybe enable to support record path function here, not sure to support both expression language and record path or not?
             */
            final RecordPath recordPath = recordPathsMap.getCompiled(expression);

            final RecordPathResult result = recordPath.evaluate(readerRecord);
            result.getSelectedFields().filter(fv -> fv.getValue() != null).forEach(fv -> expPathValuesMap.put(path, fv.getValue()));
        });

        Record writeRecord = null;

        // create the record with default values.
        final Optional<Object> defaultRecord = createDefaultValue(outputTable);

        // set the values via record paths.
        if (defaultRecord.isPresent()) {
            final Object object = defaultRecord.get();
            if (object instanceof Record) {
                writeRecord = ((Record) object);

                for (Entry<String, Object> entry : expPathValuesMap.entrySet()) {
                    final Object value = entry.getValue();
                    final RecordPath recordPath = recordPathsMap.getCompiled(entry.getKey());
                    final RecordPathResult result = recordPath.evaluate(writeRecord);

                    final List<FieldValue> selectedFields = result.getSelectedFields().collect(Collectors.toList());
                    if (selectedFields.size() > 0) { // if found
                        selectedFields.forEach(f -> f.updateValue(coerceValue(f.getField(), value)));
                    } else if (recordPath instanceof ArrayIndexPath) {
                        setArrayValue(recordPathsMap, (ArrayIndexPath) recordPath, writeRecord, value);
                    }
                }
            }
        }

        // make sure no NPE
        if (writeRecord == null) {
            final RecordSchema writeRecordSchema = AvroTypeUtil.createSchema(outputTable.getSchema());
            writeRecord = new MapRecord(writeRecordSchema, new LinkedHashMap<>());
        }

        // FIXME, only deal with first level simply
        final Record outputRecord = writeRecord;
        outputTable.getSchema().getFields().stream().filter(f -> !processedPathsMap.contains(checkPath(f.name()))).forEach(f -> {
            final String fieldName = f.name();
            final Object oldValue = outputRecord.getValue(fieldName);
            // if existed, and is complex value, nothing to do
            if (oldValue != null && (oldValue.getClass().isArray() || oldValue instanceof Record || oldValue instanceof Map)) {
                return;
            }
            final Object value = readerRecord.getValue(fieldName);
            if (value != null) {
                outputRecord.setValue(fieldName, value);
            }
        });

        // need remove the null values, else will have NPE for output to write in flowfile
        Optional<Object> recordOp = cleanupValue(outputRecord);
        if (recordOp.isPresent()) {
            return Optional.of((Record) recordOp.get());
        }
        return Optional.empty();

    }

    public String checkPath(String path) {
        if (!path.startsWith(OP_SLASH)) {
            path = OP_SLASH + path; // if not standard record path, force adding the operator: slash character (/)
        }
        return path;
    }

    Object coerceValue(RecordField field, Object value) {
        if (isTypeChecked()) {
            final DataType dataType = chooseDataType(field.getDataType(), value);
            final Object coerceValue = DataTypeUtils.convertType(value, dataType, field.getFieldName());
            return coerceValue;
        } else {
            return value;
        }
    }

    DataType chooseDataType(DataType dataType, Object value) {
        DataType chosenDataType = dataType.getFieldType() == RecordFieldType.CHOICE ? DataTypeUtils.chooseDataType(value, (ChoiceDataType) dataType) : dataType;
        return chosenDataType;
    }

    Optional<Object> createDefaultValue(final MapperTable outputTable) {
        final RecordSchema writeRecordSchema = AvroTypeUtil.createSchema(outputTable.getSchema());
        RecordDataType recordDataType = new RecordDataType(writeRecordSchema);
        return createDefaultValue(recordDataType, null);
    }

    Optional<Object> createDefaultValue(final RecordField field) {
        return createDefaultValue(field.getDataType(), field.getDefaultValue());
    }

    Optional<Object> createDefaultValue(final DataType dataType, final Object defaultValue) {
        final Optional<RecordSchema> childRecordSchemaOp = RecordUtil.getChildSchema(dataType);
        if (childRecordSchemaOp.isPresent()) {
            final RecordSchema childRecordSchema = childRecordSchemaOp.get();
            return createDefaultValue(childRecordSchema, dataType);
        } else {
            final Optional<Object> createDefaultValue = createDefaultValue((RecordSchema) null, dataType);
            if (createDefaultValue.isPresent()) {
                return createDefaultValue;
            }
            // // get default value of primitive type
            if (defaultValue != null) {
                return Optional.of(defaultValue);
            }
        }
        return Optional.empty();
    }

    Optional<Object> createDefaultValue(final RecordSchema recordSchema, final DataType dataType) {
        final RecordFieldType parentFieldType = dataType.getFieldType();
        if (parentFieldType == RecordFieldType.RECORD && recordSchema != null) { // RecordDataType
            final Record record = new MapRecord(recordSchema, new LinkedHashMap<>(), false, isDropUnknownFields());
            recordSchema.getFields().stream().forEach(f -> {
                final String fieldName = f.getFieldName();

                final Optional<Object> childValue = createDefaultValue(f);
                if (childValue.isPresent()) {
                    record.setValue(fieldName, childValue.get());
                } else {
                    record.setValue(fieldName, f.getDefaultValue());
                }
            });
            return Optional.of(record);

        } else if (parentFieldType == RecordFieldType.MAP) {// MapDataType
            final Map<String, Object> map = new LinkedHashMap<>();

            if (recordSchema != null) {
                recordSchema.getFields().stream().forEach(f -> {
                    final String fieldName = f.getFieldName();
                    final Optional<Object> childValue = createDefaultValue(f);
                    if (childValue.isPresent()) {
                        map.put(fieldName, childValue.get());
                    } else {
                        map.put(fieldName, f.getDefaultValue());
                    }
                });
            }
            return Optional.of(map);
        } else if (parentFieldType == RecordFieldType.ARRAY) { // ArrayDataType
            List<Object> list = new ArrayList<>();

            final DataType elementType = ((ArrayDataType) dataType).getElementType();
            final Optional<Object> childValue = createDefaultValue(elementType, null);
            if (childValue.isPresent()) {
                list.add(childValue.get());
            } // else { // empty list

            return Optional.of(list.toArray());
        } else if (parentFieldType == RecordFieldType.CHOICE) { // ChoiceDataType
            // TODO
        } else {

        }
        return Optional.empty();
    }

    void setArrayValue(final RecordPathsMap recordPathsMap, final ArrayIndexPath recordPath, final Record writeRecord, final Object value) {
        try {
            final Field indexField = ArrayIndexPath.class.getDeclaredField("index"); //$NON-NLS-1$
            indexField.setAccessible(true);
            final Object object = indexField.get(recordPath);
            if (object == null) {
                return;
            }

            final Method getParentPathMethod = RecordPathSegment.class.getDeclaredMethod("getParentPath"); //$NON-NLS-1$
            getParentPathMethod.setAccessible(true);
            final RecordPath parentRecordPath = (RecordPath) getParentPathMethod.invoke(recordPath);
            if (parentRecordPath == null) {
                return;
            }

            final int index = Integer.parseInt(object.toString());
            final RecordPathResult result = parentRecordPath.evaluate(writeRecord);
            result.getSelectedFields().forEach(field -> {
                final Object fieldValue = field.getValue();
                Object[] newValue = null;
                if (fieldValue == null || !(fieldValue instanceof Object[]) || ((Object[]) fieldValue).length == 0) {
                    newValue = new Object[index + 1];
                } else {
                    Object[] fieldValues = (Object[]) fieldValue;
                    if (fieldValues.length <= index) {// grow, and make sure the data of index is last one.
                        newValue = new Object[index + 1];
                        System.arraycopy(fieldValues, 0, newValue, 0, fieldValues.length);
                    } else {
                        newValue = fieldValues; // reuse old one
                    }
                }
                newValue[index] = coerceValue(field.getField(), value); // TODO, need check the type for array
                field.updateValue(newValue);
            });

        } catch (Exception e) {
            // nothing to do
        }
    }

    @SuppressWarnings("unchecked")
    Optional<Object> cleanupValue(final Object value) {
        if (value instanceof Record) {
            final Record record = (Record) value;

            Map<String, Object> fieldValues = new LinkedHashMap<String, Object>();

            record.getRawFieldNames().stream().filter(fn -> record.getValue(fn) != null).forEach(fn -> {
                Object rValue = record.getValue(fn);
                Optional<Object> valueOp = cleanupValue(rValue);
                if (valueOp.isPresent())
                    fieldValues.put(fn, valueOp.get());
            });
            if (!fieldValues.isEmpty()) {
                Record newRecord = new MapRecord(record.getSchema(), fieldValues, record.isTypeChecked(), record.isDropUnknownFields());
                return Optional.of(newRecord);
            }
        } else if (value instanceof Map) {
            Map<String, Object> newMap = new LinkedHashMap<String, Object>();
            for (Entry<String, Object> entry : ((Map<String, Object>) value).entrySet()) {
                Object eValue = entry.getValue();
                if (eValue != null) {
                    Optional<Object> valueOp = cleanupValue(eValue);
                    if (valueOp.isPresent())
                        newMap.put(entry.getKey(), valueOp.get());
                }
            }
            if (!newMap.isEmpty()) {
                return Optional.of(newMap);
            }
        } else if (value instanceof List) {
            List<Object> newList = new ArrayList<>();
            for (Object obj : (List) value) {
                Optional<Object> valueOp = cleanupValue(obj);
                if (valueOp.isPresent())
                    newList.add(valueOp.get());
            }
            if (!newList.isEmpty()) {
                return Optional.of(newList);
            }
        } else if (value instanceof Object[]) {
            List<Object> newList = new ArrayList<>();
            for (Object obj : (Object[]) value) {
                Optional<Object> valueOp = cleanupValue(obj);
                if (valueOp.isPresent())
                    newList.add(valueOp.get());
            }
            if (!newList.isEmpty()) {
                return Optional.of(newList.toArray());
            }

        } else if (value != null) {
            return Optional.of(value);
        }
        return Optional.empty();
    }
}
