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
package org.apache.nifi.distributed.cache.client;

import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;

import java.io.IOException;

@Tags({"distributed", "client", "cluster", "map", "cache", "expire"})
@CapabilityDescription("Provides the ability to communicate with a DistributedMapCacheServer. This allows "
        + "multiple nodes to coordinate state with a single remote entity.")
public interface ExpireAtomicDistributedMapCacheClient<R> extends DistributedMapCacheClient {

    <K, V> AtomicCacheEntry<K, V, R> fetch(K key, Serializer<K> keySerializer, Deserializer<V> valueDeserializer) throws IOException;

    <K, V> boolean replace(AtomicCacheEntry<K, V, R> entry, Serializer<K> keySerializer, Serializer<V> valueSerializer) throws IOException;

    //update key and value with sepcific expire time
    <K, V> boolean putIfAbsent(K key, V value, long expire, Serializer<K> keySerializer, Serializer<V> valueSerializer) throws IOException;

    
    <K, V> V getAndPutIfAbsent(K key, V value, long expire, Serializer<K> keySerializer, Serializer<V> valueSerializer, Deserializer<V> valueDeserializer) throws IOException;

    
    <K, V> void put(K key, V value, long expire, Serializer<K> keySerializer, Serializer<V> valueSerializer) throws IOException;
}