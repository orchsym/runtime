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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.nifi.components.AllowableValue;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.MockProcessContext;
import org.apache.nifi.util.MockValidationContext;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthenticationServiceTest {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationServiceTest.class);

    @Test
    public void testBlackList() {
        try {
            TestRunner runner = TestRunners.newTestRunner(TestProcessor.class);
            AuthenticationService service = new AuthenticationService();
            HashMap<String, String> properties = new HashMap<String, String>();
            properties.put(AuthenticationService.BLACK_LIST.getName(), "127.0.0.1,192.168.0.1");
            runner.addControllerService("test-blacklist", service, properties);
            runner.enableControllerService(service);
            runner.assertValid();
            Assert.assertNotNull(service);
            assertTrue(service instanceof AuthenticationService);

            assertEquals(false, service.authenticateAddress("127.0.0.1"));
            assertEquals(true, service.authenticateAddress("192.0.0.1"));
        } catch (Exception e) {
            Assert.fail("Should not have thrown a exception " + e.getMessage());
        }
    }

    @Test
    public void testWhiteList() {
        try {
            TestRunner runner = TestRunners.newTestRunner(TestProcessor.class);
            AuthenticationService service = new AuthenticationService();
            HashMap<String, String> properties = new HashMap<String, String>();
            properties.put(AuthenticationService.WHITE_LIST.getName(), "127.0.0.1");
            runner.addControllerService("test-whitelist", service, properties);
            runner.enableControllerService(service);
            runner.assertValid();
            Assert.assertNotNull(service);
            assertTrue(service instanceof AuthenticationService);

            assertEquals(true, service.authenticateAddress("127.0.0.1"));
            assertEquals(false, service.authenticateAddress("192.0.0.1"));
        } catch (Exception e) {
            Assert.fail("Should not have thrown a exception " + e.getMessage());
        }
    }

    @Test
    public void testBasicAuthentication() {
        try {
            TestRunner runner = TestRunners.newTestRunner(TestProcessor.class);
            AuthenticationService service = new AuthenticationService();
            HashMap<String, String> properties = new HashMap<String, String>();
            properties.put(AuthenticationService.AUTHENTICATION_METHOD.getName(), "Basic Authentication");
            properties.put(AuthenticationService.AUTHORIZED_USER_LIST.getName(), "chyingp:123456,lujiangbin:123");
            runner.addControllerService("test-basic", service, properties);
            runner.enableControllerService(service);
            runner.assertValid();
            Assert.assertNotNull(service);
            assertTrue(service instanceof AuthenticationService);

            String authenticationInfo = "Y2h5aW5ncDoxMjM0NTY=";// “chyingp:123456”base64编码
            assertEquals(true, service.authenticateAuthorizationInfo("GET", authenticationInfo));

            String invalidAuthenticationInfo = "AAAAaW5ncDoxMBBBBBB=";
            assertEquals(false, service.authenticateAuthorizationInfo("GET", invalidAuthenticationInfo));
        } catch (Exception e) {
            Assert.fail("Should not have thrown a exception " + e.getMessage());
        }
    }

    @Test
    public void testDigestAuthentication() {
        try {
            TestRunner runner = TestRunners.newTestRunner(TestProcessor.class);
            AuthenticationService service = new AuthenticationService();
            HashMap<String, String> properties = new HashMap<String, String>();
            properties.put(AuthenticationService.AUTHENTICATION_METHOD.getName(), "Digest Authentication");
            properties.put(AuthenticationService.AUTHORIZED_USER_LIST.getName(), "chyingp:123456,lujiangbin:123,Mufasa:Circle Of Life");
            properties.put(AuthenticationService.REALM.getName(), "testrealm@host.com");
            properties.put(AuthenticationService.NONCE.getName(), "dcd98b7102dd2f0e8b11d0f600bfb0c093");
            runner.addControllerService("test-digest", service, properties);
            runner.enableControllerService(service);
            runner.assertValid();
            Assert.assertNotNull(service);
            assertTrue(service instanceof AuthenticationService);

            String authenticationInfo = "Digest username=\"Mufasa\",realm=\"testrealm@host.com\",nonce=\"dcd98b7102dd2f0e8b11d0f600bfb0c093\",uri=\"/dir/index.html\",qop=auth,nc=00000001,cnonce=\"0a4f113b\",response=\"6629fae49393a05397450978507c4ef1\",opaque=\"5ccc069c403ebaf9f0171e9517f40e41\"";
            assertEquals(true, service.authenticateAuthorizationInfo("GET", authenticationInfo));

        } catch (Exception e) {
            Assert.fail("Should not have thrown a exception " + e.getMessage());
        }
    }

    @Test
    public void testStandardBasicAuthentication() {
        try {
            TestRunner runner = TestRunners.newTestRunner(TestProcessor.class);
            AuthenticationService service = new AuthenticationService();
            HashMap<String, String> properties = new HashMap<String, String>();
            properties.put(AuthenticationService.AUTHENTICATION_METHOD.getName(), "Basic Authentication");
            properties.put(AuthenticationService.AUTHORIZED_USER_LIST.getName(), "chyingp:123456,lujiangbin:123");
            runner.addControllerService("test-basic", service, properties);
            runner.enableControllerService(service);
            runner.assertValid();
            Assert.assertNotNull(service);
            assertTrue(service instanceof AuthenticationService);

            String authenticationInfo = "Basic Y2h5aW5ncDoxMjM0NTY=";// “chyingp:123456”base64编码
            assertEquals(true, service.authenticateAuthorizationInfo("GET", authenticationInfo));

            String invalidAuthenticationInfo = "Basic AAAAaW5ncDoxMBBBBBB=";
            assertEquals(false, service.authenticateAuthorizationInfo("GET", invalidAuthenticationInfo));
        } catch (Exception e) {
            Assert.fail("Should not have thrown a exception " + e.getMessage());
        }
    }

    @Test
    public void testDigestAuthentication_AuthenticationInfo_Bad_Response() {
        try {
            TestRunner runner = TestRunners.newTestRunner(TestProcessor.class);
            AuthenticationService service = new AuthenticationService();
            HashMap<String, String> properties = new HashMap<String, String>();
            properties.put(AuthenticationService.AUTHENTICATION_METHOD.getName(), "Digest Authentication");
            properties.put(AuthenticationService.AUTHORIZED_USER_LIST.getName(), "chyingp:123456,lujiangbin:123,Mufasa:Circle Of Life");
            properties.put(AuthenticationService.REALM.getName(), "testrealm@host.com");
            properties.put(AuthenticationService.NONCE.getName(), "dcd98b7102dd2f0e8b11d0f600bfb0c093");
            runner.addControllerService("test-digest", service, properties);
            runner.enableControllerService(service);
            runner.assertValid();
            Assert.assertNotNull(service);
            assertTrue(service instanceof AuthenticationService);

            String authenticationInfo = "Digest username=\"Mufasa\",realm=\"testrealm@host.com\",nonce=\"dcd98b7102dd2f0e8b11d0f600bfb0c093\",uri=\"/dir/index.html\",qop=auth,nc=00000001,cnonce=\"0a4f113b\",response=\"6629fae49393a0539745091111111111\"";
            assertEquals(false, service.authenticateAuthorizationInfo("GET", authenticationInfo));

        } catch (Exception e) {
            Assert.fail("Should not have thrown a exception " + e.getMessage());
        }
    }

    @Test
    public void testDigestAuthentication_AuthenticationInfo_Bad_Nonce() {
        try {
            TestRunner runner = TestRunners.newTestRunner(TestProcessor.class);
            AuthenticationService service = new AuthenticationService();
            HashMap<String, String> properties = new HashMap<String, String>();
            properties.put(AuthenticationService.AUTHENTICATION_METHOD.getName(), "Digest Authentication");
            properties.put(AuthenticationService.AUTHORIZED_USER_LIST.getName(), "chyingp:123456,lujiangbin:123,Mufasa:Circle Of Life");
            properties.put(AuthenticationService.REALM.getName(), "testrealm@host.com");
            properties.put(AuthenticationService.NONCE.getName(), "dcd98b7102dd2f0e8b11d0f61111111111");
            runner.addControllerService("test-digest", service, properties);
            runner.enableControllerService(service);
            runner.assertValid();
            Assert.assertNotNull(service);
            assertTrue(service instanceof AuthenticationService);

            String authenticationInfo = "Digest username=\"Mufasa\",realm=\"testrealm@host.com\",nonce=\"dcd98b7102dd2f0e8b11d0f600bfb0c093\",uri=\"/dir/index.html\",qop=auth,nc=00000001,cnonce=\"0a4f113b\",response=\"6629fae49393a05397450978507c4ef1\",opaque=\"5ccc069c403ebaf9f0171e9517f40e41\"";
            assertEquals(false, service.authenticateAuthorizationInfo("GET", authenticationInfo));

        } catch (Exception e) {
            Assert.fail("Should not have thrown a exception " + e.getMessage());
        }
    }

    @Test
    public void testDigestAuthentication_AuthenticationInfo_Bad_Realm() {
        try {
            TestRunner runner = TestRunners.newTestRunner(TestProcessor.class);
            AuthenticationService service = new AuthenticationService();
            HashMap<String, String> properties = new HashMap<String, String>();
            properties.put(AuthenticationService.AUTHENTICATION_METHOD.getName(), "Digest Authentication");
            properties.put(AuthenticationService.AUTHORIZED_USER_LIST.getName(), "chyingp:123456,lujiangbin:123,Mufasa:Circle Of Life");
            properties.put(AuthenticationService.REALM.getName(), "testrealm@host.com");
            properties.put(AuthenticationService.NONCE.getName(), "dcd98b7102dd2f0e8b11d0f600bfb0c093");
            runner.addControllerService("test-digest", service, properties);
            runner.enableControllerService(service);
            runner.assertValid();
            Assert.assertNotNull(service);
            assertTrue(service instanceof AuthenticationService);

            String authenticationInfo = "Digest username=\"Mufasa\",realm=\"badrealm\",nonce=\"dcd98b7102dd2f0e8b11d0f600bfb0c093\",uri=\"/dir/index.html\",qop=auth,nc=00000001,cnonce=\"0a4f113b\",response=\"6629fae49393a05397450978507c0000\",opaque=\"5ccc069c403ebaf9f0171e9517f40e41\"";
            assertEquals(false, service.authenticateAuthorizationInfo("GET", authenticationInfo));

        } catch (Exception e) {
            Assert.fail("Should not have thrown a exception " + e.getMessage());
        }
    }

}
