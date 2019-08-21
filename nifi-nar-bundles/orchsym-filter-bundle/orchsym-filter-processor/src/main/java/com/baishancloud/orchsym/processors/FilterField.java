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
package com.baishancloud.orchsym.processors;

import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Marks;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.Validator;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Marks(categories = { "Convert & Control/Convert" }, createdDate = "2019-07-17")
@Tags({"JSON", "Filter Field", "Avro"})
@CapabilityDescription("Filter the specified fields in JSON or Avro.")
@WritesAttributes({
        @WritesAttribute(attribute="", description="")
})

public class FilterField extends AbstractProcessor {
    // ---------------------------------------- 属性参数 --------------------------------------------------
    public static final PropertyDescriptor CONTENT_TYPE = new PropertyDescriptor.Builder()
            .name("content-type")
            .displayName("Input Content Type")
            .description("Content Type of data present in the incoming FlowFile's,Only \"JSON\" or \"Avro\" are supported,The default is \"JSON\".")
            .allowableValues(Constant.CONTENT_TYPE_JSON, Constant.CONTENT_TYPE_AVRO)
            .required(true)
            .defaultValue(Constant.CONTENT_TYPE_JSON)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor INCLUDE = new PropertyDescriptor.Builder()
            .name("include")
            .displayName("Include")
            .description("Fields to be reserved in the FlowFile, multiple fields separated by commas.")
            .addValidator(Validator.VALID)
            .build();

    public static final PropertyDescriptor EXCLUDE = new PropertyDescriptor.Builder()
            .name("exclude")
            .displayName("Exclude")
            .description("Fields to be deleted in the FlowFile, multiple fields are separated by commas; if \"Include\" and \"Exclude\" are set at the same time, then \"Include\" as the reference, and then delete according to \"Exclude\" in the result; if neither is set, the original FlowFile is output by default.")
            .addValidator(Validator.VALID)
            .build();

    // ----------------------------------------- 输出流对象 -------------------------------------------------------

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("Success") //
            .description("Successfully filter fields from FlowFile.")
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("Failure") //
            .description("Failed to filter fields from FlowFile.")
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    // --------------------------------------- 初始化 --------------------------------------------------------------
    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<>();
        descriptors.add(CONTENT_TYPE);
        descriptors.add(INCLUDE);
        descriptors.add(EXCLUDE);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<>();
        relationships.add(REL_FAILURE);
        relationships.add(REL_SUCCESS);
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

//    @OnScheduled
//    public void onScheduled(final ProcessContext context) {
//
//    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        final StopWatch stopWatch = new StopWatch(true);
        FlowFile flowFile = session.get();
        if(flowFile == null) {
            flowFile = session.create();
        }
        //获取输入值
        String dataType = context.getProperty(CONTENT_TYPE).getValue();
        String include = context.getProperty(INCLUDE).getValue();
        String exclude = context.getProperty(EXCLUDE).getValue();

        List<String> includeList = include == null || include.trim().length() == 0 ? Utils.parseCommaDelimitedStr("") : Utils.parseCommaDelimitedStr(include);
        List<String> excludeList = exclude == null || exclude.trim().length() == 0 ? Utils.parseCommaDelimitedStr("") : Utils.parseCommaDelimitedStr(exclude);
        try {
            if (includeList.isEmpty() && excludeList.isEmpty()){
                session.transfer(flowFile,REL_SUCCESS);
            }else {
                flowFile = session.write(flowFile,(StreamCallback)(in,out) ->{//读取输入作为输出
                    try (final InputStream inRow = new BufferedInputStream(in);
                         final OutputStream outRow = new BufferedOutputStream((out))){
                        if (dataType.contains(Constant.CONTENT_TYPE_JSON)){
                            outRow.write(Utils.jsonIncludeAndExclude(inRow,includeList,excludeList).getBytes());
                        }else{
                            outRow.write(Utils.avroIncludeAndExclude(inRow,includeList,excludeList));
                        }
                    }
                });
            }
        }catch (ProcessException e) {
            session.transfer(flowFile, REL_FAILURE);
            return;
        }
        session.transfer(flowFile,REL_SUCCESS);
        //数据溯源
        session.getProvenanceReporter().modifyContent(flowFile, stopWatch.getElapsed(TimeUnit.MILLISECONDS));
    }
}