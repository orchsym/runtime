/**
 * Created by wz on 2018/6/19.
 */
$(function(){
    var Swagger = {};
    var ApiTools = {};

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
    ApiTools.description.get = function(){
        Swagger.description = ApiTools.description.node.input.val();
    };
    ApiTools.description.set = function(){
        ApiTools.description.node.val(Swagger.description);
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
        var method = Swagger.method;
        $(".apiEdit input[type=checkbox][name=method]:checked").each(function(){
            $(this)[0].checked = false;
        });
        method.forEach(function(item){
            $(".apiEdit input[type=checkbox][name=method][value='" + item + "']")[0].checked = true;
        });
        twoWayBinding(ApiTools.method.node,Swagger,"method");
    };

    ApiTools.contentType = {
        node: $(".apiEdit input[type=checkbox][name=rootContentType]")
    };
    ApiTools.contentType.get = function(){
        var contentType = [];
        $(".apiEdit input[type=checkbox][name=rootContentType]:checked").each(function(){
            contentType.push($(this).val());
        });
        Swagger.contentType = contentType;
    };
    ApiTools.contentType.set = function(){
        var contentType = Swagger.contentType;
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
        nodeTbodyTr: $(".apiEdit table[name=parameters] tbody tr[repeat]"),
        getTr: function(){
            return $('<tr repeat><td><input type="text" name="name"></td><td><select name="position"><option value="query">query</option><option value="header">header</option><option value="cookie">cookie</option></select></td><td><select name="type"><option value="string">string</option><option value="boolean">boolean</option><option value="number">number</option></select></td><td class="text-center"><label style="line-height: 20px;" class="mda-checkbox"><input type="checkbox" value="true" name="required"><em style="margin-right: 20px;"></em></label></td><td><input type="text" name="description"></td><td class="text-center"><div class="icon-close"></div></td></tr>');
        },
        getTr1: function(){
            return $('<tr repeat><td><input type="text" name="name" readonly="readonly"></td><td><select name="position"><option value="path">path</option></select></td><td><select name="type"><option value="string">string</option><option value="boolean">boolean</option><option value="number">number</option></select></td><td class="text-center"><label style="line-height: 20px;" class="mda-checkbox"><input type="checkbox" value="true" name="required" disabled="disabled"><em style="margin-right: 20px;"></em></label></td><td><input type="text" name="description"></td><td class="text-center"></td></tr>');
        }
    };
    ApiTools.parameters.get = function(){
        Swagger.parameters = [];
        ApiTools.parameters.nodeTbodyTr.forEach(function(){
            var item = {};
            item.name = $(this).find("input[name=name]").val();
            item.position = $(this).find("select[name=position] option:selected").val();
            item.required = $(this).find("input[type=checkbox][name=required]").checked;
            item.type = $(this).find("select[name=type] option:selected").val();
            item.description = $(this).find("input[name=description]").val();
            Swagger.parameters.push(item);
        });
    };
    ApiTools.parameters.set = function(){
        $(".apiEdit table[name=parameters] tbody tr[repeat]").remove();
        Swagger.parameters.forEach(function(item){
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
                twoWayBinding(tr.find("input[name=name]"),item,"name");
                //twoWayBinding(tr.find("select[name=position]"),item,"position");
                //twoWayBinding(tr.find("input[type=checkbox][name=required]"),item,"required");
                twoWayBinding(tr.find("select[name=type]"),item,"type");
                twoWayBinding(tr.find("input[name=description]"),item,"description");
            }
            else
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
                twoWayBinding(tr.find("input[name=name]"),item,"name");
                twoWayBinding(tr.find("select[name=position]"),item,"position");
                twoWayBinding(tr.find("input[type=checkbox][name=required]"),item,"required");
                twoWayBinding(tr.find("select[name=type]"),item,"type");
                twoWayBinding(tr.find("input[name=description]"),item,"description");
            }
        });
    };
    ApiTools.parameters.addNew = function(val,position){
        if(!val) return;
        var parameters = Swagger.parameters.filter(function(item){
            return item.name == val;
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
            Swagger.parameters.push(item);
            ApiTools.parameters.set();
        }
    };
    ApiTools.parameters.delete = function(item){
        Swagger.parameters = Swagger.parameters.filter(function(_item){
            return item.name != _item.name;
        });
        console.log(Swagger.parameters);
        ApiTools.parameters.set();
    };
    ApiTools.parameters.addPath = function(){
        var pathList = getPathParameter(ApiTools.path.node.val());
        Swagger.parameters = Swagger.parameters.filter(function(item){
            return item.position != "path";
        });
        if(pathList.length)
        {
            pathList.map(function(item){
                var parameters = Swagger.parameters.filter(function(_item){
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
                    Swagger.parameters.push(item);
                }
                ApiTools.parameters.set();
            });
        }
        else {
            ApiTools.parameters.set();
        }
    };

    ApiTools.respInfos = {
        node: $(".apiEdit #respInfos"),
        getResponse: function(item){
            var response = $('<div class="response" />');
            var responseBody = $('<div class="response-body">' +
                '<div class="triangle"></div>' +
                '<div class="response-tr" style="font-weight: 600;"><input type="text" name="code" value="' + item.code + '" /></div>' +
                '<div class="response-tr"><input type="text" name="description" value="' + item.description + '" /></div>' +
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
            twoWayBinding(responseBody.find("input[name=description]"),item,"description");
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
                var tr3 = $('<div class="response-tr">' + models.description + '</div>');
                select.on("change",function(){
                    mod.ref = select.find("option:selected").attr("value");
                    var _models = Swagger.respModels.filter(function(item){
                        return item.name == mod.ref;
                    })[0];
                    tr2.html(_models.contentType);
                    tr3.html(_models.description);
                });
                responseModels.append(tr1,tr2,tr3);
                return responseModels;
            }
            return "";
        }
    };
    ApiTools.respInfos.delete = function(item){
        Swagger.respInfos = Swagger.respInfos.filter(function(_item){
            return item.code != _item.code;
        });
        ApiTools.respInfos.set();
    };
    ApiTools.respInfos.get = function(){
        Swagger.respInfos = [];
    };
    ApiTools.respInfos.set = function(){
        ApiTools.respInfos.node.html("");
        Swagger.respInfos.forEach(function(item){
            var response = ApiTools.respInfos.getResponse(item);
            ApiTools.respInfos.node.prepend(response);
        });
    };
    ApiTools.respInfos.addNew = function(){
        if(Swagger.respModels.length)
        {
            var item = {
                "code":  "code",
                "description":  "description",
                "type":  "array",
                "ref":  Swagger.respModels[0]["name"]
            };
            Swagger.respInfos.push(item);
            ApiTools.respInfos.set();
        }
        else
        {
            console.log("error");
        }
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
        var url = 'api/property/info';
        var httpMethod = 'POST';
        Swagger.processorId = getProcessorId();
        Swagger.revision = getRevision(),
        Swagger.clientId = getClientId();

        return $.ajax({
            type: httpMethod,
            url: url,
            data: JSON.stringify(Swagger),
            processData: false,
            contentType: 'application/json'
        }).then(function (response) {
            console.log(response);
            Initialization();
        }, function (xhr, status, error) {
            console.log(xhr.responseText);
            alert(xhr.responseText);
        });
    };
    ModelTools.model = {};
    ModelTools.model.addNew = function(){
        var item = {
            "id":  randomWord(),
            "name":  randomWord(),
            "description": "description",
            "contentType": ["application/json"],
            "properties":{}
        };
        Swagger.respModels.push(item);
        ModelTools.model.showList();
        ModelTools.model.select(item.id);
    };

    ModelTools.model.delete = function(id){
        var respModels = Swagger.respModels.filter(function(item){
            return item.id == id;
        });

        var respInfos = Swagger.respInfos.filter(function(item){
            return item.ref == respModels[0]["name"];
        });

        if(respInfos.length)
        {
            alert("模型使用中，无法删除！");
            return;
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
    };

    function autoInput(){
        $(".modelEdit input[name=name]").on("input",function(){
            var model = ModelTools.model.getModel(ModelTools.select.attr("id"));
            ModelTools.select.find(".li-title").html($(this).val());
            model.name = $(this).val();
        });
        $(".modelEdit input[name=description]").on("input",function(){
            var model = ModelTools.model.getModel(ModelTools.select.attr("id"));
            model.description = $(this).val();
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
            for(x in ApiTools) {
                if(ApiTools[x]["set"]) {
                    ApiTools[x]["set"]();
                }
            }
            alert("保存模型成功!");
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
            var model = ModelTools.model.getModel(ModelTools.select.attr("id"));
            var value = ModelTools.editor.getValue();
            value = value.replace(/\n/g, "");
            value = trim(value);
            try {
                var valueJson = JSON.parse(value);
                model.properties = valueJson;
            } catch (e) {
                try {
                    var valueJson = eval('(' + value + ')');
                    model.properties = valueJson;
                } catch (e) {
                }
            }
        }
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
        $("#respInfosAdd").on("click",function(){
            ApiTools.respInfos.addNew();
        });
        $("input[name=parametersAddNew]").on("blur",function(){
            ApiTools.parameters.addNew($(this).val());
            $(this).val("");
        });
        $("input[name=path]").on("input",function(){
            ApiTools.parameters.addPath();
        });
        $("button#saveAll").on("click",function(){
            ModelTools.save();
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
            })
        }).done(function (response) {
            Swagger = response;
            Swagger.respModels.forEach(function(item){
                item.id = randomWord();
            });
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
            model.description = $(".modelEdit input[name=description]").val();
            var val = [];
            $(".modelEdit [name=contentType]:checked").each(function(){
                val.push($(this).val());
            });
            model.contentType = val;
            formatJson();
        }


        /*
        静态模式
         */
        /*Swagger.respModels.forEach(function(item){
            item.id = randomWord();
        });
        ModelTools.model.showList();
        for(x in ApiTools) {
            if(ApiTools[x]["set"]) {
                ApiTools[x]["set"]();
            }
        }*/
    }

    function twoWayBinding(input,obj,key){
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
            else {
                //input.off();
                input.on("input",function(e){
                    obj[key] = input.val();
                });
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