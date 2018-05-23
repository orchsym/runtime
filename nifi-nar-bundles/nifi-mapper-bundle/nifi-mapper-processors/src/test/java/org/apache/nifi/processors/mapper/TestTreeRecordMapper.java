package org.apache.nifi.processors.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.nifi.avro.AvroTypeUtil;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.processors.mapper.exp.MapperExpField;
import org.apache.nifi.processors.mapper.exp.MapperTable;
import org.apache.nifi.processors.mapper.exp.MapperTableType;
import org.apache.nifi.processors.mapper.record.MockTreeRecordWriter;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.serialization.record.MapRecord;
import org.apache.nifi.serialization.record.MockRecordParser;
import org.apache.nifi.serialization.record.MockRecordWriter;
import org.apache.nifi.serialization.record.RecordField;
import org.apache.nifi.serialization.record.RecordSchema;
import org.apache.nifi.serialization.record.type.RecordDataType;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 * @author GU Guoqiang
 * 
 */
public class TestTreeRecordMapper {
    final static String PRE_MAIN_INPUT_EXP = TestFlatRecordMapper.PRE_MAIN_INPUT_EXP;
    private static Schema studentSchema;

    private TestRunner runner;
    private MockRecordParser readerService;
    private MockRecordWriter writerService;

    @BeforeClass
    public static void init() throws IOException {
        studentSchema = new Schema.Parser().parse(new File("src/test/resources/student.avsc"));
    }

    @AfterClass
    public static void cleanup() {
        studentSchema = null;
    }

    @Before
    public void setup() throws IOException, InitializationException {

        readerService = new MockRecordParser();
        writerService = new MockTreeRecordWriter("junit");

        runner = TestRunners.newTestRunner(new RecordMapper());
        runner.addControllerService("reader", readerService);
        runner.enableControllerService(readerService);
        runner.addControllerService("writer", writerService);
        runner.enableControllerService(writerService);

        runner.setProperty(RecordMapper.RECORD_READER, "reader");
        runner.setProperty(RecordMapper.RECORD_WRITER, "writer");

    }

    @After
    public void clean() throws IOException {
        runner = null;
        readerService = null;
        writerService = null;
    }

    private void createMainStaffRecords(final int numOfStaff, final MockRecordParser readerService) throws IOException {
        final RecordSchema recordSchema = AvroTypeUtil.createSchema(studentSchema);
        recordSchema.getFields().stream().forEach(f -> readerService.addSchemaField(f.getFieldName(), f.getDataType().getFieldType()));
        for (int i = 0; i < numOfStaff; i++) {
            List<Object> lessons = new ArrayList<>();
            for (int j = 1; j < 4; j++) {
                Map<String, Object> lesson = new HashMap<>();
                lesson.put("id", j);
                lesson.put("name", "LN " + j);
                lessons.add(lesson);
            }

            final RecordField homeField = recordSchema.getField("home").get();
            final RecordSchema homeSchema = ((RecordDataType) homeField.getDataType()).getChildSchema();
            MapRecord home = new MapRecord(homeSchema, new LinkedHashMap<String, Object>());
            home.setValue("post", "12345" + i);

            final RecordField addressField = homeSchema.getField("address").get();
            MapRecord address = new MapRecord(((RecordDataType) addressField.getDataType()).getChildSchema(), new LinkedHashMap<String, Object>());
            home.setValue("address", address);

            address.setValue("country", "China");
            address.setValue("city", "Beijing");
            address.setValue("street", "world");
            address.setValue("number", 123);

            readerService.addRecord(i, "World " + i, 18 - i, i % 2 == 0, lessons, home);
        }
    }

    private MapperTable createOutputTable(String outSchemaName) {
        MapperTable table = new MapperTable();
        table.setName(outSchemaName);
        table.setId("xxx-" + System.currentTimeMillis());
        table.setType(MapperTableType.OUTPUT);

        return table;
    }

    private void doRunnerTest(TestRunner runner, MapperTable outTable, int dataCount, String testContents) throws Exception {
        doRunnerTest(runner, outTable, dataCount, null, testContents);
    }

    private void doRunnerTest(TestRunner runner, MapperTable outTable, int dataCount, Integer filteredCount, String testContents) throws Exception {
        final Schema outSchema = outTable.getSchema();
        final String outSchemaName = outSchema.getName();
        final String outputFlowName = RecordMapper.PRE_OUTPUT + outSchemaName;

        PropertyDescriptor outputTableSettings = new PropertyDescriptor.Builder().name(outputFlowName).description(outSchemaName + " relationship").addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
                .expressionLanguageSupported(ExpressionLanguageScope.NONE).dynamic(true).build();
        runner.setProperty(outputTableSettings, new MapperTable.Writer().write(outTable));

        createMainStaffRecords(dataCount, readerService);

        runner.enqueue("");
        runner.run();

        runner.assertTransferCount(RecordMapper.REL_ORIGINAL, 1);
        runner.assertTransferCount(RecordMapper.REL_FAILURE, 0);
        runner.assertTransferCount(outputFlowName, 1);

        final List<MockFlowFile> flowfiles = runner.getFlowFilesForRelationship(outputFlowName);
        assertEquals(flowfiles.size(), 1);

        final MockFlowFile out = flowfiles.get(0);
        if (filteredCount == null) {// same as dataCount
            filteredCount = dataCount;
        }
        out.assertAttributeEquals("map.count", String.valueOf(filteredCount));

        // contents
        if (testContents != null) {
            final MockFlowFile flows = runner.getFlowFilesForRelationship(outputFlowName).get(0);
            flows.assertContentEquals(testContents);
        }
    }

    /**
     * {"id": 2, "name":{ "first":"hello", "last": "world" } }
     * 
     */
    @Test
    public void testMapRecord_outputAvroRecord_depth1() throws Exception {
        final String outSchemaName = "out_avrorecord_depth1";
        final MapperTable table = createOutputTable(outSchemaName);

        // expression for first name and last name for children
        final List<MapperExpField> expressions = table.getExpressions();
        expressions.add(new MapperExpField("/name/first", "${" + PRE_MAIN_INPUT_EXP + ".name}"));
        expressions.add(new MapperExpField("/name/last", "${" + PRE_MAIN_INPUT_EXP + ".name}"));

        Schema nameSchema = AvroCreator.createSchema("name", AvroCreator.createField("first", Type.STRING), AvroCreator.createField("last", Type.STRING));
        Schema schema = AvroCreator.createSchema(outSchemaName, AvroCreator.createField("id", Type.INT), AvroCreator.createField("name", nameSchema));
        table.setSchema(schema);

        //
        final int dataNum = 3;
        final String testContents = "junit\n" + //
                "MapRecord[{id=0, name=MapRecord[{first=World 0, last=World 0}]}]\n" + //
                "MapRecord[{id=1, name=MapRecord[{first=World 1, last=World 1}]}]\n" + //
                "MapRecord[{id=2, name=MapRecord[{first=World 2, last=World 2}]}]\n";
        doRunnerTest(runner, table, dataNum, testContents);
    }

    /**
     * {"id": 2, "test":{"name":{ "first":"hello", "mid":"-", "last": "world","age":30 } }}
     * 
     */
    @Test
    public void testMapRecord_outputAvroRecord_depth2() throws Exception {
        final String outSchemaName = "out_avrorecord_depth2";
        final MapperTable table = createOutputTable(outSchemaName);

        final List<MapperExpField> expressions = table.getExpressions();
        expressions.add(new MapperExpField("/test/name/first", "${" + PRE_MAIN_INPUT_EXP + ".name}"));
        expressions.add(new MapperExpField("/test/name/age", "${" + PRE_MAIN_INPUT_EXP + ".age}"));

        Schema nameSchema = AvroCreator.createSchema("name", AvroCreator.createField("first", Type.STRING), AvroCreator.createField("last", Type.STRING), AvroCreator.createField("age", Type.INT));
        Schema testSchema = AvroCreator.createSchema("test", AvroCreator.createField("name", nameSchema));
        Schema schema = AvroCreator.createSchema(outSchemaName, AvroCreator.createField("id", Type.INT), AvroCreator.createField("test", testSchema));
        table.setSchema(schema);

        //
        final int dataNum = 3;
        final String testContents = "junit\n" + //
                "MapRecord[{id=0, test=MapRecord[{name=MapRecord[{first=World 0, last=null, age=18}]}]}]\n" + //
                "MapRecord[{id=1, test=MapRecord[{name=MapRecord[{first=World 1, last=null, age=17}]}]}]\n" + //
                "MapRecord[{id=2, test=MapRecord[{name=MapRecord[{first=World 2, last=null, age=16}]}]}]\n";
        doRunnerTest(runner, table, dataNum, testContents);
    }

    /**
     * If don't set the expression for output fields, will be null or default value.
     */
    @Test
    public void testMapRecord_outputAvroRecord_defualtValue() throws Exception {
        final String outSchemaName = "out_avrorecord_defualtValue";
        final MapperTable table = createOutputTable(outSchemaName);

        final List<MapperExpField> expressions = table.getExpressions();
        expressions.add(new MapperExpField("/test/name/first", "${" + PRE_MAIN_INPUT_EXP + ".name}"));
        expressions.add(new MapperExpField("/test/name/age", "${" + PRE_MAIN_INPUT_EXP + ".age}"));

        Schema nameSchema = AvroCreator.createSchema("name", AvroCreator.createField("first", Type.STRING), AvroCreator.createField("mid", Type.STRING, "-"),
                AvroCreator.createField("last", Type.STRING), AvroCreator.createField("age", Type.INT));
        Schema testSchema = AvroCreator.createSchema("test", AvroCreator.createField("name", nameSchema));
        Schema schema = AvroCreator.createSchema(outSchemaName, AvroCreator.createField("id", Type.INT), AvroCreator.createField("test", testSchema));
        table.setSchema(schema);

        //
        final int dataNum = 3;
        final String testContents = "junit\n" + //
                "MapRecord[{id=0, test=MapRecord[{name=MapRecord[{first=World 0, mid=-, last=null, age=18}]}]}]\n" + //
                "MapRecord[{id=1, test=MapRecord[{name=MapRecord[{first=World 1, mid=-, last=null, age=17}]}]}]\n" + //
                "MapRecord[{id=2, test=MapRecord[{name=MapRecord[{first=World 2, mid=-, last=null, age=16}]}]}]\n";
        doRunnerTest(runner, table, dataNum, testContents);
    }

    /**
     * {"id": 2, "name":{ "first":"hello", "last": "world" } }
     * 
     */
    @Test
    public void testMapRecord_outputAvroMap_depth1() throws Exception {
        final String outSchemaName = "out_avromap_depth1";
        final MapperTable table = createOutputTable(outSchemaName);

        final List<MapperExpField> expressions = table.getExpressions();
        expressions.add(new MapperExpField("/name['first']", "${" + PRE_MAIN_INPUT_EXP + ".name}"));
        expressions.add(new MapperExpField("/name['last']", "${" + PRE_MAIN_INPUT_EXP + ".name}"));
        expressions.add(new MapperExpField("/name['mid']", "${" + PRE_MAIN_INPUT_EXP + ".abc}")); // not existed from input, will be empty value
        expressions.add(new MapperExpField("/test['name']", "${" + PRE_MAIN_INPUT_EXP + ".name}")); // not existed in output, will discard

        Schema schema = AvroCreator.createSchema(outSchemaName, AvroCreator.createField("id", Type.INT), AvroCreator.createField("name", Schema.createMap((Schema.create(Type.STRING)))));
        table.setSchema(schema);

        //
        final int dataNum = 3;
        final String testContents = "junit\n" + //
                "MapRecord[{id=0, name={first=World 0, last=World 0, mid=}}]\n" + //
                "MapRecord[{id=1, name={first=World 1, last=World 1, mid=}}]\n" + //
                "MapRecord[{id=2, name={first=World 2, last=World 2, mid=}}]\n";
        doRunnerTest(runner, table, dataNum, testContents);
    }

    /**
     * {"id": 2, "test":{"name":{ "first":"hello", "last": "world" },"age":30 }}
     * 
     */
    @Test
    public void testMapRecord_outputAvroRecordMap_depth2() throws Exception {
        final String outSchemaName = "out_avrorecordmap_depth2";
        final MapperTable table = createOutputTable(outSchemaName);

        final List<MapperExpField> expressions = table.getExpressions();
        expressions.add(new MapperExpField("/test/name['first']", "${" + PRE_MAIN_INPUT_EXP + ".name}"));
        expressions.add(new MapperExpField("/test/age", "${" + PRE_MAIN_INPUT_EXP + ".age}"));
        expressions.add(new MapperExpField("/test/abc", "/home/address/city"));

        Schema testSchema = AvroCreator.createSchema("test", AvroCreator.createField("name", Schema.createMap(Schema.create(Type.STRING))), AvroCreator.createField("age", Type.INT));
        Schema schema = AvroCreator.createSchema(outSchemaName, AvroCreator.createField("id", Type.INT), AvroCreator.createField("test", testSchema));
        table.setSchema(schema);
        //
        final int dataNum = 3;
        final String testContents = "junit\n" + //
                "MapRecord[{id=0, test=MapRecord[{name={first=World 0}, age=18}]}]\n" + //
                "MapRecord[{id=1, test=MapRecord[{name={first=World 1}, age=17}]}]\n" + //
                "MapRecord[{id=2, test=MapRecord[{name={first=World 2}, age=16}]}]\n";
        doRunnerTest(runner, table, dataNum, testContents);
    }

    /**
     * {"id": 2, "list":["world", "Beijing" ] }
     * 
     */
    @Test
    public void testMapRecord_outputAvroArray_depth1() throws Exception {
        final String outSchemaName = "out_avroarr_depth";
        final MapperTable table = createOutputTable(outSchemaName);

        final List<MapperExpField> expressions = table.getExpressions();
        expressions.add(new MapperExpField("/list[0]", "/home/address/street"));
        expressions.add(new MapperExpField("/list[1]", "/home/address/city"));

        Schema schema = AvroCreator.createSchema(outSchemaName, AvroCreator.createField("id", Type.INT), AvroCreator.createField("list", Schema.createArray((Schema.create(Type.STRING)))));
        table.setSchema(schema);

        //
        writerService = new MockTreeRecordWriter("junit", schema);
        runner.addControllerService("treeWriter", writerService);
        runner.enableControllerService(writerService);
        runner.setProperty(RecordMapper.RECORD_WRITER, "treeWriter");

        //
        final int dataNum = 3;
        final String testContents = "junit\n" + //
                "{\"id\": 0, \"list\": [\"world\", \"Beijing\"]}\n" + //
                "{\"id\": 1, \"list\": [\"world\", \"Beijing\"]}\n" + //
                "{\"id\": 2, \"list\": [\"world\", \"Beijing\"]}\n";
        doRunnerTest(runner, table, dataNum, testContents);
    }

    /**
     * {"id": 2, "list":["Beijing", "China", , ,"123" ] }
     * 
     */
    @Test
    public void testMapRecord_outputAvroArray_orderless() throws Exception {
        final String outSchemaName = "out_avroarr_orderless";
        final MapperTable table = createOutputTable(outSchemaName);

        final List<MapperExpField> expressions = table.getExpressions();
        expressions.add(new MapperExpField("/list[1]", "/home/address/country"));
        expressions.add(new MapperExpField("/list[0]", "/home/address/city"));
        expressions.add(new MapperExpField("/list[4]", "/home/address/number"));

        Schema schema = AvroCreator.createSchema(outSchemaName, AvroCreator.createField("id", Type.INT), AvroCreator.createField("list", Schema.createArray((Schema.create(Type.STRING)))));
        table.setSchema(schema);

        //
        writerService = new MockTreeRecordWriter("junit", schema);
        runner.addControllerService("treeWriter", writerService);
        runner.enableControllerService(writerService);
        runner.setProperty(RecordMapper.RECORD_WRITER, "treeWriter");

        //
        final int dataNum = 3;
        final String testContents = "junit\n" + //
                "{\"id\": 0, \"list\": [\"Beijing\", \"China\", null, null, \"123\"]}\n" + //
                "{\"id\": 1, \"list\": [\"Beijing\", \"China\", null, null, \"123\"]}\n" + //
                "{\"id\": 2, \"list\": [\"Beijing\", \"China\", null, null, \"123\"]}\n";
        doRunnerTest(runner, table, dataNum, testContents);
    }

    /**
     * {"id": 2, "name":{"list":["hello", "world" ]} }
     * 
     */
    @Test
    public void testMapRecord_outputAvroArray_depth2() throws Exception {
        final String outSchemaName = "out_avromap_depth1";
        final MapperTable table = createOutputTable(outSchemaName);

        // expression for first name and last name for children
        final List<MapperExpField> expressions = table.getExpressions();
        expressions.add(new MapperExpField("/name/list[0]", "/home/address/street"));
        expressions.add(new MapperExpField("/name/list[1]", "/home/address/city"));

        // schema for id name first and last
        Field listField = AvroCreator.createField("list", Schema.createArray(Schema.create(Type.STRING)));
        Schema schema = AvroCreator.createSchema(outSchemaName, AvroCreator.createField("id", Type.INT), AvroCreator.createField("name", AvroCreator.createSchema("name", listField)));
        table.setSchema(schema);

        //
        writerService = new MockTreeRecordWriter("junit", schema);
        runner.addControllerService("treeWriter", writerService);
        runner.enableControllerService(writerService);
        runner.setProperty(RecordMapper.RECORD_WRITER, "treeWriter");

        //
        final int dataNum = 3;
        final String testContents = "junit\n" + //
                "{\"id\": 0, \"name\": {\"list\": [\"world\", \"Beijing\"]}}\n" + //
                "{\"id\": 1, \"name\": {\"list\": [\"world\", \"Beijing\"]}}\n" + //
                "{\"id\": 2, \"name\": {\"list\": [\"world\", \"Beijing\"]}}\n";
        doRunnerTest(runner, table, dataNum, testContents);
    }

    @Test
    public void testMapRecord_outputAvroRecord_unmatchedExpression() throws Exception {
        final String outSchemaName = "out_avrorecord_unmatched";
        final MapperTable table = createOutputTable(outSchemaName);

        final List<MapperExpField> expressions = table.getExpressions();
        // expressions.add(new MapperExpField("/id", "/id")); //implicitly
        expressions.add(new MapperExpField("/name/first", "${" + PRE_MAIN_INPUT_EXP + ".name}"));
        expressions.add(new MapperExpField("/name/last", "${" + PRE_MAIN_INPUT_EXP + ".last_name}")); // not existed from input
        expressions.add(new MapperExpField("/name/age", "/age"));
        expressions.add(new MapperExpField("/city", "/home/address/city")); // not existed in output
        expressions.add(new MapperExpField("/country", "/home/address/country"));

        Schema nameSchema = AvroCreator.createSchema("name", AvroCreator.createField("first", Type.STRING), AvroCreator.createField("last", Type.STRING), AvroCreator.createField("age", Type.INT));
        Schema schema = AvroCreator.createSchema(outSchemaName, AvroCreator.createField("id", Type.INT), AvroCreator.createField("name", nameSchema), AvroCreator.createField("country", Type.STRING));
        table.setSchema(schema);

        //
        final int dataNum = 3;
        final String testContents = "junit\n" + //
                "MapRecord[{id=0, name=MapRecord[{first=World 0, last=, age=18}], country=China}]\n" + //
                "MapRecord[{id=1, name=MapRecord[{first=World 1, last=, age=17}], country=China}]\n" + //
                "MapRecord[{id=2, name=MapRecord[{first=World 2, last=, age=16}], country=China}]\n";
        doRunnerTest(runner, table, dataNum, testContents);
    }

    @Test
    public void testMapRecord_outputAvroRecord_pathRecord_sameSchema() throws Exception {
        final String outSchemaName = "out_avrorecord_sameSchema";

        Schema addressSchema = AvroCreator.createSchema("address", AvroCreator.createField("country", Type.STRING), AvroCreator.createField("city", Type.STRING),
                AvroCreator.createField("street", Type.STRING), AvroCreator.createField("number", Type.INT));
        Schema schema = AvroCreator.createSchema(outSchemaName, AvroCreator.createField("id", Type.INT), AvroCreator.createField("user", Type.STRING), AvroCreator.createField("age", Type.INT),
                AvroCreator.createField("address", addressSchema));

        final String testContents = "junit\n" + //
                "{\"id\": 0, \"user\": \"World 0\", \"age\": 18, \"address\": {\"country\": \"China\", \"city\": \"Beijing\", \"street\": \"world\", \"number\": 123}}\n" + //
                "{\"id\": 1, \"user\": \"World 1\", \"age\": 17, \"address\": {\"country\": \"China\", \"city\": \"Beijing\", \"street\": \"world\", \"number\": 123}}\n" + //
                "{\"id\": 2, \"user\": \"World 2\", \"age\": 16, \"address\": {\"country\": \"China\", \"city\": \"Beijing\", \"street\": \"world\", \"number\": 123}}\n";

        doTest_pathRecord(outSchemaName, schema, testContents);
    }

    /**
     * the output will depend on the schema always.
     */
    @Test
    public void testMapRecord_outputAvroRecord_pathRecord_partSchema() throws Exception {
        final String outSchemaName = "out_avrorecord_partSchema";

        Schema addressSchema = AvroCreator.createSchema("address", AvroCreator.createField("country", Type.STRING), AvroCreator.createField("city", Type.STRING),
                AvroCreator.createField("street", Type.STRING));
        Schema schema = AvroCreator.createSchema(outSchemaName, AvroCreator.createField("id", Type.INT), AvroCreator.createField("user", Type.STRING), AvroCreator.createField("age", Type.INT),
                AvroCreator.createField("address", addressSchema));

        final String testContents = "junit\n" + //
                "{\"id\": 0, \"user\": \"World 0\", \"age\": 18, \"address\": {\"country\": \"China\", \"city\": \"Beijing\", \"street\": \"world\"}}\n" + //
                "{\"id\": 1, \"user\": \"World 1\", \"age\": 17, \"address\": {\"country\": \"China\", \"city\": \"Beijing\", \"street\": \"world\"}}\n" + //
                "{\"id\": 2, \"user\": \"World 2\", \"age\": 16, \"address\": {\"country\": \"China\", \"city\": \"Beijing\", \"street\": \"world\"}}\n";

        doTest_pathRecord(outSchemaName, schema, testContents);
    }

    /**
     * if have more fields, even have default value, because the expression is full path the record, so will be null always.
     */
    @Test
    public void testMapRecord_outputAvroRecord_pathRecord_overSchema() throws Exception {
        final String outSchemaName = "out_avrorecord_overSchema";

        Schema addressSchema = AvroCreator.createSchema("address", AvroCreator.createField("country", Type.STRING), AvroCreator.createField("city", Type.STRING),
                AvroCreator.createField("street", Type.STRING), AvroCreator.createField("house", Type.STRING, "110#"), AvroCreator.createField("number", Type.INT));
        Schema schema = AvroCreator.createSchema(outSchemaName, AvroCreator.createField("id", Type.INT), AvroCreator.createField("user", Type.STRING), AvroCreator.createField("age", Type.INT),
                AvroCreator.createField("address", addressSchema));

        final String testContents = "junit\n" + //
                "{\"id\": 0, \"user\": \"World 0\", \"age\": 18, \"address\": {\"country\": \"China\", \"city\": \"Beijing\", \"street\": \"world\", \"house\": null, \"number\": 123}}\n" + //
                "{\"id\": 1, \"user\": \"World 1\", \"age\": 17, \"address\": {\"country\": \"China\", \"city\": \"Beijing\", \"street\": \"world\", \"house\": null, \"number\": 123}}\n" + //
                "{\"id\": 2, \"user\": \"World 2\", \"age\": 16, \"address\": {\"country\": \"China\", \"city\": \"Beijing\", \"street\": \"world\", \"house\": null, \"number\": 123}}\n";

        doTest_pathRecord(outSchemaName, schema, testContents);
    }

    /**
     * even set the expression, but without schema, the output will depend on the schema always, and drop the unknown fields.
     */
    @Test
    public void testMapRecord_outputAvroRecord_pathRecord_missingSchema() throws Exception {
        final String outSchemaName = "out_avrorecord_missingSchema";

        Schema schema = AvroCreator.createSchema(outSchemaName, AvroCreator.createField("id", Type.INT), AvroCreator.createField("user", Type.STRING), AvroCreator.createField("age", Type.INT));

        final String testContents = "junit\n" + //
                "{\"id\": 0, \"user\": \"World 0\", \"age\": 18}\n" + //
                "{\"id\": 1, \"user\": \"World 1\", \"age\": 17}\n" + //
                "{\"id\": 2, \"user\": \"World 2\", \"age\": 16}\n";

        doTest_pathRecord(outSchemaName, schema, testContents);
    }

    /**
     * because the MapRecord can't cast to Array, then will throw ClassCastException, but because use mock to test, finally throw AssertionError.
     */
    @Test(expected = AssertionError.class)
    public void testMapRecord_outputAvroRecord_pathRecord_diffSchema_diffType() throws Exception {
        final String outSchemaName = "out_avrorecord_diffType";

        Schema schema = AvroCreator.createSchema(outSchemaName, AvroCreator.createField("id", Type.INT), AvroCreator.createField("user", Type.STRING), AvroCreator.createField("age", Type.INT),
                AvroCreator.createField("address", Schema.createArray(Schema.create(Type.STRING))));

        doTest_pathRecord(outSchemaName, schema, "");
    }

    /**
     * The MapRecord will do toString for output field.
     */
    @Test
    public void testMapRecord_outputAvroRecord_pathRecord_diffSchema_outputSimpleType() throws Exception {
        final String outSchemaName = "out_avrorecord_outputSimpleTyp";

        Schema schema = AvroCreator.createSchema(outSchemaName, AvroCreator.createField("id", Type.INT), AvroCreator.createField("user", Type.STRING), AvroCreator.createField("age", Type.INT),
                AvroCreator.createField("address", Type.STRING));

        final String testContents = "junit\n" + //
                "{\"id\": 0, \"user\": \"World 0\", \"age\": 18, \"address\": \"MapRecord[{country=China, city=Beijing, street=world, number=123}]\"}\n" + //
                "{\"id\": 1, \"user\": \"World 1\", \"age\": 17, \"address\": \"MapRecord[{country=China, city=Beijing, street=world, number=123}]\"}\n" + //
                "{\"id\": 2, \"user\": \"World 2\", \"age\": 16, \"address\": \"MapRecord[{country=China, city=Beijing, street=world, number=123}]\"}\n";

        doTest_pathRecord(outSchemaName, schema, testContents);
    }

    private void doTest_pathRecord(final String outSchemaName, final Schema schema, final String testContents) throws Exception {
        final MapperTable table = createOutputTable(outSchemaName);

        final List<MapperExpField> expressions = table.getExpressions();
        // expressions.add(new MapperExpField("/id", "/id")); //implicitly
        expressions.add(new MapperExpField("/user", "/name"));
        expressions.add(new MapperExpField("/age", "/age"));
        expressions.add(new MapperExpField("/address", "/home/address")); // whold record
        table.setSchema(schema);

        //
        writerService = new MockTreeRecordWriter("junit", schema);
        runner.addControllerService("treeWriter", writerService);
        runner.enableControllerService(writerService);
        runner.setProperty(RecordMapper.RECORD_WRITER, "treeWriter");

        //
        final int dataNum = 3;
        doRunnerTest(runner, table, dataNum, testContents);
    }

    /**
     * number: int -> string, post: string -> int, age: int -> boolean
     */
    @Test
    public void testMapRecord_outputAvroRecord_coerceTypes_int() throws Exception {
        final String outSchemaName = "out_coerceTypes_int";

        final MapperTable table = createOutputTable(outSchemaName);

        final List<MapperExpField> expressions = table.getExpressions();
        // expressions.add(new MapperExpField("/id", "/id")); //implicitly
        expressions.add(new MapperExpField("/user", "/name"));
        expressions.add(new MapperExpField("/young", "${" + PRE_MAIN_INPUT_EXP + ".age:lt(18)}"));
        expressions.add(new MapperExpField("/post", "/home/post"));
        expressions.add(new MapperExpField("/number", "/home/address/number"));

        Schema schema = AvroCreator.createSchema(outSchemaName, AvroCreator.createField("id", Type.INT), AvroCreator.createField("user", Type.STRING), AvroCreator.createField("young", Type.BOOLEAN),
                AvroCreator.createField("post", Type.INT), AvroCreator.createField("number", Type.STRING));

        table.setSchema(schema);

        //
        writerService = new MockTreeRecordWriter("junit", schema);
        runner.addControllerService("treeWriter", writerService);
        runner.enableControllerService(writerService);
        runner.setProperty(RecordMapper.RECORD_WRITER, "treeWriter");

        final String testContents = "junit\n" + //
                "{\"id\": 0, \"user\": \"World 0\", \"young\": false, \"post\": 123450, \"number\": \"123\"}\n" + //
                "{\"id\": 1, \"user\": \"World 1\", \"young\": true, \"post\": 123451, \"number\": \"123\"}\n" + //
                "{\"id\": 2, \"user\": \"World 2\", \"young\": true, \"post\": 123452, \"number\": \"123\"}\n";

        //
        final int dataNum = 3;
        doRunnerTest(runner, table, dataNum, testContents);
    }
}
