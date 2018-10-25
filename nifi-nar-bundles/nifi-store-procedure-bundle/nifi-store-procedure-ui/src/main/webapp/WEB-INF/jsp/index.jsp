<%@ page contentType="text/html" pageEncoding="UTF-8" session="false" %>
<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="utf-8">
	<title>存储过程组件参数配置</title>
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
		
		<div class="workbench-title">方法名称</div>
		<button id="saveAll" ng-click="saveAll()">保存</button>
		<div class="workbench-body">
	        <input type="text" id="storageMethod" class="input" ng-model="METHOD" />
	    </div>
		<div class="workbench-title">
			<span>参数</span>
			<div class="icon icon-add-big pull-right" ng-click="add()"></div>
		</div>
	    <div class="workbench-body">
	        <table class="api-setup-box" name="parameters">
	            <thead>
	            <tr>
	            	<th>名称</th>
	                <th width="20%">类型</th>
	                <th width="20%">值</th>
	                <th width="20%">值类型</th>
	                <th width="20%">格式</th>
	                <th width="120" style="text-align: center;">操作</th>
	            </tr>
	            </thead>
	            <tbody>
	            <tr ng-repeat="item in list">
	            	<td style="text-align: center;" class="storageName">
	            		<input type="text" ng-model="item['param.name']">
	            	</td>
	                <td>
	                	<select ng-model="item['param.type']">
	                        <option ng-repeat="type in types" value="{{type}}">{{type}}</option>
	                    </select>
	                </td>
	                <td ng-class="{true:'', false:'gray'}[item['param.type']!='OUT']" class="storageValue">
	                	<sapn ng-if=" item['param.type']!='OUT' ">
		                	<input type="text" ng-model="item['param.value']">
		                </sapn>
	                </td>
	                <td>
	                	<sapn>
		                	<select ng-model="item['param.value.type']" ng-change="initFormatValue(item)">
		                        <option ng-repeat="valueType in valueTypes" value="{{valueType.key}}">{{valueType.text}}</option>
		                    </select>
	                	</sapn>
	                </td>
	                <td  ng-class="{true:'', false:'gray'}[( item['param.value.type'] && hasDateFormat(item['param.value.type']) || (item['param.value.type'] && hasCodingFormat(item['param.value.type']))) && item['param.type']!='OUT' ]" class="storageFormat">
	                	<span ng-if="item['param.value.type'] && hasDateFormat(item['param.value.type']) && item['param.type']!='OUT'">
		                	<span class="format-in" ng-if="!item['isInput']">
			                	<select ng-model="item['param.format']">
			                        <option ng-repeat="format in dateFormats" value="{{format}}">{{format}}</option>
			                    </select>
		                	</span>
		                	<span ng-if="item['isInput']" class="format-in">
		                		<input type="text" ng-model="item['param.format']">
		                	</span>
		                	<span class="switch-img" ng-click="toggleFormatType(item)">
		                		<img src="images/qiehuan.png">
		                	</span>
		                </span>
		                <span ng-if="(item['param.value.type'] && hasCodingFormat(item['param.value.type'])) && item['param.type']!='OUT'"">
		                	<select ng-model="item['param.format']">
			                    <option ng-repeat="format in codingFormats" value="{{format}}">{{format}}</option>
			                 </select>
		                </span>
	                </td>
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
	    		&nbsp;&nbsp;*  方法名称项必填 <br/>
	    		&nbsp;&nbsp;*  表格名称栏必填且唯一 <br/>
	    		&nbsp;&nbsp;*  表格值栏非置灰情况下必填</br>
	    		&nbsp;&nbsp;*  格式栏非置灰情况下必填</br>
	    </div>
	</div>
	
	<script type="text/javascript">
		
		var processorId = $('#processor-id').html()
		var revisionId = $('#revision').html()
		var clientId = $('#client-id').html()

		var paramList = []
		var methodName = ''

		var token = 'Bearer '
		if(JSON.parse(localStorage.getItem('jwt'))){
			token = token + JSON.parse(localStorage.getItem('jwt'))['item']
		}
		// $(function(){
		// 	console.log(processorId)
		// 	$.ajax({
     //         type: 'GET',
     //         url: 'api/store/procedure/processor/details?processorId=' + processorId
	 //        }).done(function (response) {
	 //        	console.log(response)
	 //        	methodName = response.properties['MethodName']
	 //            paramList = JSON.parse(response.properties['Params'])
	 //        });
		// })


		var defaultObj = {}
		defaultObj['param.name'] = ''
		defaultObj['param.type'] = 'IN'
		defaultObj['param.value'] = ''
		defaultObj['param.value.type'] = '0'
		defaultObj['param.format'] = 'BASIC_ISO_DATE(20111203)'

		var dateFormats = ['BASIC_ISO_DATE(20111203)', 'ISO_LOCAL_DATE(2011-12-03)', 'ISO_OFFSET_DATE(2011-12-03+01:00)', 'ISO_DATE(2011-12-03)', 'ISO_DATE(2011-12-03+01:00)', 'ISO_LOCAL_TIME(10:15)', 'ISO_LOCAL_TIME(10:15:30)', 'ISO_OFFSET_TIME(10:15+01:00)', 'ISO_OFFSET_TIME(10:15:30+01:00)', 'ISO_TIME(10:15)', 'ISO_TIME(10:15:30)', 'ISO_TIME(10:15:30+01:00)', 'ISO_LOCAL_DATE_TIME(2011-12-03T10:15:30)', 'ISO_OFFSET_DATE_TIME(2011-12-03T10:15:30+01:00)', 'ISO_ZONED_DATE_TIME(2011-12-03T10:15:30+01:00[Europe/Paris])', 'ISO_DATE_TIME(2011-12-03T10:15:30)', 'ISO_DATE_TIME(2011-12-03T10:15:30+01:00)', 'ISO_DATE_TIME(2011-12-03T10:15:30+01:00[Europe/Paris])', 'ISO_ORDINAL_DATE(2012-337)', 'ISO_WEEK_DATE(2012-W48-6)', 'ISO_INSTANT(2011-12-03T10:15:30Z)', 'RFC_1123_DATE_TIME(Tue, 3 Jun 2008 11:05:30 GMT)'];

		var codingFormats = ['ASCII', 'HEX', 'BASE64'];

		var dateArr = ['10', '11', '12'];
		var codingArr = ['13', '14', '15'];

		var app = angular.module('storageApp',[])
		app.controller('storageCtrl', function($scope) {
			$scope.METHOD = 'addUser';
			$scope.list = [];
			$.ajax({
	            type: 'GET',
	            url: 'api/store/procedure/processor/details?processorId=' + processorId,
	            async: false,
	            headers:{
	            	Authorization: token,
	            },
		        }).done(function (response) {
		        	console.log(response)
		        	
		        	$scope.METHOD = response.properties['MethodName']
		            $scope.list = JSON.parse(response.properties['Params'])
		            $scope.list = Array.isArray($scope.list) ? $scope.list : []
		            $scope.list = $scope.list.map(function(item){
		            	item['param.value.type'] = String(item['param.value.type'])
		            	return item
		            })
		        });
			$scope.types = ['IN', 'OUT', 'IN/OUT'];
			$scope.dateFormats = dateFormats;
			$scope.codingFormats = codingFormats;
			$scope.valueTypes = [
				{key: 0, text: 'BIT'},
				{key: 1, text: 'BOOLEAN'},
				{key: 2, text: 'SMALLINT'},
				{key: 3, text: 'INTEGER'},
				{key: 4, text: 'BIGINT'},
				{key: 5, text: 'REAL'},
				{key: 6, text: 'FLOAT'},
				{key: 7, text: 'DOUBLE'},
				{key: 8, text: 'DECIMAL'},
				{key: 9, text: 'NUMERIC'},
				{key: 10, text: 'DATE'},
				{key: 11, text: 'TIME'},
				{key: 12, text: 'TIMESTAMP'},
				{key: 13, text: 'BINARY'},
				{key: 14, text: 'VARBINARY'},
				{key: 15, text: 'LONGVARBINARY'},
				{key: 16, text: 'CHAR'},
				{key: 17, text: 'VARCHAR'},
				{key: 18, text: 'LONGVARCHAR'},
				{key: 19, text: 'CLOB'}
			];
			$scope.saveAll = function() {
				var methodDom = false
				var nameList = []
				var valueList = []
				var formatList = []

				// var nameReg = /^[a-zA-Z\$_][a-zA-Z\d_]*$/

				$scope.METHOD = $scope.METHOD.trim()
				if ($scope.METHOD == '') {
					methodDom = true
				}
				var PARAMS = JSON.parse(JSON.stringify($scope.list))
				var nameTmpList = []
				var tmpArr = dateArr.map(function(value){
							return parseInt(value)
						})
				PARAMS = PARAMS.map(function(item, index){
					item['param.name'] = item['param.name'] ? item['param.name'].trim() : ''
					nameTmpList.push(item['param.name'])
					item['param.value'] = item['param.value'] ? item['param.value'].trim() : ''
					item['param.value.type'] = parseInt(item['param.value.type'])

					if(item['param.type'] == 'OUT'){
						delete item['param.value']
						delete item['param.format']
					}else{
						if(!$scope.hasDateFormat(String(item['param.value.type'])) && !$scope.hasCodingFormat(String(item['param.value.type']))){
							delete item['param.format']
						}

						if(item['param.value'] == ''){
							valueList.push(index)
						}
						if(tmpArr.indexOf(item['param.value.type']) != -1 && item['param.format'] == ''){
							formatList.push(index)
						}
					}
					delete item.isInput
					delete item['$$hashKey']
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
					if(errData.indexOf(PARAMS[i]['param.name']) != -1){
						nameList.push(i)
					}
				}

				
				// 进行错误提示
				if (methodDom || nameList.length>0 || valueList.length>0 || formatList.length>0) {
					if(methodDom){
						$('#storageMethod').css('backgroundColor', '#FF0000')
						$('#storageMethod').animate({'backgroundColor': 'rgba(114, 142, 155, 0.26)'}, 800)
					}


					var nameElements = $('.storageName')
					for(var i=0;i<nameList.length;i++){
						$(nameElements[nameList[i]]).css('backgroundColor', '#FF0000')
						$(nameElements[nameList[i]]).animate({'backgroundColor': '#FFFFFF'},800)
					}

					var valueElements = $('.storageValue')
					for(var i=0;i<valueList.length;i++){
						$(valueElements[valueList[i]]).css('backgroundColor', '#FF0000')
						$(valueElements[valueList[i]]).animate({'backgroundColor': '#FFFFFF'},800)
					}

					var formatElements = $('.storageFormat')
					for(var i=0;i<formatList.length;i++){
						$(formatElements[formatList[i]]).css('backgroundColor', '#FF0000')
						$(formatElements[formatList[i]]).animate({'backgroundColor': '#FFFFFF'},800)
					}
					return 
				}


				var request = {
					MethodName: $scope.METHOD,
					Params: JSON.stringify(PARAMS)
				}

				var obj = {
					properties: request
				}
				// 保存数据				
				$.ajax({
					type: 'PUT',
					url: 'api/store/procedure/processor/properties?processorId='+processorId+'&revisionId='+revisionId+'&clientId='+clientId,
					data: JSON.stringify(request),
					contentType: 'application/json',
					headers:{
		            	Authorization: token,
		            },
				}).done(function (response) {
	            	alert('保存成功')
	        	});


			};
			$scope.initFormatValue = function(item) {
				item.isInput = false
				if(dateArr.indexOf(item['param.value.type']) != -1){
					item['param.format'] = dateFormats[0]
				}else if(codingArr.indexOf(item['param.value.type']) != -1){
					item['param.format'] = codingFormats[0]
				}
			};
			$scope.toggleFormatType = function(item) {
				if (item['isInput']){
					item['param.format'] = dateFormats[0]
					item['isInput'] = false
				}else{
					item['param.format'] = ''
					item['isInput'] = true
				}
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
			$scope.hasDateFormat = function(key) {
				var key = key.trim()
				if (dateArr.indexOf(key) == -1) {
					return false
				} else {
					return true
				}
			};
			$scope.hasCodingFormat = function(key) {
				var key = key.trim()
				if (codingArr.indexOf(key) == -1) {
					return false
				} else {
					return true
				}
			};

		})
		
	</script>


</body>
</html>