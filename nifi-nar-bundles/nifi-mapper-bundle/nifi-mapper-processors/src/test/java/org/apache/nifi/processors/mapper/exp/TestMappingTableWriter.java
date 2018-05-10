package org.apache.nifi.processors.mapper.exp;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.nifi.processors.mapper.exp.MapperExpField;
import org.apache.nifi.processors.mapper.exp.MapperTable;
import org.apache.nifi.processors.mapper.exp.MapperTableType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

public class TestMappingTableWriter {
    private MapperTable.Writer writer;

    @Before
    public void init() {
        writer = new MapperTable.Writer();
    }

    @After
    public void clean() {
        writer = null;
    }

    @Test
    public void testWriteTable_null() throws JsonProcessingException {
        String json = writer.write((MapperTable) null);
        assertNull(json);
    }

    @Test
    public void testWriteTable_noFields() throws JsonProcessingException, ParseException {
        MapperTable table = new MapperTable();
        table.setName("out1");
        table.setId("xxx-001");
        table.setType(MapperTableType.OUTPUT);

        String jsonStr = writer.write(table);
        assertNotNull(jsonStr);

        JSONObject json = new JSONParser(JSONParser.MODE_JSON_SIMPLE).parse(jsonStr, JSONObject.class);
        assertThat(json, hasEntry("name", "out1"));
        assertThat(json, hasEntry("id", "xxx-001"));
        assertThat(json, hasEntry("type", "OUTPUT"));
        assertThat(json, not(hasKey("filter")));
        assertThat(json, hasKey("expressions"));
        String fields = json.getAsString("expressions");
        assertThat(fields, is(MapperTable.EMPTY_ARR));
    }

    @Test
    public void testWriteTable_mulitFields() throws JsonProcessingException, ParseException {
        MapperTable table = new MapperTable();
        table.setName("out1");
        table.setId("xxx-001");
        table.setType(MapperTableType.OUTPUT);

        MapperExpField id = new MapperExpField();
        id.setPath("/id");
        id.setExp("${main.id}");
        table.getExpressions().add(id);

        MapperExpField name = new MapperExpField();
        name.setPath("/name");
        name.setExp("${main.name}");
        table.getExpressions().add(name);

        MapperExpField age = new MapperExpField();
        age.setPath("/age");
        age.setExp("${main.age}");
        table.getExpressions().add(age);

        String jsonStr = writer.write(table);
        assertNotNull(jsonStr);

        JSONObject json = new JSONParser(JSONParser.MODE_JSON_SIMPLE).parse(jsonStr, JSONObject.class);
        assertThat(json, hasEntry("name", "out1"));
        assertThat(json, hasEntry("id", "xxx-001"));
        assertThat(json, hasEntry("type", "OUTPUT"));
        assertThat(json, not(hasKey("filter")));
        assertThat(json, hasKey("expressions"));

        Object fs = json.get("expressions");
        assertNotNull(fs);
        assertTrue(fs instanceof JSONArray);
        JSONArray fileds = (JSONArray) fs;

        assertThat(fileds.size(), is(3));

        for (Object f : fileds) {
            if (f instanceof JSONObject) {
                JSONObject fJson = (JSONObject) f;
                final String path = fJson.getAsString("path");
                String fExp = fJson.getAsString("exp");

                switch (path) {
                case "/id":
                    assertThat(fExp, is("${main.id}"));
                    break;

                case "/name":
                    assertThat(fExp, is("${main.name}"));
                    break;
                case "/age":
                    assertThat(fExp, is("${main.age}"));
                    break;
                default:
                    fail("Shouldn't be exited other values :" + path);
                }
            }
        }

    }

    @Test
    public void testWriteTable_schema() throws JsonProcessingException, ParseException {
        MapperTable table = new MapperTable();
        table.setName("out1");
        table.setId("xxx-001");
        table.setType(MapperTableType.OUTPUT);
        table.setDesc("out1 test");

        table.setControllerService("AvroWriter");
        // table.setControllerDesc("avro writer");

        table.setFilter("id>100");
        table.setFilterDescription("bigger than 100");

        // create schema
        final Schema schema = Schema.createRecord(table.getName(), "", "junit.test", false);
        table.setSchema(schema);

        Field avroId = new Field("id", Schema.create(Type.INT), "staff id", (Object) null);
        Field avroName = new Field("name", Schema.create(Type.STRING), "staff name", (Object) null);
        Field avroAge = new Field("age", Schema.create(Type.INT), "staff age", (Object) null);
        schema.setFields(Arrays.asList(avroId, avroName, avroAge));

        // expressions
        MapperExpField id = new MapperExpField();
        id.setPath("/id");
        id.setExp("${main.id}");
        table.getExpressions().add(id);

        MapperExpField name = new MapperExpField();
        name.setPath("/name");
        name.setExp("${main.name}");
        table.getExpressions().add(name);

        MapperExpField age = new MapperExpField();
        age.setPath("/age");
        age.setExp("${main.age}");
        table.getExpressions().add(age);

        String jsonStr = writer.write(table);
        assertNotNull(jsonStr);

        JSONObject json = new JSONParser(JSONParser.MODE_JSON_SIMPLE).parse(jsonStr, JSONObject.class);
        assertThat(json, hasEntry("name", "out1"));
        assertThat(json, hasEntry("id", "xxx-001"));
        assertThat(json, hasEntry("type", "OUTPUT"));
        assertThat(json, hasEntry("desc", "out1 test"));

        assertThat(json, hasEntry("controller", "AvroWriter"));
        assertThat(json, not(hasKey("controller_desc")));

        assertThat(json, hasEntry("filter", "id>100"));
        assertThat(json, hasEntry("filter_desc", "bigger than 100"));

        //
        assertThat(json, hasKey(MapperTable.NAME_AVRO_SCHEMA));

        JSONObject avroJson = (JSONObject) json.get(MapperTable.NAME_AVRO_SCHEMA);
        assertThat(avroJson, hasEntry("name", "out1"));
        assertThat(avroJson, hasEntry("type", Schema.Type.RECORD.name().toLowerCase()));
        assertThat(avroJson, hasEntry("namespace", "junit.test"));
        assertThat(avroJson, hasKey("fields"));

        JSONArray fieldsJsonArr = (JSONArray) avroJson.get("fields");
        assertThat(fieldsJsonArr, hasSize(3));
        for (Object f : fieldsJsonArr) {
            if (f instanceof JSONObject) {
                JSONObject fJson = (JSONObject) f;
                final String fName = fJson.getAsString("name");
                switch (fName) {
                case "id":
                    assertThat(fJson, hasEntry("type", "int"));
                    assertThat(fJson, hasEntry("doc", "staff id"));
                    break;
                case "name":
                    assertThat(fJson, hasEntry("type", "string"));
                    assertThat(fJson, hasEntry("doc", "staff name"));
                    break;
                case "age":
                    assertThat(fJson, hasEntry("type", "int"));
                    assertThat(fJson, hasEntry("doc", "staff age"));
                    break;
                default:
                    fail("Shouldn't be exited other field :" + fName);
                }

                assertThat(json, not(hasKey("default")));
            }
        }

        //
        assertThat(json, hasKey("expressions"));

        Object fs = json.get("expressions");
        assertNotNull(fs);
        assertTrue(fs instanceof JSONArray);
        JSONArray fileds = (JSONArray) fs;

        assertThat(fileds.size(), is(3));

        for (Object f : fileds) {
            if (f instanceof JSONObject) {
                JSONObject fJson = (JSONObject) f;
                final String path = fJson.getAsString("path");
                String fExp = fJson.getAsString("exp");

                switch (path) {
                case "/id":
                    assertThat(fExp, is("${main.id}"));
                    break;

                case "/name":
                    assertThat(fExp, is("${main.name}"));
                    break;
                case "/age":
                    assertThat(fExp, is("${main.age}"));
                    break;
                default:
                    fail("Shouldn't be exited other values :" + path);
                }
            }
        }
    }

}
