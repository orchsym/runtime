/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.processors.saphana;

import static org.apache.nifi.processors.database.util.JdbcCommon.DEFAULT_PRECISION;
import static org.apache.nifi.processors.database.util.JdbcCommon.DEFAULT_SCALE;
import static org.apache.nifi.processors.database.util.JdbcCommon.NORMALIZE_NAMES_FOR_AVRO;
import static org.apache.nifi.processors.database.util.JdbcCommon.USE_AVRO_LOGICAL_TYPES;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.commons.lang.StringUtils;
import org.apache.nifi.annotation.behavior.EventDriven;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.InputRequirement.Requirement;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.dbcp.DBCPService;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.InputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.processors.database.util.DBUtil;
import org.apache.nifi.processors.saphana.enums.OperationEnum;

@EventDriven
@InputRequirement(Requirement.INPUT_REQUIRED)
@Tags({ "sql", "select", "jdbc", "put", "database", "sap", "hana" })
@CapabilityDescription("Incomming FlowFile need to be Avro format." + " All the data will be write into sap hana database with table")
@WritesAttributes({ 
    @WritesAttribute(attribute = "record-count", description = "Contains the number of rows returned in the select query") 
})
@SuppressWarnings("deprecation")
public class PutSAPHana extends AbstractProcessor {

    public static final String RECORD_COUNT = "record-count";

    // Relationships
    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("Successfully created FlowFile from SQL query result set.")
            .build();
    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("SQL query execution failed. Incoming FlowFile will be penalized and routed to this relationship")
            .build();
    private final Set<Relationship> relationships;

    public static final PropertyDescriptor DBCP_SERVICE = new PropertyDescriptor.Builder()
            .name("Database Connection Pooling Service")
            .description("The Controller Service that is used to obtain connection to database")
            .required(true)
            .identifiesControllerService(DBCPService.class)
            .build();

    public static final PropertyDescriptor OPERATION = new PropertyDescriptor.Builder()
            .name("Operation for table")
            .description("Operation for table,INSERT,UPDATE,UPSERT or DELETE.")
            .allowableValues("INSERT","UPDATE","UPSERT","DELETE")
            .defaultValue("INSERT")
            .required(true)
            .build();
    public static final PropertyDescriptor KEYCOLUMN = new PropertyDescriptor.Builder()
            .name("Key Column for unique")
            .description("KEY FOR UPDATE DATA")
            .required(false)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .expressionLanguageSupported(true)
            .build();

    public static final PropertyDescriptor BATCHSIZE = new PropertyDescriptor.Builder()
            .name("Batch Size")
            .description("BATCH SIZE FOR EXECUTE DATA")
            .required(true)
            .addValidator(StandardValidators.INTEGER_VALIDATOR)
            .expressionLanguageSupported(true)
            .build();

    public static final PropertyDescriptor TABLE_NAME = new PropertyDescriptor.Builder()
            .name("Table Name")
            .description("Table Name.")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .expressionLanguageSupported(true)
            .build();

    public static final PropertyDescriptor QUERY_TIMEOUT = new PropertyDescriptor.Builder()
            .name("Max Wait Time")
            .description("The maximum amount of time allowed for a running SQL select query " 
                    + " , zero means there is no limit. Max time less than 1 second will be equal to zero.")
            .defaultValue("0 seconds")
            .required(true)
            .addValidator(StandardValidators.TIME_PERIOD_VALIDATOR)
            .sensitive(false)
            .build();

    private final List<PropertyDescriptor> propDescriptors;

    public PutSAPHana() {
        final Set<Relationship> r = new HashSet<>();
        r.add(REL_SUCCESS);
        r.add(REL_FAILURE);
        relationships = Collections.unmodifiableSet(r);

        final List<PropertyDescriptor> pds = new ArrayList<>();
        pds.add(DBCP_SERVICE);
        pds.add(TABLE_NAME);
        pds.add(OPERATION);
        pds.add(KEYCOLUMN);
        pds.add(BATCHSIZE);
        pds.add(QUERY_TIMEOUT);
        pds.add(NORMALIZE_NAMES_FOR_AVRO);
        pds.add(USE_AVRO_LOGICAL_TYPES);
        pds.add(DEFAULT_PRECISION);
        pds.add(DEFAULT_SCALE);
        propDescriptors = Collections.unmodifiableList(pds);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return relationships;
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return propDescriptors;
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        FlowFile flowFile = session.get();
        if (flowFile == null) {
            return;
        }

        final ComponentLog logger = getLogger();

        final String outputTable = context.getProperty(TABLE_NAME).evaluateAttributeExpressions(flowFile).getValue();
        final String operation = context.getProperty(OPERATION).evaluateAttributeExpressions(flowFile).getValue();
        final DBCPService dbcpService = context.getProperty(DBCP_SERVICE).asControllerService(DBCPService.class);
        
        StringBuffer preparedSQL = new StringBuffer("");
        final String keyColumnPV = context.getProperty(KEYCOLUMN).evaluateAttributeExpressions(flowFile).getValue();
        try (final Connection con = dbcpService.getConnection();) {
            con.setAutoCommit(false);
            Map<String, String> map = new HashMap<String, String>();
            final Integer batchSize = context.getProperty(BATCHSIZE).evaluateAttributeExpressions(flowFile).asInteger();
            session.read(flowFile, new InputStreamCallback() {
                @Override
                public void process(final InputStream rawIn) throws IOException {
                    String keyColumn = keyColumnPV;
                    if(keyColumnPV!=null){
                        keyColumn = keyColumnPV.toUpperCase();
                    }else{
                        keyColumn = "";
                    }
                    try (final InputStream in = new BufferedInputStream(rawIn); 
                            final DataFileStream<GenericRecord> reader = new DataFileStream<>(in, new GenericDatumReader<GenericRecord>())) {
                        List<Field> list = reader.getSchema().getFields();
                        List<String> columns = new ArrayList<String>();
                        List<String> columns_withoutKey = new ArrayList<String>();
                        List<String> values = new ArrayList<String>();
                        String columnName = "";
                        Object value = null;
                        for (int i = 0; i < list.size(); i++) {
                            columnName = list.get(i).name();
                            columns.add(columnName.toUpperCase());
                            if(OperationEnum.UPDATE.toString().equals(operation)){
                                if (!keyColumn.equalsIgnoreCase(columnName)) {
                                    columns_withoutKey.add(columnName.toUpperCase());
                                }
                            }
                            values.add("?");
                        }
                        if ((keyColumn.length()==0 || !columns.contains(keyColumn)) && (OperationEnum.UPDATE.toString().equals(operation) || OperationEnum.UPSERT.toString().equals(operation) || OperationEnum.DELETE.toString().equals(operation))) {
                            throw new RuntimeException("Input schema must include key column named "+keyColumn+".Please check input schema!");
                        }
                        if(OperationEnum.INSERT.toString().equals(operation)){
                            preparedSQL.append("insert into " + outputTable + " ");
                            preparedSQL.append("(" + StringUtils.join(columns, ",") + "  ) values(" + StringUtils.join(values, ",") + ")");
                        }else if(OperationEnum.UPDATE.toString().equals(operation)){
                            preparedSQL.append("update " + outputTable + " set ");
                            preparedSQL.append( StringUtils.join(columns_withoutKey, "=?,")).append("=?");
                            preparedSQL.append("  where " + keyColumn + "=?");
                        }else if(OperationEnum.UPSERT.toString().equals(operation)){
                            preparedSQL.append("upsert " + outputTable + " ");
                            preparedSQL.append("(" + StringUtils.join(columns, ",") + "  ) values(" + StringUtils.join(values, ",") + ") where " + keyColumn + "=?");
                        }else if(OperationEnum.DELETE.toString().equals(operation)){
                            preparedSQL.append("DELETE FROM " + outputTable);
                            preparedSQL.append(" where " + keyColumn + "=?");
                        }
                        final PreparedStatement ps = con.prepareStatement(preparedSQL.toString());
                        GenericRecord currRecord = null;
                        int batchIndex = 0;
                        int recordCount = 0;
                        while (reader.hasNext()) {
                            currRecord = reader.next(currRecord);
                            int paramIndex = 1;
                            for (int i = 0; i < list.size(); i++) {
                                Field field = list.get(i);
                                Schema schema = field.schema();
                                Type fieldType = schema.getType();
                                if (Type.UNION.equals(fieldType)) {
                                    fieldType = schema.getTypes().get(1).getType();
                                }
                                value = currRecord.get(i);
                                
                                if(OperationEnum.INSERT.toString().equals(operation)){
                                    DBUtil.setValueForParam(ps, paramIndex++, fieldType, value);
                                }else if(OperationEnum.UPDATE.toString().equals(operation)){
                                    if (keyColumn.equalsIgnoreCase(field.name())) {
                                        DBUtil.setValueForParam(ps, list.size(), fieldType, value);
                                    }else{
                                        DBUtil.setValueForParam(ps, paramIndex++, fieldType, value);
                                    }
                                }else if(OperationEnum.UPSERT.toString().equals(operation)){
                                    DBUtil.setValueForParam(ps, paramIndex++, fieldType, value);
                                    if (keyColumn.equalsIgnoreCase(field.name())) {
                                        DBUtil.setValueForParam(ps, list.size()+1, fieldType, value);
                                    }
                                }else if(OperationEnum.DELETE.toString().equals(operation)){
                                    if (keyColumn.equalsIgnoreCase(field.name())) {
                                        DBUtil.setValueForParam(ps, 1, fieldType, value);
                                        break;
                                    }
                                }
                            }
                            ps.addBatch();
                            batchIndex++;
                            recordCount++;
                            if (batchIndex >= batchSize) {
                                ps.executeBatch();
                                con.commit();
                                logger.debug("has conmmit "+recordCount+" rows");
                                batchIndex = 0;
                            }
                        }
                        if (batchIndex > 0) {
                            ps.executeBatch();
                            con.commit();
                            logger.debug("has conmmit "+recordCount+" rows");
                        }
                        map.put(RECORD_COUNT, String.valueOf(recordCount));
                    } catch (Exception e) {
                        map.put(RECORD_COUNT, "-1");
                        throw new ProcessException(e);
                    }
                }

            });
            session.putAllAttributes(flowFile, map);
        } catch (final ProcessException | SQLException e) {
            if (context.hasIncomingConnection()) {
                logger.error("Unable to update table {} for {} due to {}; routing to failure", new Object[] { outputTable, flowFile, e });
                flowFile = session.penalize(flowFile);
            } else {
                logger.error("Unable to update table {} due to {}; routing to failure", new Object[] { outputTable, e });
                context.yield();
            }
            session.transfer(flowFile, REL_FAILURE);
        }
        session.transfer(flowFile, REL_SUCCESS);
    }

}
