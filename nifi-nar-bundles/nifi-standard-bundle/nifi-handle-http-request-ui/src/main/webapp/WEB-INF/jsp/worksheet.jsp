<%@ page contentType="text/html" pageEncoding="UTF-8" session="false" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8"/>
    <title></title>
    <meta content="width=device-width, initial-scale=1.0" name="viewport"/>
    <meta http-equiv="Content-type" content="text/html; charset=utf-8">
    <link href="css/font.css" rel="stylesheet" type="text/css">
    <link href="js/plugins/font-awesome/css/font-awesome.min.css" rel="stylesheet" type="text/css">
    <link href="js/plugins/bootstrap/css/bootstrap.min.css" rel="stylesheet" type="text/css">
    <link href="js/plugins/icheck/skins/all.css" rel="stylesheet" type="text/css">
    <link href="css/components-rounded.css" id="style_components" rel="stylesheet" type="text/css">
    <link href="css/plugins.css" rel="stylesheet" type="text/css">
    <link href="css/custom.css" rel="stylesheet" type="text/css">
    <link href="js/plugins/uniform/css/uniform.default.css" rel="stylesheet" type="text/css">
</head>
<body>
<div id="http-request-processor-id" class="hidden"><%= request.getParameter("id") == null ? "" : org.apache.nifi.util.EscapeUtils.escapeHtml(request.getParameter("id")) %></div>
<div id="http-request-revision" class="hidden"><%= request.getParameter("revision") == null ? "" : org.apache.nifi.util.EscapeUtils.escapeHtml(request.getParameter("revision")) %></div>
<div id="http-request-client-id" class="hidden"><%= request.getParameter("clientId") == null ? "" : org.apache.nifi.util.EscapeUtils.escapeHtml(request.getParameter("clientId")) %></div>
<div class="page-container">
  <div class="col-md-12">
    <div class="portlet light">
      <div class="portlet-body form">
        <div class="tabbable-line tabs-below">
          <div class="tab-content">
            <div class="tab-pane active" id="tab5">
              <div>定义输入</div>
              <div class="table-content">
                <div id="tab_put_in_container" class="text-align-reverse margin-bottom-10">
                <a id="tab_put_in_pickfiles" href="javascript:;" class="btn in-add">
                  <i class="fa fa-plus"></i> 增加 </a>
                </div>
                <div class="row">
                  <div id="tab_images_uploader_filelist" class="col-md-6 col-sm-12">
                  </div>
                </div>
                <table class="table table-bordered table-hover">
                  <thead>
                  <tr role="row" class="heading">
                    <th>
                      参数名称
                    </th>
                    <th>
                      参数位置
                    </th>
                    <th>
                      必选
                    </th>
                    <th>
                      类型
                    </th>
                    <th>
                      默认值
                    </th>
                    <th>
                      描述
                    </th>
                    <th>
                      操作
                    </th>
                  </tr>
                  </thead>
                  <tbody class="table-tbody-param-in">
                  </tbody>
                </table>
              </div>
            </div>
            <div>定义输出</div>
            <div class="table-content">
              <div id="tab_put_out_container" class="text-align-reverse margin-bottom-10">
              <a id="tab_put_out_pickfiles" href="javascript:;" class="btn out-add">
                <i class="fa fa-plus"></i> 增加 </a>
              </div>
              <div class="row">
                <div id="tab_images_uploader_filelist" class="col-md-6 col-sm-12">
                </div>
              </div>
              <table class="table table-bordered table-hover">
                <thead>
                <tr role="row" class="heading">
                  <th>
                    返回码
                  </th>
                  <th>
                    描述
                  </th>
                  <th>
                    Content Type
                  </th>
                  <th>
                    Schema Type
                  </th>
                  <th>
                    操作
                  </th>
                </tr>
                </thead>
                <tbody class="table-tbody">
                </tbody>
              </table>
            </div>
            <div align="right">
              <a id="tab_save" href="javascript:;" class="btn btn-save">
              <i class="fa"></i> 保存 </a>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>
</div>
<script src="js/plugins/jquery.min.js" type="text/javascript"></script>
<script src="js/plugins/jquery-ui/jquery-ui.min.js"></script>
<script src="js/plugins/bootstrap/js/bootstrap.min.js"></script>
<script src="js/plugins/jquery-validation/js/jquery.validate.js"></script>
<script src="js/plugins/jquery-validation/js/additional-methods.min.js"></script>
<script src="js/plugins/bootstrap-wizard/jquery.bootstrap.wizard.js"></script>
<script src="js/plugins/select2/select2.min.js"></script>
<script src="js/plugins/metronic.js"></script>
<script src="js/plugins/form-wizard.js"></script>
<script src="js/plugins/icheck/icheck.min.js"></script>
<script src="js/plugins/uniform/jquery.uniform.min.js" type="text/javascript"></script>
<script>
  
    $(document).ready(function () {
      ua.init();

      ua.getPropertyInfos();
    });

    var ua = {

      init: function () {
      $(".in-add").on("click", function () {
        var trId = Math.round(Math.random() * 1000000);
        var paramName = 'name' + trId;

        var tr = '<tr><td><input type="text" data-old="' + paramName + '" value="' + paramName + '" class="form-control param-name"   placeholder="参数名称"></td><td><select class="form-control param-location"><option value="query" selected=true>query</option><option value="header">header</option><option value="path">path</option><option value="cookie">cookie</option></select></td><td><select class="form-control param-required"><option value="true">是</option><option value="false">否</option></select></td><td><select class="form-control param-type"><option value="string">string</option><option value="integer">integer</option><option value="boolean">boolean</option><option value="number">number</option><option value="array">array</option><option value="object">object</option></select></td><td><input type="text" class="form-control param-default"   placeholder="参数默认值"></td><td><input type="text" class="form-control param-description" placeholder="说明"></td><td><a href="javascript:;" class="btn default btn-sm btn-del btn' + trId + 1 + '" id="btn' + trId + 1 + '><i class="fa fa-times"></i> 删除 </a></td></tr>';

          $(this).parents(".table-content").find("tbody").append(tr);
          $(".btn" + trId + 1).on("click", function () {
            $(this).parents("tr").remove();
            var paramName = $(this).parents("tr").find(".param-name").val();
          });
        });

      $(".out-add").on("click", function () {
        var trId = Math.round(Math.random() * 1000000);
        var paramName = 200;

        var tr = '<tr><td><input type="text" data-old="' + paramName + '" value="' + paramName + '" class="form-control param-name" ></td><td><input type="text" class="form-control param-description" placeholder="OK"></td><td><select class="form-control param-type"><option value="application/json">application/json</option><option value="application/json">application/json</option></select></td><td><select class="form-control schema-type"><option value="array">array</option><option value="object">object</option></select></td><td><a href="javascript:;" class="btn default btn-sm btn-del btn' + trId + 1 + '" id="btn' + trId + 1 + '><i class="fa fa-times"></i> 删除 </a></td></tr>';

          $(this).parents(".table-content").find("tbody").append(tr);
          $(".btn" + trId + 1).on("click", function () {
            $(this).parents("tr").remove();
            var paramName = $(this).parents("tr").find(".param-name").val();
          });
        });

      $(".btn-save").on("click", function () {
          ua.savePropertyInfos();
        });
      },

      getPropertyInfos: function() {

        $.ajax({
            type: 'GET',
            url: 'api/property/info?' + $.param({
                processorId: ua.getProcessorId()
            })
        }).done(function (response) {

            var html = "";            
            var parameters = response.parameters;
            if (parameters) {
              for (var i = 0; i < parameters.length; i++) {
                var trId = Math.round(Math.random() * 10000);
                var option = '<option value="false">否</option><option value="true">是</option>';

                if (parameters[i].required == true) {
                  option = '<option value="false">否</option><option value="true" selected=true>是</option>';
                }
                var paramName = parameters[i].name.replace(/"/g, '&quot;');
                var paramValue = parameters[i].defaultValue;
                var paramType = parameters[i].type
                var paramLocation = parameters[i].position;
                var paramDescription = parameters[i].description.replace(/"/g, '&quot;');
                var locationOptions = '';
                if (paramLocation == 'query') {
                  locationOptions = '<option value="query" selected=true>query</option><option value="header">header</option><option value="path">path</option><option value="cookie">cookie</option>'
                } else if (paramLocation == 'header') {
                  locationOptions = '<option value="query">query</option><option value="header" selected=true>header</option><option value="path">path</option><option value="cookie">cookie</option>'
                } else if (paramLocation == 'path') {
                  locationOptions = '<option value="query">query</option><option value="header">header</option><option value="path" selected=true>path</option><option value="cookie">cookie</option>'
                } else if (paramLocation == 'cookie') {
                  locationOptions = '<option value="query">query</option><option value="header">header</option><option value="path">path</option><option value="cookie" selected=true>cookie</option>'
                }

                var typeOptions = '';
                if (paramType == 'string') {
                  typeOptions = '<option value="string" selected=true>string</option><option value="integer">integer</option><option value="boolean">boolean</option><option value="number">number</option><option value="array">array</option><option value="object">object</option>'
                } else if (paramType == 'integer') {
                   typeOptions = '<option value="string">string</option><option value="integer" selected=true>integer</option><option value="boolean">boolean</option><option value="number">number</option><option value="array">array</option><option value="object">object</option>'
                } else if (paramType == 'boolean') {
                   typeOptions = '<option value="string">string</option><option value="integer">integer</option><option value="boolean" selected=true>boolean</option><option value="number">number</option><option value="array">array</option><option value="object">object</option>'
                } else if (paramType == 'number') {
                   typeOptions = '<option value="string">string</option><option value="integer">integer</option><option value="boolean">boolean</option><option value="number" selected=true>number</option><option value="array">array</option><option value="object">object</option>'
                } else if (paramType == 'array') {
                   typeOptions = '<option value="string">string</option><option value="integer">integer</option><option value="boolean">boolean</option><option value="number">number</option><option value="array" selected=true>array</option><option value="object">object</option>'
                } else if (paramType == 'object') {
                   typeOptions = '<option value="string">string</option><option value="integer">integer</option><option value="boolean">boolean</option><option value="number">number</option><option value="array">array</option><option value="object" selected=true>object</option>'
                }

                html += '<tr><td><input type="text" data-old="' + paramName + '" value="' + paramName + '" class="form-control param-name"   placeholder="参数名称"></td><td><select class="form-control param-location">' + locationOptions + ' </select></td><td><select class="form-control param-required">' + option +'</select></td><td><select class="form-control param-type">' + typeOptions + '</select></td><td><input type="text" class="form-control param-default"   placeholder="参数默认值" "data-old="' + paramValue + '" value="' + paramValue + '"></td><td><input type="text" class="form-control param-description" placeholder="说明" value="' + paramDescription + '"></td><td><a href="javascript:;" class="btn default btn-sm btn-del btn' + trId + 1 + '" id="btn' + trId + 1 + '><i class="fa fa-times"></i> 删除 </a></td></tr>';
              }
            }

            $(".table-tbody-param-in").html(html);
            $(".btn-del").on("click", function() {
              $(this).parents("tr").remove();
            });
          });
      },

      savePropertyInfos: function() {

        var json = {};
        json.processorId = ua.getProcessorId();
        json.revision = ua.getRevision(),
        json.clientId = ua.getClientId();
        json.parameters = [];
        json.respInfos = [];
        json.respModels = [];

        $(".table-tbody-param-in tr").each(function(i, row) {
          var param = {};
          if ($(this).find(".param-name").val()) {
            param.name = $(this).find(".param-name").val();
            param.required = $(this).find(".param-required option:selected").val();
            param.type = $(this).find(".param-type").val();
            param.position = $(this).find(".param-location").val();
            param.defaultValue = $(this).find(".param-default").val();
            param.description = $(this).find(".param-description").val();
          }
          json.parameters.push(param);
        });

        var url = 'api/property/info';
        var httpMethod = 'POST';

        return $.ajax({
            type: httpMethod,
            url: url,
            data: JSON.stringify(json),
            processData: false,
            contentType: 'application/json'
        }).then(function (response) {
            console.log('');
        }, function (xhr, status, error) {
            alert(xhr.responseText);
        });
      },

      getClientId: function () {
        return $('#http-request-client-id').text();
      },

      getRevision: function () {
        return $('#http-request-revision').text();
      },

      getProcessorId: function () {
        return $('#http-request-processor-id').text();
      },

    };
</script>
</body>
</html>