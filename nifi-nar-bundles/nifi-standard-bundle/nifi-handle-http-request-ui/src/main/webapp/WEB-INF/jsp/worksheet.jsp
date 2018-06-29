<%@ page contentType="text/html" pageEncoding="UTF-8" session="false" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
    <link href="css/index.css" rel="stylesheet" type="text/css">
    <script src="js/jquery-3.3.1.min.js"></script>
    <link href="js/ui-ace/demo.css" rel="stylesheet" type="text/css">
    <script src="js/src-min-noconflict/ace.js"></script>
    <script src="js/src-min-noconflict/ext-language_tools.js"></script>
    <script src="js/index.js"></script>
    <title>API配置信息</title>
</head>
<body>
<div id="http-request-processor-id" class="hidden"><%= request.getParameter("id") == null ? "" : org.apache.nifi.util.EscapeUtils.escapeHtml(request.getParameter("id")) %></div>
<div id="http-request-revision" class="hidden"><%= request.getParameter("revision") == null ? "" : org.apache.nifi.util.EscapeUtils.escapeHtml(request.getParameter("revision")) %></div>
<div id="http-request-client-id" class="hidden"><%= request.getParameter("clientId") == null ? "" : org.apache.nifi.util.EscapeUtils.escapeHtml(request.getParameter("clientId")) %></div>
<div class="apiEdit">
    <div class="workbench-title">请求路径</div>
    <button id="saveAll">保存</button>
    <div class="workbench-body">
        <input type="text" class="input" name="path" />
    </div>
    <div class="workbench-title">请求方法</div>
    <div class="workbench-body">
        <label style="line-height: 20px;" class="mda-checkbox">
            <input type="checkbox" value="get" name="method"><em style="margin-right: 20px;"></em>
            <span>GET</span>
        </label>
        <label style="line-height: 20px;" class="mda-checkbox">
            <input type="checkbox" value="put" name="method"><em style="margin-right: 20px;"></em>
            <span>PUT</span>
        </label>
        <label style="line-height: 20px;" class="mda-checkbox">
            <input type="checkbox" value="post" name="method"><em style="margin-right: 20px;"></em>
            <span>POST</span>
        </label>
        <label style="line-height: 20px;" class="mda-checkbox">
            <input type="checkbox" value="delete" name="method"><em style="margin-right: 20px;"></em>
            <span>DELETE</span>
        </label>
    </div>
    <div class="workbench-title">描述信息</div>
    <div class="workbench-body">
        <input type="text" class="input" id="root" name="description" />
    </div>
    <div class="workbench-title">响应类型</div>
    <div class="workbench-body">
        <label style="line-height: 20px;" class="mda-checkbox">
            <input type="checkbox" value="application/xml" name="rootContentType"><em style="margin-right: 20px;"></em>
            <span>application/xml</span>
        </label>
        <label style="line-height: 20px;" class="mda-checkbox">
            <input type="checkbox" value="application/json" name="rootContentType"><em style="margin-right: 20px;"></em>
            <span>application/json</span>
        </label>
        <label style="line-height: 20px;" class="mda-checkbox">
            <input type="checkbox" value="text/xml" name="rootContentType"><em style="margin-right: 20px;"></em>
            <span>text/xml</span>
        </label>
        <label style="line-height: 20px;" class="mda-checkbox">
            <input type="checkbox" value="text/html" name="rootContentType"><em style="margin-right: 20px;"></em>
            <span>text/html</span>
        </label>
    </div>
    <div class="workbench-title">参数类型</div>
    <div class="workbench-body">
        <table class="api-setup-box" name="parameters">
            <thead>
            <tr>
                <th>参数名</th>
                <th width="80">位置</th>
                <th width="80">类型</th>
                <th width="40">必填</th>
                <th>注释</th>
                <th width="40">删除</th>
            </tr>
            </thead>
            <tbody>
            <tr>
                <td>
                    <input type="text" name="parametersAddNew">
                </td>
                <td></td>
                <td></td>
                <td></td>
                <td></td>
                <td></td>
            </tr>
            </tbody>
        </table>
    </div>
    <div class="workbench-title"><span>响应设置</span><div class="icon icon-add-big pull-right" id="respInfosAdd"></div></div>
    <div class="workbench-body api-setup-box" id="respInfos">
        <div class="response">
            <div class="response-body">
                <div class="triangle"></div>
                <div class="response-tr" style="font-weight: 600;">400</div>
                <div class="response-tr">If request failed, return 400</div>
                <div class="response-tr"></div>
            </div>
            <div class="response-body">
                <div class="response-tr">
                    <select>
                        <option value="string" selected="selected">string</option>
                        <option value="boolean">boolean</option>
                        <option value="number">number</option>
                    </select>
                </div>
                <div class="response-tr">application/json</div>
                <div class="response-tr">This is default model</div>
            </div>
        </div>
    </div>
</div>
<div class="modelEdit">
    <div class="workbench-title">模型管理</div>
    <button id="saveModel">保存模型</button>
    <div class="model-box">
        <div class="left">
            <div class="title-icon icon-add-big"></div>
            <div class="title">模型名称</div>
            <ul>
                <!--<li select="true">
                <div class="li-title">Default</div>
                <div class="icon-close"></div>
                </li>
                <li>
                    <div class="li-title">ErrorModel</div>
                    <div class="icon-close"></div>
                </li>-->
            </ul>
        </div>
        <div class="right">
            <div class="title">详细信息</div>
            <div class="title-box api-setup-box" style="width: 50%;">
                <div class="title-name">模型名称：</div>
                <input type="text" class="input" name="name" />
            </div>
            <div class="title-box api-setup-box" style="width: 50%;">
                <div class="title-name">描述信息：</div>
                <input type="text" class="input" name="description" />
            </div>
            <div class="title-box api-setup-box">
                <div class="title-name">内容类型：</div>
                <div class="title-body">
                    <label style="line-height: 20px;" class="mda-checkbox">
                        <input type="checkbox" value="application/xml" name="contentType"><em style="margin-right: 20px;"></em>
                        <span>application/xml</span>
                    </label>
                    <label style="line-height: 20px;" class="mda-checkbox">
                        <input type="checkbox" value="application/json" name="contentType"><em style="margin-right: 20px;"></em>
                        <span>application/json</span>
                    </label>
                    <label style="line-height: 20px;" class="mda-checkbox">
                        <input type="checkbox" value="text/xml" name="contentType"><em style="margin-right: 20px;"></em>
                        <span>text/xml</span>
                    </label>
                    <label style="line-height: 20px;" class="mda-checkbox">
                        <input type="checkbox" value="text/html" name="contentType"><em style="margin-right: 20px;"></em>
                        <span>text/html</span>
                    </label>
                </div>
            </div>
            <div class="title-box api-setup-box">
                <div class="title-name">Body详情：</div>
            </div>
            <div class="textarea-box">
            <pre class="ace_editor respone-code" style="height: calc(100%);">
                <textarea class="ace_text-input"></textarea>
            </pre>
            </div>
        </div>
    </div>
</div>
</body>
</html>