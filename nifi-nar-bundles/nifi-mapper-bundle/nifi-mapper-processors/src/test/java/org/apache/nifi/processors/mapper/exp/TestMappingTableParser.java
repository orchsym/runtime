package org.apache.nifi.processors.mapper.exp;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * 
 * @author GU Guoqiang
 * 
 */
public class TestMappingTableParser {
    private MapperTable.Parser parser;

    @Before
    public void init() {
        parser = new MapperTable.Parser();
    }

    @After
    public void clean() {
        parser = null;
    }

    @Test
    public void testParseTable_empty() throws JsonProcessingException, IOException {
        MapperTable table = parser.parseTable(null);
        assertNull(table);

        table = parser.parseTable("");
        assertNull(table);

        table = parser.parseTable("   ");
        assertNull(table);
    }

    @Test(expected = JsonProcessingException.class)
    public void testParseTable_invalidJson() throws JsonProcessingException, IOException {
        parser.parseTable("abcdfef");
    }

    @Test
    public void testParseTable_withoutFields() throws JsonProcessingException, IOException {
        MapperTable table = parser.parseTable("{\"name\":\"out1\",\"id\":\"xxx-001\",\"type\":\"OUTPUT\"}");
        assertNotNull(table);

        assertThat(table.getName(), is("out1"));
        assertThat(table.getId(), is("xxx-001"));
        assertThat(table.getType(), is(MapperTableType.OUTPUT));
        assertThat(table.getFilter(), is(nullValue()));
        assertTrue("No fields yet", table.getExpressions().isEmpty());
    }

    @Test
    public void testParseTable_withFields() throws JsonProcessingException, IOException {
        MapperTable table = parser.parseTable("{\"name\":\"out1\",\"id\":\"xxx-001\",\"type\":\"OUTPUT\"" //
                + ",\"filter\":\"${input1.name:startsWith(\\\"Hello\\\")}\"" //
                + ",\"expressions\":["//
                + "{\"path\":\"/id\",\"exp\":\"${input1.id}\"}"//
                + ",{\"path\":\"/name\",\"exp\":\"${input1.name}\"}"//
                + ",{\"path\":\"/age\",\"exp\":\"${input1.age}\",\"default\":-1}"//
                + "]}");
        assertNotNull(table);

        assertThat(table.getName(), is("out1"));
        assertThat(table.getId(), is("xxx-001"));
        assertThat(table.getType(), is(MapperTableType.OUTPUT));
        assertThat(table.getFilter(), is("${input1.name:startsWith(\"Hello\")}"));

        assertThat("should have 3 fields", table.getExpressions().size(), is(3));

        MapperExpField id = table.getExpressions().get(0);
        assertThat(id.getPath(), is("/id"));
        assertThat(id.getExp(), is("${input1.id}"));
        assertThat(id.getDefaultValue(), is(nullValue()));

        MapperExpField name = table.getExpressions().get(1);
        assertThat(name.getPath(), is("/name"));
        assertThat(name.getExp(), is("${input1.name}"));
        assertThat(name.getDefaultValue(), is(nullValue()));

        MapperExpField age = table.getExpressions().get(2);
        assertThat(age.getPath(), is("/age"));
        assertThat(age.getExp(), is("${input1.age}"));
        assertThat(age.getDefaultValue(), is("-1"));
    }

    @Test
    public void testParseTable_withSchema() throws JsonProcessingException, IOException {
        final File file = new File("src/test/resources/example-output-table.json");
        final String tableSettings = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        MapperTable table = parser.parseTable(tableSettings);
        assertNotNull(table);

        assertThat(table.getName(), is("output1"));
        assertThat(table.getId(), is("xxx-001"));
        assertThat(table.getDesc(), is("mapping for the fullname"));
        assertThat(table.getType(), is(MapperTableType.OUTPUT));

        assertThat(table.getFilter(), is("${full_name:startsWith('Hello')}"));
        assertThat(table.getFilterDescription(), is("full name is start with hello"));

        assertThat(table.getControllerService(), is("AvroRecordSetWriter"));
        assertThat(table.getControllerDesc(), is("Avro writer"));

        // schema
        final Schema schema = table.getSchema();
        assertNotNull(schema);

        assertThat(schema.getName(), is(table.getName()));
        assertThat(schema.getNamespace(), is("example.output"));
        assertThat(schema.getType(), is(Type.RECORD));

        final Field avroId = schema.getField("id");
        assertNotNull(avroId);
        assertThat(avroId.defaultVal(), is(nullValue()));
        assertThat(avroId.schema().getName(), is("int"));

        assertNotNull(schema.getField("full_name"));
        assertNotNull(schema.getField("age"));
        assertNotNull(schema.getField("address"));

        // expression
        assertThat(table.getExpressions().size(), is(3));

        MapperExpField id = table.getExpressions().get(0);
        assertThat(id.getPath(), is("/id"));
        assertThat(id.getExp(), is("${id}"));
        assertThat(id.getDefaultValue(), is(nullValue()));
        assertThat(id.getDesc(), is("staff id"));

        MapperExpField name = table.getExpressions().get(1);
        assertThat(name.getPath(), is("/full_name"));
        assertThat(name.getExp(), is("${input.main.first_name:append(' '):append(${input.main.last_name})}"));
        assertThat(name.getDefaultValue(), is("<undefined>"));
        assertThat(name.getDesc(), is("staff full name"));

        MapperExpField age = table.getExpressions().get(2);
        assertThat(age.getPath(), is("/age"));
        assertThat(age.getExp(), is("${now():format('yyyy'):toNumber():minus(${input.main.birth:toDate('yyyy-MM-dd'):format('yyyy'):toNumber()})}"));
        assertThat(age.getDefaultValue(), is(nullValue()));
        assertThat(age.getDesc(), is("staff age"));

    }

    @Test
    public void testParseTable_write_IT() throws JsonProcessingException, IOException {
        final File file = new File("src/test/resources/example-output-table.json");
        final String tableSettings = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        MapperTable table = parser.parseTable(tableSettings);
        assertNotNull(table);
        assertThat(table.getExpressions(), hasSize(3));

        assertNotNull(table.getSchema());
        assertThat(table.getSchema().getFields(), hasSize(4));

        final String newTableSettings = new MapperTable.Writer().write(table);
        assertNotNull(newTableSettings);

        MapperTable reloadTable = parser.parseTable(newTableSettings);
        assertNotNull(reloadTable);
        assertThat(reloadTable.getExpressions(), hasSize(3));

        assertNotNull(reloadTable.getSchema());
        assertThat(reloadTable.getSchema().getFields(), hasSize(4));
    }
}
