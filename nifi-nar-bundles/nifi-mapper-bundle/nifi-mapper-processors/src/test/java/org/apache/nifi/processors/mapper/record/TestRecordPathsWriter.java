package org.apache.nifi.processors.mapper.record;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import org.apache.avro.Schema;
import org.apache.nifi.processors.mapper.exp.MapperTable;
import org.apache.nifi.processors.mapper.exp.MapperTableType;
import org.apache.nifi.serialization.record.DataType;
import org.apache.nifi.serialization.record.Record;
import org.apache.nifi.serialization.record.RecordField;
import org.apache.nifi.serialization.record.RecordSchema;
import org.apache.nifi.serialization.record.type.RecordDataType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 * @author GU Guoqiang
 * 
 */
public class TestRecordPathsWriter {
    private static Schema studentSchema;

    @BeforeClass
    public static void init() throws IOException {
        studentSchema = new Schema.Parser().parse(new File("src/test/resources/student.avsc"));
    }

    @AfterClass
    public static void cleanup() {
        studentSchema = null;
    }

    @Test
    public void test_createDefaultValue() {
        final String tablename = "test";

        MapperTable table = new MapperTable();
        table.setName(tablename);
        table.setId("xxx-" + System.currentTimeMillis());
        table.setType(MapperTableType.OUTPUT);
        table.setSchema(studentSchema);

        final Optional<Object> defaultValueOp = new RecordPathsWriter().createDefaultValue(table);

        assertTrue(defaultValueOp.isPresent());
        final Object object = defaultValueOp.get();
        assertNotNull(object);
        assertTrue(object instanceof Record);
        Record record = ((Record) object);

        assertNull(record.getValue("id")); // no default value, so will be null
        assertThat(record.getRawFieldNames(), hasItem("id"));
        assertEquals("<undefined>", record.getValue("name"));
        assertEquals(1, record.getAsInt("age").intValue());
        assertTrue(true == record.getAsBoolean("flag"));

        final Object[] lessonsArray = record.getAsArray("lessons");
        assertEquals(1, lessonsArray.length);
        assertTrue(lessonsArray[0] instanceof Record);
        Record lesson = (Record) lessonsArray[0];
        assertNull(lesson.getAsString("id"));
        assertThat(lesson.getRawFieldNames(), hasItem("id"));
        assertEquals("<undefined>", lesson.getValue("name"));

        final Optional<RecordField> homeField = record.getSchema().getField("home");
        assertTrue(homeField.isPresent());
        final DataType homeDataType = homeField.get().getDataType();
        assertTrue(homeDataType instanceof RecordDataType);
        final RecordSchema homeSchema = ((RecordDataType) homeDataType).getChildSchema();
        final Record homeRecord = record.getAsRecord("home", homeSchema);
        assertEquals("100000", homeRecord.getValue("post"));

        final Optional<RecordField> addressField = homeSchema.getField("address");
        assertTrue(addressField.isPresent());
        final DataType addressDataType = addressField.get().getDataType();
        assertTrue(addressDataType instanceof RecordDataType);
        final RecordSchema addressSchema = ((RecordDataType) addressDataType).getChildSchema();
        final Record addressRecord = homeRecord.getAsRecord("address", addressSchema);
        final Set<String> rawFieldNames = addressRecord.getRawFieldNames();
        assertThat(rawFieldNames, hasItem("country"));
        assertNull(homeRecord.getValue("country"));
        assertThat(rawFieldNames, hasItem("city"));
        assertNull(homeRecord.getValue("city"));
        assertThat(rawFieldNames, hasItem("street"));
        assertNull(homeRecord.getValue("street"));
        assertThat(rawFieldNames, hasItem("number"));
        assertNull(homeRecord.getValue("number"));
    }
}
