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
<div id="user-policies-dialog" class="hidden large-dialog">
    <div class="dialog-content">
        <div class="setting">
            <div class="setting-name"><fmt:message key="partials.users.user-policies-dialog.User"/></div>
            <div class="setting-field">
                <div id="policies-dialog-user-name"></div>
            </div>
        </div>
        <fmt:message key="partials.users.user-policies-dialog.user-policy-message" var="userPolicyMessage"/>
        <div id="user-policy-message" title="${userPolicyMessage}">
            <fmt:message key="partials.users.user-policies-dialog.user-policy-message"/>
        </div>
        <div id="user-policies-table"></div>
    </div>
</div>