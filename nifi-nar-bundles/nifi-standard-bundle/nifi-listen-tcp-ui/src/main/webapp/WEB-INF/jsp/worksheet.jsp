<%@ page contentType="text/html" pageEncoding="UTF-8" session="false" %>
<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="utf-8">
	<title>ListenTCP参数配置</title>
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

		<div class="workbench-title">
			<span>字段定义(解包)</span>
			<div class="icon icon-add-big pull-right" ng-click="add()"></div>
		</div>
	    <div class="workbench-body">
	        <table class="api-setup-box" name="parameters">
	            <thead>
	            <tr>
	            	<th>字段名称</th>
	                <th>字段长度</th>
	                <th width=140 style="text-align: center;">操作</th>
	            </tr>
	            </thead>
	            <tbody>
	            <tr ng-repeat="item in list">
	            	<td style="text-align: center;" class="storageName">
	            		<input type="text" ng-model="item['name']">
	            	</td>
	                <td>
	                	<input type="text" ng-model="item['length']" onkeyup="this.value=this.value.replace(/\D/g,'')">
	                </td>
	                <td>
	                	<div class="icon-handle" ng-click="moveup($index)">
	                		<img src="images/moveup.png">
	                	</div>
	                	<div class="icon-handle" ng-click="movedown($index)">
	                		<img src="images/movedown.png">
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
	    		&nbsp;&nbsp;*  名称对应FlowFile的attribute key值 <br/>
	    		&nbsp;&nbsp;*  长度为字节数大小 <br/>
	    		&nbsp;&nbsp;*  解包顺序与字段定义的顺序一致</br>
	    		&nbsp;&nbsp;*  内容解包完会设置到FlowFile的attribute中</br>
	    </div>
	    <footer>
	   	 <button id="saveAll" ng-click="saveAll()">保存</button>
		</footer>
		</div>
	<script type="text/javascript">
		
		var processorId = $('#processor-id').html()
		var revisionId = $('#revision').html()
		var clientId = $('#client-id').html()
		var token = 'Bearer '
		if(JSON.parse(localStorage.getItem('jwt'))){
			token = token + JSON.parse(localStorage.getItem('jwt'))['item']
		}


		function customAlert(par){
	        var W = screen.availWidth;
	        var H = screen.availHeight;
	        $("#mask .title").html(par.title || "提示");
	        $("#mask .body").html(par.text || "");
	        $("#mask button").on("click", function(){
	            $("#mask").hide();
	        });
	        $("#mask").show();
	    };

		var app = angular.module('storageApp',[])
		app.controller('storageCtrl', function($scope) {
			$scope.list = [];
			$.ajax({
	            type: 'GET',
	            headers:{
	            	Authorization: token,
	            },
	            url: 'api/listentcp/info?' + $.param({
                processorId: processorId
            }),
	            async: false,
		        }).done(function (response) {
		            $scope.list = response.infos
		            $scope.list = Array.isArray($scope.list) ? $scope.list : []
		        })
			
			$scope.saveAll = function() {
				var nameList = []

				// var nameReg = /^[a-zA-Z\$_][a-zA-Z\d_]*$/

				var PARAMS = JSON.parse(JSON.stringify($scope.list))
				var nameTmpList = []
				PARAMS = PARAMS.map(function(item, index){
					item['name'] = item['name'] ? item['name'].trim() : ''
					nameTmpList.push(item['name'])

					item['length'] = parseInt(item['length'])
					return item
				})

				// 进行name校验，必填且唯一
				nameTmpList.sort()
				var errData = []
				if(nameTmpList[0] == ''){
					errData.push('')
				}
				for(var i=1;i<nameTmpList.length;i++){
					if(nameTmpList[i] == '' || nameTmpList[i] == nameTmpList[i-1]){
						errData.push(nameTmpList[i])
					}
				}	
				for(var i=0;i<PARAMS.length;i++){
					if(errData.indexOf(PARAMS[i]['name']) != -1){
						nameList.push(i)
					}
				}

				// 进行错误提示
				if (nameList.length>0) {
					var nameElements = $('.storageName')
					for(var i=0;i<nameList.length;i++){
						$(nameElements[nameList[i]]).css('backgroundColor', '#FF0000')
						$(nameElements[nameList[i]]).animate({'backgroundColor': '#FFFFFF'},800)
					}
					return 
				}

				var obj = {
					infos: PARAMS,
					processorId: processorId,
					revision: revisionId,
					clientId: clientId
				}
				// 保存数据
				var data = angular.toJson(obj)		
				$.ajax({
					type: 'POST',
					url: 'api/listentcp/info',
					headers: {
						Authorization: token,
					},
					data: data,
					contentType: 'application/json'
				}).then(function (response) {
		            console.log(response);
		        }, function (xhr, status, error) {
		            customAlert({text:xhr.responseText});
		        });
			};
			$scope.moveup = function(index) {
				if(index > 0){
					var obj = $scope.list[index]
					$scope.list[index] = $scope.list[index-1]
					$scope.list[index-1] = obj
				}
			};
			$scope.movedown = function(index) {
				if(index != $scope.list.length-1){
					var obj = $scope.list[index]
					$scope.list[index] = $scope.list[index+1]
					$scope.list[index+1] = obj
				}
			};
			$scope.delete = function(index) {
				$scope.list.splice(index, 1)
			};
			$scope.add = function() {
				var obj = {}
				obj['name'] = 'name'
				obj['length'] = '1'
				$scope.list.push(obj)
			};
		})
	</script>
</body>
<div class="mask" id="mask">
    <div class="alert">
        <div class="title">提示</div>
        <div class="body">你好世界</div>
        <div class="ok"><button style="float: right;margin-top: 0px;">ok</button></div>
    </div>
</div>
</html>