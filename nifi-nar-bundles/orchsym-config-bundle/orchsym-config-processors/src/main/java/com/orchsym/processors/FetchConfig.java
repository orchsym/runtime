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
package com.orchsym.processors;

import org.apache.nifi.annotation.behavior.*;
import org.apache.nifi.annotation.documentation.Marks;
import org.apache.nifi.components.*;
import org.apache.nifi.distributed.cache.client.Deserializer;
import org.apache.nifi.distributed.cache.client.DistributedMapCacheClient;
import org.apache.nifi.distributed.cache.client.Serializer;
import org.apache.nifi.distributed.cache.client.exception.DeserializationException;
import org.apache.nifi.distributed.cache.client.exception.SerializationException;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.util.StandardValidators;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author liuxun
 * @apiNote 将配置数据抽取到全局配置中
 */

@InputRequirement(InputRequirement.Requirement.INPUT_ALLOWED)
@Tags({"config", "ini", "property", "fetch"})
@Marks(categories = {"Data Process/Fetch"})
@CapabilityDescription("fetch  entry (K/V) type from distribute ")
public class FetchConfig extends AbstractProcessor {

    public static final PropertyDescriptor PROP_DISTRIBUTED_CACHE_SERVICE = new PropertyDescriptor.Builder()
            .name("distribute-cache-service")
            .description("The Controller Service that is used to get or put the cached values.")
            .required(true)
            .identifiesControllerService(DistributedMapCacheClient.class)
            .build();

    public static final PropertyDescriptor FETCH_PROPERTY = new PropertyDescriptor
            .Builder().name("fetch-properties")
            .displayName("fetch-properties")
            .description("if not null, fetch property from distribute ")
            .required(true)
            .addValidator(StandardValidators.createListValidator(true, false, StandardValidators.NON_BLANK_VALIDATOR))
            .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("FlowFiles that are sent successfully to the destination are transferred to this relationship.")
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("If unable to communicate with the cache or if the cache entry is evaluated to be blank, the FlowFile will be penalized and routed to this relationship")
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    private final Serializer<String> strSerializer = new StringSerializer();
    private final StringDeserializer strDeserializer = new StringDeserializer();


    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<>();
        descriptors.add(FETCH_PROPERTY);
        descriptors.add(PROP_DISTRIBUTED_CACHE_SERVICE);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(REL_SUCCESS);
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


    /**
     * 每次处理flowfile的时候都会触发
     */
    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session)
            throws ProcessException {
        final ComponentLog logger = getLogger();
        final FlowFile flowFile = session.get() == null ? session.create() : session.get();

        final PropertyValue fetchProp = context.getProperty(FETCH_PROPERTY);
        final String[] fetchProps = fetchProp.isSet() ? fetchProp.getValue().split(",") : new String[]{};
        final HashSet<String> fetchPropsSet = new HashSet<>(Arrays.asList(fetchProps));
        if (fetchPropsSet.isEmpty()) {
            session.transfer(flowFile, REL_SUCCESS);
            return;
        }

        try {
            DistributedMapCacheClient cacheClient = context.getProperty(PROP_DISTRIBUTED_CACHE_SERVICE).asControllerService(DistributedMapCacheClient.class);
            Map<String, String> attrMap = new HashMap<>();
            for (String fKey : fetchPropsSet) {
                if (cacheClient.containsKey(fKey, strSerializer)) {
                    final String value = cacheClient.get(fKey, strSerializer, strDeserializer);
                    attrMap.put(fKey, value);
                }
            }

            if (!attrMap.isEmpty()) {
                session.putAllAttributes(flowFile, attrMap);
            }

            session.transfer(flowFile, REL_SUCCESS);

        } catch (IOException e) {
            session.transfer(flowFile, REL_FAILURE);
            logger.error("Unable to fetch config to cache when processing {} due to {}", new Object[]{flowFile, e});
        }

    }


    public static class StringSerializer implements Serializer<String> {
        @Override
        public void serialize(final String value, final OutputStream out) throws SerializationException, IOException {
            out.write(value.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static class StringDeserializer implements Deserializer<String> {

        @Override
        public String deserialize(final byte[] input) throws DeserializationException, IOException {
            return input.length == 0 ? null : new String(input, StandardCharsets.UTF_8);
        }
    }

}
