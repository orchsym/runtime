/*
 * Licensed to the Orchsym Runtime under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * this file to You under the Orchsym License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://github.com/orchsym/runtime/blob/master/orchsym/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.curator.apps;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.BackgroundCallback;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.nifi.cluster.coordination.node.ClusterRoles;
import org.apache.nifi.controller.FlowController;
import org.apache.nifi.controller.cluster.ZooKeeperClientConfig;
import org.apache.nifi.controller.leader.election.LeaderElectionManager;
import org.apache.nifi.curator.CuratorFactory;
import org.apache.nifi.curator.apis.ApisNotifyServiceImpl;
import org.apache.nifi.curator.config.PathConfig;
import org.apache.nifi.util.NiFiProperties;
import org.apache.nifi.web.NiFiServiceFacade;
import org.apache.nifi.web.api.dto.ProcessGroupDTO;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppStateNotifyServiceImpl implements AppStateNotifyService {
    private static final Logger logger = LoggerFactory.getLogger(ApisNotifyServiceImpl.class);

    private CuratorFactory curatorFactory;

    private LeaderElectionManager leaderElectionManager;

    private FlowController flowController;

    private NiFiProperties properties;

    private NiFiServiceFacade serviceFacade;

    @Override
    public void notifyState(String groupId) {
        logger.debug("+++++ updateAppInfo通知，processGroupId={}++++++", groupId);
        if (groupId == null) {
            return;
        }
        if(!verifyAndWait()){
            return;
        }
        final ProcessGroupDTO groupDTO = serviceFacade.getProcessGroup(groupId).getComponent();
        if (groupDTO == null) {
            return;
        }
        String appJsonStr = JSON.toJSONString(groupDTO);
        ZooKeeperClientConfig zkConfig = ZooKeeperClientConfig.createConfig(properties);
        final CuratorFramework cf = getCuratorFramework();
        if (cf == null){
            logger.debug("++++++ please attempt to check ZooKeeper ++++++");
            return;
        }
        final String appPath = PathConfig.getAppPath(zkConfig.getRootPath(), groupId);
        if (!isAppPathExists(appPath, cf)) {
            // 创建并设置数据
            try {
                // 涉及API的管理交互，此处设置为异步执行
                cf.create().creatingParentsIfNeeded()
                        .withMode(CreateMode.PERSISTENT).inBackground(new BackgroundCallback() {
                    @Override
                    public void processResult(CuratorFramework client, CuratorEvent event) throws Exception {
                        logger.debug("+++++注册APP的通知完毕+++++++++");
                        logger.debug("++++event.code={}+++++event.type={}", event.getResultCode(), event.getType());
                    }
                }).forPath(appPath, appJsonStr.getBytes("UTF-8"));
            } catch (Exception e) {
                logger.error("e.message={}\n e.stack={}", e.getMessage(), e.getStackTrace());
            }
        } else {
            try {
                cf.setData().inBackground(new BackgroundCallback() {
                    @Override
                    public void processResult(CuratorFramework client, CuratorEvent event) throws Exception {
                        logger.debug("+++++更新APP的通知完毕+++++++++");
                        logger.debug("++++event.code={}+++++event.type={}", event.getResultCode(), event.getType());
                    }
                }).forPath(appPath, appJsonStr.getBytes("UTF-8"));
            } catch (Exception e) {
                logger.error("e.message={}\n e.stack={}", e.getMessage(), e.getStackTrace());
            }
        }

    }


    /**
     * @apiNote 判断是否需要 以及等待初始化执行API通知
     */
    private boolean verifyAndWait() {
        if (!properties.isNode()) {
            return false;
        }

        if (!flowController.isClustered()) {
            logger.debug("++++++current node is disconnected status, cant notify app info+++++");
            return false;
        }

        while (!(isElected() && flowController.isInitialized())) {
            try {
                Thread.sleep(500);
                logger.debug("++++ waiting elect primary or init flow controller+++++++");
            } catch (InterruptedException e) {
                logger.error("e.message={}\n e.stack={}", e.getMessage(), e.getStackTrace());
            }
        }

        if (!flowController.isPrimary()) {
            logger.debug("++++++current node is not primary for notify app info +++++++");
            return false;
        }
        return true;
    }

    /**
     * @return
     * @apiNote 获取有效的curator客户端
     */
    private CuratorFramework getCuratorFramework() {
        return curatorFactory.getCuratorFramework(30);
    }

    /**
     * @return 判断是否选举完毕
     */
    private boolean isElected() {
        return !StringUtils.isEmpty(leaderElectionManager.getLeader(ClusterRoles.CLUSTER_COORDINATOR));
    }

    /**
     * @param appPath processGroup 对应的ZooKeeper节点
     * @param cf
     * @return 路径节点是否存在
     */
    private boolean isAppPathExists(String appPath, CuratorFramework cf) {
        Stat stat = null;
        try {
            stat = cf.checkExists().forPath(appPath);
        } catch (Exception e) {
            logger.error("e.message={}\n e.stack={}", e.getMessage(), e.getStackTrace());
        }
        return stat != null;
    }


    public void setCuratorFactory(CuratorFactory curatorFactory) {
        this.curatorFactory = curatorFactory;
    }

    public void setLeaderElectionManager(LeaderElectionManager leaderElectionManager) {
        this.leaderElectionManager = leaderElectionManager;
    }

    public void setFlowController(FlowController flowController) {
        this.flowController = flowController;
    }

    public void setProperties(NiFiProperties properties) {
        this.properties = properties;
    }

    public void setServiceFacade(NiFiServiceFacade serviceFacade) {
        this.serviceFacade = serviceFacade;
    }
}
