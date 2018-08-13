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
<div class="editBox">
    <div class="apiEdit">
        <div class="workbench-title">请求路径</div>
        <div class="workbench-body">
            <input type="text" class="input" name="path" maxlength="40"/>
        </div>
        <div class="workbench-title">请求方法</div>
        <div class="workbench-body" id="methods">
            <ul></ul>
        </div>
        <div class="workbench-title">描述信息</div>
        <div class="workbench-body">
            <input type="text" class="input" id="root" name="description"  maxlength="200" />
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
                    <th width="90">格式化</th>
                    <th width="40">必填</th>
                    <th>注释</th>
                    <th width="40">删除</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td>
                        <input type="text" name="parametersAddNew" placeholder="输入名称添加新参数">
                    </td>
                    <td></td>
                    <td></td>
                    <td></td>
                    <td></td>
                    <td></td>
                    <td></td>
                </tr>
                </tbody>
            </table>
        </div>
        <div class="workbench-body" id="formBox">
            <div class="workbench-title">BODY定义</div>
            <div class="workbench-body" id="body">
                <label style="line-height: 20px;" class="mda-radio">
                    <input type="radio" value="raw" name="body"><em style="margin-right: 20px;"></em>
                    <span>raw</span>
                </label>
                <label style="line-height: 20px;" class="mda-radio">
                    <input type="radio" value="form-data" name="body"><em style="margin-right: 20px;"></em>
                    <span>form-data</span>
                </label>
                <label style="line-height: 20px;" class="mda-radio">
                    <input type="radio" value="x-www-form-urlencoded" name="body"><em style="margin-right: 20px;"></em>
                    <span>x-www-form-urlencoded</span>
                </label>
                <!--<ul>
                    <li type="raw">raw</li>
                    <li type="form-data" select="true">form-data</li>
                    <li type="x-www-form-urlencoded">x-www-form-urlencoded</li>
                </ul>-->
            </div>
            <div class="workbench-body" type="raw">
                <table class="api-setup-box" name="body">
                    <thead>
                    <tr>
                        <th>名称</th>
                        <th>模型</th>
                        <th>注释</th>
                        <th width="40">删除</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr>
                        <td>
                            <input type="text" name="bodyName" placeholder="输入名称添加BODY定义">
                        </td>
                        <td>
                            <select name="bodyRef"></select>
                        </td>
                        <td>
                            <input type="text" name="bodyDescription">
                        </td>
                        <td class="text-center"><div class="icon-close"></div></td>
                    </tr>
                    </tbody>
                </table>
            </div>
            <div class="workbench-body" type="form">
                <table class="api-setup-box" name="form">
                    <thead>
                    <tr>
                        <th>参数名</th>
                        <th width="80">类型</th>
                        <th width="90">格式化</th>
                        <th width="40">必填</th>
                        <th>注释</th>
                        <th width="40">删除</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr>
                        <td>
                            <input type="text" name="formAddNew" placeholder="输入名称添加新参数">
                        </td>
                        <td></td>
                        <td></td>
                        <td></td>
                        <td></td>
                        <td></td>
                        <td></td>
                    </tr>
                    </tbody>
                </table>
            </div>
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
                <div class="ullist">
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
            </div>
            <div class="right">
                <div class="title">详细信息</div>
                <div class="title-box api-setup-box" style="width: 50%;">
                    <div class="title-name">模型名称：</div>
                    <input type="text" class="input" name="name" maxlength="20" />
                </div>
                <div class="title-box api-setup-box" style="width: 50%;">
                    <div class="title-name">描述信息：</div>
                    <input type="text" class="input" name="description" maxlength="40" />
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
</div>
<div class="mask" id="mask">
    <div class="alert">
        <div class="title">错误提示</div>
        <div class="body">你好世界</div>
        <div class="ok"><button style="float: right;margin-top: 0px;">ok</button></div>
    </div>
</div>
<footer>
    <button id="saveAll">保存</button>
</footer>
</body>
</html>