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
package org.apache.nifi.groups;

import java.util.Arrays;
import java.util.Set;

/**
 * @author GU Guoqiang
 *
 */
public interface ProcessTags {
    String TAG_NAME = "tag";

    Set<String> getTags();

    void setTags(Set<String> tags);

    default boolean contains(String... some) {
        if (null == some || some.length == 0) {
            return false;
        }
        final Set<String> tags = getTags();
        if (null == tags || tags.isEmpty()) {
            return false;
        }
        return tags.containsAll(Arrays.asList(some));
    }
}
