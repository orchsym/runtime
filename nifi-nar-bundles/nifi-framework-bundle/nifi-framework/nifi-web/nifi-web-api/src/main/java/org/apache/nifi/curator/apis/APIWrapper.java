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
package org.apache.nifi.curator.apis;

import org.apache.nifi.apis.ApisNotifyService;
import org.apache.nifi.registry.api.APIServicesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @apiNote API包装类，用于初始化APIServiceManager的成员变量
 * @author liuxun
 *
 */
public class APIWrapper {
    private ApisNotifyService apisNotifyService;
    private static final Logger logger = LoggerFactory.getLogger(APIWrapper.class);

    public void setApisNotifyService(ApisNotifyService apisNotifyService) {
        this.apisNotifyService = apisNotifyService;
        APIServicesManager.getInstance().setNotifyService(apisNotifyService);
        logger.debug("+++++++初始化APIServiceManager成员变量ApisNotifyService成功 +++++++");
    }
}
