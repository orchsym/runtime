/**
 * Created by wz on 2018/6/19.
 */
$(function(){
    var respModels = [
        {
            "name": "Product",
            "description": "Unique identifier representing a specific product",
            "contentType": ["application/json"],
            "properties":
            {
                "product_id":
                {
                    "type":  "string",
                    "description":  "Unique identifier representing a specific product"
                },
                "url":
                {
                    "type":  "string",
                    "description":  "url of a specific product"
                }
            }
        },
        {
            "name":  "Error",
            "description": "message of an error",
            "contentType": ["application/xml"],
            "properties":
            {
                "message":
                {
                    "type":  "string",
                    "description":  "message of an error"
                }
            }
        }
    ];
    respModels.forEach(function(item){
        item.id = randomWord();
    });
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
        console.log("respModels",respModels);
        return;
        return $.ajax({
            type: httpMethod,
            url: url,
            data: JSON.stringify(respModels),
            processData: false,
            contentType: 'application/json'
        }).then(function (response) {
            console.log(response);
        }, function (xhr, status, error) {
            console.log(xhr.responseText);
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
        respModels.push(item);
        ModelTools.model.showList();
    };
    ModelTools.model.delete = function(id){
        respModels = respModels.filter(function(item){
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
        for(var i= 0,n=respModels.length; i<n; i++)
        {
            if(respModels[i]["id"]==id)
            {
                return respModels[i];
            }
        }
    };
    ModelTools.model.showList = function(){
        var ul = $(".modelEdit .left ul");
        ul.find("li").remove();
        for(var i= 0,n=respModels.length; i<n; i++)
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
            })(respModels[i],i);
        }
    };
    ModelTools.model.showList();
    autoInput();
    function autoInput(){
        if(ModelTools.select)
        {
            $(".modelEdit input[name=name]").on("input",function(){
                var model = ModelTools.model.getModel(ModelTools.select.attr("id"));
                console.log("model",model);
                ModelTools.select.find(".li-title").html($(this).val());
                model.name = $(this).val();
            });
            $(".modelEdit input[name=description]").on("input",function(){
                var model = ModelTools.model.getModel(ModelTools.select.attr("id"));
                model.description = $(this).val();
            });
            $(".modelEdit input[name=contentType]").on("click",function(e){
                var val = [];
                $(".modelEdit [name=contentType]:checked").each(function(){
                    val.push($(this).val());
                });
                model.contentType = val;
            });
            $(".modelEdit .respone-code > textarea").on('input propertychange', function() {
                formatJson();
            });
        }
        $(".modelEdit .title-icon.icon-add-big").on("click",function(){
            ModelTools.model.addNew();
        });
        $(".modelEdit button#saveModel").on("click",function(){
            ModelTools.save();
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
});