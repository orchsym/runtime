package org.apache.nifi.processors.mapper;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.commons.io.FileUtils;
import org.apache.nifi.avro.AvroReader;
import org.apache.nifi.avro.AvroRecordSetWriter;
import org.apache.nifi.avro.AvroTypeUtil;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.processors.mapper.exp.MapperExpField;
import org.apache.nifi.processors.mapper.exp.MapperTable;
import org.apache.nifi.processors.mapper.exp.MapperTableType;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.serialization.record.MockRecordParser;
import org.apache.nifi.serialization.record.MockRecordWriter;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestRecordMapper {
    private static final int TEST_NUM = 100;
    private Schema mainStaffSchema;

    private TestRunner runner;
    private MockRecordParser readerService;
    private MockRecordWriter writerService;
    final String PRE_MAIN_INPUT_EXP = RecordMapper.PRE_INPUT + RecordMapper.DEFAULT_MAIN;

    @Before
    public void setup() throws IOException, InitializationException {
        mainStaffSchema = new Schema.Parser().parse(new File("src/test/resources/staff.avsc"));

        readerService = new MockRecordParser();
        writerService = new MockRecordWriter("header", false);

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
        mainStaffSchema = null;
        runner = null;
        readerService = null;
        writerService = null;
    }

    private void createMainStaff(final int numOfStaff, final MockRecordParser readerService) throws IOException {
        AvroTypeUtil.createSchema(mainStaffSchema).getFields().stream().forEach(f -> readerService.addSchemaField(f.getFieldName(), f.getDataType().getFieldType()));

        LocalDate date = LocalDate.now().withYear(1919).withMonth(1).withDayOfMonth(1);
        for (int i = 0; i < numOfStaff; i++) {
            readerService.addRecord(i, "Hello", "World " + i, date.plusYears(i).toString(), "No. " + i + ", Beijing");
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
        final String outputFlowName = RecordMapper.PRE_OUTPUT + outSchema.getName();

        PropertyDescriptor outputTableSettings = new PropertyDescriptor.Builder().name(RecordMapper.PRE_OUTPUT + outSchemaName).description(outSchemaName + " relationship")
                .addValidator(StandardValidators.NON_EMPTY_VALIDATOR).expressionLanguageSupported(ExpressionLanguageScope.NONE).dynamic(true).build();
        runner.setProperty(outputTableSettings, new MapperTable.Writer().write(outTable));

        createMainStaff(dataCount, readerService);

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
        out.assertAttributeEquals("record.count", String.valueOf(filteredCount));

        // contents
        if (testContents != null) {
            final MockFlowFile flows = runner.getFlowFilesForRelationship(outputFlowName).get(0);
            flows.assertContentEquals(testContents);
        }
    }

    @Test
    public void testMapRecord_1vs1_full() throws Exception {
        final String outSchemaName = "out1_full";
        final MapperTable table = createOutputTable(outSchemaName);

        Schema schema = Schema.createRecord(outSchemaName, outSchemaName + " schema", mainStaffSchema.getNamespace(), false);
        List<Field> fields = mainStaffSchema.getFields().stream().map(f -> new Field(f.name(), f.schema(), f.doc(), f.defaultVal())).collect(Collectors.toList());
        schema.setFields(fields);
        table.setSchema(schema);

        doRunnerTest(runner, table, TEST_NUM, null);
    }

    @Test
    public void testMapRecord_1vs1_fullBatch() throws Exception {
        final String outSchemaName = "out1_full_batch";
        final MapperTable table = createOutputTable(outSchemaName);

        Schema schema = Schema.createRecord(outSchemaName, outSchemaName + " schema", mainStaffSchema.getNamespace(), false);
        List<Field> fields = mainStaffSchema.getFields().stream().map(f -> new Field(f.name(), f.schema(), f.doc(), f.defaultVal())).collect(Collectors.toList());
        schema.setFields(fields);
        table.setSchema(schema);

        final int dataNum = 44;
        final String testContents = FileUtils.readFileToString(new File("src/test/resources/expected-output-content"), "UTF-8");
        doRunnerTest(runner, table, dataNum, testContents);

    }

    @Test
    public void testMapRecord_1vs1_assertContents() throws Exception {
        final String outSchemaName = "out1_contents";
        final MapperTable table = createOutputTable(outSchemaName);

        Schema schema = Schema.createRecord(outSchemaName, outSchemaName + " schema", mainStaffSchema.getNamespace(), false);
        List<Field> fields = mainStaffSchema.getFields().stream().map(f -> new Field(f.name(), f.schema(), f.doc(), f.defaultVal())).collect(Collectors.toList());
        schema.setFields(fields);
        table.setSchema(schema);

        //
        final int dataNum = 3;
        final String testContents = "header\n" + "0,Hello,World 0,1919-01-01,No. 0, Beijing\n" + "1,Hello,World 1,1920-01-01,No. 1, Beijing\n" + "2,Hello,World 2,1921-01-01,No. 2, Beijing\n";
        doRunnerTest(runner, table, dataNum, testContents);

    }

    @Test
    public void testMapRecord_1vs1_lessFields() throws Exception {
        final String outSchemaName = "out1_less";
        final MapperTable table = createOutputTable(outSchemaName);

        Schema schema = Schema.createRecord(outSchemaName, outSchemaName + " schema", mainStaffSchema.getNamespace(), false);
        // only id, first name, last name
        List<Field> fields = mainStaffSchema.getFields().stream().filter(f -> f.name().equals("id") || f.name().endsWith("_name")).map(f -> new Field(f.name(), f.schema(), f.doc(), f.defaultVal()))
                .collect(Collectors.toList());
        schema.setFields(fields);
        table.setSchema(schema);

        //
        final int dataNum = 3;
        final String testContents = "header\n" + "0,Hello,World 0\n" + "1,Hello,World 1\n" + "2,Hello,World 2\n";
        doRunnerTest(runner, table, dataNum, testContents);
    }

    @Test
    public void testMapRecord_1vs1_fullExp() throws Exception {
        final String outSchemaName = "out1_full_exp";

        final MapperTable table = createOutputTable(outSchemaName);

        // expression for all fields
        final List<MapperExpField> expFields = mainStaffSchema.getFields().stream().map(f -> {
            MapperExpField ef = new MapperExpField();
            ef.setPath('/' + f.name());
            ef.setExp("${" + PRE_MAIN_INPUT_EXP + '.' + f.name() + "}");
            return ef;
        }).collect(Collectors.toList());
        table.getExpressions().addAll(expFields);

        // schema
        Schema schema = Schema.createRecord(outSchemaName, outSchemaName + " schema", mainStaffSchema.getNamespace(), false);
        List<Field> fields = mainStaffSchema.getFields().stream().map(f -> new Field(f.name(), f.schema(), f.doc(), f.defaultVal())).collect(Collectors.toList());
        schema.setFields(fields);
        table.setSchema(schema);

        //
        final int dataNum = 3;
        final String testContents = "header\n" + "0,Hello,World 0,1919-01-01,No. 0, Beijing\n" + "1,Hello,World 1,1920-01-01,No. 1, Beijing\n" + "2,Hello,World 2,1921-01-01,No. 2, Beijing\n";
        doRunnerTest(runner, table, dataNum, testContents);
    }

    @Test
    public void testMapRecord_1vs1_renameField() throws Exception {
        final String outSchemaName = "out1_rename";
        final MapperTable table = createOutputTable(outSchemaName);

        final String oldName = "birth";
        final String newName = "birth_date";
        // expression
        MapperExpField ef = new MapperExpField();
        ef.setPath("/" + newName);
        ef.setExp("${" + PRE_MAIN_INPUT_EXP + "." + oldName + "}");
        table.getExpressions().add(ef);

        // schema
        Schema schema = Schema.createRecord(outSchemaName, outSchemaName + " schema", mainStaffSchema.getNamespace(), false);
        List<Field> fields = mainStaffSchema.getFields().stream().filter(f -> !f.name().equals(oldName)).map(f -> new Field(f.name(), f.schema(), f.doc(), f.defaultVal()))
                .collect(Collectors.toList());

        // create new birth date in end
        Field birthf = mainStaffSchema.getField(oldName);
        birthf = new Field(newName, birthf.schema(), birthf.doc(), birthf.defaultVal());
        fields.add(birthf);

        schema.setFields(fields);
        table.setSchema(schema);

        //
        final int dataNum = 3;
        final String testContents = "header\n" + "0,Hello,World 0,No. 0, Beijing,1919-01-01\n" + "1,Hello,World 1,No. 1, Beijing,1920-01-01\n" + "2,Hello,World 2,No. 2, Beijing,1921-01-01\n";
        doRunnerTest(runner, table, dataNum, testContents);
    }

    @Test
    public void testMapRecord_1vs1_mergeFields() throws Exception {
        final String outSchemaName = "out1_merge";
        final MapperTable table = createOutputTable(outSchemaName);

        // expression for first name and last name to merge
        MapperExpField ef = new MapperExpField();
        ef.setPath("/full_name");
        ef.setExp("${" + PRE_MAIN_INPUT_EXP + ".first_name:append('-'):append(${" + PRE_MAIN_INPUT_EXP + ".last_name})}");
        table.getExpressions().add(ef);

        // schema for id name
        Schema schema = Schema.createRecord(outSchemaName, outSchemaName + " schema", mainStaffSchema.getNamespace(), false);

        List<Field> fields = new ArrayList<>();
        Field idf = mainStaffSchema.getField("id");
        idf = new Field(idf.name(), idf.schema(), idf.doc(), idf.defaultVal());
        fields.add(idf);

        Field namef = mainStaffSchema.getField("first_name");
        namef = new Field("full_name", namef.schema(), "full_name", (Object) null);
        fields.add(namef);

        schema.setFields(fields);
        table.setSchema(schema);

        //
        final int dataNum = 3;
        final String testContents = "header\n" + "0,Hello-World 0\n" + "1,Hello-World 1\n" + "2,Hello-World 2\n";
        doRunnerTest(runner, table, dataNum, testContents);
    }

    @Test
    public void testMapRecord_1vs1_convertType() throws Exception {
        final String outSchemaName = "out1_convert_type";
        final MapperTable table = createOutputTable(outSchemaName);

        final String oldName = "birth";
        final String newName = "age";
        // expression
        MapperExpField ef = new MapperExpField();
        ef.setPath("/" + newName);
        ef.setExp("${literal(2018):minus(${" + PRE_MAIN_INPUT_EXP + "." + oldName + ":toDate('yyyy-MM-dd'):format('yyyy'):toNumber()})}");
        table.getExpressions().add(ef);

        // schema
        Schema schema = Schema.createRecord(outSchemaName, outSchemaName + " schema", mainStaffSchema.getNamespace(), false);
        List<Field> fields = mainStaffSchema.getFields().stream().filter(f -> !f.name().equals(oldName)).map(f -> new Field(f.name(), f.schema(), f.doc(), f.defaultVal()))
                .collect(Collectors.toList());

        // create new age
        Field agef = new Field(newName, Schema.create(Type.INT), "", -1);
        fields.add(agef);

        schema.setFields(fields);
        table.setSchema(schema);

        //
        final int dataNum = 3;
        final String testContents = "header\n" + "0,Hello,World 0,No. 0, Beijing,99\n" + "1,Hello,World 1,No. 1, Beijing,98\n" + "2,Hello,World 2,No. 2, Beijing,97\n";
        doRunnerTest(runner, table, dataNum, testContents);
    }

    @Test
    public void testMapRecord_1vs1_filterInput() throws Exception {
        final String outSchemaName = "out1_filter_input";

        final MapperTable table = createOutputTable(outSchemaName);
        // filter
        table.setFilter("${input.main.id:mod(2):equals(1)}"); // filter even, left odd

        final String oldName = "birth";
        final String newName = "age";
        // expression
        MapperExpField ef = new MapperExpField();
        ef.setPath("/" + newName);
        ef.setExp("${literal(2018):minus(${" + PRE_MAIN_INPUT_EXP + "." + oldName + ":toDate('yyyy-MM-dd'):format('yyyy'):toNumber()})}");
        table.getExpressions().add(ef);

        // schema
        Schema schema = Schema.createRecord(outSchemaName, outSchemaName + " schema", mainStaffSchema.getNamespace(), false);
        List<Field> fields = mainStaffSchema.getFields().stream().filter(f -> !f.name().equals(oldName)).map(f -> new Field(f.name(), f.schema(), f.doc(), f.defaultVal()))
                .collect(Collectors.toList());

        // create new age
        Field agef = new Field(newName, Schema.create(Type.INT), "", -1);
        fields.add(agef);

        schema.setFields(fields);
        table.setSchema(schema);

        //
        final int dataNum = 6;
        final String testContents = "header\n" + "1,Hello,World 1,No. 1, Beijing,98\n" + "3,Hello,World 3,No. 3, Beijing,96\n" + "5,Hello,World 5,No. 5, Beijing,94\n";
        doRunnerTest(runner, table, dataNum, 3, testContents);
    }

    @Test
    public void testMapRecord_1vs1_filterOutput() throws Exception {
        final String outSchemaName = "out1_filter_output";

        final MapperTable table = createOutputTable(outSchemaName);
        // filter
        table.setFilter("${output." + outSchemaName + ".age:gt(95)}");// >95 years old

        doTestFilterOuput(table);
    }

    @Test
    public void testMapRecord_1vs1_filterOutput_defaultExp() throws Exception {

        final String outSchemaName = "out1_filter_output_defult_exp";

        final MapperTable table = createOutputTable(outSchemaName);
        // filter
        table.setFilter("${age:gt(95)}");// >95 years old

        doTestFilterOuput(table);

    }

    private void doTestFilterOuput(final MapperTable table) throws Exception {
        final String outSchemaName = table.getName();

        final String oldName = "birth";
        final String newName = "age";
        // expression
        MapperExpField ef = new MapperExpField();
        ef.setPath("/" + newName);
        ef.setExp("${literal(2018):minus(${" + PRE_MAIN_INPUT_EXP + "." + oldName + ":toDate('yyyy-MM-dd'):format('yyyy'):toNumber()})}");
        table.getExpressions().add(ef);

        // schema
        Schema schema = Schema.createRecord(outSchemaName, outSchemaName + " schema", mainStaffSchema.getNamespace(), false);
        List<Field> fields = mainStaffSchema.getFields().stream().filter(f -> !f.name().equals(oldName)).map(f -> new Field(f.name(), f.schema(), f.doc(), f.defaultVal()))
                .collect(Collectors.toList());

        // create new age
        Field agef = new Field(newName, Schema.create(Type.INT), "", -1);
        fields.add(agef);

        schema.setFields(fields);
        table.setSchema(schema);

        //
        final int dataNum = 20;
        final String testContents = "header\n" + "0,Hello,World 0,No. 0, Beijing,99\n" + "1,Hello,World 1,No. 1, Beijing,98\n" + "2,Hello,World 2,No. 2, Beijing,97\n"
                + "3,Hello,World 3,No. 3, Beijing,96\n";
        doRunnerTest(runner, table, dataNum, 4, testContents);
    }

    // @Test
    public void testMapRecord_1vs1_withVar() throws IOException {
        Assert.fail("Not impl yet!");
    }

    // @Test
    public void testMapRecord_1vsN_full() throws Exception {
        Assert.fail("Not impl yet!");
    }

    // @Test
    public void testMapRecord_1vsN_batch() throws Exception {
        Assert.fail("Not impl yet!");
    }

}
