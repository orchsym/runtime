<%@ page contentType="text/html" pageEncoding="UTF-8" session="false" %>
<!--
    Licensed to the Orchsym Runtime under one or more contributor license
    agreements. See the NOTICE file distributed with this work for additional
    information regarding copyright ownership.

    this file to You under the Orchsym License, Version 1.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    https://github.com/orchsym/runtime/blob/master/orchsym/LICENSE

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
-->
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>FilterRecord组件参数配置</title>
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
    <link href="css/index.css" rel="stylesheet" type="text/css">
    <script type="text/javascript" src="js/jquery-3.3.1.min.js"></script>
    <script type="text/javascript" src="js/angular.min.js"></script>
    <script type="text/javascript" src="js/jquery.color.js"></script>
</head>
<body>

<div class="apiEdit" id="storageApp" ng-app="storageApp" ng-controller="storageCtrl">
    <div id="processor-id" class="hidden"><%= request.getParameter("id") == null ? "" : org.apache.nifi.util.EscapeUtils.escapeHtml(request.getParameter("id")) %></div>
    <div id="client-id" class="hidden"><%= request.getParameter("clientId") == null ? "" : org.apache.nifi.util.EscapeUtils.escapeHtml(request.getParameter("clientId")) %></div>
    <div id="revision" class="hidden"><%= request.getParameter("revision") == null ? "" : org.apache.nifi.util.EscapeUtils.escapeHtml(request.getParameter("revision")) %></div>
    <div id="editable" class="hidden"><%= request.getParameter("editable") == null ? "" : org.apache.nifi.util.EscapeUtils.escapeHtml(request.getParameter("editable")) %></div>
    <div><br></div>
    <button id="saveAll" ng-click="saveAll()">保存</button>
    <div class="workbench-title">
        <span>高级配置</span>
        <div class="icon icon-add-big pull-right" ng-click="add()"></div>
    </div>
    <div class="workbench-body">
        <table class="api-setup-box" name="parameters">
            <thead>
            <tr>
                <th width="20%">字段名称</th>
                <th width="20%">函数名</th>
                <th width="20%">操作符</th>
                <th width="20%">值</th>
                <th width="120" style="text-align: center;">操作</th>
            </tr>
            </thead>
            <tbody>
            <tr ng-repeat="item in list">
                <%--输入字段名--%>
                <td style="text-align: center;" class="storageField">
                    <input type="text" ng-model="item['field']">
                </td>
                <td>
                    <%--选择函数名--%>
                    <select ng-model="item['function']">
                        <option ng-repeat="type in functions" value="{{type}}">{{type}}</option>
                    </select>
                </td>
                <%--选择操作符--%>
                <td>
                    <select ng-model="item['opertator']">
                        <option ng-repeat="type in types" value="{{type}}">{{type}}</option>
                    </select>
                </td>
                <%--输入参数--%>
                <td style="text-align: center;" class="storageValue">
                    <input type="text" ng-model="item['value']">
                </td>
                <%--操作--%>
                <td>
                    <div class="icon-handle" ng-click="shangyi($index)">
                        <img src="images/shangyi.png">
                    </div>
                    <div class="icon-handle" ng-click="xiayi($index)">
                        <img src="images/xiayi.png">
                    </div>
                    <div class="icon-handle" ng-click="delete($index)">
                        <img src="images/close.png" style="width:14px;height: 14px;margin-top: 4px">
                    </div>
                </td>
            </tr>
            </tbody>
        </table>
    </div>
    <div class="workbench-body prompt-info">
        注:<br/>
        &nbsp;&nbsp;*  字段名称项必填 <br/>
        &nbsp;&nbsp;*  操作符项必选</br>
        &nbsp;&nbsp;*  值字段项必填</br>
    </div>
</div>

<script type="text/javascript">

    var processorId = $('#processor-id').html()
    var revisionId = $('#revision').html()
    var clientId = $('#client-id').html()

    var token = 'Bearer '
    if(JSON.parse(localStorage.getItem('jwt'))){
        token = token + JSON.parse(localStorage.getItem('jwt'))['item']
    }
    var defaultObj = {}
    defaultObj['field'] = ''
    defaultObj['function'] = ''
    defaultObj['opertator'] = '='
    defaultObj['value'] = ''
    var app = angular.module('storageApp',[])
    app.controller('storageCtrl', function($scope) {
        $scope.list = [];
        $.ajax({
            type: 'GET',
            url: 'api/filter/record/processor/details?processorId=' + processorId,
            async: false,
            headers:{
                Authorization: token,
            },
        }).done(function (response) {
            console.log(response)
            //设置组件属性名称
            $scope.list = JSON.parse(response.properties['settings'])
        });
        $scope.types = ['=','<=','>=','<>','<','>'];
        $scope.functions = ['','length','abs','sqrt','ln','log','exp','ceil','floor','acos','asin','atan','cos','cot','degrees','radians','sign','sin','tan','upper','lower','initcap','soundex','rtrim','ltrim','reverse'];
        $scope.saveAll = function() {
            var fieldTmpList = [];
            var valueTmpList = [];
            var PARAMS = JSON.parse(JSON.stringify($scope.list))
            PARAMS = PARAMS.map(function(item){
                delete item['$$hashKey']
                fieldTmpList.push(item['field'])
                valueTmpList.push(item['value'])
                return item
            })

            //field校验，必填
            var fieldList = []
            for(var i=0;i<fieldTmpList.length;i++){
                if(fieldTmpList[i] === ""){
                    fieldList.push(i)
                }
            }
            //错误提示
            if (fieldList.length>0) {
                var fieldElements = $('.storageField')
                for(var i=0;i<fieldList.length;i++){
                    $(fieldElements[fieldList[i]]).css('backgroundColor', '#FF0000')
                    $(fieldElements[fieldList[i]]).animate({'backgroundColor': '#FFFFFF'},800)
                }
                return
            }
            //value校验，必填
            var valueList = []
            for(var i=0;i<valueTmpList.length;i++){
                if(valueTmpList[i] === ""){
                    valueList.push(i)
                }
            }
            //错误提示
            if (valueList.length>0) {
                var valueElements = $('.storageValue')
                for(var i=0;i<valueList.length;i++){
                    $(valueElements[valueList[i]]).css('backgroundColor', '#FF0000')
                    $(valueElements[valueList[i]]).animate({'backgroundColor': '#FFFFFF'},800)
                }
                return
            }

            var request = {
                settings: JSON.stringify(PARAMS)
            }
            // 保存数据
            $.ajax({
                type: 'PUT',
                url: 'api/filter/record/processor/properties?processorId='+processorId+'&revisionId='+revisionId+'&clientId='+clientId,
                data: JSON.stringify(request),
                contentType: 'application/json',
                headers:{
                    Authorization: token,
                },
            }).done(function (response) {
                alert('保存成功')
            });
        };
        $scope.shangyi = function(index) {
            if(index == 0){

            }else{
                var obj = $scope.list[index]
                $scope.list[index] = $scope.list[index-1]
                $scope.list[index-1] = obj
            }
        };
        $scope.xiayi = function(index) {
            if(index == $scope.list.length-1){

            }else{
                var obj = $scope.list[index]
                $scope.list[index] = $scope.list[index+1]
                $scope.list[index+1] = obj
            }
        };
        $scope.delete = function(index) {
            $scope.list.splice(index, 1)
        };
        $scope.add = function() {
            var tmpObj = JSON.parse(JSON.stringify(defaultObj))
            $scope.list.push(tmpObj)
        };

    })

</script>


</body>
</html>