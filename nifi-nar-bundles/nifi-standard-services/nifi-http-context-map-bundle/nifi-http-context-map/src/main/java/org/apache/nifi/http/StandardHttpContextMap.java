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
package org.apache.nifi.http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnDisabled;
import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.annotation.lifecycle.OnShutdown;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.PropertyValue;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.registry.api.APIServicesManager;
import org.apache.nifi.util.FormatUtils;

@Tags({ "http", "request", "response" })
@SeeAlso(classNames = { "org.apache.nifi.processors.standard.HandleHttpRequest", "org.apache.nifi.processors.standard.HandleHttpResponse" })
@CapabilityDescription("Provides the ability to store and retrieve HTTP requests and responses external to a Processor, so that " //
        + "multiple Processors can interact with the same HTTP request.")
public class StandardHttpContextMap extends AbstractControllerService implements HttpContextMap {

    public static final PropertyDescriptor MAX_OUTSTANDING_REQUESTS = new PropertyDescriptor.Builder()//
            .name("Maximum Outstanding Requests")//
            .description("The maximum number of HTTP requests that can be outstanding at any one time. Any attempt to register an additional HTTP Request will cause an error")//
            .required(true)//
            .addValidator(StandardValidators.POSITIVE_INTEGER_VALIDATOR)//
            .defaultValue("5000")//
            .build();
    public static final PropertyDescriptor REQUEST_EXPIRATION = new PropertyDescriptor.Builder()//
            .name("Request Expiration")//
            .description("Specifies how long an HTTP Request should be left unanswered before being evicted from the cache and being responded to with a Service Unavailable status code")//
            .required(true)//
            .expressionLanguageSupported(ExpressionLanguageScope.NONE)//
            .defaultValue("1 min")//
            .addValidator(StandardValidators.TIME_PERIOD_VALIDATOR)//
            .build();

    private final ConcurrentMap<String, Wrapper> wrapperMap = new ConcurrentHashMap<>();

    private volatile int maxSize = 5000;
    private volatile long maxRequestNanos;
    private volatile ScheduledExecutorService executor;

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        final List<PropertyDescriptor> properties = new ArrayList<>(2);
        properties.add(MAX_OUTSTANDING_REQUESTS);
        properties.add(REQUEST_EXPIRATION);
        return properties;
    }

    @Override
    public void onPropertyModified(PropertyDescriptor descriptor, String oldValue, String newValue) {
        super.onPropertyModified(descriptor, oldValue, newValue);

        if (descriptor.equals(REQUEST_EXPIRATION)) {
            updateApiManager(newValue);
        }
    }

    @OnEnabled
    public void onConfigured(final ConfigurationContext context) {
        maxSize = context.getProperty(MAX_OUTSTANDING_REQUESTS).asInteger();
        executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(final Runnable r) {
                final Thread thread = Executors.defaultThreadFactory().newThread(r);
                thread.setName("StandardHttpContextMap-" + getIdentifier());
                return thread;
            }
        });

        final PropertyValue requestExpirationProp = context.getProperty(REQUEST_EXPIRATION);
        maxRequestNanos = requestExpirationProp.asTimePeriod(TimeUnit.NANOSECONDS);
        final long scheduleNanos = maxRequestNanos / 2;
        executor.scheduleWithFixedDelay(new CleanupExpiredRequests(), scheduleNanos, scheduleNanos, TimeUnit.NANOSECONDS);

        updateApiManager(requestExpirationProp.getValue());
    }

    private void updateApiManager(String requestExpirationValue) {
        if (requestExpirationValue == null || requestExpirationValue.trim().isEmpty()) {
            return;
        }
        final String identifier = this.getIdentifier();

        final Long requestTimeout = FormatUtils.getTimeDuration(requestExpirationValue.trim(), TimeUnit.MILLISECONDS);// the time unit must be same as HandleHttpRequest
        final APIServicesManager apiManager = APIServicesManager.getInstance();
        apiManager.getInfos().stream().filter(info -> identifier.equals(info.controllerServiceId)).forEach(info -> info.requestTimeout = requestTimeout);
    }

    @OnShutdown
    @OnDisabled
    public void cleanup() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    @Override
    public boolean register(final String identifier, final HttpServletRequest request, final HttpServletResponse response, final AsyncContext context) {
        return register(identifier, request, response, context, Collections.emptyMap());
    }

    @Override
    public boolean register(String identifier, HttpServletRequest request, HttpServletResponse response, AsyncContext context, Map<String, Object> additions) {
        // fail if there are too many already. Maybe add a configuration property for how many
        // outstanding, with a default of say 5000
        if (wrapperMap.size() >= maxSize) {
            return false;
        }
        final Wrapper wrapper = new Wrapper(request, response, context, additions);
        final Wrapper existing = wrapperMap.putIfAbsent(identifier, wrapper);
        if (existing != null) {
            throw new IllegalStateException("HTTP Request already registered with identifier " + identifier);
        }

        return true;
    }

    @Override
    public HttpServletResponse getResponse(final String identifier) {
        final Wrapper wrapper = wrapperMap.get(identifier);
        if (wrapper == null) {
            return null;
        }

        return wrapper.getResponse();
    }

    @Override
    public Map<String, Object> getAdditions(String identifier) {
        final Wrapper wrapper = wrapperMap.get(identifier);
        if (wrapper == null) {
            return Collections.emptyMap();
        }

        return wrapper.getAdditions();
    }

    @Override
    public void complete(final String identifier) {
        final Wrapper wrapper = wrapperMap.remove(identifier);
        if (wrapper == null) {
            throw new IllegalStateException("No HTTP Request registered with identifier " + identifier);
        }

        wrapper.getAsync().complete();
    }

    private static class Wrapper {

        @SuppressWarnings("unused")
        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private final AsyncContext async;
        private final long nanoTimeAdded = System.nanoTime();
        private final Map<String, Object> additions;

        public Wrapper(final HttpServletRequest request, final HttpServletResponse response, final AsyncContext async) {
            this(request, response, async, Collections.emptyMap());
        }

        public Wrapper(final HttpServletRequest request, final HttpServletResponse response, final AsyncContext async, Map<String, Object> additions) {
            this.request = request;
            this.response = response;
            this.async = async;
            this.additions = additions;
        }

        public HttpServletResponse getResponse() {
            return response;
        }

        public AsyncContext getAsync() {
            return async;
        }

        public Map<String, Object> getAdditions() {
            return additions;
        }

        public long getNanoTimeAdded() {
            return nanoTimeAdded;
        }
    }

    private class CleanupExpiredRequests implements Runnable {

        @Override
        public void run() {
            final long now = System.nanoTime();
            final long threshold = now - maxRequestNanos;

            final Iterator<Map.Entry<String, Wrapper>> itr = wrapperMap.entrySet().iterator();
            while (itr.hasNext()) {
                final Map.Entry<String, Wrapper> entry = itr.next();
                if (entry.getValue().getNanoTimeAdded() < threshold) {
                    itr.remove();

                    // send SERVICE_UNAVAILABLE
                    try {
                        final AsyncContext async = entry.getValue().getAsync();
                        ((HttpServletResponse) async.getResponse()).sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                        async.complete();
                    } catch (final Exception e) {
                        // we are trying to indicate that we are unavailable. If we have an exception and cannot respond,
                        // then so be it. Nothing to really do here.
                    }
                }
            }
        }
    }

    @Override
    public long getRequestTimeout(final TimeUnit timeUnit) {
        return timeUnit.convert(maxRequestNanos, TimeUnit.NANOSECONDS);
    }
}
