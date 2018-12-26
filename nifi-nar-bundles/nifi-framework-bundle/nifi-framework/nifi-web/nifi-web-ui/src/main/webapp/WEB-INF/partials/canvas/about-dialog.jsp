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
<div id="nf-about">
    <div id="nf-about-pic-container">
        <div id="nf-about-pic" layout="column" layout-align="center center">
            <div id="about-logo" layout="column" layout-align="end center" style="width:50%;height:40%">
                <img src="./images/logo.svg"/>
            </div>
            <div id="about-version" layout="row" layout-align="end end" style="width:100%;height:40%">
                <p style="font-size:12px; color:white; padding-right: 10px">{{appCtrl.serviceProvider.globalMenuCtrl.getProjectVersion()}} &nbsp;&nbsp;{{appCtrl.serviceProvider.globalMenuCtrl.getBuildDate()}}</p>
            </div>
        </div>
    </div>
    <div class="dialog-content" style="position: initial;padding: 15px;">
        <div id="nf-about-content">
            <p>
                {{ appCtrl.serviceProvider.globalMenuCtrl.getDialogContent() }}
                <br><br>
            </p>
            <p>{{ appCtrl.serviceProvider.globalMenuCtrl.getSupportEmail() }}</p>
        </div>
    </div>
</div>
