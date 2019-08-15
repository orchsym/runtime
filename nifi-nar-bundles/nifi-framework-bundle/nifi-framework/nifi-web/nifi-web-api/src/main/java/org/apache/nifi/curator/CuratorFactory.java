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
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.RetryNTimes;
import org.apache.nifi.controller.cluster.ZooKeeperClientConfig;
import org.apache.nifi.controller.leader.election.CuratorACLProviderFactory;
import org.apache.nifi.util.NiFiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

/**
 * @author liuxun
 * @apiNote 封装Curator的Bean工具类
 */
public class CuratorFactory {
    private static final Logger logger = LoggerFactory.getLogger(CuratorFactory.class);

    private NiFiProperties niFiProperties;

    private CuratorFramework curatorFramework = null;

    /**
     * @apiNote 用于初始化curatorFramework参数，并开始连接 lazy=true
     */
    public void initCurator() throws InterruptedException {
        if (!niFiProperties.isNode()){
            return;
        }

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
        if (this.curatorFramework != null){
            this.curatorFramework.close();
        }
    }

    public void setNiFiProperties(NiFiProperties niFiProperties) {
        this.niFiProperties = niFiProperties;
    }

    /**
     * @return 返回已连接的框架
     */
    public  CuratorFramework getCuratorFramework(int getRetrys) {
        if (curatorFramework == null){
            return null;
        }

        int retrys = 0;
        while (!curatorFramework.isStarted() && retrys < getRetrys){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.error("e.message={}\n e.stack={}", e.getMessage(),e.getStackTrace());
            }
            retrys++;
        }
        return curatorFramework.isStarted() ? curatorFramework : null;
    }

}
