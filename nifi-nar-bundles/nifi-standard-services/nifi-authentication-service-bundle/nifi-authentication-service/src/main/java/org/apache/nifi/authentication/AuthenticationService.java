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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.math.BigInteger;
import java.security.MessageDigest;
import org.apache.commons.lang3.StringUtils;

import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Marks;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.AllowableValue;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.controller.ControllerServiceInitializationContext;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.reporting.InitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Marks(createdDate = "2019-01-04")
@Tags({ "authentication service" })
@CapabilityDescription("This service is for authentication")
public class AuthenticationService extends AbstractControllerService implements APIAuthenticationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private static final String AS_LIST_SEPARATOR = ",";
    private static final String AS_ITEM_SEPARATOR = ":";
    private static final String PREFIX_BASIC = "Basic ";

    public static final AllowableValue AUTHENTICATION_BASIC = new AllowableValue("Basic Authentication", "Basic Authentication", //
            "HTTP Basic Authentication"); //
    public static final AllowableValue AUTHENTICATION_DIGEST = new AllowableValue("Digest Authentication", "Digest Authentication", //
            "HTTP Digest Authentication"); //

    public static final PropertyDescriptor WHITE_LIST = new PropertyDescriptor.Builder()
        .name("White List")
        .description("Comma separated list of the white list address, whitelist and blacklist can only choose one.")
        .required(false)//
        .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
        .build();

    public static final PropertyDescriptor BLACK_LIST = new PropertyDescriptor.Builder()
        .name("Black List")
        .description("Comma separated list of the black list address, whitelist and blacklist can only choose one.")
        .required(false)//
        .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
        .build();

    public static final PropertyDescriptor AUTHENTICATION_METHOD = new PropertyDescriptor.Builder()
        .name("Authentication Method")
        .description("The method used when authenticating")
        .required(false) //
        .allowableValues(AUTHENTICATION_BASIC, AUTHENTICATION_DIGEST) //
        .build();

    public static final PropertyDescriptor AUTHORIZED_USER_LIST = new PropertyDescriptor.Builder()
        .name("Authorized User List")
        .description("Comma separated list of the authorized users, each user info consists of a username and password, separated by ':', eg: 'user1:pwd1,user2:pwd2'")
        .required(false) //
        .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
        .build();

    public static final PropertyDescriptor REALM = new PropertyDescriptor.Builder()
        .name("Realm")
        .description("Authentication domain")
        .required(false) //
        .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
        .build();

    public static final PropertyDescriptor NONCE = new PropertyDescriptor.Builder()
        .name("Nonce")
        .description("Random string")
        .required(false) //
        .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
        .build();

    protected List<PropertyDescriptor> properties;
    private Set<String> blackList;
    private Set<String> whiteList;
    private boolean shouldAuthenticate = false;
    private String authenticationMethod;
    private Map<String, String> authorizedUserMap; //key: name, value: password
    private String realm;
    private String nonce;

    @Override
    protected void init(ControllerServiceInitializationContext config) throws InitializationException {
        final List<PropertyDescriptor> props = new ArrayList<>();
        props.add(BLACK_LIST);
        props.add(WHITE_LIST);
        props.add(AUTHENTICATION_METHOD);
        props.add(AUTHORIZED_USER_LIST);
        props.add(REALM);
        props.add(NONCE);
        properties = Collections.unmodifiableList(props);
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return properties;
    }

    @OnEnabled
    public void onEnabled(final ConfigurationContext context) throws InitializationException {
        blackList = getAddressList(context.getProperty(BLACK_LIST).getValue());
        whiteList = getAddressList(context.getProperty(WHITE_LIST).getValue());
        realm = context.getProperty(REALM).getValue() != null ? context.getProperty(REALM).getValue() : "";
        nonce = context.getProperty(NONCE).getValue() != null ? context.getProperty(NONCE).getValue() : "";
        authenticationMethod = context.getProperty(AUTHENTICATION_METHOD).getValue();
        authorizedUserMap = getAuthorizedUserMap(context.getProperty(AUTHORIZED_USER_LIST).getValue());
        if (authenticationMethod != null) {
            shouldAuthenticate = true;
        } else {
            shouldAuthenticate = false;
        }
    }

    @Override
    public boolean authenticateAddress(String address) {
        if (blackList != null && blackList.contains(address) ||
            (whiteList != null && !whiteList.contains(address))) {
            return false;
        }
        return true;
    }

    @Override
    public boolean shouldAuthenticateAuthorizationInfo() {
        return shouldAuthenticate;
    }

    //验证认证信息
    @Override
    public boolean authenticateAuthorizationInfo(String method, String authorizationInfo){
        try {
            if (StringUtils.isEmpty(authorizationInfo)) {
                return false;
            }
            if (AUTHENTICATION_BASIC.equals(authenticationMethod)) {
                return authenticateAuthorizationInfoByBasic(authorizationInfo);
            } else if (AUTHENTICATION_DIGEST.equals(authenticationMethod)) {
                return authenticateAuthorizationInfoByDigest(method, authorizationInfo, realm, nonce);
            } 
        } catch (Exception e) {
            return false;
        }
        
        return true;
    }

    //basic认证，返回验证信息中的用户名密码是否包含在Authorized User List中，如果是则验证通过
    private boolean authenticateAuthorizationInfoByBasic(String authorizationInfo) {
        authorizationInfo = authorizationInfo.trim();
        if (authorizationInfo.toLowerCase().startsWith(PREFIX_BASIC.toLowerCase())) {
            authorizationInfo = authorizationInfo.substring(PREFIX_BASIC.length()).trim();
        }
        try {
            // base64 decode
            byte[] decoded = Base64.getDecoder().decode(authorizationInfo);
            String info = new String(decoded);
            String[] userItems = StringUtils.split(info, AS_ITEM_SEPARATOR);
            if (userItems != null) {
                String name = userItems.length > 0 ? userItems[0].trim() : "";
                String pwd = userItems.length > 1 ? userItems[1].trim() : "";
                if (authorizedUserMap != null && authorizedUserMap.containsKey(name) && authorizedUserMap.get(name).equals(pwd)) {
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return false;
    }

    //digest认证
    private boolean authenticateAuthorizationInfoByDigest(String requestMethod, String authorizationInfo, String realm, String nonce) throws Exception {
        if(StringUtils.isEmpty(authorizationInfo)){
            return false;
        }
        String username_value = getParameter(authorizationInfo, "username");
        if(StringUtils.isEmpty(username_value)){
            return false;
        }
        String realm_value = getParameter(authorizationInfo, "realm");
        if(StringUtils.isEmpty(realm_value)){
            return false;
        }
        String nonce_value = getParameter(authorizationInfo, "nonce");
        if(StringUtils.isEmpty(nonce_value)){
            return false;
        }
        String uri_value = getParameter(authorizationInfo, "uri");
        if(StringUtils.isEmpty(uri_value)){
            return false;
        }
        String qop_value = getParameter(authorizationInfo, "qop");
        if(StringUtils.isEmpty(qop_value)){
            return false;
        }
        String nc_value = getParameter(authorizationInfo, "nc");
        if(StringUtils.isEmpty(nc_value)){
            return false;
        }
        String cnonce_value = getParameter(authorizationInfo, "cnonce");
        if(StringUtils.isEmpty(cnonce_value)){
            return false;
        }
        String response_value = getParameter(authorizationInfo, "response");
        if(StringUtils.isEmpty(response_value)){
            return false;
        }
        //获取用户密码
        String password = authorizedUserMap.get(username_value);
        if(StringUtils.isEmpty(password)){
            return false;
        }
        //验证response_value
        String ha1 = getMD5Str(username_value + ":" + realm + ":" + password);
        String ha2 = getMD5Str(requestMethod + ":" + uri_value);
        String response = getMD5Str(ha1 + ":" + nonce + ":" + nc_value + ":" + cnonce_value + ":" + qop_value + ":" + ha2);

        return response.equalsIgnoreCase(response_value);
    }

    private Set<String> getAddressList(String addressList) {
        if (StringUtils.isNotBlank(addressList)) {
            String[] addresses = StringUtils.split(addressList, AS_LIST_SEPARATOR);
            if (addresses != null) {
                Set<String> result = new HashSet<>(addresses.length);
                for (String str : addresses) {
                    String trim = str.trim();
                    result.add(trim);
                }
                return result;
            }
        }
        return null;
    }

    private Map<String, String> getAuthorizedUserMap(String authorizedUsersStr) {
        if (StringUtils.isNotBlank(authorizedUsersStr)) {
            String[] authorizedUsers = StringUtils.split(authorizedUsersStr, AS_LIST_SEPARATOR);
            if (authorizedUsers != null) {
                Map<String, String> result= new HashMap();
                for (String userStr : authorizedUsers) {
                    String userStrTrim = userStr.trim();
                    String[] userItems = StringUtils.split(userStrTrim, AS_ITEM_SEPARATOR);
                    if (userItems != null) {
                        String name = userItems.length > 0 ? userItems[0].trim() : "";
                        String pwd = userItems.length > 1 ? userItems[1].trim() : "";
                        result.put(name, pwd);
                    }
                }
                return result;
            }
        }
        return null;
    }

    private String getParameter(String authz,String name){
        if(StringUtils.isEmpty(authz) || StringUtils.isEmpty(name)) return null;
        String regex = name + "=((.+?,)|((.+?)$))";
        Matcher m = Pattern.compile(regex).matcher(authz);
        if(m.find()){
            String p = m.group(1);
            if(!StringUtils.isEmpty(p)){
                if(p.endsWith(",")){
                    p = p.substring(0, p.length() - 1);
                }
                if(p.startsWith("\"")){
                    p = p.substring(1);
                }
                if(p.endsWith("\"")){
                    p = p.substring(0, p.length() - 1);
                }
                return p;
            }
        }
        return null;
    }
    
    public String getMD5Str(String str) throws Exception {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes());
            return new BigInteger(1, md.digest()).toString(16);
        } catch (Exception e) {
            throw new Exception("MD5加密出现错误，"+e.toString());
        }
    }
}
