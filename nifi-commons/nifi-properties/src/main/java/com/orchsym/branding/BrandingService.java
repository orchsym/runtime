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

import java.io.File;

public interface BrandingService {
    String DEFAULT_RUNTIME_NAME = "Orchsym Runtime";
    String DEFAULT_SHORT_RUNTIME_NAME = "Runtime";
    String DEFAULT_PRODUCT_NAME = "Orchsym Studio";
    String DEFAULT_SUPPORT_EMAIL = "orchsym-support@baishancloud.com";

    default String getRuntimeName() {
        return DEFAULT_RUNTIME_NAME;
    }

    default String getProductName() {
        return DEFAULT_PRODUCT_NAME;
    }

    default String getRootGroupName() {
        return getProductName();
    }

    default String getSupportEmail() {
        return DEFAULT_SUPPORT_EMAIL;
    }

    default void syncWebImages(File webImagesFolder) {
        //
    }

}
