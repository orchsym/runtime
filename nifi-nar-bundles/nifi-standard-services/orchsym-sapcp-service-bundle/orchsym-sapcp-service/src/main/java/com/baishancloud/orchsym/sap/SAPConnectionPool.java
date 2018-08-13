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
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.annotation.behavior.DynamicProperty;
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
import com.sap.conn.jco.ext.DestinationDataProvider;

/**
 * Implementation of for SAP Connection Pooling Service.
 * 
 * @author GU Guoqiang
 */
@DynamicProperty(name = "A SAP connection property", value = "A property for SAP connection", description = "Can set the additional properties for SAP to connection")
public abstract class SAPConnectionPool extends AbstractControllerService implements SAPConnectionPoolService {

    public static final PropertyDescriptor SERVER_TYPE = new PropertyDescriptor.Builder().name("server-type") //$NON-NLS-1$
            .displayName(Messages.getString("SAPConnectionPool.ServerType"))// //$NON-NLS-1$
            .description(Messages.getString("SAPConnectionPool.ServerType_desc"))// //$NON-NLS-1$
            .required(true)//
            .defaultValue(ESAPServerType.ASP.getValue()).allowableValues(ESAPServerType.getAll())//
            .addValidator(new Validator() {
                @Override
                public ValidationResult validate(final String subject, final String value, final ValidationContext context) {
                    return new ValidationResult.Builder().subject(subject).input(value)
                            .valid(value != null && !value.trim().isEmpty() && Arrays.asList(ESAPServerType.getAll()).stream().anyMatch(l -> l.getValue().equals(value)))
                            .explanation(Messages.getString("SAPConnectionPool.Invalid", subject)).build(); //$NON-NLS-1$
                }
            })//
            .build();

    public static final PropertyDescriptor HOST = new PropertyDescriptor.Builder().name("host")//$NON-NLS-1$
            .displayName(Messages.getString("SAPConnectionPool.Host"))// //$NON-NLS-1$
            .description(Messages.getString("SAPConnectionPool.Host_desc"))// //$NON-NLS-1$
            .required(true)//
            .defaultValue(null)//
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
            .build();

    // for AS
    public static final PropertyDescriptor SYSNR = new PropertyDescriptor.Builder().name("sysnr")//$NON-NLS-1$
            .displayName(Messages.getString("SAPConnectionPool.SystemNumber")) // //$NON-NLS-1$
            .description(Messages.getString("SAPConnectionPool.SystemNumber_desc"))// //$NON-NLS-1$
            .required(false)//
            .defaultValue("00")//$NON-NLS-1$
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
            .build();

    // for MS
    public static final PropertyDescriptor SYSID = new PropertyDescriptor.Builder().name("r3name")//$NON-NLS-1$
            .displayName(Messages.getString("SAPConnectionPool.SystemId")) // //$NON-NLS-1$
            .description(Messages.getString("SAPConnectionPool.SystemId_desc"))// //$NON-NLS-1$
            .required(false)//
            .defaultValue(null)//
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
            .build();
    // for MS
    public static final PropertyDescriptor GROUP = new PropertyDescriptor.Builder().name("group")//$NON-NLS-1$
            .displayName(Messages.getString("SAPConnectionPool.Group")) // //$NON-NLS-1$
            .description(Messages.getString("SAPConnectionPool.Group_desc"))// //$NON-NLS-1$
            .required(false)//
            .defaultValue(null)//
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
            .build();

    public static final PropertyDescriptor CLIENT = new PropertyDescriptor.Builder().name("client")//$NON-NLS-1$
            .displayName(Messages.getString("SAPConnectionPool.Client")) // //$NON-NLS-1$
            .description(Messages.getString("SAPConnectionPool.Client_desc"))// //$NON-NLS-1$
            .required(true)//
            .defaultValue("100")//$NON-NLS-1$
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
            .build();

    public static final PropertyDescriptor USER = new PropertyDescriptor.Builder().name("user")//$NON-NLS-1$
            .displayName(Messages.getString("SAPConnectionPool.User")) // //$NON-NLS-1$
            .description(Messages.getString("SAPConnectionPool.User_desc"))// //$NON-NLS-1$
            .required(false)//
            .defaultValue(null)//
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
            .build();

    public static final PropertyDescriptor PASSWORD = new PropertyDescriptor.Builder().name("passwd")//$NON-NLS-1$
            .displayName(Messages.getString("SAPConnectionPool.Password")) // //$NON-NLS-1$
            .description(Messages.getString("SAPConnectionPool.Password_desc"))// //$NON-NLS-1$
            .required(false)//
            .defaultValue(null).sensitive(true)//
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
            .build();

    public static final PropertyDescriptor LANGUAGE = new PropertyDescriptor.Builder().name("lang")//$NON-NLS-1$
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

    public static final PropertyDescriptor POOL_CAPACITY = new PropertyDescriptor.Builder().name("pool_capacity")//$NON-NLS-1$
            .displayName(Messages.getString("SAPConnectionPool.PoolCapacity")) // //$NON-NLS-1$
            .description(Messages.getString("SAPConnectionPool.PoolCapacity_desc"))// //$NON-NLS-1$
            .required(false)//
            .defaultValue("3")//$NON-NLS-1$
            .addValidator(StandardValidators.NON_NEGATIVE_INTEGER_VALIDATOR)//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
            .build();

    public static final PropertyDescriptor PEAK_LIMIT = new PropertyDescriptor.Builder().name("peak_limit")//$NON-NLS-1$
            .displayName(Messages.getString("SAPConnectionPool.PeakLimit")) // //$NON-NLS-1$
            .description(Messages.getString("SAPConnectionPool.PeakLimit_desc"))// //$NON-NLS-1$
            .required(false)//
            .defaultValue("10")//$NON-NLS-1$
            .addValidator(StandardValidators.NON_NEGATIVE_INTEGER_VALIDATOR)//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
            .build();

    public static final PropertyDescriptor ROUTER = new PropertyDescriptor.Builder().name("saprouter")//$NON-NLS-1$
            .displayName(Messages.getString("SAPConnectionPool.Router")) // //$NON-NLS-1$
            .description(Messages.getString("SAPConnectionPool.Router_desc"))// //$NON-NLS-1$
            .required(false)//
            .defaultValue(null)//
            .addValidator(Validator.VALID)//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
            .build();

    public static final PropertyDescriptor CODEPAGE = new PropertyDescriptor.Builder().name("codepage")//$NON-NLS-1$
            .displayName(Messages.getString("SAPConnectionPool.CodePage")) // //$NON-NLS-1$
            .description(Messages.getString("SAPConnectionPool.CodePage_desc"))// //$NON-NLS-1$
            .required(false)//
            .defaultValue("8400")//
            .addValidator(StandardValidators.NON_NEGATIVE_INTEGER_VALIDATOR)//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
            .build();

    public static final PropertyDescriptor EXPIRATION_TIME = new PropertyDescriptor.Builder().name("expiration_time")//$NON-NLS-1$
            .displayName(Messages.getString("SAPConnectionPool.ExpirationTime")) // //$NON-NLS-1$
            .description(Messages.getString("SAPConnectionPool.ExpirationTime_desc"))// //$NON-NLS-1$
            .required(false)//
            .defaultValue("5 min")// 5 min
            .addValidator(StandardValidators.createTimePeriodValidator(1, TimeUnit.MILLISECONDS, Integer.MAX_VALUE, TimeUnit.MINUTES))//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
            .build();

    protected List<PropertyDescriptor> properties;

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
        props.add(CODEPAGE);
        props.add(EXPIRATION_TIME);

        properties = Collections.unmodifiableList(props);
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return properties;
    }

    @Override
    protected PropertyDescriptor getSupportedDynamicPropertyDescriptor(final String propertyDescriptorName) {
        return new PropertyDescriptor.Builder().name(propertyDescriptorName)//
                .expressionLanguageSupported(ExpressionLanguageScope.NONE)//
                .addValidator(Validator.VALID)//
                .required(false).dynamic(true).build();
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
        final String codepage = context.getProperty(CODEPAGE).evaluateAttributeExpressions().getValue();
        final int expirTime = context.getProperty(EXPIRATION_TIME).evaluateAttributeExpressions().asTimePeriod(TimeUnit.MILLISECONDS).intValue();

        clientProperties = new Properties();
        clientProperties.setProperty(DestinationDataProvider.JCO_CLIENT, client);
        clientProperties.setProperty(DestinationDataProvider.JCO_USER, user);
        clientProperties.setProperty(DestinationDataProvider.JCO_PASSWD, password);
        clientProperties.setProperty(DestinationDataProvider.JCO_LANG, language);
        clientProperties.setProperty(DestinationDataProvider.JCO_CODEPAGE, codepage);
        clientProperties.setProperty(DestinationDataProvider.JCO_EXPIRATION_TIME, String.valueOf(expirTime));
        clientProperties.setProperty(DestinationDataProvider.JCO_EXPIRATION_PERIOD, String.valueOf(60000)); // 1min by default
        clientProperties.setProperty(DestinationDataProvider.JCO_MAX_GET_TIME, String.valueOf(30000)); // 30s by default

        // set Dynamic properties
        context.getProperties().entrySet().stream().filter(e -> e.getKey().isDynamic()).forEach(e -> {
            clientProperties.setProperty(e.getKey().getName(), e.getValue());
        });

        if (StringUtils.isNotBlank(router))
            clientProperties.setProperty(DestinationDataProvider.JCO_SAPROUTER, router);

        switch (serverType) {
        case AS: // Application Server
            checkEmptyProperty(SYSNR, sysnr);

            Properties aSConnectionProps = loadASConnectionProps(host, sysnr);
            clientProperties.putAll(aSConnectionProps);
            break;
        case ASP:// Application Server with pool
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
        //
    }

    @Override
    public Map<String, String> getAttributes() throws SAPException {
        return Collections.emptyMap();
    }
}
