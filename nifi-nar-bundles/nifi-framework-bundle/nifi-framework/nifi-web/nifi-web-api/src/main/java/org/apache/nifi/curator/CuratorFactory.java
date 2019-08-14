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
package org.apache.nifi.curator;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.nifi.controller.cluster.ZooKeeperClientConfig;
import org.apache.nifi.controller.leader.election.CuratorACLProviderFactory;
import org.apache.nifi.util.NiFiProperties;

/**
 * @author liuxun
 * @apiNote 封装Curator的Bean工具类
 */
public class CuratorFactory {
    private NiFiProperties niFiProperties;

    private CuratorFramework curatorFramework;

    /**
     * @apiNote 用于初始化curatorFramework参数，并开始连接 lazy=true
     */
    public void initCurator() {
        final RetryPolicy retryPolicy = new RetryNTimes(1, 100);
        final CuratorACLProviderFactory aclProviderFactory = new CuratorACLProviderFactory();
        final ZooKeeperClientConfig zkConfig = ZooKeeperClientConfig.createConfig(niFiProperties);
        curatorFramework = CuratorFrameworkFactory.builder()
                .connectString(zkConfig.getConnectString())
                .sessionTimeoutMs(zkConfig.getSessionTimeoutMillis())
                .connectionTimeoutMs(zkConfig.getConnectionTimeoutMillis())
                .retryPolicy(retryPolicy)
                .aclProvider(aclProviderFactory.create(zkConfig))
                .defaultData(new byte[0])
                .build();

        curatorFramework.start();
    }

    /**
     * @apiNote 销毁
     */
    public void destoryCurator(){
        this.curatorFramework.close();
    }

    public void setNiFiProperties(NiFiProperties niFiProperties) {
        this.niFiProperties = niFiProperties;
    }

    /**
     * @return 返回已连接的框架
     */
    public CuratorFramework getCuratorFramework() {
        if (!this.curatorFramework.isStarted()){
            return null;
        }
        return curatorFramework;
    }

}
