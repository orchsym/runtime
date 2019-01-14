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

package org.apache.nifi.authentication;

import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.controller.ControllerService;

@Tags({"Authentication Service"})
@CapabilityDescription("Authentication Service.")

public interface APIAuthenticationService extends ControllerService {

	//根据配置的白名单或黑名单，验证地址是否通过
    public boolean authenticateAddress(String address);

    //返回是否需要验证授权信息
    public boolean shouldAuthenticateAuthorizationInfo();

    //验证授权信息是否通过
    public boolean authenticateAuthorizationInfo(String method, String authorizationInfo);
}