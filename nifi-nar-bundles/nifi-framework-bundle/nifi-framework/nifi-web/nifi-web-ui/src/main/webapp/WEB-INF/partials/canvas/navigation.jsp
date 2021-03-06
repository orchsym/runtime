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
<div id="graph-controls">
    <div id="navigation-control" class="graph-control">
        <div class="graph-control-docked pointer fa fa-compass" title="{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-canvas-navigate.Navigate'] }}"
             ng-click="appCtrl.serviceProvider.graphControlsCtrl.expand($event)">
        </div>
        <div class="graph-control-header-container pointer"
             ng-click="appCtrl.serviceProvider.graphControlsCtrl.expand($event)">
            <div class="graph-control-header-icon fa fa-compass">
            </div>
            <div class="graph-control-header">{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-canvas-navigate.Navigate'] }}</div>
            <div class="graph-control-header-action">
                <div class="graph-control-expansion fa fa-plus-square-o pointer"></div>
            </div>
            <div class="clear"></div>
        </div>
        <div class="graph-control-content hidden">
            <div id="navigation-buttons">
                <!-- <fmt:message key="partials.canvas.navigation.zoom-in-button" var="zoomin"/> -->
                <div id="naviagte-zoom-in" class="action-button" title="{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-canvas-navigate.zoom-in-button'] }}"
                     ng-click="appCtrl.serviceProvider.graphControlsCtrl.navigateCtrl.zoomIn();">
                    <button><div class="graph-control-action-icon icon icon-zoom-in"></div></button>
                </div>
                <div class="button-spacer-small">&nbsp;</div>
                <!-- <fmt:message key="partials.canvas.navigation.zoom-out-button" var="zoomout"/> -->
                <div id="naviagte-zoom-out" class="action-button" title="{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-canvas-navigate.zoom-out-button'] }}"
                     ng-click="appCtrl.serviceProvider.graphControlsCtrl.navigateCtrl.zoomOut();">
                    <button><div class="graph-control-action-icon icon icon-zoom-out"></div></button>
                </div>
                <div class="button-spacer-large">&nbsp;</div>
                <!-- <fmt:message key="partials.canvas.navigation.zoom-fit-button" var="fit"/> -->
                <div id="naviagte-zoom-fit" class="action-button" title="{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-canvas-navigate.zoom-fit-button'] }}"
                     ng-click="appCtrl.serviceProvider.graphControlsCtrl.navigateCtrl.zoomFit();">
                    <button><div class="graph-control-action-icon icon icon-zoom-fit"></div></button>
                </div>
                <div class="button-spacer-small">&nbsp;</div>
                <!-- <fmt:message key="partials.canvas.navigation.zoom-actual-button" var="actual"/> -->
                <div id="naviagte-zoom-actual-size" class="action-button" title="{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-canvas-navigate.zoom-actual-button'] }}"
                     ng-click="appCtrl.serviceProvider.graphControlsCtrl.navigateCtrl.zoomActualSize();">
                    <button><div class="graph-control-action-icon icon icon-zoom-actual"></div></button>
                </div>
                <div class="clear"></div>
            </div>
            <div id="birdseye"></div>
        </div>
    </div>
    <div id="operation-control" class="graph-control">
        <fmt:message key="partials.canvas.navigation.Operate" var="operate"/>
        <div class="graph-control-docked pointer fa fa-hand-o-up" title="${operate}"
             ng-click="appCtrl.serviceProvider.graphControlsCtrl.expand($event)">
        </div>
        <div class="graph-control-header-container pointer"
             ng-click="appCtrl.serviceProvider.graphControlsCtrl.expand($event)">
            <div class="graph-control-header-icon fa fa-hand-o-up">
            </div>
            <div class="graph-control-header">{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-canvas-navigate.Operate'] }}</div>
            <div class="graph-control-header-action">
                <div class="graph-control-expansion fa fa-plus-square-o pointer"></div>
            </div>
            <div class="clear"></div>
        </div>
        <div class="graph-control-content hidden">
            <div id="operation-context">
                <div id="operation-context-logo">
                    <i class="icon" ng-class="appCtrl.serviceProvider.graphControlsCtrl.getContextIcon()"></i>
                </div>
                <div id="operation-context-details-container">
                    <div id="operation-context-name">{{appCtrl.serviceProvider.graphControlsCtrl.getContextName()}}</div>
                    <div id="operation-context-type" ng-class="appCtrl.serviceProvider.graphControlsCtrl.hide()">{{appCtrl.serviceProvider.graphControlsCtrl.getContextType()}}</div>
                </div>
                <div class="clear"></div>
                <div id="operation-context-id" ng-class="appCtrl.serviceProvider.graphControlsCtrl.hide()">{{appCtrl.serviceProvider.graphControlsCtrl.getContextId()}}</div>
            </div>
            <div id="operation-buttons">
                <div>
                    <!-- <fmt:message key="partials.canvas.navigation.configuration" var="configuration"/> -->
                    <div id="operate-configure" class="action-button" title="{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-canvas-navigate.Configuration'] }}">
                        <button ng-click="appCtrl.serviceProvider.graphControlsCtrl.openConfigureOrDetailsView();"
                                ng-disabled="!(appCtrl.serviceProvider.graphControlsCtrl.canConfigureOrOpenDetails())">
                            <div class="graph-control-action-icon icon icon-settings"></div></button>
                    </div>
                    <!-- <fmt:message key="partials.canvas.navigation.accesspolicies" var="accesspolicies"/> -->
                    <div class="button-spacer-small" ng-if="appCtrl.nf.CanvasUtils.isConfigurableAuthorizer()">&nbsp;</div>
                    <div id="operate-policy" class="action-button" 
                         title="{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-canvas-navigate.Enable'] }}" 
                         ng-if="appCtrl.nf.CanvasUtils.isConfigurableAuthorizer()">
                        <button ng-click="appCtrl.nf.Actions['managePolicies'](appCtrl.nf.CanvasUtils.getSelection());"
                                ng-disabled="!(appCtrl.nf.CanvasUtils.canManagePolicies())">
                            <div class="graph-control-action-icon fa fa-key"></div></button>
                    </div>
                    <div class="button-spacer-large">&nbsp;</div>
                    <!-- <fmt:message key="partials.canvas.navigation.enable" var="enable"/> -->
                    <div id="operate-enable" class="action-button" title="{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-canvas-navigate.Enable'] }}">
                        <button ng-click="appCtrl.nf.Actions['enable'](appCtrl.nf.CanvasUtils.getSelection());"
                                ng-disabled="!appCtrl.nf.CanvasUtils.getSelection().empty() && !appCtrl.nf.CanvasUtils.canModify(appCtrl.nf.CanvasUtils.getSelection());">
                            <div class="graph-control-action-icon icon icon-enable"></div></button>
                    </div>
                    <div class="button-spacer-small">&nbsp;</div>
                    <!-- <fmt:message key="partials.canvas.navigation.disable" var="disable"/> -->
                    <div id="operate-disable" class="action-button" title="{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-canvas-navigate.Disable'] }}">
                        <button ng-click="appCtrl.nf.Actions['disable'](appCtrl.nf.CanvasUtils.getSelection());"
                                ng-disabled="!appCtrl.nf.CanvasUtils.getSelection().empty() && !appCtrl.nf.CanvasUtils.canModify(appCtrl.nf.CanvasUtils.getSelection());">
                            <div class="graph-control-action-icon icon icon-enable-false"></div></button>
                    </div>
                    <div class="button-spacer-large">&nbsp;</div>
                    <!-- <fmt:message key="partials.canvas.navigation.start" var="start"/> -->
                    <div id="operate-start" class="action-button" title="{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-canvas-navigate.Start'] }}">
                        <button ng-click="appCtrl.nf.Actions['start'](appCtrl.nf.CanvasUtils.getSelection());"
                                ng-disabled="!appCtrl.nf.CanvasUtils.getSelection().empty() && !appCtrl.nf.CanvasUtils.canModify(appCtrl.nf.CanvasUtils.getSelection());">
                            <div class="graph-control-action-icon icon icon-start"></div></button>
                    </div>
                    <div class="button-spacer-small">&nbsp;</div>
                    <!-- <fmt:message key="partials.canvas.navigation.stop" var="stop"/> -->
                    <div id="operate-stop" class="action-button" title="{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-canvas-navigate.Stop'] }}">
                        <button ng-click="appCtrl.nf.Actions['stop'](appCtrl.nf.CanvasUtils.getSelection());"
                                ng-disabled="!appCtrl.nf.CanvasUtils.getSelection().empty() && !appCtrl.nf.CanvasUtils.canModify(appCtrl.nf.CanvasUtils.getSelection());">
                            <div class="graph-control-action-icon icon icon-stop"></div></button>
                    </div>
                    <div class="button-spacer-large">&nbsp;</div>
                    <!-- <fmt:message key="partials.canvas.navigation.createTemplate" var="createTemplate"/> -->
                    <div id="operate-template" class="action-button" title="{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-canvas-navigate.CreateTemplate'] }}">
                        <button ng-click="appCtrl.nf.Actions['template'](appCtrl.nf.CanvasUtils.getSelection());"
                                ng-disabled="!(appCtrl.nf.CanvasUtils.canWriteCurrentGroup() && (appCtrl.nf.CanvasUtils.getSelection().empty() || appCtrl.nf.CanvasUtils.canRead(appCtrl.nf.CanvasUtils.getSelection())));">
                            <div class="graph-control-action-icon icon icon-template-save"></div></button>
                    </div>
                    <div class="button-spacer-small">&nbsp;</div>
                    <!-- <fmt:message key="partials.canvas.navigation.uploadTemplate" var="uploadTemplate"/> -->
                    <div id="operate-template-upload" class="action-button" title="{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-canvas-navigate.UploadTemplate'] }}">
                        <button ng-click="appCtrl.nf.Actions['uploadTemplate']();"
                                ng-disabled="!(appCtrl.nf.CanvasUtils.canWriteCurrentGroup() && appCtrl.nf.CanvasUtils.getSelection().empty());">
                            <div class="graph-control-action-icon icon icon-template-import"></div></button>
                    </div>
                    <div class="clear"></div>
                </div>
                <div style="margin-top: 5px;">
                    <!-- <fmt:message key="partials.canvas.navigation.copy" var="copy"/> -->
                    <div id="operate-copy" class="action-button" title="{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-canvas-navigate.Copy'] }}">
                        <button ng-click="appCtrl.nf.Actions['copy'](appCtrl.nf.CanvasUtils.getSelection());"
                                ng-disabled="!appCtrl.nf.CanvasUtils.isCopyable(appCtrl.nf.CanvasUtils.getSelection());">
                            <div class="graph-control-action-icon icon icon-copy"></div></button>
                    </div>
                    <div class="button-spacer-small">&nbsp;</div>
                    <!-- <fmt:message key="partials.canvas.navigation.paste" var="paste"/> -->
                    <div id="operate-paste" class="action-button" title="{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-canvas-navigate.Paste'] }}">
                        <button ng-click="appCtrl.nf.Actions['paste'](appCtrl.nf.CanvasUtils.getSelection());"
                                ng-disabled="!appCtrl.nf.CanvasUtils.isPastable()">
                            <div class="graph-control-action-icon icon icon-paste"></div></button>
                    </div>
                    <div class="button-spacer-large">&nbsp;</div>
                    <!-- <fmt:message key="partials.canvas.navigation.group" var="group"/> -->
                    <div id="operate-group" class="action-button" title="{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-canvas-navigate.Group'] }}">
                        <button ng-click="appCtrl.nf.Actions['group'](appCtrl.nf.CanvasUtils.getSelection());"
                                ng-disabled="!(appCtrl.nf.CanvasUtils.getComponentByType('Connection').isDisconnected(appCtrl.nf.CanvasUtils.getSelection()) && appCtrl.nf.CanvasUtils.canModify(appCtrl.nf.CanvasUtils.getSelection()));">
                            <div class="graph-control-action-icon icon icon-group"></div></button>
                    </div>
                    <div class="button-spacer-large">&nbsp;</div>
                    <!-- <fmt:message key="partials.canvas.navigation.fillcolor" var="fillcolor"/> -->
                    <div id="operate-color" class="action-button" title="{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-canvas-navigate.fillcolor'] }}">
                        <button ng-click="appCtrl.nf.Actions['fillColor'](appCtrl.nf.CanvasUtils.getSelection());"
                                ng-disabled="!appCtrl.nf.CanvasUtils.isColorable(appCtrl.nf.CanvasUtils.getSelection());">
                            <div class="graph-control-action-icon icon icon-change-color"></div></button>
                    </div>
                    <div class="button-spacer-large">&nbsp;</div>
                    <!-- <fmt:message key="partials.canvas.navigation.expand" var="expand"/> -->
                    <div id="operate-expand" class="action-button" title="{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-canvas-navigate.Expand'] }}">
                        <button ng-click="appCtrl.nf.Actions['expand'](appCtrl.nf.CanvasUtils.getSelection());"
                                ng-disabled="!appCtrl.nf.CanvasUtils.areDeletable(appCtrl.nf.CanvasUtils.getSelection());">
                            <div class="graph-control-action-icon fa fa-plus"></div></button>
                    </div>
                    <div class="button-spacer-small">&nbsp;</div>
                    <!-- <fmt:message key="partials.canvas.navigation.collapse" var="collapse"/> -->
                    <div id="operate-collapse" class="action-button" title="{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-canvas-navigate.Collapse'] }}">
                        <button ng-click="appCtrl.nf.Actions['collapse'](appCtrl.nf.CanvasUtils.getSelection());"
                                ng-disabled="!appCtrl.nf.CanvasUtils.areDeletable(appCtrl.nf.CanvasUtils.getSelection());">
                            <div class="graph-control-action-icon fa fa-minus"></div></button>
                    </div>
                    <div class="button-spacer-large">&nbsp;</div>
                    <!-- <fmt:message key="partials.canvas.navigation.delete" var="delete"/> -->
                    <div id="operate-delete" class="action-button" title="{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-canvas-navigate.Delete'] }}">
                        <button ng-click="appCtrl.nf.Actions['delete'](appCtrl.nf.CanvasUtils.getSelection());"
                                ng-disabled="!appCtrl.nf.CanvasUtils.getSelection().empty() && !appCtrl.nf.CanvasUtils.canModify(appCtrl.nf.CanvasUtils.getSelection());">
                            <div class="graph-control-action-icon fa fa-trash"></div><span>{{ appCtrl.serviceProvider.globalMenuCtrl.constant['nf-canvas-navigate.Delete'] }}</span></button>
                    </div>
                    <div class="clear"></div>
                </div>
            </div>
        </div>
    </div>
</div>