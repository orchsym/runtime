/*
 * Licensed to the Orchsym Runtime under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 * 
 * this file to You under the Orchsym License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * https://github.com/orchsym/runtime/blob/master/orchsym/LICENSE
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baishancloud.orchsym.processors;

import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Marks;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.StreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.util.StopWatch;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@InputRequirement(InputRequirement.Requirement.INPUT_REQUIRED)
@Marks(categories = { "Convert & Control/Convert" }, createdDate = "2019-08-08")
@Tags({"JSON", "Avro", "CSV", "Filter"})
@CapabilityDescription("Filter Avro RecordSet or JSON array or Text line by setting.")
public class FilterRecord extends AbstractProcessor {
    // ---------------------------------------- 属性参数 --------------------------------------------------
    public static final PropertyDescriptor CONTENT_TYPE = new PropertyDescriptor.Builder()
            .name("content-type")
            .displayName("Input Content Type")
            .description("Content Type of data present for the incoming FlowFile's,Only \"JSON\" or \"Avro\" are supported,The default is \"JSON\".")
            .allowableValues(Constant.CONTENT_TYPE_JSON, Constant.CONTENT_TYPE_AVRO , Constant.CONTENT_TYPE_TXT)
            .required(true)
            .defaultValue(Constant.CONTENT_TYPE_JSON)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor LOGICAL_TYPE = new PropertyDescriptor.Builder()
            .name("logical-type")
            .displayName("Logical Condition")
            .description("Filtered logic conditions, only support \"AND\", \"OR\" or \"NOT\", default is \"AND\",For \"OR/AND\", multiple conditions can be supported. \"NOT\" only supports one. When the logical condition is \"NOT\", only the first condition in the conditional JSON array takes effect..")
            .allowableValues(Constant.LOGICAL_TYPE_AND, Constant.LOGICAL_TYPE_OR , Constant.LOGICAL_TYPE_NOT)
            .required(true)
            .defaultValue(Constant.LOGICAL_TYPE_AND)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor SETTINGS = new PropertyDescriptor.Builder()
            .name("settings")
            .displayName("Settings")
            .description("Filtered query conditions, set in JSON array format, the following fields must exist in the JSON array: \"field\", \"function\", \"opertator\", \"value\".")
            .addValidator(CustomValidators.JSON_SCHEMA_VALIDATOR)
            .build();

    // ----------------------------------------- 输出流对象 -------------------------------------------------------

    public static final Relationship REL_FILTER = new Relationship.Builder()
            .name("Filter") //
            .description("Content output that satisfies the condition.")
            .build();

    public static final Relationship REL_REJECT = new Relationship.Builder()
            .name("Reject") //
            .description("Content output that does not meet the criteria.")
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("Failure") //
            .description("The query fails to route to this connection.")
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    // --------------------------------------- 初始化 --------------------------------------------------------------
    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<>();
        descriptors.add(CONTENT_TYPE);
        descriptors.add(LOGICAL_TYPE);
        descriptors.add(SETTINGS);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<>();
        relationships.add(REL_FILTER);
        relationships.add(REL_REJECT);
        relationships.add(REL_FAILURE);
        this.relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        final StopWatch stopWatch = new StopWatch(true);
        FlowFile flowFile = session.get();
        if (flowFile == null) {
            return;
        }{
            //获取输入值
            String dataType = context.getProperty(CONTENT_TYPE).getValue();
            String logical = context.getProperty(LOGICAL_TYPE).getValue();
            String settings = context.getProperty(SETTINGS).getValue();
            if (settings.isEmpty()) {
                session.transfer(flowFile, REL_FILTER);
                return;
            } else {
                FlowFile rejectFlowFile = session.clone(flowFile);
                try {
                    flowFile = session.write(flowFile, (StreamCallback) (in, out) -> {//满足查询条件的结果
                        try (final InputStream inRow = new BufferedInputStream(in);
                             final OutputStream outRow = new BufferedOutputStream((out))) {
                            String sql = SQLUtils.generateInSQL(settings, logical);
                            outRow.write(Utils.filterRecord(inRow, dataType, sql));
                        } catch (final SQLException | ClassNotFoundException e) {
                            throw new ProcessException(e);
                        }
                    });
                    rejectFlowFile = session.write(rejectFlowFile, (StreamCallback) (in, out) -> {//不满足查询条件的结果
                        try (final InputStream inRow = new BufferedInputStream(in);
                             final OutputStream outRow = new BufferedOutputStream((out))) {
                            String noSql = SQLUtils.generateNotInSQL(settings, logical);
                            outRow.write(Utils.filterRecord(inRow, dataType, noSql));
                        } catch (final SQLException | ClassNotFoundException e) {
                            throw new ProcessException(e);
                        }
                    });

                } catch(ProcessException e){
                    session.transfer(flowFile, REL_FAILURE);
                    return;
                }
                if (flowFile.getSize() > 0) {
                    session.transfer(flowFile, REL_FILTER);
                    session.getProvenanceReporter().modifyContent(flowFile, stopWatch.getElapsed(TimeUnit.MILLISECONDS));
                } else {
                    session.remove(flowFile);
                }
                if (rejectFlowFile.getSize() > 0) {
                    session.transfer(rejectFlowFile, REL_REJECT);
                    session.getProvenanceReporter().modifyContent(rejectFlowFile, stopWatch.getElapsed(TimeUnit.MILLISECONDS));
                } else {
                    session.remove(rejectFlowFile);
                }
            }
        }
    }
}