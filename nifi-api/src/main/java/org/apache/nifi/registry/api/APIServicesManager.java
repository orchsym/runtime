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
package org.apache.nifi.registry.api;

import org.apache.nifi.apis.ApisNotifyService;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author GU Guoqiang
 *
 */
public class APIServicesManager {

    private ApisNotifyService notifyService;

    private APIServicesManager() {
    }

    private static class SingletonInstance {
        private static final APIServicesManager INSTANCE = new APIServicesManager();
    }

    public static APIServicesManager getInstance() {
        return SingletonInstance.INSTANCE;
    }

    private Lock lock = new ReentrantLock();
    private final CopyOnWriteArrayList<ApiInfo> apiInfos = new CopyOnWriteArrayList<>();

    public List<ApiInfo> getInfos() {
        return Collections.unmodifiableList(apiInfos);
    }

    public void register(ApiInfo apiInfo) {
        lock.lock();
        try {
            removeApiInfo(apiInfo.id);

            this.apiInfos.add(apiInfo);
            if (notifyService != null){
                notifyService.register(apiInfo);
            }
        } finally {
            lock.unlock();
        }
    }

    public void unregister(String id) {
        lock.lock();
        try {
            removeApiInfo(id);
            if (notifyService != null){
                notifyService.unregister(id);
            }
        } finally {
            lock.unlock();
        }
    }

    public void update(String id, String field, Object value) {
        if (field == null || field.isEmpty()) {
            return;
        }
        lock.lock();
        try {
            final Field apiField = ApiInfo.class.getDeclaredField(field);
            apiField.setAccessible(true);

            Iterator<ApiInfo> infoItr = this.apiInfos.iterator();
            // API组件的ID是唯一的
            ApiInfo targetApiInfo = null;
            while (infoItr.hasNext()) {
                ApiInfo apiInfo = (ApiInfo) infoItr.next();
                if (apiInfo.id.equals(id)) {
                    apiField.set(apiInfo, value);
                    targetApiInfo = apiInfo;
                }
            }
            if (notifyService != null){
                notifyService.update(targetApiInfo);
            }
        } catch (Exception e) {
            // if no field, will ignore
        } finally {
            lock.unlock();
        }
    }

    private void removeApiInfo(String id) {
        Iterator<ApiInfo> infoItr = this.apiInfos.iterator();
        while (infoItr.hasNext()) {
            ApiInfo apiInfo = (ApiInfo) infoItr.next();
            String apiId = apiInfo.id;
            if (apiId.equals(id)) {
                this.apiInfos.remove(apiInfo);
            }
        }
    }

    public void setNotifyService(ApisNotifyService notifyService) {
        this.notifyService = notifyService;
    }
}
