package com.baishancloud.orchsym.sap.record;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericData.Array;
import org.apache.avro.generic.GenericRecord;
import org.apache.nifi.avro.AvroTypeUtil;
import org.apache.nifi.serialization.record.RecordSchema;
import org.junit.Test;

import com.baishancloud.orchsym.sap.record.RecordValuesUtil;

/**
 * @author GU Guoqiang
 *
 */
@SuppressWarnings("unchecked")
public class TestRecordValuesUtil {

    private Schema createSchema(String schemaName, List<Field> fields) {
        return Schema.createRecord(schemaName, null, "junit.avro", false, fields);
    }

    private Schema createSchema(String schemaName, Field... fields) {
        return createSchema(schemaName, fields != null ? Arrays.asList(fields) : Collections.emptyList());
    }

    private Field createField(String name) {
        return createField(name, Type.STRING);
    }

    private Field createField(String name, Type type) {
        return new Schema.Field(name, Schema.create(type), null, (Object) null);
    }

    private Field createField(String name, Schema fieldSchema) {
        return new Schema.Field(name, fieldSchema, null, (Object) null);
    }

    private GenericRecord createRecord(String schemaName, String[] names, Type[] types, Object[] values) {
        Schema[] fieldSchemas = Arrays.asList(types).stream().map(t -> Schema.create(t)).collect(Collectors.toList()).toArray(new Schema[0]);
        return createRecord(schemaName, names, fieldSchemas, values);
    }

    private GenericRecord createRecord(String schemaName, String[] names, Schema[] fieldSchemas, Object[] values) {
        if (names.length != fieldSchemas.length || names.length != values.length || names.length != fieldSchemas.length) {
            assertFalse("Unmatched names, types and values", true);
        }
        List<Field> fields = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            fields.add(createField(names[i], fieldSchemas[i]));
        }
        GenericRecord record = new GenericData.Record(createSchema(schemaName, fields));
        for (int i = 0; i < names.length; i++) {
            record.put(names[i], values[i]);
        }
        return record;
    }

    @Test
    public void test_createImportValues_simpleTypes() {
        dotest_createImportValues_record("simple", null, null, null);
    }

    private void dotest_createImportValues_record(String schemaName, String thirdName, Schema thirdSchema, Object thirdValue) {
        GenericRecord record = null;
        if (thirdName == null || thirdSchema == null || thirdValue == null) {
            record = createRecord(schemaName, new String[] { "id", "name", "birth", "sex" }, //
                    new Schema[] { Schema.create(Type.INT), Schema.create(Type.STRING), Schema.create(Type.STRING), Schema.create(Type.INT) }, //
                    new Object[] { 3, "abc", "2018-07-05", 1 });
        } else {
            record = createRecord(schemaName, new String[] { "id", "name", "birth", "sex", thirdName }, //
                    new Schema[] { Schema.create(Type.INT), Schema.create(Type.STRING), Schema.create(Type.STRING), Schema.create(Type.INT), thirdSchema }, //
                    new Object[] { 3, "abc", "2018-07-05", 1, thirdValue });
        }
        final Map<String, Object> params = RecordValuesUtil.createImportValues(record);

        assertThat(params.keySet(), hasSize(4)); // only have the simple types
        assertThat(params, hasEntry("id", 3));
        assertThat(params, hasEntry("name", "abc"));
        assertThat(params, hasEntry("birth", "2018-07-05"));
    }

    @Test
    public void test_createImportValues_hasRecord() {
        Schema addrSchema = createSchema("addr", createField("city"), createField("street"), createField("building"));
        GenericRecord addrRecord = new GenericData.Record(addrSchema);
        addrRecord.put("city", "BJ");
        addrRecord.put("street", "TAM");
        addrRecord.put("building", "123");

        dotest_createImportValues_record("record", "address", addrSchema, addrRecord);
    }

    @Test
    public void test_createImportValues_hasMap() {
        Schema addrSchema = Schema.createMap(Schema.create(Type.STRING));
        Map<String, String> addrRecord = new HashMap<>();
        addrRecord.put("city", "BJ");
        addrRecord.put("street", "TAM");
        addrRecord.put("building", "123");

        dotest_createImportValues_record("map", "address", addrSchema, addrRecord);
    }

    @Test
    public void test_createImportValues_hasArray() {
        Schema addrArrSchema = Schema.createArray(Schema.create(Type.STRING));
        Array<String> array = new Array<>(addrArrSchema, Arrays.asList("BJ", "TAM", "123"));

        dotest_createImportValues_record("arr", "addresses", addrArrSchema, array);
    }

    @Test
    public void test_createImportValues_hasArrayRecord() {
        Schema addrSchema = createSchema("addr", createField("city"), createField("street"), createField("building"));
        Schema addrArrSchema = Schema.createArray(addrSchema);
        GenericRecord addrRecord = new GenericData.Record(addrSchema);
        addrRecord.put("city", "BJ");
        addrRecord.put("street", "TAM");
        addrRecord.put("building", "123");
        Array<GenericRecord> array = new Array<>(addrArrSchema, Arrays.asList(addrRecord));

        dotest_createImportValues_record("arr_record", "addresses", addrArrSchema, array);
    }

    @Test
    public void test_createImportValues_hasUnion() {
        Schema unionSchema = Schema.createUnion(Schema.create(Type.STRING), Schema.create(Type.INT));

        dotest_createImportValues_union("un1", "addition", unionSchema, "ABC");
        dotest_createImportValues_union("un2", "addition", unionSchema, 123);
    }

    private void dotest_createImportValues_union(String schemaName, String thirdName, Schema thirdSchema, Object thirdValue) {
        GenericRecord record = createRecord(schemaName, new String[] { "id", "name", "birth", "sex", thirdName }, //
                new Schema[] { Schema.create(Type.INT), Schema.create(Type.STRING), Schema.create(Type.STRING), Schema.create(Type.INT), thirdSchema }, //
                new Object[] { 3, "abc", "2018-07-05", 1, thirdValue });
        final Map<String, Object> params = RecordValuesUtil.createImportValues(record);

        assertThat(params.keySet(), hasSize(5));
        assertThat(params, hasEntry("id", 3));
        assertThat(params, hasEntry("name", "abc"));
        assertThat(params, hasEntry("birth", "2018-07-05"));
        assertThat(params, hasEntry(thirdName, thirdValue.toString())); //use string for union always.
    }

    @Test
    public void test_creatImportTableValues_map() {
        Schema addrSchema = Schema.createMap(Schema.create(Type.STRING));
        Map<String, String> addrRecord = new HashMap<>();
        addrRecord.put("city", "BJ");
        addrRecord.put("street", "TAM");
        addrRecord.put("building", "123");

        Schema flagSchema = Schema.createMap(Schema.create(Type.INT));
        Map<String, Integer> flagRecord = new HashMap<>();
        flagRecord.put("id", 123);
        flagRecord.put("no", 456);

        dotest_creatImportTableValues("map", addrSchema, addrRecord, flagSchema, flagRecord);
    }

    private void dotest_creatImportTableValues(String schemaName, Schema addrSchema, Object addrRecord, Schema flagSchema, Object flagRecord) {
        GenericRecord record = createRecord(schemaName, new String[] { "id", "address", "flag" }, //
                new Schema[] { Schema.create(Type.INT), addrSchema, flagSchema }, //
                new Object[] { 3, addrRecord, flagRecord });

        final Map<String, Object> params = RecordValuesUtil.createImportTableValues(record);

        assertThat(params.keySet(), hasSize(2));
        assertThat(params, not(hasEntry("id", 3))); // only check for table
        assertThat(params, hasKey("address"));
        assertThat(params, hasKey("flag"));

        assertTrue(params.get("address") instanceof List);
        List<Map<String, Object>> addrList = (List<Map<String, Object>>) params.get("address");
        assertThat(addrList, hasSize(1));

        Map<String, Object> addrLine = addrList.get(0);
        assertThat(addrLine, hasEntry("city", "BJ"));
        assertThat(addrLine, hasEntry("street", "TAM"));
        assertThat(addrLine, hasEntry("building", "123"));

        List<Map<String, Object>> flagList = (List<Map<String, Object>>) params.get("flag");
        assertThat(flagList, hasSize(1));

        Map<String, Object> flagLine = flagList.get(0);
        assertThat(flagLine, hasEntry("id", 123));
        assertThat(flagLine, hasEntry("no", 456));
    }

    @Test
    public void test_creatImportTableValues_record() {
        Schema addrSchema = createSchema("addr", createField("city"), createField("street"), createField("building"));
        GenericRecord addrRecord = new GenericData.Record(addrSchema);
        addrRecord.put("city", "BJ");
        addrRecord.put("street", "TAM");
        addrRecord.put("building", "123");

        Schema flagSchema = createSchema("flag", createField("id", Type.INT), createField("no", Type.INT));
        GenericRecord flagRecord = new GenericData.Record(flagSchema);
        flagRecord.put("id", 123);
        flagRecord.put("no", 456);

        dotest_creatImportTableValues("record", addrSchema, addrRecord, flagSchema, flagRecord);
    }

    @Test
    public void test_creatImportTableValues_array() {
        Schema addrArrSchema = Schema.createArray(Schema.create(Type.STRING));
        Array<String> array = new Array<>(addrArrSchema, Arrays.asList("BJ", "TAM", "123"));

        GenericRecord record = createRecord("arr", new String[] { "id", "address" }, //
                new Schema[] { Schema.create(Type.INT), addrArrSchema }, //
                new Object[] { 3, array });

        final Map<String, Object> params = RecordValuesUtil.createImportTableValues(record);

        assertThat(params.keySet(), is(empty()));

    }

    @Test
    public void test_creatImportTableValues_arrayRecord() {
        Schema addrSchema = createSchema("addr", createField("city"), createField("street"), createField("building"));
        GenericRecord addrRecord = new GenericData.Record(addrSchema);
        addrRecord.put("city", "BJ");
        addrRecord.put("street", "TAM");
        addrRecord.put("building", "123");

        Schema addrsArrSchema = Schema.createArray(addrSchema);
        Array<GenericRecord> addrsArr = new Array<>(addrsArrSchema, Arrays.asList(addrRecord));

        Schema flagSchema = createSchema("flag", createField("id", Type.INT), createField("no", Type.INT));
        GenericRecord flagRecord = new GenericData.Record(flagSchema);
        flagRecord.put("id", 123);
        flagRecord.put("no", 456);

        Schema flagsArrSchema = Schema.createArray(flagSchema);
        Array<GenericRecord> flagsArr = new Array<>(flagsArrSchema, Arrays.asList(flagRecord));

        dotest_creatImportTableValues("arr_record", addrsArrSchema, addrsArr, flagsArrSchema, flagsArr);
    }

    @Test
    public void test_creatImportTableValues_arrayMultiRecords() {
        Schema addrSchema = createSchema("addr", createField("city"), createField("street"), createField("building"));
        GenericRecord addrRecord1 = new GenericData.Record(addrSchema);
        addrRecord1.put("city", "BJ");
        addrRecord1.put("street", "TAM");
        addrRecord1.put("building", "123");

        GenericRecord addrRecord2 = new GenericData.Record(addrSchema);
        addrRecord2.put("city", "CD");
        addrRecord2.put("street", "WH");
        addrRecord2.put("building", "136");

        Schema addrsArrSchema = Schema.createArray(addrSchema);
        Array<GenericRecord> addrsArr = new Array<>(addrsArrSchema, Arrays.asList(addrRecord1, addrRecord2));

        //
        Schema flagSchema = createSchema("flag", createField("id", Type.INT), createField("no", Type.INT));
        GenericRecord flagRecord1 = new GenericData.Record(flagSchema);
        flagRecord1.put("id", 123);
        flagRecord1.put("no", 456);

        GenericRecord flagRecord2 = new GenericData.Record(flagSchema);
        flagRecord2.put("id", 700);
        flagRecord2.put("no", 812);

        Schema flagsArrSchema = Schema.createArray(flagSchema);
        Array<GenericRecord> flagsArr = new Array<>(flagsArrSchema, Arrays.asList(flagRecord1, flagRecord2));

        //

        GenericRecord record = createRecord("mult_record", new String[] { "id", "addresses", "flags" }, //
                new Schema[] { Schema.create(Type.INT), addrsArrSchema, flagsArrSchema }, //
                new Object[] { 3, addrsArr, flagsArr });

        final Map<String, Object> params = RecordValuesUtil.createImportTableValues(record);

        assertThat(params.keySet(), hasSize(2));
        assertThat(params, not(hasEntry("id", 3))); // only check for table
        assertThat(params, hasKey("addresses"));
        assertThat(params, hasKey("flags"));

        assertTrue(params.get("addresses") instanceof List);
        List<Map<String, Object>> addrList = (List<Map<String, Object>>) params.get("addresses");
        assertThat(addrList, hasSize(2));

        Map<String, Object> addrLine = addrList.get(0);
        assertThat(addrLine, hasEntry("city", "BJ"));
        assertThat(addrLine, hasEntry("street", "TAM"));
        assertThat(addrLine, hasEntry("building", "123"));

        addrLine = addrList.get(1);
        assertThat(addrLine, hasEntry("city", "CD"));
        assertThat(addrLine, hasEntry("street", "WH"));
        assertThat(addrLine, hasEntry("building", "136"));

        List<Map<String, Object>> flagList = (List<Map<String, Object>>) params.get("flags");
        assertThat(flagList, hasSize(2));

        Map<String, Object> flagLine = flagList.get(0);
        assertThat(flagLine, hasEntry("id", 123));
        assertThat(flagLine, hasEntry("no", 456));

        flagLine = flagList.get(1);
        assertThat(flagLine, hasEntry("id", 700));
        assertThat(flagLine, hasEntry("no", 812));
    }

    @Test
    public void test_createExportParamNames_simpleTypes() {
        dotest_createExportParamNames_record("simple", null, null);
    }

    private void dotest_createExportParamNames_record(String schemaName, String thirdName, Schema thirdSchema) {
        Schema schema = null;
        if (thirdName == null || thirdSchema == null) {
            schema = createSchema(schemaName, //
                    createField("id", Type.INT), //
                    createField("name", Type.STRING), //
                    createField("birth", Type.STRING), //
                    createField("sex", Type.INT));
        } else {
            schema = createSchema(schemaName, //
                    createField("id", Type.INT), //
                    createField("name", Type.STRING), //
                    createField("birth", Type.STRING), //
                    createField("sex", Type.INT), //
                    createField(thirdName, thirdSchema));
        }
        RecordSchema writeSchema = AvroTypeUtil.createSchema(schema);

        final List<String> params = RecordValuesUtil.createExportParamNames(writeSchema);

        assertThat(params, hasSize(4)); // only have the simple types
        assertThat(params, hasItem("id"));
        assertThat(params, hasItem("name"));
        assertThat(params, hasItem("birth"));
        assertThat(params, hasItem("sex"));
    }

    @Test
    public void test_createExportParamNames_hasRecord() {
        Schema addrSchema = createSchema("addr", createField("city"), createField("street"), createField("building"));

        dotest_createExportParamNames_record("record", "address", addrSchema);
    }

    @Test
    public void test_createExportParamNames_hasMap() {
        Schema addrSchema = Schema.createMap(Schema.create(Type.STRING));

        dotest_createExportParamNames_record("map", "address", addrSchema);
    }

    @Test
    public void test_createExportParamNames_hasArray() {
        Schema addrArrSchema = Schema.createArray(Schema.create(Type.STRING));

        dotest_createExportParamNames_record("arr", "addresses", addrArrSchema);
    }

    @Test
    public void test_createExportParamNames_hasArrayRecord() {
        Schema addrSchema = createSchema("addr", createField("city"), createField("street"), createField("building"));
        Schema addrArrSchema = Schema.createArray(addrSchema);

        dotest_createExportParamNames_record("arr_record", "addresses", addrArrSchema);
    }

    @Test
    public void test_createExportTableParamNames_map() {
        Schema addrSchema = Schema.createMap(Schema.create(Type.STRING));
        Schema schema = createSchema("map", //
                createField("id", Type.INT), //
                createField("name", Type.STRING), //
                createField("address", addrSchema));
        RecordSchema writeSchema = AvroTypeUtil.createSchema(schema);

        final Map<String, List<String>> tableParams = RecordValuesUtil.createExportTableParamNames(writeSchema);
        assertThat(tableParams.keySet(), is(empty())); // don't support for map, because don't know the key
    }

    @Test
    public void test_createExportTableParamNames_array() {
        Schema schema = createSchema("arr", //
                createField("id", Type.INT), //
                createField("name", Type.STRING), //
                createField("addresses", Schema.createArray(Schema.create(Type.STRING))), //
                createField("flags", Schema.createArray(Schema.create(Type.INT))));

        RecordSchema writeSchema = AvroTypeUtil.createSchema(schema);
        final Map<String, List<String>> tableParams = RecordValuesUtil.createExportTableParamNames(writeSchema);
        assertThat(tableParams.keySet(), is(empty())); // don't support for list without record
    }

    @Test
    public void test_createExportTableParamNames_record() {
        Schema addrSchema = createSchema("addr", createField("city"), createField("street"), createField("building"));
        Schema flagSchema = createSchema("flag", createField("id", Type.INT), createField("no", Type.INT));
        Schema schema = createSchema("record", //
                createField("id", Type.INT), //
                createField("name", Type.STRING), //
                createField("addresses", addrSchema), //
                createField("flags", flagSchema));

        dotest_createExportTableParamNames(schema);
    }

    @Test
    public void test_createExportTableParamNames_arrayRecord() {
        Schema addrSchema = createSchema("addr", createField("city"), createField("street"), createField("building"));
        Schema flagSchema = createSchema("flag", createField("id", Type.INT), createField("no", Type.INT));
        Schema schema = createSchema("arr_record", //
                createField("id", Type.INT), //
                createField("name", Type.STRING), //
                createField("addresses", Schema.createArray(addrSchema)), //
                createField("flags", Schema.createArray(flagSchema)));

        dotest_createExportTableParamNames(schema);

    }

    private void dotest_createExportTableParamNames(Schema schema) {
        RecordSchema writeSchema = AvroTypeUtil.createSchema(schema);

        final Map<String, List<String>> tableParams = RecordValuesUtil.createExportTableParamNames(writeSchema);

        assertThat(tableParams.keySet(), hasSize(2));
        assertThat(tableParams, not(hasKey("id")));// only check for table
        assertThat(tableParams, not(hasKey("name"))); // only check for table
        assertThat(tableParams, hasKey("addresses"));
        assertThat(tableParams, hasKey("flags"));

        List<String> addrLis = tableParams.get("addresses");
        assertThat(addrLis, hasSize(3));
        assertThat(addrLis, hasItem("city"));
        assertThat(addrLis, hasItem("street"));
        assertThat(addrLis, hasItem("building"));

        List<String> flagLis = tableParams.get("flags");
        assertThat(flagLis, hasSize(2));
        assertThat(flagLis, hasItem("id"));
        assertThat(flagLis, hasItem("no"));
    }

}
