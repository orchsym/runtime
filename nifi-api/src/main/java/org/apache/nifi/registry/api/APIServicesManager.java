package org.apache.nifi.registry.api;

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
        } finally {
            lock.unlock();
        }
    }

    public void unregister(String id) {
        lock.lock();
        try {
            removeApiInfo(id);
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
            while (infoItr.hasNext()) {
                ApiInfo apiInfo = (ApiInfo) infoItr.next();
                if (apiInfo.id.equals(id)) {
                    apiField.set(apiInfo, value);
                }
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

}
