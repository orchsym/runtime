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

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.nifi.cluster.coordination.node.ClusterRoles;
import org.apache.nifi.controller.FlowController;
import org.apache.nifi.controller.cluster.ZooKeeperClientConfig;
import org.apache.nifi.controller.leader.election.LeaderElectionManager;
import org.apache.nifi.curator.config.PathConfig;
import org.apache.nifi.util.NiFiProperties;
import org.apache.nifi.web.NiFiServiceFacade;
import org.apache.nifi.web.api.dto.ClusterDTO;
import org.apache.nifi.web.api.entity.ClusterEntity;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author liuxun
 * @apiNote 检测到节点信息变化后，将集群最新信息上传至ZooKeeper
 */
public class ClusterInfoNotify {
    private static final Logger logger = LoggerFactory.getLogger(ClusterInfoNotify.class);

    private CuratorFactory curatorFactory;

    private NiFiProperties properties;

    private FlowController flowController;

    private LeaderElectionManager leaderElectionManager;

    private NiFiServiceFacade serviceFacade;

    /**
     * @apiNote Bean 初始化时，开始监听并执行操作，lazy=false
     */
    public void startListen() throws Exception {
        // 如果配置的不是集群，则不需要进行监听
        if (!properties.isNode()) {
            return;
        }
        CuratorFramework curatorFramework = curatorFactory.getCuratorFramework();
        if (curatorFramework == null) {
            do {
                logger.error("++++ 集群信息通知连接获得curatorFramework 失败, 重新获取中......");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logger.error("e.message={}\n e.stack={}", e.getMessage(),e.getStackTrace());
                }
                curatorFramework = curatorFactory.getCuratorFramework();
            } while (curatorFramework != null);
        }

        final ZooKeeperClientConfig zkConfig = ZooKeeperClientConfig.createConfig(properties);
        // 要监听的根目录
        final String rootPath = zkConfig.getRootPath();
        logger.debug("++++ 监听通知的根目录: {}", rootPath);

        final TreeCache treeCache = new TreeCache(curatorFramework, rootPath);
        treeCache.getListenable().addListener(new TreeCacheListener() {
            @Override
            public void childEvent(CuratorFramework curatorFramework, TreeCacheEvent treeCacheEvent) throws Exception {

                if (treeCacheEvent != null) {
                    logger.debug("{}", treeCacheEvent.getType());
                    logger.debug("{}", treeCacheEvent.getData() == null ? null : treeCacheEvent.getData().getPath());
                    logger.debug("{}", treeCacheEvent.getData() != null && treeCacheEvent.getData().getData() != null ? new String(treeCacheEvent.getData().getData()) : null);
                }
                handleClusterChangeInfo(curatorFramework,zkConfig);
            }
        });
        treeCache.start();
    }

    /**
     * @apiNote 对集群信息改变的通知进行处理
     */
    private void handleClusterChangeInfo(CuratorFramework curatorFramework,ZooKeeperClientConfig zkConfig) {
        if (!flowController.isInitialized()) {
            logger.warn("+++++ flow controller not inited waiting ....");
            do {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logger.error("e.message={}\n e.stack={}", e.getMessage(),e.getStackTrace());
                }
            } while (!flowController.isInitialized());
            logger.warn("+++++ flow controller init success ....");
        }
        logger.debug("+++++++controller.isPrimary={} ", flowController.isPrimary());

        // 检查是否选举除了协调节点，如果没有 则等待选举完毕，再进行数据的获取
        while (!isElected()) {
            try {
                Thread.sleep(1000);
                logger.debug("++++ waiting elect primary+++++++");
            } catch (InterruptedException e) {
                logger.error("e.message={}\n e.stack={}", e.getMessage(),e.getStackTrace());
            }
        }


        if (flowController.isPrimary()) {
            // 获取cluster信息
            final ClusterDTO dto = serviceFacade.getCluster();

            // create entity
            final ClusterEntity entity = new ClusterEntity();
            entity.setCluster(dto);

            String entityJsonStr = JSON.toJSONString(entity);

            logger.debug("+++++ clusterEntity: {} +++++++", entityJsonStr);

            // 然后将ZooKeeper指定节点的信息内容进行更新

            final String infoPath = PathConfig.getInfoNodePath(zkConfig.getRootPath());
            try {
                final Stat stat = curatorFramework.checkExists().forPath(infoPath);
                // 如果节点不存在，则创建并赋值，若存在则只更新值
                if (stat == null) {
                    curatorFramework.create().creatingParentsIfNeeded()
                            .withMode(CreateMode.PERSISTENT).
                            forPath(infoPath, entityJsonStr.getBytes("UTF-8"));
                } else {
                    curatorFramework.setData().forPath(infoPath, entityJsonStr.getBytes("UTF-8"));
                }
            } catch (Exception e) {
                logger.error("e.message={}\n e.stack={}", e.getMessage(),e.getStackTrace());
            }

        }
    }

    /**
     * @return 判断是否选举完毕
     */
    private boolean isElected() {
        return !StringUtils.isEmpty(leaderElectionManager.getLeader(ClusterRoles.CLUSTER_COORDINATOR));
    }


    public void setCuratorFactory(CuratorFactory curatorFactory) {
        this.curatorFactory = curatorFactory;
    }

    public void setProperties(NiFiProperties properties) {
        this.properties = properties;
    }

    public void setFlowController(FlowController flowController) {
        this.flowController = flowController;
    }

    public void setServiceFacade(NiFiServiceFacade serviceFacade) {
        this.serviceFacade = serviceFacade;
    }

    public void setLeaderElectionManager(LeaderElectionManager leaderElectionManager) {
        this.leaderElectionManager = leaderElectionManager;
    }
}
