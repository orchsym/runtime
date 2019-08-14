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
package org.apache.nifi.curator.config;

/**
 * @author liuxun
 * @apiNote 路径节点的配置类(zookeeper)
 */
public class PathConfig {
    private static String RUNTIME_PREFIX = "/runtime";
    private static String APIS_PREFIX = "/apis";
    private static String CLUSTER_PREFIX = "/cluster";
    private static String APP_PREFIX = "/apps";


    /**
     * @return 获取集群信息的信息节点(节点的绝对路径)
     */
    public static String getInfoNodePath(String clusterFlag) {
        clusterFlag = clusterFlag.endsWith("/") ? clusterFlag.substring(0, clusterFlag.length() - 1) : clusterFlag;
        clusterFlag = clusterFlag.startsWith("/") ? clusterFlag : "/" + clusterFlag;
        return RUNTIME_PREFIX + CLUSTER_PREFIX + clusterFlag;
    }

    /**
     * @param apiId api组件的ID
     * @return 根据组件ID获取 API组件的绝对路径
     */
    public static String getApisPath(String clusterFlag, String apiId) {
        clusterFlag = clusterFlag.endsWith("/") ? clusterFlag : clusterFlag + "/";
        clusterFlag = clusterFlag.startsWith("/") ? clusterFlag : "/" + clusterFlag;
        return RUNTIME_PREFIX + APIS_PREFIX + clusterFlag + apiId;
    }

    /**
     *
     * @param clusterFlag
     * @param groupId processGroup的ID 即APP的ID
     * @return 根据processGroup的ID获取绝对路径
     */
    public static String getAppPath(String clusterFlag, String groupId){
        clusterFlag = clusterFlag.endsWith("/") ? clusterFlag : clusterFlag + "/";
        clusterFlag = clusterFlag.startsWith("/") ? clusterFlag : "/" + clusterFlag;
        return RUNTIME_PREFIX + APP_PREFIX + clusterFlag +groupId;
    }

}
