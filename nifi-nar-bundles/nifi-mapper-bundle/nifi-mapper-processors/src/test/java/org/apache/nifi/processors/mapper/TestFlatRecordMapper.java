package org.apache.nifi.processors.mapper;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.commons.io.FileUtils;
import org.apache.nifi.avro.AvroTypeUtil;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.processors.mapper.exp.ExpVar;
import org.apache.nifi.processors.mapper.exp.ExpVarTable;
import org.apache.nifi.processors.mapper.exp.MapperExpField;
import org.apache.nifi.processors.mapper.exp.MapperTable;
import org.apache.nifi.processors.mapper.exp.MapperTableType;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.serialization.record.MockRecordParser;
import org.apache.nifi.serialization.record.MockRecordWriter;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 
 * @author GU Guoqiang
 * 
 */
public class TestFlatRecordMapper {
    private static final int TEST_NUM = 100;
    final static String PRE_INPUT_MAIN_VAR = MapperTableType.INPUT.getPrefix(RecordMapper.DEFAULT_MAIN);

    private static Schema mainStaffSchema;

    private TestRunner runner;
    private MockRecordParser readerService;
    private MockRecordWriter writerService;

    @BeforeAll
    public static void init() throws IOException {
        mainStaffSchema = new Schema.Parser().parse(new File("src/test/resources/staff.avsc"));
    }

    @AfterAll
    public static void cleanup() {
        mainStaffSchema = null;
    }

    @BeforeEach
    public void setup() throws IOException, InitializationException {

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

    @AfterEach
    public void clean() throws IOException {
        runner = null;
        readerService = null;
        writerService = null;
    }

    private void createMainStaffRecords(final int numOfStaff, final MockRecordParser readerService) throws IOException {
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

    private Schema copySchema(String outSchemaName) {
        Schema schema = Schema.createRecord(outSchemaName, outSchemaName + " schema", mainStaffSchema.getNamespace(), false);
        List<Field> fields = mainStaffSchema.getFields().stream().map(f -> new Field(f.name(), f.schema(), f.doc(), f.defaultVal())).collect(Collectors.toList());
        schema.setFields(fields);
        return schema;
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

    @Test
    public void testMapRecord_1vs1_full() throws Exception {
        final String outSchemaName = "out1_full";

        final MapperTable table = createOutputTable(outSchemaName);
        table.setSchema(copySchema(outSchemaName));

        doRunnerTest(runner, table, TEST_NUM, null);
    }

    @Test
    public void testMapRecord_1vs1_fullBatch() throws Exception {
        final String outSchemaName = "out1_full_batch";

        final MapperTable table = createOutputTable(outSchemaName);
        table.setSchema(copySchema(outSchemaName));

        final int dataNum = 44;
        final String testContents = FileUtils.readFileToString(new File("src/test/resources/expected-output-content"), "UTF-8");
        doRunnerTest(runner, table, dataNum, testContents);

    }

    @Test
    public void testMapRecord_1vs1_assertContents() throws Exception {
        final String outSchemaName = "out1_contents";

        final MapperTable table = createOutputTable(outSchemaName);
        table.setSchema(copySchema(outSchemaName));

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
            ef.setExp("${" + PRE_INPUT_MAIN_VAR + '.' + f.name() + "}");
            return ef;
        }).collect(Collectors.toList());
        table.getExpressions().addAll(expFields);

        // schema
        table.setSchema(copySchema(outSchemaName));

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
        ef.setExp("${" + PRE_INPUT_MAIN_VAR + '.' + oldName + "}");
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
        ef.setExp("${" + PRE_INPUT_MAIN_VAR + ".first_name:append('-'):append(${" + PRE_INPUT_MAIN_VAR + ".last_name})}");
        table.getExpressions().add(ef);

        Schema schema = AvroCreator.createSchema(outSchemaName, AvroCreator.createField("id", Type.INT), AvroCreator.createField("full_name", Type.STRING));
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
        ef.setExp("${literal(2018):minus(${" + PRE_INPUT_MAIN_VAR + '.' + oldName + ":toDate('yyyy-MM-dd'):format('yyyy'):toNumber()})}");
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
        ef.setExp("${literal(2018):minus(${" + PRE_INPUT_MAIN_VAR + '.' + oldName + ":toDate('yyyy-MM-dd'):format('yyyy'):toNumber()})}");
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
        ef.setExp("${literal(2018):minus(${" + PRE_INPUT_MAIN_VAR + '.' + oldName + ":toDate('yyyy-MM-dd'):format('yyyy'):toNumber()})}");
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

    @Test
    public void testMapRecord_UpdateDynamicOutputTables() throws Exception {
        final String out1 = "out_test1";
        final MapperTable table1 = createOutputTable(out1);
        table1.setSchema(copySchema(out1));

        PropertyDescriptor outputTableSetting1 = new PropertyDescriptor.Builder().name(RecordMapper.PRE_OUTPUT + out1).description(out1 + " relationship")
                .addValidator(StandardValidators.NON_EMPTY_VALIDATOR).expressionLanguageSupported(ExpressionLanguageScope.NONE).dynamic(true).build();
        runner.setProperty(outputTableSetting1, new MapperTable.Writer().write(table1));

        assertEquals(3, runner.getProcessContext().getAvailableRelationships().size());
        assertEquals(RecordMapper.PRE_OUTPUT + out1, runner.getProcessContext().getAvailableRelationships().iterator().next().getName());

        // add another one
        final String out2 = "out_test2";
        final MapperTable table2 = createOutputTable(out2);
        table2.setSchema(copySchema(out2));

        PropertyDescriptor outputTableSetting2 = new PropertyDescriptor.Builder().name(RecordMapper.PRE_OUTPUT + out2).description(out2 + " relationship")
                .addValidator(StandardValidators.NON_EMPTY_VALIDATOR).expressionLanguageSupported(ExpressionLanguageScope.NONE).dynamic(true).build();
        runner.setProperty(outputTableSetting2, new MapperTable.Writer().write(table2));

        assertThat(runner.getProcessContext().getAvailableRelationships(), hasSize(4));
        Set<String> newRelationships = runner.getProcessContext().getAvailableRelationships().stream().map(r -> r.getName()).collect(Collectors.toSet());
        assertThat(newRelationships, hasItem(RecordMapper.PRE_OUTPUT + out1));
        assertThat(newRelationships, hasItem(RecordMapper.PRE_OUTPUT + out2));

        // remove 1
        runner.removeProperty(outputTableSetting1);
        // runner.setProperty(outputTableSetting1, (String) null);

        assertThat(runner.getProcessContext().getAvailableRelationships(), hasSize(3));
        Set<String> removedRelationships = runner.getProcessContext().getAvailableRelationships().stream().map(r -> r.getName()).collect(Collectors.toSet());
        assertThat(removedRelationships, not(hasItem(RecordMapper.PRE_OUTPUT + out1)));
        assertThat(removedRelationships, hasItem(RecordMapper.PRE_OUTPUT + out2));

        // rename?
        runner.removeProperty(outputTableSetting2);
        // runner.setProperty(outputTableSetting2, (String) null);

        final String out4 = "out_test4";
        final MapperTable table4 = createOutputTable(out4);
        table4.setSchema(copySchema(out4));

        PropertyDescriptor outputTableSetting4 = new PropertyDescriptor.Builder().name(RecordMapper.PRE_OUTPUT + out4).description(out4 + " relationship")
                .addValidator(StandardValidators.NON_EMPTY_VALIDATOR).expressionLanguageSupported(ExpressionLanguageScope.NONE).dynamic(true).build();
        runner.setProperty(outputTableSetting4, new MapperTable.Writer().write(table4));

        assertThat(runner.getProcessContext().getAvailableRelationships(), hasSize(3));
        Set<String> renameRelationships = runner.getProcessContext().getAvailableRelationships().stream().map(r -> r.getName()).collect(Collectors.toSet());
        assertThat(renameRelationships, not(hasItem(RecordMapper.PRE_OUTPUT + out1)));
        assertThat(renameRelationships, not(hasItem(RecordMapper.PRE_OUTPUT + out2)));
        assertThat(renameRelationships, hasItem(RecordMapper.PRE_OUTPUT + out4));
    }

    @Test
    public void testMapRecord_1vsN_full() throws Exception {
        // 1
        final String outSchemaName1 = "out_outputN1";
        final MapperTable table1 = createOutputTable(outSchemaName1);

        final List<MapperExpField> expFields = mainStaffSchema.getFields().stream().map(f -> {
            MapperExpField ef = new MapperExpField();
            ef.setPath('/' + f.name());
            ef.setExp("${" + PRE_INPUT_MAIN_VAR + '.' + f.name() + "}");
            return ef;
        }).collect(Collectors.toList());
        table1.getExpressions().addAll(expFields);
        table1.setSchema(copySchema(outSchemaName1));

        String outputFlowName1 = RecordMapper.PRE_OUTPUT + outSchemaName1;
        PropertyDescriptor outputTableSetting1 = new PropertyDescriptor.Builder().name(outputFlowName1).description(outSchemaName1 + " relationship")
                .addValidator(StandardValidators.NON_EMPTY_VALIDATOR).expressionLanguageSupported(ExpressionLanguageScope.NONE).dynamic(true).build();
        runner.setProperty(outputTableSetting1, new MapperTable.Writer().write(table1));

        // 2
        final String outSchemaName2 = "out_outputN2";
        final MapperTable table2 = createOutputTable(outSchemaName2);
        table2.setSchema(copySchema(outSchemaName2));

        String outputFlowName2 = RecordMapper.PRE_OUTPUT + outSchemaName2;
        PropertyDescriptor outputTableSetting2 = new PropertyDescriptor.Builder().name(outputFlowName2).description(outSchemaName2 + " relationship")
                .addValidator(StandardValidators.NON_EMPTY_VALIDATOR).expressionLanguageSupported(ExpressionLanguageScope.NONE).dynamic(true).build();
        runner.setProperty(outputTableSetting2, new MapperTable.Writer().write(table2));

        //

        final int dataCount = 44;
        createMainStaffRecords(dataCount, readerService);

        runner.enqueue("");
        runner.run();

        runner.assertTransferCount(RecordMapper.REL_ORIGINAL, 1);
        runner.assertTransferCount(RecordMapper.REL_FAILURE, 0);

        final String testContents = FileUtils.readFileToString(new File("src/test/resources/expected-output-content"), StandardCharsets.UTF_8);
        //
        runner.assertTransferCount(outputFlowName1, 1);
        final List<MockFlowFile> flowfiles1 = runner.getFlowFilesForRelationship(outputFlowName1);
        assertEquals(flowfiles1.size(), 1);

        final MockFlowFile outFlow1 = flowfiles1.get(0);
        outFlow1.assertAttributeEquals("map.count", String.valueOf(dataCount));
        outFlow1.assertContentEquals(testContents);

        //
        runner.assertTransferCount(outputFlowName2, 1);
        final List<MockFlowFile> flowfiles2 = runner.getFlowFilesForRelationship(outputFlowName2);
        assertEquals(flowfiles2.size(), 1);

        final MockFlowFile outFlow2 = flowfiles2.get(0);
        outFlow2.assertAttributeEquals("map.count", String.valueOf(dataCount));
        outFlow2.assertContentEquals(testContents);

    }

    @Test
    public void testVar_1vs1_globalVar() throws Exception {
        // global var
        ExpVarTable globalVarTable = new ExpVarTable("global", new ExpVar("yearOfBirth", "${" + PRE_INPUT_MAIN_VAR + ".birth" + ":toDate('yyyy-MM-dd'):format('yyyy')}"),
                new ExpVar("g_age", "${literal(2018):minus(${yearOfBirth:toNumber()})}"));
        runner.setProperty(RecordMapper.RECORD_GLOBAL_VARS, new ExpVarTable.Writer().write(globalVarTable));

        final String outSchemaName = "out_global_var";

        final MapperTable table = createOutputTable(outSchemaName);

        // output var
        table.getVars().add(new ExpVar("out_id", "/id")); // out id

        // filter with var
        table.setFilter("${out_id:mod(2):equals(1)}");

        // expression
        MapperExpField ef = new MapperExpField();
        ef.setPath("/age");
        ef.setExp("${global._var_.g_age}");
        table.getExpressions().add(ef);

        // schema
        Schema schema = Schema.createRecord(outSchemaName, outSchemaName + " schema", mainStaffSchema.getNamespace(), false);
        List<Field> fields = mainStaffSchema.getFields().stream().filter(f -> !f.name().equals("birth")).map(f -> new Field(f.name(), f.schema(), f.doc(), f.defaultVal()))
                .collect(Collectors.toList());

        // create new age
        Field agef = new Field("age", Schema.create(Type.INT), "", -1);
        fields.add(agef);

        schema.setFields(fields);
        table.setSchema(schema);

        //
        final int dataNum = 6;
        final String testContents = "header\n" + "1,Hello,World 1,No. 1, Beijing,98\n" + "3,Hello,World 3,No. 3, Beijing,96\n" + "5,Hello,World 5,No. 5, Beijing,94\n";
        doRunnerTest(runner, table, dataNum, 3, testContents);
    }

    @ParameterizedTest
    @ValueSource(strings = { "${in_age}", "${output.out_inner_var._var_.in_age}" })
    public void testVar_1vs1_innerVar(String ageVar) throws Exception {
        final String outSchemaName = "out_inner_var";

        final MapperTable table = createOutputTable(outSchemaName);

        // output var
        table.getVars().add(new ExpVar("out_id", "/id")); // out id
        table.getVars().add(new ExpVar("yearOfBirth", "${" + PRE_INPUT_MAIN_VAR + ".birth" + ":toDate('yyyy-MM-dd'):format('yyyy')}"));
        table.getVars().add(new ExpVar("in_age", "${literal(2018):minus(${yearOfBirth:toNumber()})}")); // use inner var

        // filter with var
        table.setFilter("${out_id:mod(2):equals(1)}");

        // expression
        MapperExpField ef = new MapperExpField();
        ef.setPath("/age");
        ef.setExp(ageVar);
        table.getExpressions().add(ef);

        // schema
        Schema schema = Schema.createRecord(outSchemaName, outSchemaName + " schema", mainStaffSchema.getNamespace(), false);
        List<Field> fields = mainStaffSchema.getFields().stream().filter(f -> !f.name().equals("birth")).map(f -> new Field(f.name(), f.schema(), f.doc(), f.defaultVal()))
                .collect(Collectors.toList());

        // create new age
        Field agef = new Field("age", Schema.create(Type.INT), "", -1);
        fields.add(agef);

        schema.setFields(fields);
        table.setSchema(schema);

        //
        final int dataNum = 6;
        final String testContents = "header\n" + "1,Hello,World 1,No. 1, Beijing,98\n" + "3,Hello,World 3,No. 3, Beijing,96\n" + "5,Hello,World 5,No. 5, Beijing,94\n";
        doRunnerTest(runner, table, dataNum, 3, testContents);
    }

    @ParameterizedTest
    @ValueSource(strings = { "id_mod", "output.out_inner_rp_var._var_.id_mod" })
    public void testVar_1vs1_innerRecordPathVar(String idVar) throws Exception {
        final String outSchemaName = "out_inner_rp_var";

        final MapperTable table = createOutputTable(outSchemaName);

        // output var
        table.getVars().add(new ExpVar("out_id", "/id")); // make sure will check for filter
        table.getVars().add(new ExpVar("id_mod", "${out_id:mod(2)}"));

        // filter with var
        table.setFilter("${" + idVar + ":equals(1)}");

        // schema
        Schema schema = Schema.createRecord(outSchemaName, outSchemaName + " schema", mainStaffSchema.getNamespace(), false);
        List<Field> fields = mainStaffSchema.getFields().stream().map(f -> new Field(f.name(), f.schema(), f.doc(), f.defaultVal())).collect(Collectors.toList());
        schema.setFields(fields);
        table.setSchema(schema);

        //
        final int dataNum = 6;
        final String testContents = "header\n" + "1,Hello,World 1,1920-01-01,No. 1, Beijing\n" + "3,Hello,World 3,1922-01-01,No. 3, Beijing\n" + "5,Hello,World 5,1924-01-01,No. 5, Beijing\n";
        doRunnerTest(runner, table, dataNum, 3, testContents);
    }

}
