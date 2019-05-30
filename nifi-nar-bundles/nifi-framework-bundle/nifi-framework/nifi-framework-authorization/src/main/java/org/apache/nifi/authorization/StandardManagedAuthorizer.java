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
package org.apache.nifi.authorization;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.authorization.exception.AuthorizationAccessException;
import org.apache.nifi.authorization.exception.AuthorizerCreationException;
import org.apache.nifi.authorization.exception.AuthorizerDestructionException;
import org.apache.nifi.authorization.exception.UninheritableAuthorizationsException;
import org.apache.nifi.authorization.User;
import org.apache.nifi.authorization.resource.ResourceType;
import org.apache.nifi.authorization.user.NiFiUserUtils;
import org.apache.nifi.authorization.util.MD5Util;
import org.apache.nifi.authorization.ConfigurableUserGroupProvider;
import org.apache.nifi.authorization.ConfigurableAccessPolicyProvider;
import org.apache.nifi.components.PropertyValue;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

public class StandardManagedAuthorizer implements ManagedAuthorizer {

    private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
    private static final XMLOutputFactory XML_OUTPUT_FACTORY = XMLOutputFactory.newInstance();

    private static final String USER_GROUP_PROVIDER_ELEMENT = "userGroupProvider";
    private static final String ACCESS_POLICY_PROVIDER_ELEMENT = "accessPolicyProvider";

    private AccessPolicyProviderLookup accessPolicyProviderLookup;
    private AccessPolicyProvider accessPolicyProvider;
    private UserGroupProvider userGroupProvider;

    @Override
    public void initialize(AuthorizerInitializationContext initializationContext) throws AuthorizerCreationException {
        accessPolicyProviderLookup = initializationContext.getAccessPolicyProviderLookup();
    }

    @Override
    public void onConfigured(AuthorizerConfigurationContext configurationContext) throws AuthorizerCreationException {
        final PropertyValue accessPolicyProviderKey = configurationContext.getProperty("Access Policy Provider");
        if (!accessPolicyProviderKey.isSet()) {
            throw new AuthorizerCreationException("The Access Policy Provider must be set.");
        }

        accessPolicyProvider = accessPolicyProviderLookup.getAccessPolicyProvider(accessPolicyProviderKey.getValue());

        // ensure the desired access policy provider was found
        if (accessPolicyProvider == null) {
            throw new AuthorizerCreationException(String.format("Unable to locate configured Access Policy Provider: %s", accessPolicyProviderKey));
        }

        userGroupProvider = accessPolicyProvider.getUserGroupProvider();

        // ensure the desired access policy provider has a user group provider
        if (userGroupProvider == null) {
            throw new AuthorizerCreationException(String.format("Configured Access Policy Provider %s does not contain a User Group Provider", accessPolicyProviderKey));
        }
    }

    @Override
    public AuthorizationResult authorize(AuthorizationRequest request) throws AuthorizationAccessException {
        if (isAdmin(request.getIdentity())) {
            //if user is admin, gives all access rights.
            return AuthorizationResult.approved();
        }
        String resourceIdentifier = request.getResource().getIdentifier();
        //Resolve bug which provenance data not shown,it's from https://issues.apache.org/jira/browse/NIFI-5804
        if(resourceIdentifier.startsWith(ResourceType.ProvenanceData.getValue())){
            resourceIdentifier = ResourceType.Provenance.getValue();
        }
        final AccessPolicy policy = accessPolicyProvider.getAccessPolicy(resourceIdentifier, request.getAction());
        if (policy == null) {
            return AuthorizationResult.resourceNotFound();
        }

        final UserAndGroups userAndGroups = userGroupProvider.getUserAndGroups(request.getIdentity());

        final User user = userAndGroups.getUser();
        if (user == null) {
            if (request.isAnonymous()) {
                if (request.getRequestedResource().getName().equals("HandleHttpRequest")) {
                    //该请求可能是请求api的swagger信息，没有携带authentication信息，为匿名用户，给予通过
                    return AuthorizationResult.approved();
                }
                return AuthorizationResult.denied(String.format("user identity can't be '%s'.", request.getIdentity()));
            }
            User newUser = new User.Builder().identifier(generateUuid(request.getIdentity())).identity(request.getIdentity()).build();

            if(!autoGenerateUser(newUser)){
                return AuthorizationResult.denied("Unable to access!");
            }
            // create new user and add to userGroup
            ((ConfigurableUserGroupProvider)userGroupProvider).addUser(newUser);
            //set access pilicy for the new user
            ((ConfigurableAccessPolicyProvider) accessPolicyProvider).setUserDefaultAccesPilicy(newUser);
            
            return AuthorizationResult.approved();
        }

        final Set<Group> userGroups = userAndGroups.getGroups();
        if (policy.getUsers().contains(user.getIdentifier()) || containsGroup(userGroups, policy)) {
            return AuthorizationResult.approved();
        }
        return AuthorizationResult.denied(request.getExplanationSupplier().get());
    }

    public boolean isSecurityUserAutoGenerateEnabled() {
        org.apache.nifi.util.NiFiProperties nifiProperties = org.apache.nifi.util.NiFiProperties.createBasicNiFiProperties(null, null);
        return Boolean.parseBoolean(nifiProperties.getProperty("orchsym.security.user.auto.generate", Boolean.TRUE.toString())) ; //if not set, enable to generate by default
    }

    private boolean autoGenerateUser(User newUser) {
        try {
            boolean isSecurityUserAutoGenerateEnabled = isSecurityUserAutoGenerateEnabled();
            // boolean configured = ((ConfigurableUserGroupProvider)userGroupProvider).isConfigurable(newUser);
            // return isSecurityUserAutoGenerateEnabled || configured;
            return isSecurityUserAutoGenerateEnabled;
        } catch (Exception e) {
            return false;
        }
    }

    private String generateUuid(String key) {
        String md5Key = MD5Util.MD5(key);
        if (md5Key.length() < 20) {
            return md5Key;
        }
        String uuid = md5Key.substring(0, 8) + '-' + md5Key.substring(8, 12) + '-' + md5Key.substring(12, 16) + '-' + md5Key.substring(16, 20) + '-' + md5Key.substring(20);
        return uuid;
    }

    private boolean isAdmin(String requestIdentity) {
        final UserAndGroups userAndGroups = userGroupProvider.getUserAndGroups(requestIdentity);
        final User user = userAndGroups.getUser();
        if (user == null) {
            return false;
        }
        String userIdentity = user.getIdentity();
        String adminIdentity = NiFiUserUtils.getAdminIdentity();
        return userIdentity.equals(adminIdentity);
    }

    /**
     * Determines if the policy contains one of the user's groups.
     *
     * @param userGroups the set of the user's groups
     * @param policy the policy
     * @return true if one of the Groups in userGroups is contained in the policy
     */
    private boolean containsGroup(final Set<Group> userGroups, final AccessPolicy policy) {
        if (userGroups == null || userGroups.isEmpty() || policy.getGroups().isEmpty()) {
            return false;
        }

        for (Group userGroup : userGroups) {
            if (policy.getGroups().contains(userGroup.getIdentifier())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String getFingerprint() throws AuthorizationAccessException {
        XMLStreamWriter writer = null;
        final StringWriter out = new StringWriter();
        try {
            writer = XML_OUTPUT_FACTORY.createXMLStreamWriter(out);
            writer.writeStartDocument();
            writer.writeStartElement("managedAuthorizations");

            writer.writeStartElement(ACCESS_POLICY_PROVIDER_ELEMENT);
            if (accessPolicyProvider instanceof ConfigurableAccessPolicyProvider) {
                writer.writeCharacters(((ConfigurableAccessPolicyProvider) accessPolicyProvider).getFingerprint());
            }
            writer.writeEndElement();

            writer.writeStartElement(USER_GROUP_PROVIDER_ELEMENT);
            if (userGroupProvider instanceof ConfigurableUserGroupProvider) {
                writer.writeCharacters(((ConfigurableUserGroupProvider) userGroupProvider).getFingerprint());
            }
            writer.writeEndElement();

            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
        } catch (XMLStreamException e) {
            throw new AuthorizationAccessException("Unable to generate fingerprint", e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (XMLStreamException e) {
                    // nothing to do here
                }
            }
        }

        return out.toString();
    }

    @Override
    public void inheritFingerprint(String fingerprint) throws AuthorizationAccessException {
        if (StringUtils.isBlank(fingerprint)) {
            return;
        }

        final FingerprintHolder fingerprintHolder = parseFingerprint(fingerprint);

        if (StringUtils.isNotBlank(fingerprintHolder.getPolicyFingerprint()) && accessPolicyProvider instanceof ConfigurableAccessPolicyProvider) {
            ((ConfigurableAccessPolicyProvider) accessPolicyProvider).inheritFingerprint(fingerprintHolder.getPolicyFingerprint());
        }

        if (StringUtils.isNotBlank(fingerprintHolder.getUserGroupFingerprint()) && userGroupProvider instanceof ConfigurableUserGroupProvider) {
            ((ConfigurableUserGroupProvider) userGroupProvider).inheritFingerprint(fingerprintHolder.getUserGroupFingerprint());
        }
    }

    @Override
    public void checkInheritability(String proposedFingerprint) throws AuthorizationAccessException, UninheritableAuthorizationsException {
        final FingerprintHolder fingerprintHolder = parseFingerprint(proposedFingerprint);

        if (StringUtils.isNotBlank(fingerprintHolder.getPolicyFingerprint())) {
            if (accessPolicyProvider instanceof ConfigurableAccessPolicyProvider) {
                ((ConfigurableAccessPolicyProvider) accessPolicyProvider).checkInheritability(fingerprintHolder.getPolicyFingerprint());
            } else {
                throw new UninheritableAuthorizationsException("Policy fingerprint is not blank and the configured AccessPolicyProvider does not support fingerprinting.");
            }
        }

        if (StringUtils.isNotBlank(fingerprintHolder.getUserGroupFingerprint())) {
            if (userGroupProvider instanceof ConfigurableUserGroupProvider) {
                ((ConfigurableUserGroupProvider) userGroupProvider).checkInheritability(fingerprintHolder.getUserGroupFingerprint());
            } else {
                throw new UninheritableAuthorizationsException("User/Group fingerprint is not blank and the configured UserGroupProvider does not support fingerprinting.");
            }
        }
    }

    private final FingerprintHolder parseFingerprint(final String fingerprint) throws AuthorizationAccessException {
        final byte[] fingerprintBytes = fingerprint.getBytes(StandardCharsets.UTF_8);

        try (final ByteArrayInputStream in = new ByteArrayInputStream(fingerprintBytes)) {
            final DocumentBuilder docBuilder = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
            final Document document = docBuilder.parse(in);
            final Element rootElement = document.getDocumentElement();

            final NodeList accessPolicyProviderList = rootElement.getElementsByTagName(ACCESS_POLICY_PROVIDER_ELEMENT);
            if (accessPolicyProviderList.getLength() != 1) {
                throw new AuthorizationAccessException(String.format("Only one %s element is allowed: %s", ACCESS_POLICY_PROVIDER_ELEMENT, fingerprint));
            }

            final NodeList userGroupProviderList = rootElement.getElementsByTagName(USER_GROUP_PROVIDER_ELEMENT);
            if (userGroupProviderList.getLength() != 1) {
                throw new AuthorizationAccessException(String.format("Only one %s element is allowed: %s", USER_GROUP_PROVIDER_ELEMENT, fingerprint));
            }

            final Node accessPolicyProvider = accessPolicyProviderList.item(0);
            final Node userGroupProvider = userGroupProviderList.item(0);
            return new FingerprintHolder(accessPolicyProvider.getTextContent(), userGroupProvider.getTextContent());
        } catch (SAXException | ParserConfigurationException | IOException e) {
            throw new AuthorizationAccessException("Unable to parse fingerprint", e);
        }
    }

    @Override
    public AccessPolicyProvider getAccessPolicyProvider() {
        return accessPolicyProvider;
    }

    @Override
    public void preDestruction() throws AuthorizerDestructionException {

    }

    private static class FingerprintHolder {
        private final String policyFingerprint;
        private final String userGroupFingerprint;

        public FingerprintHolder(String policyFingerprint, String userGroupFingerprint) {
            this.policyFingerprint = policyFingerprint;
            this.userGroupFingerprint = userGroupFingerprint;
        }

        public String getPolicyFingerprint() {
            return policyFingerprint;
        }

        public String getUserGroupFingerprint() {
            return userGroupFingerprint;
        }
    }
}
