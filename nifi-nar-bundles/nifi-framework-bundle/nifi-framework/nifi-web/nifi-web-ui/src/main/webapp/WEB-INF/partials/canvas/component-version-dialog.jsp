<%--
 Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
--%>
<%@ page contentType="text/html" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<div id="component-version-dialog" layout="column" class="hidden">
    <div class="dialog-content">
        <div class="setting">
            <div class="setting-name"><fmt:message key="partials.canvas.component-version-dialog.name"/></div>
            <div class="setting-field">
                <div id="component-version-name"></div>
            </div>
        </div>
        <div class="setting">
            <div class="setting-name"><fmt:message key="partials.canvas.component-version-dialog.bundle"/></div>
            <div class="setting-field">
                <div id="component-version-bundle"></div>
            </div>
        </div>
        <div class="setting">
            <div class="setting-name"><fmt:message key="partials.canvas.component-version-dialog.version"/></div>
            <div class="setting-field">
                <div id="component-version-selector"></div>
            </div>
        </div>
        <div id="component-version-controller-service-apis-container" class="setting hidden">
            <div class="setting-name"><fmt:message key="partials.canvas.component-version-dialog.supports-controller-services"/></div>
            <div class="setting-field">
                <div id="component-version-controller-service-apis"></div>
            </div>
        </div>
        <div class="setting">
            <div class="setting-name"><fmt:message key="partials.canvas.component-version-dialog.tags"/></div>
            <div class="setting-field">
                <div id="component-version-tags"></div>
            </div>
        </div>
        <div class="setting">
            <div class="setting-name"><fmt:message key="partials.canvas.component-version-dialog.restriction"/></div>
            <div class="setting-field">
                <div id="component-version-restriction"></div>
            </div>
        </div>
        <div class="setting">
            <div class="setting-name"><fmt:message key="partials.canvas.component-version-dialog.description"/></div>
            <div class="setting-field">
                <div id="component-version-description"></div>
            </div>
        </div>
    </div>
</div>