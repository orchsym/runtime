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
<div class="component-panel" id="component-panel">

    <div class="graph-control right docked"
         ng-class="{true:'hidden',false:''}[appCtrl.serviceProvider.graphControlsCtrl.isComponentOpen]"
         style="position: absolute; width:32px; right:0; top:25px;">
        <div class="graph-control-docked pointer icon icon-processor" 
             title="组件" 
             ng-click="appCtrl.serviceProvider.graphControlsCtrl.openComponent()" 
             role="button" 
             tabindex="0" 
             style="">
        </div>
    </div>

    <div class="panel-main"
         ng-class="{true:'',false:'hidden'}[appCtrl.serviceProvider.graphControlsCtrl.isComponentOpen]">
      <div class="panel-main-left">
        <div class="main-left-header">
          <div class="collapse">
            <img src="images/youshou.svg"
                 ng-click="appCtrl.serviceProvider.graphControlsCtrl.closeComponent()">
          </div>
          {{appCtrl.serviceProvider.graphControlsCtrl.bigClassificationName}}
        </div>
        <div style="width:100%; height:52px; z-index:1; background-color: #F7F9FB; position: relative;">
          <div class="search-kuang">
            <img src="images/search.svg">
            <input type="text" 
                   placeholder="{{appCtrl.serviceProvider.graphControlsCtrl.bigClassificationName}}"
                   ng-model="appCtrl.serviceProvider.graphControlsCtrl.filterText"
                   ng-focus="appCtrl.serviceProvider.graphControlsCtrl.inputFocus()"
                   ng-change="appCtrl.serviceProvider.graphControlsCtrl.filter($event)" 
                   id="search-kuang-input">
          </div>
        </div>
        <div class="component-list" id="component-list">
          <div ng-repeat="item in appCtrl.serviceProvider.graphControlsCtrl.componentList">
              <div class="classification-title">
                <span class="shouzhan-outer">
                    <i class="shouzhan fa"
                       ng-class="{true:'fa-caret-down',false:'fa-caret-right'}[item.open]"
                       ng-click="appCtrl.serviceProvider.graphControlsCtrl.changeDomStatus(item)"></i>
                 </span>
                {{item.name}}
              </div>

              <div ng-class="{true:'',false:'hidden'}[item.open]" 
                   class="component-item" 
                   ng-repeat="component in item.components"
                   nf-draggable="appCtrl.serviceProvider.headerCtrl.toolboxCtrl.draggableRightComponentConfig(appCtrl.serviceProvider.graphControlsCtrl.componentDict[component.name],component.icon);"
                   ng-mouseover="appCtrl.serviceProvider.graphControlsCtrl.showTooltip($event, component.name, appCtrl.serviceProvider.graphControlsCtrl.componentDict[component.name].description)"
                   ng-mouseleave="appCtrl.serviceProvider.graphControlsCtrl.hiddenTootip()">
                <div class="component-img">
                  <img src="images/{{component.icon}}">
                </div>
                <div class="component-name">
                  {{component.name}}
                </div>
                <!-- <div class="component-info">
                  <div class="shang-caret"></div>
                  <span class="component-info-title">{{component.name}}</span>
                  <span class="component-info-content">{{appCtrl.serviceProvider.graphControlsCtrl.componentDict[component.name].description}}</span>
                </div> -->
              </div>
          </div>

          <div class="emptydata-warning" ng-class="{true:'hidden',false:''}[appCtrl.serviceProvider.graphControlsCtrl.componentList.length > 0]">
            未找到该组件
          </div>


        </div>
      </div>
      <div class="panel-main-right">
        <ul class="classification">
          <li ng-repeat="classification in appCtrl.serviceProvider.graphControlsCtrl.componentDuanwuData"
              ng-click="appCtrl.serviceProvider.graphControlsCtrl.toggleClassification(classification)"
              ng-class="{true:'active',false:''}[appCtrl.serviceProvider.graphControlsCtrl.bigClassificationName == classification.name]"
              title="{{classification.name}}">
            <img src=" images/{{classification.icon}}">
          </li>
        </ul>
      </div>
    </div>


    </div>