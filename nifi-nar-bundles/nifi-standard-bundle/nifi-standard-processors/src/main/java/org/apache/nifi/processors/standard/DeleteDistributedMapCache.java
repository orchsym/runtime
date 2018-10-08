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
package org.apache.nifi.processors.standard;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.annotation.behavior.EventDriven;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.InputRequirement.Requirement;
import org.apache.nifi.annotation.behavior.SupportsBatching;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.AllowableValue;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.distributed.cache.client.DistributedMapCacheClient;
import org.apache.nifi.distributed.cache.client.Serializer;
import org.apache.nifi.distributed.cache.client.exception.SerializationException;
import org.apache.nifi.expression.AttributeExpression.ResultType;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;

@EventDriven
@SupportsBatching
@Tags({"map", "cache", "delete", "distributed"})
@InputRequirement(Requirement.INPUT_REQUIRED)
@WritesAttribute(attribute = "removed", description = "All FlowFiles will have an attribute 'removed'. The value of this " +
    "attribute is true, is the cache is removed, otherwise false.")
@CapabilityDescription("Delete the content of a distributed map cache, using a cache key computed from FlowFile attributes")
@SeeAlso(classNames = {"org.apache.nifi.distributed.cache.client.DistributedMapCacheClientService", "org.apache.nifi.distributed.cache.server.map.DistributedMapCacheServer", "org.apache.nifi.processors.standard.FetchDistributedMapCache", "org.apache.nifi.processors.standard.PutDistributedMapCache"})
public class DeleteDistributedMapCache extends AbstractProcessor {

    public static final String REMOVED_ATTRIBUTE_NAME = "removed";

    // Identifies the distributed map cache client
    public static final PropertyDescriptor DISTRIBUTED_CACHE_SERVICE = new PropertyDescriptor.Builder()
        .name("Distributed Cache Service")
        .description("The Controller Service that is used to cache flow files")
        .required(true)
        .identifiesControllerService(DistributedMapCacheClient.class)
        .build();

    // Selects the FlowFile attribute, whose value is used as cache key
    public static final PropertyDescriptor CACHE_ENTRY_IDENTIFIER = new PropertyDescriptor.Builder()
        .name("Cache Entry Identifier")
        .description("A FlowFile attribute, or the results of an Attribute Expression Language statement, which will " +
            "be evaluated against a FlowFile in order to determine the cache key")
        .required(true)
        .addValidator(StandardValidators.createAttributeExpressionLanguageValidator(ResultType.STRING, true))
        .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
        .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
        .name("success")
        .description("Any FlowFile that is successfully inserted into cache will be routed to this relationship")
        .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
        .name("failure")
        .description("Any FlowFile that cannot be inserted into the cache will be routed to this relationship")
        .build();
    private final Set<Relationship> relationships;

    private final Serializer<String> keySerializer = new StringSerializer();

    public DeleteDistributedMapCache() {
        final Set<Relationship> rels = new HashSet<>();
        rels.add(REL_SUCCESS);
        rels.add(REL_FAILURE);
        relationships = Collections.unmodifiableSet(rels);
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        final List<PropertyDescriptor> descriptors = new ArrayList<>();
        descriptors.add(CACHE_ENTRY_IDENTIFIER);
        descriptors.add(DISTRIBUTED_CACHE_SERVICE);
        return descriptors;
    }

    @Override
    public Set<Relationship> getRelationships() {
        return relationships;
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {

        FlowFile flowFile = session.get();
        if (flowFile == null) {
            return;
        }

        final ComponentLog logger = getLogger();

        // cache key is computed from attribute 'CACHE_ENTRY_IDENTIFIER' with expression language support
        final String cacheKey = context.getProperty(CACHE_ENTRY_IDENTIFIER).evaluateAttributeExpressions(flowFile).getValue();

        // if the computed value is null, or empty, we transfer the flow file to failure relationship
        if (StringUtils.isBlank(cacheKey)) {
            logger.error("FlowFile {} has no attribute for given Cache Entry Identifier", new Object[] {flowFile});
            flowFile = session.penalize(flowFile);
            session.transfer(flowFile, REL_FAILURE);
            return;
        }

        // the cache client used to interact with the distributed cache
        final DistributedMapCacheClient cache = context.getProperty(DISTRIBUTED_CACHE_SERVICE).asControllerService(DistributedMapCacheClient.class);

        try {
            boolean removed = cache.remove(cacheKey, keySerializer);
            flowFile = session.putAttribute(flowFile, REMOVED_ATTRIBUTE_NAME, String.valueOf(removed));
            if (removed) {
                session.transfer(flowFile, REL_SUCCESS);
            } else {
                session.transfer(flowFile, REL_FAILURE);
            }
        } catch (final IOException e) {
            flowFile = session.penalize(flowFile);
            session.transfer(flowFile, REL_FAILURE);
            logger.error("Unable to communicate with cache when processing {} due to {}", new Object[] {flowFile, e});
        }
    }

    /**
     * Simple string serializer, used for serializing the cache key
     */
    public static class StringSerializer implements Serializer<String> {

        @Override
        public void serialize(final String value, final OutputStream out) throws SerializationException, IOException {
            out.write(value.getBytes(StandardCharsets.UTF_8));
        }
    }

}
