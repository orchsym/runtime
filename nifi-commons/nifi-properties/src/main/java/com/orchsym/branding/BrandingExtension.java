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
package com.orchsym.branding;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BrandingExtension {
    private static final Logger logger = LoggerFactory.getLogger(BrandingExtension.class);

    private static volatile BrandingService INSTANCE;

    public static BrandingService get() {
        if (null == INSTANCE) {
            synchronized (BrandingExtension.class) {
                if (null == INSTANCE) {
                    try {
                        final ServiceLoader<BrandingService> serviceLoader = ServiceLoader.load(BrandingService.class);
                        final Iterator<BrandingService> iterator = serviceLoader.iterator();
                        if (iterator.hasNext()) { // only one
                            INSTANCE = iterator.next();
                        }
                    } catch (Throwable e) {
                        logger.error(e.getMessage(), e);
                        // ignore
                    }

                }
                if (null == INSTANCE) {
                    INSTANCE = new BrandingService() {
                    };
                }
            }
        }
        return INSTANCE;
    }
}
