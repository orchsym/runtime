/**
 * Created by wz on 2018/6/19.
 */
$(function(){
    var Swagger = {};
    var ApiTools = {};
    var token = 'Bearer '
    if(JSON.parse(localStorage.getItem('jwt'))){
        token = token + JSON.parse(localStorage.getItem('jwt'))['item']
    }
    var locale = localStorage.getItem('locale')

    ApiTools.path = {
        node: $("input[name=path]")
    };
    ApiTools.path.get = function(){
        Swagger.path = ApiTools.path.node.input.val();
    };
    ApiTools.path.set = function(){
        ApiTools.path.node.val(Swagger.path);
        twoWayBinding(ApiTools.path.node,Swagger,"path");
    };

    ApiTools.description = {
        node: $(".apiEdit input#root[name=description]")
    };
    ApiTools.description.set = function(){
        ApiTools.description.node.val(Swagger.description[ApiTools.method.selectMethod]);
        twoWayBinding(ApiTools.description.node,Swagger,"description");
    };

    ApiTools.method = {
        node: $(".apiEdit input[type=checkbox][name=method]")
    };

    ApiTools.method.get = function(){
        var method = [];
        $(".apiEdit input[type=checkbox][name=method]:checked").each(function(){
            method.push($(this).val());
        });
        Swagger.method = method;
    };

    ApiTools.method.set = function(){
        /*var method = Swagger.method;
        $(".apiEdit input[type=checkbox][name=method]:checked").each(function(){
            $(this)[0].checked = false;
        });
        method.forEach(function(item){
            $(".apiEdit input[type=checkbox][name=method][value='" + item + "']")[0].checked = true;
        });*/
        //$("#methods ul")
        $("#methods ul").html("");
        Swagger.methods.forEach(function(item){
            var li = $('<li name="' + item + '">' + item + '</li>');
            li.on("click",function(){
                ApiTools.method.select($(this).attr("name"),true);
            });
            $("#methods ul").append(li);
        });
        ApiTools.method.selectMethod = ApiTools.method.selectMethod || Swagger.methods[0];
        ApiTools.method.select(ApiTools.method.selectMethod);
        if(ApiTools.method.selectMethod=="post" || ApiTools.method.selectMethod=="put")
        {
            $("#formBox").show();
        }
        else {
            $("#formBox").hide();
        }
        //twoWayBinding(ApiTools.method.node,Swagger,"method");
    };

    ApiTools.method.select = function(method,boo){
        ApiTools.method.selectMethod = method;
        $("#methods ul li[select=true]").attr({
            select: "false"
        });
        $("#methods ul li[name=" + method + "]").attr({
            select: "true"
        });
        if(method=="post" || method=="put")
        {
            $("#formBox").show();
        }
        else {
            $("#formBox").hide();
        }
        if(boo)
        {
            for(x in ApiTools) {
                if(ApiTools[x]["set"]) {
                    ApiTools[x]["set"]();
                }
            }
        }
    };

    ApiTools.contentType = {
        node: $(".apiEdit input[type=checkbox][name=rootContentType]")
    };
    ApiTools.contentType.set = function(){
        var contentType = Swagger.contentType[ApiTools.method.selectMethod];
        if(!contentType)
        {
            contentType = Swagger.contentType[ApiTools.method.selectMethod] = ["application/json"];
        }
        $(".apiEdit input[type=checkbox][name=rootContentType]:checked").each(function(){
            $(this)[0].checked = false;
        });
        contentType.forEach(function(item){
            $(".apiEdit input[type=checkbox][name=rootContentType][value='" + item + "']")[0].checked = true;
        });
        twoWayBinding(ApiTools.contentType.node,Swagger,"contentType");
    };

    ApiTools.parameters = {
        node: $(".apiEdit table[name=parameters]"),
        nodeTbody: $(".apiEdit table[name=parameters] tbody"),
        nodeTbodyForm: $(".apiEdit #formBox table[name=form] tbody"),
        nodeTbodyTr: $(".apiEdit table[name=parameters] tbody tr[repeat]"),
        getTr: function(){
            return $('<tr repeat><td><input type="text" name="name"></td><td><select name="position"><option value="query">query</option><option value="header">header</option><option value="cookie">cookie</option></select></td><td><select name="type"><option value="string">string</option><option value="boolean">boolean</option><option value="number">number</option></select></td><td><select name="format"></select></td></td><td class="text-center"><label style="line-height: 20px;" class="mda-checkbox"><input type="checkbox" value="true" name="required"><em style="margin-right: 20px;"></em></label></td><td><input type="text" name="description" maxlength="40"></td><td class="text-center"><div class="icon-close"></div></td></tr>');
        },
        getTr1: function(){
            return $('<tr repeat><td><input type="text" name="name" readonly="readonly"></td><td><select name="position"><option value="path">path</option></select></td><td><select name="type"><option value="string">string</option><option value="boolean">boolean</option><option value="number">number</option></select></td><td><select name="format"></select></td><td class="text-center"><label style="line-height: 20px;" class="mda-checkbox"><input type="checkbox" value="true" name="required" disabled="disabled"><em style="margin-right: 20px;"></em></label></td><td><input type="text" name="description" maxlength="40"></td><td class="text-center"></td></tr>');
        },
        getTr2: function(){
            return $('<tr repeat><td><input type="text" name="name"></td><td><select name="type"><option value="string">string</option><option value="boolean">boolean</option><option value="number">number</option></select></td><td><select name="format"></select></td><td class="text-center"><label style="line-height: 20px;" class="mda-checkbox"><input type="checkbox" value="true" name="required" disabled="disabled"><em style="margin-right: 20px;"></em></label></td><td><input type="text" name="description" maxlength="40"></td><td class="text-center"><div class="icon-close"></div></td></tr>');
        },
        format:{//
            string:[
                {
                    name:"无",
                    value:""
                },
                {
                    name:"date",
                    value:"date"
                },
                {
                    name:"date-titme",
                    value:"date-titme"
                },
                {
                    name:"password",
                    value:"password"
                },
                {
                    name:"byte",
                    value:"byte"
                },
                {
                    name:"binary",
                    value:"binary"
                }
            ],
            number:[
                {
                    name:"无",
                    value:""
                },
                {
                    name:"float",
                    value:"float"
                },
                {
                    name:"double",
                    value:"double"
                }
            ]
        }
    };

    ApiTools.parameters.formatSelect = function(val, item, select){
    //<option value="">无</option><option value="date">date</option><option value="date-titme">date-titme</option><option value="password">password</option><option value="byte">byte</option><option value="binary">binary</option>
        var arr = ApiTools.parameters["format"][val] || [{
            name:"无",
            value:""
        }];
        var x = false;
        select.children().remove();
        for(var i=0, n=arr.length; i<n; i++)
        {
            var option = $('<option value="' + arr[i]["value"] + '">' + arr[i]["name"] + '</option>');
            if(arr[i]["value"] == item.format)
            {
                option.attr({selected:"selected"});
                x = true;
            }
            select.append(option);
        }
        if(!x)
        {
            item.format = arr[0]["value"];
        }
    };

    ApiTools.parameters.set = function(){
        $(".apiEdit table[name=parameters] tbody tr[repeat]").remove();
        $(".apiEdit #formBox table[name=form] tbody tr[repeat]").remove();
        if(!Swagger.parameters[ApiTools.method.selectMethod])
        {
            Swagger.parameters[ApiTools.method.selectMethod] = [];
        }
        Swagger.parameters[ApiTools.method.selectMethod].forEach(function(item){
            if(item.position=="path")
            {
                var tr = ApiTools.parameters.getTr1();
                tr.find("input[name=name]").val(item.name);
                tr.find("select[name=position] option[value=" + item.position + "]").attr({selected:"selected"});
                item.required && (tr.find("input[type=checkbox][name=required]").attr({checked:"checked"}));
                tr.find("select[name=type] option[value=" + item.type + "]").attr({selected:"selected"});
                tr.find("input[name=description]").val(item.description);
                ApiTools.parameters.nodeTbody.prepend(tr);
                tr.find(".icon-close").on("click",function(){
                    ApiTools.parameters.delete(item);
                });
                tr.find("select[name=type]").on("change",function(){
                    var val = $(this).find("option:selected").attr("value");
                    ApiTools.parameters.formatSelect(val, item, tr.find("select[name=format]"));
                });
                ApiTools.parameters.formatSelect(tr.find("select[name=type]").find("option:selected").attr("value"), item, tr.find("select[name=format]"));
                twoWayBinding(tr.find("input[name=name]"),item,"name");
                //twoWayBinding(tr.find("select[name=position]"),item,"position");
                //twoWayBinding(tr.find("input[type=checkbox][name=required]"),item,"required");
                twoWayBinding(tr.find("select[name=format]"),item,"format");
                twoWayBinding(tr.find("select[name=type]"),item,"type");
                twoWayBinding(tr.find("input[name=description]"),item,"description",true);
            }
            else if(item.position=="form")
            {
                var tr = ApiTools.parameters.getTr2();
                tr.find("input[name=name]").val(item.name);
                tr.find("select[name=position] option[value=" + item.position + "]").attr({selected:"selected"});
                item.required && (tr.find("input[type=checkbox][name=required]").attr({checked:"checked"}));
                tr.find("select[name=type] option[value=" + item.type + "]").attr({selected:"selected"});
                tr.find("input[name=description]").val(item.description);
                ApiTools.parameters.nodeTbodyForm.prepend(tr);
                tr.find(".icon-close").on("click",function(){
                    ApiTools.parameters.delete(item);
                });
                tr.find("select[name=type]").on("change",function(){
                    var val = $(this).find("option:selected").attr("value");
                    ApiTools.parameters.formatSelect(val, item, tr.find("select[name=format]"));
                });
                ApiTools.parameters.formatSelect(tr.find("select[name=type]").find("option:selected").attr("value"), item, tr.find("select[name=format]"));
                twoWayBinding(tr.find("input[name=name]"),item,"name");
                //twoWayBinding(tr.find("select[name=position]"),item,"position");
                //twoWayBinding(tr.find("input[type=checkbox][name=required]"),item,"required");
                twoWayBinding(tr.find("select[name=format]"),item,"format");
                twoWayBinding(tr.find("select[name=type]"),item,"type");
                twoWayBinding(tr.find("input[name=description]"),item,"description",true);
            }
            else if(item.position=="query" || item.position=="header" || item.position=="cookie")
            {
                var tr = ApiTools.parameters.getTr();
                tr.find("input[name=name]").val(item.name);
                tr.find("select[name=position] option[value=" + item.position + "]").attr({selected:"selected"});
                item.required && (tr.find("input[type=checkbox][name=required]").attr({checked:"checked"}));
                tr.find("select[name=type] option[value=" + item.type + "]").attr({selected:"selected"});
                tr.find("input[name=description]").val(item.description);
                ApiTools.parameters.nodeTbody.prepend(tr);
                tr.find(".icon-close").on("click",function(){
                    ApiTools.parameters.delete(item);
                });
                tr.find("select[name=type]").on("change",function(){
                    var val = $(this).find("option:selected").attr("value");
                    ApiTools.parameters.formatSelect(val, item, tr.find("select[name=format]"));
                });
                ApiTools.parameters.formatSelect(tr.find("select[name=type]").find("option:selected").attr("value"), item, tr.find("select[name=format]"));
                twoWayBinding(tr.find("input[name=name]"),item,"name");
                twoWayBinding(tr.find("select[name=position]"),item,"position");
                twoWayBinding(tr.find("input[type=checkbox][name=required]"),item,"required");
                twoWayBinding(tr.find("select[name=format]"),item,"format");
                twoWayBinding(tr.find("select[name=type]"),item,"type");
                twoWayBinding(tr.find("input[name=description]"),item,"description",true);
            }
        });
    };
    ApiTools.parameters.addNew = function(val,position){
        if(!val) return;
        if(!Swagger.parameters[ApiTools.method.selectMethod])
        {
            Swagger.parameters[ApiTools.method.selectMethod] = [];
        }
        var parameters = Swagger.parameters[ApiTools.method.selectMethod].filter(function(item){
            return item.name == val && item.position != "form" && item.position != "path";
        });
        if(!parameters.length)
        {
            var item = {
                "name":  val,
                "position":  position || "query",
                "required":  true,
                "type":  "string",
                "description":  "description"
            };
            Swagger.parameters[ApiTools.method.selectMethod].push(item);
            ApiTools.parameters.set();
        }
    };
    ApiTools.parameters.addNewForm = function(val,position){
        if(!val) return;
        if(!Swagger.parameters[ApiTools.method.selectMethod])
        {
            Swagger.parameters[ApiTools.method.selectMethod] = [];
        }
        var parameters = Swagger.parameters[ApiTools.method.selectMethod].filter(function(item){
            return item.name == val && item.position == "form";
        });
        if(!parameters.length)
        {
            var item = {
                "name":  val,
                "position":  "form",
                "required":  true,
                "type":  "string",
                "description":  "description"
            };
            Swagger.parameters[ApiTools.method.selectMethod].push(item);

            Swagger.parameters[ApiTools.method.selectMethod].forEach(function(item){
                item.consumes = ApiTools.body.type;
            });
            ApiTools.parameters.set();
        }
    };
    ApiTools.parameters.delete = function(item){
        if(!Swagger.parameters[ApiTools.method.selectMethod])
        {
            Swagger.parameters[ApiTools.method.selectMethod] = [];
        }
        Swagger.parameters[ApiTools.method.selectMethod] = Swagger.parameters[ApiTools.method.selectMethod].filter(function(_item){
            return !(item.name == _item.name && item.position == _item.position);
        });
        ApiTools.parameters.set();
    };
    ApiTools.parameters.addPath = function(){
        if(!Swagger.parameters[ApiTools.method.selectMethod])
        {
            Swagger.parameters[ApiTools.method.selectMethod] = [];
        }
        var pathList = getPathParameter(ApiTools.path.node.val());
        Swagger.parameters[ApiTools.method.selectMethod] = Swagger.parameters[ApiTools.method.selectMethod].filter(function(item){
            return item.position != "path";
        });
        if(pathList.length)
        {
            pathList.map(function(item){
                var parameters = Swagger.parameters[ApiTools.method.selectMethod].filter(function(_item){
                    return item == _item.name && _item.position == "path";
                });
                if(!parameters.length)
                {
                    var item = {
                        "name":  item,
                        "position":  "path",
                        "required":  true,
                        "type":  "string",
                        "description":  "description"
                    };
                    Swagger.parameters[ApiTools.method.selectMethod].push(item);
                }
                ApiTools.parameters.set();
            });
        }
        else {
            ApiTools.parameters.set();
        }
    };

    ApiTools.body = {
        type: "raw",
        getBodyParameters: function(){
            if(!Swagger.parameters[ApiTools.method.selectMethod])
            {
                Swagger.parameters[ApiTools.method.selectMethod] = [];
            }
            for(var i=0,n=Swagger.parameters[ApiTools.method.selectMethod].length; i<n; i++)
            {
                if(Swagger.parameters[ApiTools.method.selectMethod][i]["position"]=="body")
                {
                    return Swagger.parameters[ApiTools.method.selectMethod][i];
                }
            }
            return false;
        },
        getFormParameters: function(){
            if(!Swagger.parameters[ApiTools.method.selectMethod])
            {
                Swagger.parameters[ApiTools.method.selectMethod] = [];
            }
            for(var i=0,n=Swagger.parameters[ApiTools.method.selectMethod].length; i<n; i++)
            {
                if(Swagger.parameters[ApiTools.method.selectMethod][i]["position"]=="form")
                {
                    return Swagger.parameters[ApiTools.method.selectMethod][i];
                }
            }
            return false;
        }
    };

    ApiTools.body.delete = function(){
        Swagger.parameters[ApiTools.method.selectMethod] = Swagger.parameters[ApiTools.method.selectMethod].filter(function(item){
            return item.position != "body";
        });
        ApiTools.body.set();
    };

    ApiTools.body.addNew = function(val,ref){
        if(!val || !ref)
        {
            return;
        }
        if(!Swagger.parameters[ApiTools.method.selectMethod])
        {
            Swagger.parameters[ApiTools.method.selectMethod] = [];
        }
        Swagger.parameters[ApiTools.method.selectMethod].push({
            "name": val,
            "position": "body",
            "required": true,
            "description": val + " description",
            "ref": ref
        });
        ApiTools.body.set();
    };

    ApiTools.body.set = function(){
        var bodyParameters = ApiTools.body.getBodyParameters();
        var formParameters = ApiTools.body.getFormParameters();
        if(bodyParameters){
            ApiTools.body.type = "raw";
        }
        else if(formParameters)
        {
            ApiTools.body.type = formParameters.consumes;
        }
        else {
            ApiTools.body.type = "raw";
        }
        $('#body input[value="' + ApiTools.body.type + '"]').attr({checked:"checked"});
        $('#body input[value="' + ApiTools.body.type + '"]')[0].checked = true;
        if(ApiTools.body.type=="raw")
        {
            $(".workbench-body[type=raw]").show();
            $(".workbench-body[type=form]").hide();
        }
        else {
            $(".workbench-body[type=raw]").hide();
            $(".workbench-body[type=form]").show();
        }

        var select = $('select[name=bodyRef]');
        select.html("");
        for(var i= 0,n=Swagger.respModels.length; i<n; i++)
        {
            var option = $('<option value="' + Swagger.respModels[i]["name"] + '">' + Swagger.respModels[i]["name"] + '</option>');
            if(bodyParameters && Swagger.respModels[i]["name"] == bodyParameters.ref)
            {
                option.attr({selected:"selected"});
            }
            select.append(option);
        }
        if(bodyParameters)
        {
            $("#formBox input[name=bodyName]").off();
            $("#formBox input[name=bodyDescription]").off();
            select.off();
            $("#formBox input[name=bodyName]").val(bodyParameters.name);
            $("#formBox input[name=bodyDescription]").val(bodyParameters.description);
            twoWayBinding($("#formBox input[name=bodyName]"),bodyParameters,"name");
            twoWayBinding($("#formBox input[name=bodyDescription]"),bodyParameters,"description",true);
            twoWayBinding(select,bodyParameters,"ref");
            $(".apiEdit #formBox table[name=body] td .icon-close").off();
            $(".apiEdit #formBox table[name=body] td .icon-close").show();
            $(".apiEdit #formBox table[name=body] td .icon-close").on("click",function(){
                ApiTools.body.delete();
            });
        }
        else {
            $("#formBox input[name=bodyName]").off();
            $("#formBox input[name=bodyDescription]").off();
            select.off();
            $("#formBox input[name=bodyName]").val("");
            $("#formBox input[name=bodyDescription]").val("");
            $(".apiEdit #formBox table[name=body] td .icon-close").off();
            $(".apiEdit #formBox table[name=body] td .icon-close").hide();
            $("#formBox input[name=bodyName]").on("blur",function(){
                var ref = select.find("option:selected").val();
                ApiTools.body.addNew($(this).val(),ref);
            });
        }
    };

    ApiTools.respInfos = {
        node: $(".apiEdit #respInfos"),
        getResponse: function(item){
            var response = $('<div class="response" />');
            var responseBody = $('<div class="response-body">' +
                '<div class="triangle"></div>' +
                '<div class="response-tr" style="font-weight: 600;"><input name="code" value="' + item.code + '" type="number" /></div>' +
                '<div class="response-tr"><input maxlength="40" type="text" name="description" value="' + item.description + '" /></div>' +
                '<div class="response-tr" name="type"></div>' +
                '</div>');

            var types = ["object","array"];
            var select = $('<select />');
            for(var i= 0,n=types.length; i<n; i++)
            {
                var option = $('<option value="' + types[i] + '">' + types[i] + '</option>');
                if(types[i] == item.type)
                {
                    option.attr({selected:"selected"});
                }
                select.append(option);
            }

            var close = $('<div class="icon-close"></div>');
            close.on("click",function(){
                ApiTools.respInfos.delete(item);
            });

            responseBody.find("[name=type]").append(select);

            var responseModels = ApiTools.respInfos.getModels(item);
            response.append(close,responseBody,responseModels);
            twoWayBinding(responseBody.find("input[name=code]"),item,"code");
            twoWayBinding(responseBody.find("input[name=description]"),item,"description",true);
            twoWayBinding(select,item,"type");
            return response;

        },
        getModels: function(mod){
            var models = Swagger.respModels.filter(function(item){
                return item.name == mod.ref;
            })[0];
            if(models)
            {
                var responseModels = $('<div class="response-body" />');
                var tr1 = $('<div class="response-tr" />');
                var select = $('<select />');
                for(var i= 0,n=Swagger.respModels.length; i<n; i++)
                {
                    var option = $('<option value="' + Swagger.respModels[i]["name"] + '">' + Swagger.respModels[i]["name"] + '</option>');
                    if(Swagger.respModels[i]["name"] == mod.ref)
                    {
                        option.attr({selected:"selected"});
                    }
                    select.append(option);
                }
                twoWayBinding(select,mod,"ref");
                tr1.append(select);
                var tr2 = $('<div class="response-tr">' + models.contentType + '</div>');
                select.on("change",function(){
                    mod.ref = select.find("option:selected").attr("value");
                    var _models = Swagger.respModels.filter(function(item){
                        return item.name == mod.ref;
                    })[0];
                    tr2.html(_models.contentType);
                });
                responseModels.append(tr1,tr2);
                return responseModels;
            }
            return "";
        }
    };
    ApiTools.respInfos.delete = function(item){
        if(!Swagger.respInfos[ApiTools.method.selectMethod])
        {
            Swagger.respInfos[ApiTools.method.selectMethod] = [];
        }
        Swagger.respInfos[ApiTools.method.selectMethod] = Swagger.respInfos[ApiTools.method.selectMethod].filter(function(_item){
            return item.code != _item.code;
        });
        ApiTools.respInfos.set();
    };
    ApiTools.respInfos.get = function(){
        Swagger.respInfos = [];
    };
    ApiTools.respInfos.set = function(){
        ApiTools.respInfos.node.html("");
        if(!Swagger.respInfos[ApiTools.method.selectMethod])
        {
            Swagger.respInfos[ApiTools.method.selectMethod] = [];
        }
        Swagger.respInfos[ApiTools.method.selectMethod].forEach(function(item){
            var response = ApiTools.respInfos.getResponse(item);
            ApiTools.respInfos.node.prepend(response);
        });
    };
    ApiTools.respInfos.addNew = function(){
        if(Swagger.respModels.length)
        {
            var item = {
                "code":  "200",
                "description":  "description",
                "type":  "array",
                "ref":  Swagger.respModels[0]["name"]
            };
            Swagger.respInfos[ApiTools.method.selectMethod].push(item);
            ApiTools.respInfos.set();
        }
        else
        {
            console.log("error");
        }
    };

    ApiTools.alert = function(par){
        var W = screen.availWidth;
        var H = screen.availHeight;
        $("#mask .title").html(par.title || "提示");
        $("#mask .body").html(par.text || "");
        $("#mask button").on("click", function(){
            $("#mask").hide();
        });
        $("#mask").show();
    };

    /*
     ModelTools
     模型管理
     */
    var ModelTools = {};

    ModelTools.editor = ace.edit($(".modelEdit .respone-code")[0]);

    //设置风格和语言（更多风格和语言，请到github上相应目录查看）
    theme = "tomorrow_night";
    language = "json";
    ModelTools.editor.setTheme("ace/theme/" + theme);
    ModelTools.editor.session.setMode("ace/mode/" + language);

    //字体大小
    ModelTools.editor.setFontSize(12);

    //设置只读（true时只读，用于展示代码）
    ModelTools.editor.setReadOnly(false);
    ModelTools.editor.setOption("wrap", "free");
    ModelTools.save = function(){
        var error = ModelTools.isError();
        if(!error)
        {
            return false;
        }
        var url = 'api/property/info';
        var httpMethod = 'POST';
        if(ApiTools.method.selectMethod=="post"||ApiTools.method.selectMethod=="put")
        {
            if(ApiTools.body.type=="raw")
            {
                Swagger.parameters[ApiTools.method.selectMethod] = Swagger.parameters[ApiTools.method.selectMethod].filter(function(item){
                    return item.position != "form";
                });
                ApiTools.parameters.set();
            }
            else {
                Swagger.parameters[ApiTools.method.selectMethod] = Swagger.parameters[ApiTools.method.selectMethod].filter(function(item){
                    return item.position != "body";
                });
                Swagger.parameters[ApiTools.method.selectMethod].forEach(function(item){
                    item.consumes = ApiTools.body.type;
                });
                ApiTools.body.delete();
            }
        }

        Swagger.processorId = getProcessorId();
        Swagger.revision = getRevision(),
        Swagger.clientId = getClientId();
        return $.ajax({
            type: httpMethod,
            url: url,
            data: JSON.stringify(Swagger),
            processData: false,
            contentType: 'application/json',
            headers:{
                Authorization: token,
                Locale: locale,
            },
        }).then(function (response) {
            console.log(response);
            Initialization();
        }, function (xhr, status, error) {
            ApiTools.alert({text: xhr.responseText});
        });
    };
    ModelTools.model = {};
    ModelTools.model.addNew = function(){
        var item = {
            "id":  randomWord(),
            "name":  randomWord(),
            "description": "description",
            "contentType": ["application/json"],
            "properties":
            {

            }
        };
        Swagger.respModels.push(item);
        ModelTools.model.showList();
        ModelTools.model.select(item.id);
    };
    ModelTools.model.delete = function(id){
        var respModels = Swagger.respModels.filter(function(item){
            return item.id == id;
        });

        for(x in Swagger.respInfos)
        {
            var respInfos = Swagger.respInfos[x].filter(function(item){
                return item.ref == respModels[0]["name"];
            });

            if(respInfos.length)
            {
                ApiTools.alert({text: "模型 “" + respModels[0]["name"] + "” 在使用中，不能删除！"});
                return;
            }
        }

        Swagger.respModels = Swagger.respModels.filter(function(item){
            return item.id != id;
        });
        ModelTools.model.showList();
    };
    ModelTools.model.select = function(id){
        if(ModelTools.select)
        {
            ModelTools.select.attr({select:false});
        }
        ModelTools.select = $(".modelEdit li#" + id);
        ModelTools.select.attr({select:true});
        ModelTools.model.showInfo(id);

    };
    ModelTools.model.showInfo = function(id){
        var model = ModelTools.model.getModel(id);
        $(".modelEdit input[name=name]").val(model["name"]);
        $(".modelEdit input[name=description]").val(model["description"]);

        $(".modelEdit input[type=checkbox][name=contentType]:checked").each(function(){
            $(this)[0].checked = false;
        });
        model.contentType.forEach(function(item){
            $(".modelEdit input[type=checkbox][name=contentType][value='" + item + "']")[0].checked = true;
        });

        var jsonText = formatCode(model.properties,0,"    ");
        ModelTools.editor.setValue(jsonText);
        ModelTools.editor.gotoLine(1);

    };
    ModelTools.model.getModel = function(id){
        for(var i= 0,n=Swagger.respModels.length; i<n; i++)
        {
            if(Swagger.respModels[i]["id"]==id)
            {
                return Swagger.respModels[i];
            }
        }
    };
    ModelTools.model.showList = function(){
        var ul = $(".modelEdit .left ul");
        ul.find("li").remove();
        for(var i= 0,n=Swagger.respModels.length; i<n; i++)
        {
            (function(item){
                var li = $('<li id="' + item["id"] + '"></li>');
                var title = $('<div class="li-title">' + item["name"] + '</div>');
                var close = $('<div class="icon-close"></div>');
                li.append(title,close);
                close.on("click",function(){
                    ModelTools.model.delete(item["id"]);
                });
                li.on("click",function(){
                    ModelTools.model.select(item["id"]);
                });
                ul.append(li);
                if(!i)
                {
                    ModelTools.model.select(item["id"]);
                }
            })(Swagger.respModels[i],i);
        }
        if(Swagger.respModels.length)
        {
            $("#saveModel").show();
        }
        else
        {
            $("#saveModel").hide();
        }
    };

    function autoInput(){
        $(".modelEdit input[name=name]").on("input",function(){
            var model = ModelTools.model.getModel(ModelTools.select.attr("id"));
            ModelTools.select.find(".li-title").html($(this).val());
            model.name = $(this).val();
        });
        $(".modelEdit input[name=contentType]").on("click",function(e){
            var model = ModelTools.model.getModel(ModelTools.select.attr("id"));
            var val = [];
            $(".modelEdit [name=contentType]:checked").each(function(){
                val.push($(this).val());
            });
            model.contentType = val;
        });
        $(".modelEdit .respone-code > textarea").on('input propertychange', function() {
            formatJson();
        });
        $(".modelEdit .title-icon.icon-add-big").on("click",function(){
            ModelTools.model.addNew();
        });
        $(".modelEdit button#saveModel").on("click",function(){
            //Initialization(true);
            var error = ModelTools.isError();
            if(error)
            {
                for(x in ApiTools) {
                    if(ApiTools[x]["set"]) {
                        ApiTools[x]["set"]();
                    }
                }
                ApiTools.alert({text: "保存模型成功!"});
            }
        });
    }

    function randomWord(){
        return Math.random().toString(36).substr(2);
    }

    function trim(x) {
        return x.replace(/^\s+|\s+$/gm,'');
    }

    function formatJson(){
        if(ModelTools.select) {
            var id = ModelTools.select.attr("id");
            var model = ModelTools.model.getModel(ModelTools.select.attr("id"));
            var value = ModelTools.editor.getValue();
            value = value.replace(/\n/g, "");
            value = trim(value);
            try {
                var valueJson = JSON.parse(value);
                model.properties = valueJson;
                deleteErrorModel(id)
            } catch (e) {
                try {
                    var valueJson = eval('(' + value + ')');
                    model.properties = valueJson;
                    deleteErrorModel(id);
                } catch (e) {
                    addErrorModel(id);
                }
            }
        }
    }

    ModelTools.errorModel = {};

    ModelTools.isError = function()
    {
        formatJson();
        var names = "";
        for(x in ModelTools.errorModel)
        {
            var model = ModelTools.model.getModel(ModelTools.select.attr("id"));
            names += names ? ",“" + model.name + "”" : "“" + model.name + "”";
        }
        var pathReg = /^\/(([\w\{\}\-\.,@%\+:])+[\/]?)*$/;
        if(names)
        {
            ApiTools.alert({text: "模型 " + names + " 格式错误，请重新编辑相关模型！"});
            return false;
        }
        else if(!pathReg.test(Swagger.path))
        {
            ApiTools.alert({text: "路径格式错误，请重新输入路径！", title:"路径格式错误"});
            return false;
        }
        {
            return true;
        }
    };

    function addErrorModel(id){
        ModelTools.errorModel[id] = true;
    }
    function deleteErrorModel(id){
        delete ModelTools.errorModel[id];
    }

    function formatCode(input,n,space) {
        if (typeof(input)=="undefined"||input==null) return 'null';
        n = n || 0;
        var Space = getSpace(n,space);
        var Space1 = getSpace(n+1,space);
        switch (typeof(input)) {
            case "string":
                //return '"' + ((input.replace(/\"/gi,"\\\"")).replace(/\n/gi,"")).replace(/\r/gi,"") + '"';
                return '"' + html_encode_json(input) + '"';
            //str += "<br />" + k + " : " + html_encode(input[k]);
            case "number":
                return input.toString();//number
            case "boolean":
                return input.toString();//boolean
            case "object": //object
                if(Array.isArray(input))
                {
                    var buf = [];
                    for (var i=0; i<input.length; i++)
                    {
                        if(formatCode(input[i],n+1))
                        {
                            buf.push(formatCode(input[i],n+1,space));
                        }
                    }
                    var ent = ((buf[buf.length - 2]+"").indexOf("\n")!="-1" || (buf[buf.length - 1]+"").indexOf("\n")!="-1")?"\n" + Space:"";
                    return '[' + buf.join(',') + ent + ']';
                }
                else
                {
                    var buf = [];
                    for (var k in input)
                    {
                        buf.push("\n" + Space1  + '"' + k + '":  ' + formatCode(input[k],n+1,space));
                    }
                    return (n?'\n':'') + Space + '{' + Space1 + buf.join(',') + '\n' + Space + '}';
                }
            case "function": //object
                return false;
            default:
                return false;
        }
    }

    function getSpace(n,space){
        var str = "";
        for(var i=0;i<n;i++)
        {
            str += space || "        ";
        }
        return str;
    }

    function html_encode_json(str)
    {
        if (str.length == 0) return "";
        str = str.replace(/\'/g, "\\\'");
        str = str.replace(/\"/g, "\\\"");
        //s = s.replace(/\n/g, "<br>");
        //console.log(s);
        return str;
    }

    (function(){
        // ApiTools.method.selectMethod = Swagger.methods[0];
        $("#respInfosAdd").on("click",function(){
            ApiTools.respInfos.addNew();
        });
        $("input[name=parametersAddNew]").on("blur",function(){
            ApiTools.parameters.addNew($(this).val());
            $(this).val("");
        });
        $("input[name=formAddNew]").on("blur",function(){
            ApiTools.parameters.addNewForm($(this).val());
            $(this).val("");
        });
        $("input[name=path]").on("input",function(){
            ApiTools.parameters.addPath();
        });
        $("button#saveAll").on("click",function(){
            ModelTools.save();
        });

        $("#body input").on("click",function(){
            ApiTools.body.type = $(this).val();
            if($(this).val()=="raw")
            {
                $(".workbench-body[type=raw]").show();
                $(".workbench-body[type=form]").hide();
            }
            else {
                $(".workbench-body[type=raw]").hide();
                $(".workbench-body[type=form]").show();
            }
        });
        autoInput();
        Initialization();
        /*
        ajax 请求
         */
        $.ajax({
            type: 'GET',
            url: 'api/property/info?' + $.param({
                processorId: getProcessorId()
            }),
            headers:{
                Authorization: token,
                Locale: locale,
            },
        }).done(function (response) {
            Swagger = response;
            Swagger.respModels.forEach(function(item){
                item.id = randomWord();
            });

            if (Swagger.methods.length > 0) {
                ApiTools.method.selectMethod = Swagger.methods[0];
            };

            ModelTools.model.showList();
            for(x in ApiTools) {
                if(ApiTools[x]["set"]) {
                    ApiTools[x]["set"]();
                }
            }
        });
    })();

    /*
     Initialization
     保存后重新刷新
     */
    function Initialization(boo){
        if(boo)
        {
            var model = ModelTools.model.getModel(ModelTools.select.attr("id"));
            model.name = $(".modelEdit input[name=name]").val();
            var val = [];
            $(".modelEdit [name=contentType]:checked").each(function(){
                val.push($(this).val());
            });
            model.contentType = val;
            formatJson();
        }

        /*
        ajax 请求
         */
        /*$.ajax({
            type: 'GET',
            url: 'http://jsonplaceholder.typicode.com/posts/1',
            dataType: 'jsonp'
        }).done(function (response) {
            Swagger = {
                "method":  ["GET"],
                "title":  "test tittle",
                "host":  "localhost",
                "path":  "/testssss",
                "basePath":  "/v1",
                "description":  "this is description",
                "summary":  "this is summary",
                "contentType":["application/json"],
                "parameters":  [],
                "respInfos":  [],
                "respModels":  []
            };
            console.log("response",response);
            Swagger.respModels.forEach(function(item){
                item.id = randomWord();
            });
            ModelTools.model.showList();
            for(x in ApiTools) {
                if(ApiTools[x]["set"]) {
                    ApiTools[x]["set"]();
                }
            }
        });*/

        /*
        静态模式
         */
        // Swagger.respModels.forEach(function(item){
        //     item.id = randomWord();
        // });
        // ModelTools.model.showList();
        // for(x in ApiTools) {
        //     if(ApiTools[x]["set"]) {
        //         ApiTools[x]["set"]();
        //     }
        // }
    }

    function twoWayBinding(input,obj,key,boo){
        if(input[0].nodeName=="INPUT")
        {
            if(input.attr("type")=='radio')
            {
                //input.off();
                input.on("click",function(e){
                    obj[key] = input.val();
                    console.log("Swagger",Swagger);
                });
            }
            else if(input.attr("type")=='checkbox')
            {
                //input.off();
                if(key=="contentType")
                {
                    input.on("click",function(e){
                        if(Array.isArray(obj[key][ApiTools.method.selectMethod]))
                        {
                            var val = [];
                            var name = input.attr("name");
                            $("[name=" + name + "]:checked").each(function(){
                                val.push($(this).val());
                            });
                            obj[key][ApiTools.method.selectMethod] = val;
                        }
                        else
                        {
                            if(input[0].checked)
                            {
                                obj[key][ApiTools.method.selectMethod] = true;
                            }
                            else
                            {
                                obj[key][ApiTools.method.selectMethod] = false;
                            }
                        }
                    });
                }
                else {
                    input.on("click",function(e){
                        if(Array.isArray(obj[key]))
                        {
                            var val = [];
                            var name = input.attr("name");
                            $("[name=" + name + "]:checked").each(function(){
                                val.push($(this).val());
                            });
                            obj[key] = val;
                        }
                        else
                        {
                            if(input[0].checked)
                            {
                                obj[key] = true;
                            }
                            else
                            {
                                obj[key] = false;
                            }
                        }
                    });
                }
            }
            else {
                //input.off();
                if(key=="description" && !boo)
                {
                    input.on("input",function(e){
                        obj[key][ApiTools.method.selectMethod] = input.val();
                    });
                }
                else {
                    input.on("input",function(e){
                        obj[key] = input.val();
                    });
                }
            }
        }
        else if(input[0].nodeName=="SELECT")
        {
            //input.off();
            input.on("change",function(e){
                var val = input.find("option:selected").attr("value") || input.find("option:selected").text() || input.find("option").eq(0).attr("value") || input.find("option").eq(0).text();
                obj[key] = val;
            });
        }
    }

    function getPathParameter(input){
        var pathList = input.match(/\{([a-zA-Z_\-]+)+[a-zA-Z0-9]*[\}]/gi);
        if(pathList)
        {
            var arr = pathList.map(function(item){
                return item.slice(1,-1);
            });
            return arr;
        }
        return [];
    }

    function getClientId() {
        return $('#http-request-client-id').text();
    }

    function getRevision() {
        return $('#http-request-revision').text();
    }

    function getProcessorId() {
        return $('#http-request-processor-id').text();
    }
});
