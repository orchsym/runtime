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
<div id="port-configuration" class="hidden medium-dialog">
    <div class="dialog-content">
        <div class="port-setting">
            <div class="setting-name">{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-port-configuration.PortNumber'] }}</div>
            <div class="setting-field">
                <div id="port-name-container">
                    <input type="text" id="port-name"/>
                    <div class="port-enabled-container">
                        <div id="port-enabled" class="port-enabled nf-checkbox checkbox-unchecked"></div>
                        <span class="nf-checkbox-label">{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-port-configuration.Enabled'] }}</span>
                    </div>
                </div>
            </div>
        </div>
        <div class="port-setting">
            <div class="setting-name">
                {{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-port-configuration.Id'] }}
            </div>
            <div class="setting-field">
                <span id="port-id"></span>
            </div>
        </div>
        <div id="port-concurrent-task-container" class="port-setting">
            <div class="setting-name">
                {{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-port-configuration.ConcurrentTasks'] }}
                <div class="fa fa-question-circle" alt="Info" 
                     title="{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-port-configuration.ConcurrentTasks.title'] }}">
                </div>
            </div>
            <div class="setting-field">
                <input type="text" id="port-concurrent-tasks" class="port-field"></divnput>
            </div>
        </div>
        <div class="port-setting">
            <div class="setting-name">{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-port-configuration.Comments'] }}</div>
            <div class="setting-field">
                <textarea cols="30" rows="4" id="port-comments" class="port-field"></textarea>
            </div>
        </div>
    </div>
</div>