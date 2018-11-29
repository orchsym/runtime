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
<div id="new-remote-process-group-dialog" class="hidden large-dialog">
    <div class="dialog-content">
        <div class="setting">
            <div class="setting-name">{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-new-remote.URL'] }}
                <div class="fa fa-question-circle" alt="Info" 
                     title="{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-new-remote.URL.title'] }}">    
                </div>
             </div>
            <div class="setting-field">
                <input id="new-remote-process-group-uris" type="text" placeholder="https://remotehost:8080/nifi"/>
            </div>
        </div>
        <div class="setting">
            <div class="remote-process-group-setting-left">
                <div class="setting-name">
                    {{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-new-remote.Transport'] }}
                <div class="fa fa-question-circle" alt="Info" 
                     title="{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-new-remote.Transport.title'] }}">
                </div>
                </div>
                <div class="setting-field">
                    <div id="new-remote-process-group-transport-protocol-combo"></div>
                </div>
            </div>
            <div class="remote-process-group-setting-right">
                <div class="setting-name">
                    {{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-new-remote.LocalNetwork'] }}
                    <div class="fa fa-question-circle" alt="Info" 
                         title="{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-new-remote.LocalNetwork.title'] }}">
                    </div>
                </div>
                <div class="setting-field">
                    <input type="text" class="small-setting-input" id="new-remote-process-group-local-network-interface"/>
                </div>
            </div>
            <div class="clear"></div>
        </div>
        <div class="setting">
            <div class="remote-process-group-setting-left">
                <div class="setting-name">
                    {{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-new-remote.HttpProxyHostname'] }}
                    <div class="fa fa-question-circle" alt="Info" title="{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-new-remote.HttpProxyHostname.title'] }}"></div>
                </div>
                <div class="setting-field">
                    <input type="text" class="small-setting-input" id="new-remote-process-group-proxy-host"/>
                </div>
            </div>
            <div class="remote-process-group-setting-right">
                <div class="setting-name">
                    {{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-new-remote.HttpProxyPort'] }}
                    <div class="fa fa-question-circle" alt="Info" title="{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-new-remote.HttpProxyPort.title'] }}"></div>
                </div>
                <div class="setting-field">
                    <input type="text" class="small-setting-input" id="new-remote-process-group-proxy-port"/>
                </div>
            </div>
            <div class="clear"></div>
        </div>
        <div class="setting">
            <div class="remote-process-group-setting-left">
                <div class="setting-name">
                    {{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-new-remote.HttpProxyUser'] }}
                    <div class="fa fa-question-circle" alt="Info" title="{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-new-remote.HttpProxyUser.title'] }}"></div>
                </div>
                <div class="setting-field">
                    <input type="text" class="small-setting-input" id="new-remote-process-group-proxy-user"/>
                </div>
            </div>
            <div class="remote-process-group-setting-right">
                <div class="setting-name">
                    {{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-new-remote.HttpProxyPassword'] }}
                    <div class="fa fa-question-circle" alt="Info" title="{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-new-remote.HttpProxyPassword.title'] }}"></div>
                </div>
                <div class="setting-field">
                    <input type="password" class="small-setting-input" id="new-remote-process-group-proxy-password"/>
                </div>
            </div>
            <div class="clear"></div>
        </div>
        <div class="setting">
            <div class="remote-process-group-setting-left">
                <div class="setting-name">
                    {{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-new-remote.timeout'] }}
                    <div class="fa fa-question-circle" alt="Info" title="{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-new-remote.timeout.title'] }}"></div>
                </div>
                <div class="setting-field">
                    <input type="text" class="small-setting-input" id="new-remote-process-group-timeout"/>
                </div>
            </div>
            <div class="remote-process-group-setting-right">
                <div class="setting-name">
                    {{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-new-remote.yield'] }}
                    <div class="fa fa-question-circle" alt="Info" title="{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-new-remote.yield.title'] }}"></div>
                </div>
                <div class="setting-field">
                    <input type="text" class="small-setting-input" id="new-remote-process-group-yield-duration"/>
                </div>
            </div>
            <div class="clear"></div>
        </div>
    </div>
</div>