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
package com.baishancloud.orchsym.sap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.annotation.lifecycle.OnDisabled;
import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.components.Validator;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.controller.ControllerServiceInitializationContext;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.reporting.InitializationException;

import com.baishancloud.orchsym.sap.i18n.Messages;
import com.baishancloud.orchsym.sap.option.ESAPLanguage;
import com.baishancloud.orchsym.sap.option.ESAPServerType;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.ext.DestinationDataProvider;

/**
 * Implementation of for SAP Connection Pooling Service.
 * 
 * @author GU Guoqiang
 */
public class SAPConnectionPool extends AbstractControllerService implements SAPConnectionPoolService {

    public static final PropertyDescriptor SERVER_TYPE = new PropertyDescriptor.Builder().name("Server Type") //$NON-NLS-1$
            .displayName(Messages.getString("SAPConnectionPool.ServerType"))// //$NON-NLS-1$
            .description(Messages.getString("SAPConnectionPool.ServerType_desc"))// //$NON-NLS-1$
            .required(true)//
            .defaultValue(ESAPServerType.AS_POOL.getValue()).allowableValues(ESAPServerType.getAll())//
            .addValidator(new Validator() {
                @Override
                public ValidationResult validate(final String subject, final String value, final ValidationContext context) {
                    return new ValidationResult.Builder().subject(subject).input(value)
                            .valid(value != null && !value.trim().isEmpty() && Arrays.asList(ESAPServerType.getAll()).stream().anyMatch(l -> l.getValue().equals(value)))
                            .explanation(Messages.getString("SAPConnectionPool.Invalid", subject)).build(); //$NON-NLS-1$
                }
            })//
            .build();

    public static final PropertyDescriptor HOST = new PropertyDescriptor.Builder().name("Host")//$NON-NLS-1$
            .displayName(Messages.getString("SAPConnectionPool.Host"))// //$NON-NLS-1$
            .description(Messages.getString("SAPConnectionPool.Host_desc"))// //$NON-NLS-1$
            .required(true)//
            .defaultValue(null)//
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
            .build();

    // for AS
    public static final PropertyDescriptor SYSNR = new PropertyDescriptor.Builder().name("System Number")//$NON-NLS-1$
            .displayName(Messages.getString("SAPConnectionPool.SystemNumber")) // //$NON-NLS-1$
            .description(Messages.getString("SAPConnectionPool.SystemNumber_desc"))// //$NON-NLS-1$
            .required(false)//
            .defaultValue("00")//$NON-NLS-1$
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
            .build();

    // for MS
    public static final PropertyDescriptor SYSID = new PropertyDescriptor.Builder().name("System Id")//$NON-NLS-1$
            .displayName(Messages.getString("SAPConnectionPool.SystemId")) // //$NON-NLS-1$
            .description(Messages.getString("SAPConnectionPool.SystemId_desc"))// //$NON-NLS-1$
            .required(false)//
            .defaultValue(null)//
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
            .build();
    // for MS
    public static final PropertyDescriptor GROUP = new PropertyDescriptor.Builder().name("Group Name")//$NON-NLS-1$
            .displayName(Messages.getString("SAPConnectionPool.Group")) // //$NON-NLS-1$
            .description(Messages.getString("SAPConnectionPool.Group_desc"))// //$NON-NLS-1$
            .required(false)//
            .defaultValue(null)//
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
            .build();

    public static final PropertyDescriptor CLIENT = new PropertyDescriptor.Builder().name("Client")//$NON-NLS-1$
            .displayName(Messages.getString("SAPConnectionPool.Client")) // //$NON-NLS-1$
            .description(Messages.getString("SAPConnectionPool.Client_desc"))// //$NON-NLS-1$
            .required(true)//
            .defaultValue("100")//$NON-NLS-1$
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
            .build();

    public static final PropertyDescriptor USER = new PropertyDescriptor.Builder().name("User")//$NON-NLS-1$
            .displayName(Messages.getString("SAPConnectionPool.User")) // //$NON-NLS-1$
            .description(Messages.getString("SAPConnectionPool.User_desc"))// //$NON-NLS-1$
            .required(false)//
            .defaultValue(null)//
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
            .build();

    public static final PropertyDescriptor PASSWORD = new PropertyDescriptor.Builder().name("Password")//$NON-NLS-1$
            .displayName(Messages.getString("SAPConnectionPool.Password")) // //$NON-NLS-1$
            .description(Messages.getString("SAPConnectionPool.Password_desc"))// //$NON-NLS-1$
            .required(false)//
            .defaultValue(null).sensitive(true)//
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
            .build();

    public static final PropertyDescriptor LANGUAGE = new PropertyDescriptor.Builder().name("Language")//$NON-NLS-1$
            .displayName(Messages.getString("SAPConnectionPool.Language")) // //$NON-NLS-1$
            .description(Messages.getString("SAPConnectionPool.Language_desc")) // //$NON-NLS-1$
            .required(false)//
            .defaultValue(ESAPLanguage.ZH.getValue()).allowableValues(ESAPLanguage.getAll())//
            .addValidator(new Validator() {
                @Override
                public ValidationResult validate(final String subject, final String value, final ValidationContext context) {
                    return new ValidationResult.Builder().subject(subject).input(value)
                            .valid(value != null && !value.trim().isEmpty() && Arrays.asList(ESAPLanguage.getAll()).stream().anyMatch(l -> l.getValue().equals(value.toUpperCase())))
                            .explanation(Messages.getString("SAPConnectionPool.Invalid", subject)).build();
                }
            })//
            .build();

    public static final PropertyDescriptor POOL_CAPACITY = new PropertyDescriptor.Builder().name("Max idle connections")//$NON-NLS-1$
            .displayName(Messages.getString("SAPConnectionPool.PoolCapacity")) // //$NON-NLS-1$
            .description(Messages.getString("SAPConnectionPool.PoolCapacity_desc"))// //$NON-NLS-1$
            .required(false)//
            .defaultValue("3")//$NON-NLS-1$
            .addValidator(StandardValidators.NON_NEGATIVE_INTEGER_VALIDATOR)//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
            .build();

    public static final PropertyDescriptor PEAK_LIMIT = new PropertyDescriptor.Builder().name("Max active connections")//$NON-NLS-1$
            .displayName(Messages.getString("SAPConnectionPool.PeakLimit")) // //$NON-NLS-1$
            .description(Messages.getString("SAPConnectionPool.PeakLimit_desc"))// //$NON-NLS-1$
            .required(false)//
            .defaultValue("10")//$NON-NLS-1$
            .addValidator(StandardValidators.NON_NEGATIVE_INTEGER_VALIDATOR)//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
            .build();

    public static final PropertyDescriptor ROUTER = new PropertyDescriptor.Builder().name("Router")//$NON-NLS-1$
            .displayName(Messages.getString("SAPConnectionPool.Router")) // //$NON-NLS-1$
            .description(Messages.getString("SAPConnectionPool.Router_desc"))// //$NON-NLS-1$
            .required(false)//
            .defaultValue(null)//
            .addValidator(Validator.VALID)//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
            .build();

    protected List<PropertyDescriptor> properties;

    protected volatile JCoDestination destination;

    protected volatile ESAPServerType serverType;
    protected volatile Properties clientProperties;

    @Override
    protected void init(ControllerServiceInitializationContext config) throws InitializationException {
        final List<PropertyDescriptor> props = new ArrayList<>();
        props.add(SERVER_TYPE);
        props.add(HOST);
        props.add(SYSNR);
        props.add(SYSID);
        props.add(GROUP);
        props.add(CLIENT);
        props.add(USER);
        props.add(PASSWORD);
        props.add(LANGUAGE);
        props.add(POOL_CAPACITY);
        props.add(PEAK_LIMIT);
        props.add(ROUTER);

        properties = Collections.unmodifiableList(props);
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return properties;
    }

    @OnEnabled
    public void onConfigured(final ConfigurationContext context) throws InitializationException {
        serverType = ESAPServerType.indexOf(context.getProperty(SERVER_TYPE).getValue());

        final String host = context.getProperty(HOST).evaluateAttributeExpressions().getValue();

        final String sysnr = context.getProperty(SYSNR).evaluateAttributeExpressions().getValue();
        final String sysid = context.getProperty(SYSID).evaluateAttributeExpressions().getValue();
        final String group = context.getProperty(GROUP).evaluateAttributeExpressions().getValue();

        final String client = context.getProperty(CLIENT).evaluateAttributeExpressions().getValue();
        final String user = context.getProperty(USER).evaluateAttributeExpressions().getValue();
        final String password = context.getProperty(PASSWORD).evaluateAttributeExpressions().getValue();
        final String language = context.getProperty(LANGUAGE).evaluateAttributeExpressions().getValue();

        final Integer poolCap = context.getProperty(POOL_CAPACITY).evaluateAttributeExpressions().asInteger();
        final Integer peakLimit = context.getProperty(PEAK_LIMIT).evaluateAttributeExpressions().asInteger();

        final String router = context.getProperty(ROUTER).evaluateAttributeExpressions().getValue();

        clientProperties = new Properties();
        clientProperties.setProperty(DestinationDataProvider.JCO_CLIENT, client);
        clientProperties.setProperty(DestinationDataProvider.JCO_USER, user);
        clientProperties.setProperty(DestinationDataProvider.JCO_PASSWD, password);
        clientProperties.setProperty(DestinationDataProvider.JCO_LANG, language);

        if (StringUtils.isNotBlank(router))
            clientProperties.setProperty(DestinationDataProvider.JCO_SAPROUTER, router);

        switch (serverType) {
        case AS: // Application Server
            checkEmptyProperty(SYSNR, sysnr);

            Properties aSConnectionProps = loadASConnectionProps(host, sysnr);
            clientProperties.putAll(aSConnectionProps);
            break;
        case AS_POOL:// Application Server with pool
            checkEmptyProperty(SYSNR, sysnr);

            Properties aSWithPoolConnectionProps = loadASWithPoolConnectionProps(host, sysnr, poolCap, peakLimit);
            clientProperties.putAll(aSWithPoolConnectionProps);
            break;
        case MS:// Message Server without pool
            checkEmptyProperty(SYSID, sysid);
            checkEmptyProperty(GROUP, group);

            Properties mSConnectionProps = loadMSConnectionProps(host, sysid, group);
            clientProperties.putAll(mSConnectionProps);
            break;
        }

        SAPDataManager.getInstance().updateClientProp(this.getIdentifier(), serverType, clientProperties);
    }

    protected void checkEmptyProperty(PropertyDescriptor pd, String value) throws InitializationException {
        if (StringUtils.isBlank(value)) { // can't be empty for AS
            throw new InitializationException(Messages.getString("SAPConnectionPool.RequiredProp", pd.getDisplayName())); //$NON-NLS-1$
        }
    }

    /**
     * ABAP Application Server without pool
     */
    protected Properties loadASConnectionProps(String asHost, String sysnr) {
        Properties connectProperties = new Properties();
        connectProperties.setProperty(DestinationDataProvider.JCO_ASHOST, asHost);
        connectProperties.setProperty(DestinationDataProvider.JCO_SYSNR, sysnr);

        return connectProperties;
    }

    /**
     * ABAP Application Server with pool
     */
    protected Properties loadASWithPoolConnectionProps(String asHost, String sysnr, Integer poolCap, Integer peakLimit) {
        Properties connectProperties = loadASConnectionProps(asHost, sysnr);

        connectProperties.setProperty(DestinationDataProvider.JCO_POOL_CAPACITY, poolCap.toString());
        connectProperties.setProperty(DestinationDataProvider.JCO_PEAK_LIMIT, peakLimit.toString());

        return connectProperties;
    }

    /**
     * ABAP Message Server without pool
     */
    protected Properties loadMSConnectionProps(String msHost, String r3name, String group) {
        Properties connectProperties = new Properties();
        connectProperties.setProperty(DestinationDataProvider.JCO_MSHOST, msHost);
        connectProperties.setProperty(DestinationDataProvider.JCO_R3NAME, r3name);
        connectProperties.setProperty(DestinationDataProvider.JCO_GROUP, group);

        return connectProperties;
    }

    /**
     * Shutdown pool, close all open connections.
     */
    @OnDisabled
    public void shutdown() {
        destination = null;
        if (serverType != null)
            SAPDataManager.getInstance().updateClientProp(this.getIdentifier(), serverType, null);
    }

    public void connect() throws SAPException {
        try {
            destination = SAPDataManager.getInstance().getDestination(this.getIdentifier(), serverType); // registry

            destination.ping();
        } catch (JCoException e) {
            throw new SAPException(Messages.getString("SAPConnectionPool.Disconnect"), e); //$NON-NLS-1$
        }
    }

    public boolean isConnected() {
        if (destination != null) {
            try {
                destination.ping();
                return true;
            } catch (JCoException e) {
                //
            }
        }
        return false;
    }

    @Override
    public Map<String, String> getAttributes() throws SAPException {
        return Collections.emptyMap();
    }
}
