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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.annotation.behavior.DynamicProperty;
import org.apache.nifi.annotation.behavior.EventDriven;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.InputRequirement.Requirement;
import org.apache.nifi.annotation.behavior.SupportsBatching;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Marks;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.distributed.cache.client.ExpireAtomicDistributedMapCacheClient;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.DataUnit;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;

@EventDriven
@SupportsBatching
@Marks(categories = { "Data Process/Output" }, createdDate = "2019-03-26")
@Tags({"map", "cache", "put", "distributed", "redis"})
@InputRequirement(Requirement.INPUT_REQUIRED)
@CapabilityDescription("Gets the content of a FlowFile and puts it to a redis cache, using a cache key " +
    "computed from FlowFile attributes. If the cache already contains the entry and the cache update strategy is " +
    "'keep original' the entry is not replaced.'")
@WritesAttribute(attribute = "cached", description = "All FlowFiles will have an attribute 'cached'. The value of this " +
    "attribute is true, is the FlowFile is cached, otherwise false.")
@DynamicProperty(name = "Defined attribute to put into cache", value = "Define value for attribute to put into cache", expressionLanguageScope = ExpressionLanguageScope.FLOWFILE_ATTRIBUTES, description = "Specifies an attribute for put into cache defined by the Dynamic Property's key and value.")
@SeeAlso(classNames = {"org.apache.nifi.distributed.cache.client.DistributedMapCacheClientService", "org.apache.nifi.distributed.cache.server.map.DistributedMapCacheServer",
        "org.apache.nifi.processors.standard.FetchDistributedMapCache"})
public class PutRedis extends PutDistributedMapCache {

    private Long expire;
    public static final PropertyDescriptor EXPIRE = new PropertyDescriptor.Builder()
            .name("redis-cache-ttl")
            .displayName("Redis Cache TTL")
            .description("Indicates how long the data should exist in Redis. Setting '0 secs' would mean the data would exist forever")
            .addValidator(StandardValidators.TIME_PERIOD_VALIDATOR)
            .required(true)
            .defaultValue("0 secs")
            .build();
    public static final PropertyDescriptor REDIS_DISTRIBUTED_CACHE_SERVICE = new PropertyDescriptor.Builder()
        .name("Distributed Cache Service")
        .description("The Controller Service that is used to cache flow files")
        .required(true)
        .identifiesControllerService(ExpireAtomicDistributedMapCacheClient.class)
        .build();

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        final List<PropertyDescriptor> descriptors = new ArrayList<>();
        descriptors.add(CACHE_ENTRY_IDENTIFIER);
        descriptors.add(REDIS_DISTRIBUTED_CACHE_SERVICE);
        descriptors.add(CACHE_UPDATE_STRATEGY);
        descriptors.add(CACHE_ENTRY_MAX_BYTES);
        descriptors.add(EXPIRE);
        return descriptors;
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
        final ExpireAtomicDistributedMapCacheClient cache = context.getProperty(DISTRIBUTED_CACHE_SERVICE).asControllerService(ExpireAtomicDistributedMapCacheClient.class);

        try {

            final long maxCacheEntrySize = context.getProperty(CACHE_ENTRY_MAX_BYTES).asDataSize(DataUnit.B).longValue();
            long flowFileSize = flowFile.getSize();

            // too big flow file
            if (flowFileSize > maxCacheEntrySize) {
                logger.warn("Flow file {} size {} exceeds the max cache entry size ({} B).", new Object[] {flowFile, flowFileSize, maxCacheEntrySize});
                session.transfer(flowFile, REL_FAILURE);
                return;
            }

            if (flowFileSize == 0) {
                logger.warn("Flow file {} is empty, there is nothing to cache.", new Object[] {flowFile});
                session.transfer(flowFile, REL_FAILURE);
                return;

            }

            final String updateStrategy = context.getProperty(CACHE_UPDATE_STRATEGY).getValue();
            Map<String, byte[]> cacheMap = getCacheMap(context, session, flowFile);
            expire = context.getProperty(EXPIRE).asTimePeriod(TimeUnit.SECONDS);
            expire = expire == 0 ? -1 : expire;
            boolean cached = putAllCacheWithExpire(updateStrategy, cache, cacheMap);

            // set 'cached' attribute
            flowFile = session.putAttribute(flowFile, CACHED_ATTRIBUTE_NAME, String.valueOf(cached));

            if (cached) {
                session.getProvenanceReporter().modifyContent(flowFile);
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

    private boolean putAllCacheWithExpire(String updateStrategy, ExpireAtomicDistributedMapCacheClient cache, Map<String, byte[]> cacheMap) throws IOException {
        boolean cached = false;
        boolean first = true;
        Set<Entry<String, byte[]>> entrySet = cacheMap.entrySet();
        for (Entry<String, byte[]> entry : entrySet) {
            String cacheKey = entry.getKey();
            byte[] cacheValue = entry.getValue();
            if (cached || first) {
                first = false;
                cached = putCacheWithExpire(updateStrategy, cache, cacheKey, cacheValue);
            }
        }
        return cached;
    }

    private boolean putCacheWithExpire(String updateStrategy, ExpireAtomicDistributedMapCacheClient cache, String cacheKey, byte[] cacheValue) throws IOException {
        boolean cached = false;
        if (updateStrategy.equals(CACHE_UPDATE_REPLACE.getValue())) {
            cache.put(cacheKey, cacheValue, expire, keySerializer, valueSerializer);
            cached = true;
        } else if (updateStrategy.equals(CACHE_UPDATE_KEEP_ORIGINAL.getValue())) {
            final Object oldValue = cache.getAndPutIfAbsent(cacheKey, cacheValue, expire, keySerializer, valueSerializer, valueDeserializer);
            if (oldValue == null) {
                cached = true;
            }
        }
        return cached;
    }

}
