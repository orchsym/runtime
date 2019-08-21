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
package org.apache.nifi.processors.standard;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map.Entry;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import java.util.concurrent.TimeUnit;
import java.nio.charset.Charset;
import java.net.URLEncoder;
import java.net.URLDecoder;

import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.InputRequirement.Requirement;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.documentation.Marks;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.StreamCallback;
import org.apache.nifi.processor.io.InputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.util.StopWatch;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

@Marks(categories={"Data Process/Fetch", "Convert & Control/Convert"}, createdDate="2018-09-02")
@Tags({"encode", "urlencode"})
@CapabilityDescription("Encodes or decodes json content to and from urlencode, the content must be json")
@InputRequirement(Requirement.INPUT_REQUIRED)
public class URLEncodeContent extends AbstractProcessor {

    public static final String ENCODE_MODE = "Encode";
    public static final String DECODE_MODE = "Decode";

    public static final String ORDER_ASCENDING = "Ascending";
    public static final String ORDER_DESCENDING = "Descending";    

    public static final String DECODE_DESTINATION_ATTRIBUTE = "flowfile-attribute";
    public static final String DECODE_DESTINATION_CONTENT = "flowfile-content";

    public static final PropertyDescriptor CHARSET = new PropertyDescriptor.Builder()
        .name("Character Set")
        .description("Specifies the character set of the received data.")
        .required(true)
        .defaultValue("UTF-8")
        .addValidator(StandardValidators.CHARACTER_SET_VALIDATOR)
        .build();

    public static final PropertyDescriptor MODE = new PropertyDescriptor.Builder()
        .name("Mode")
        .description("Specifies whether the content should be encoded or decoded")
        .required(true)
        .allowableValues(ENCODE_MODE, DECODE_MODE)
        .defaultValue(ENCODE_MODE)
        .build();
    public static final PropertyDescriptor ORDER = new PropertyDescriptor.Builder()
        .name("Encode Key Order")
        .description("Specifies the key order of the encoded content")
        .required(false)
        .allowableValues(ORDER_ASCENDING, ORDER_DESCENDING)
        .build();

    public static final PropertyDescriptor DECODE_DESTINATION = new PropertyDescriptor.Builder()
        .name("Decode Destination")
        .description("Specifies the decoded content to is placed flowfile's attribute or content, default is placed to content")
        .required(false)
        .allowableValues(DECODE_DESTINATION_CONTENT, DECODE_DESTINATION_ATTRIBUTE)
        .defaultValue(DECODE_DESTINATION_CONTENT)
        .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
        .name("success")
        .description("Any FlowFile that is successfully encoded or decoded will be routed to success")
        .build();
    public static final Relationship REL_FAILURE = new Relationship.Builder()
        .name("failure")
        .description("Any FlowFile that cannot be encoded or decoded will be routed to failure")
        .build();

    private List<PropertyDescriptor> properties;
    private Set<Relationship> relationships;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> properties = new ArrayList<>();
        properties.add(CHARSET);
        properties.add(MODE);
        properties.add(ORDER);
        properties.add(DECODE_DESTINATION);
        this.properties = Collections.unmodifiableList(properties);

        final Set<Relationship> relationships = new HashSet<>();
        relationships.add(REL_SUCCESS);
        relationships.add(REL_FAILURE);
        this.relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return relationships;
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return properties;
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) {
        FlowFile flowFile = session.get();
        if (flowFile == null) {
            return;
        }

        final ComponentLog logger = getLogger();
        boolean encode = context.getProperty(MODE).getValue().equalsIgnoreCase(ENCODE_MODE);
        final Charset charset = Charset.forName(context.getProperty(CHARSET).getValue());
        try {
            final StopWatch stopWatch = new StopWatch(true);
            if (encode) {
                flowFile = encodeFlowfile(context, session, flowFile, charset);
            } else {
                flowFile = decodeFlowfile(context, session, flowFile, charset);
            }
            logger.info("Successfully {} {}", new Object[] {encode ? "encoded" : "decoded", flowFile});
            session.getProvenanceReporter().modifyContent(flowFile, stopWatch.getElapsed(TimeUnit.MILLISECONDS));
            session.transfer(flowFile, REL_SUCCESS);
        } catch (ProcessException e) {
            logger.error("Failed to {} {} due to {}", new Object[] {encode ? "encode" : "decode", flowFile, e});
            session.transfer(flowFile, REL_FAILURE);
        }
    }

    private FlowFile encodeFlowfile(final ProcessContext context, final ProcessSession session, FlowFile flowFile, Charset charset) throws ProcessException {
        session.write(flowFile, new StreamCallback() {
            @Override
            public void process(InputStream in, OutputStream out) throws IOException {

                StringWriter writer = new StringWriter();
                IOUtils.copy(in, writer, charset);
                String content = writer.toString();
                try {
                    JsonObject infoObject = new JsonParser().parse(content).getAsJsonObject();
                    String result = "";
                    Map<String, String> map = null;
                    Map<String, String> objectMap = new HashMap();
                    for (Map.Entry<String, JsonElement> entry: infoObject.entrySet()) {
                        objectMap.put(entry.getKey(), entry.getValue().getAsString());
                    }
                    String order = context.getProperty(ORDER).getValue();
                    if (StringUtils.isEmpty(order)) {
                        map = objectMap;
                    } else {
                        map = order.equalsIgnoreCase(ORDER_ASCENDING) ? new TreeMap() : new TreeMap(Collections.reverseOrder());
                        map.putAll(objectMap);
                    }
                    
                    Iterator<Map.Entry<String, String>> iterator = map.entrySet().iterator();
                    int index = 0;
                    while (iterator.hasNext()) {
                        index++;
                        Entry<String, String> entry = iterator.next();
                        result += URLEncoder.encode(entry.getKey(), charset.name()) + "=" + URLEncoder.encode(entry.getValue(), charset.name());
                        if(index < map.size()) {
                            result +="&";
                        }
                    }
                    //write the encode content to outputstresam
                    out.write(result.getBytes(charset));
                    out.flush();

                } catch (Exception e) {
                    throw new ProcessException(e);
               }
            }
        });
        return flowFile;
    }

    private FlowFile decodeFlowfile(final ProcessContext context, final ProcessSession session, FlowFile flowFile, Charset charset) throws ProcessException {
        String destination = context.getProperty(DECODE_DESTINATION).getValue();
        boolean encodeToContent = (destination.equalsIgnoreCase(DECODE_DESTINATION_CONTENT)||StringUtils.isEmpty(destination)) ? true : false;
        if (!encodeToContent) {
            final Map<String, String> result = new HashMap();
            session.read(flowFile, new InputStreamCallback() {
                @Override
                public void process(InputStream in) throws IOException {
                    try {
                        getContentMap(in, charset, result, true);
                    }catch (Exception e) {
                        throw new ProcessException(e);
                    }
                }
            });
            flowFile = session.putAllAttributes(flowFile, result);
        } else {
            flowFile = session.write(flowFile, new StreamCallback() {
                @Override
                public void process(InputStream in, OutputStream out) throws IOException {
                    try{
                        Map<String, String> result = new HashMap();
                        getContentMap(in, charset,result, false);
                        Gson gson = new Gson();
                        String retStr = gson.toJson(result);
                        out.write(retStr.getBytes(charset));
                        out.flush(); 
                    }catch (Exception e) {
                        throw new ProcessException(e);
                    }
                }
            });
        }
       
        return flowFile;
    }

    private void getContentMap(InputStream in, Charset charset, Map<String, String> result, boolean rewriteKey) throws Exception {
  
        StringWriter writer = new StringWriter();
        IOUtils.copy(in, writer, charset);
        String content = writer.toString();
        content = URLDecoder.decode(content, charset.name());
        String[] sArray = content.split("&");
        for(String str: sArray) {
            String[] itemArray = str.split("=");
            //添加key前缀，防止与flowflie的attribute key重复
            String key = rewriteKey ? "URIDecode."+itemArray[0] : itemArray[0];
            String value = itemArray[1];
            result.put(key, value);
        } 
    }
}
