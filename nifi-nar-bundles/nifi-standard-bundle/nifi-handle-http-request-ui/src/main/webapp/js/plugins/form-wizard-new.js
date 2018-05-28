var FormWizard = function () {


    return {
        //main function to initiate the module
        init: function () {
            if (!jQuery().bootstrapWizard) {
                return;
            }

            function format(state) {
                if (!state.id) return state.text; // optgroup
                return "<img class='flag' src='../../assets/global/img/flags/" + state.id.toLowerCase() + ".png'/>&nbsp;&nbsp;" + state.text;
            }

            $("#country_list").select2({
                placeholder: "Select",
                allowClear: true,
                formatResult: format,
                formatSelection: format,
                escapeMarkup: function (m) {
                    return m;
                }
            });

            var form    = $('#submit_form');
            var error   = $('.alert-danger', form);
            var success = $('.alert-success', form);

            form.validate({
                doNotHideMessage: true, //this option enables to show the error/success messages on tab switch.
                errorElement: 'span', //default input error message container
                errorClass: 'help-block help-block-error', // default input error message class
                focusInvalid: false, // do not focus the last invalid input
                rules: {
                    //account
                    username: {
                        minlength: 1,
                        required: true,
                        projectname: true
                    },
                    iwa: {
                        minlength: 1,
                        required: true,
                        iwa: true,
                        iwcmdRepeat: true,
                        javareservedword: true
                    },
                    password: {
                        minlength: 5,
                        required: true
                    },
                    rpassword: {
                        minlength: 5,
                        required: true,
                        equalTo: "#submit_form_password"
                    },
                    //profile
                    fullname: {
                        required: true
                    },
                    email: {
                        required: true,
                        email: true
                    },
                    phone: {
                        required: true
                    },
                    gender: {
                        required: true
                    },
                    address: {
                        required: true
                    },
                    city: {
                        required: true
                    },
                    country: {
                        required: true
                    },
                    //payment
                    card_name: {
                        required: true
                    },
                    card_number: {
                        minlength: 16,
                        maxlength: 16,
                        required: true
                    },
                    card_cvc: {
                        digits: true,
                        required: true,
                        minlength: 3,
                        maxlength: 4
                    },
                    card_expiry_date: {
                        required: true
                    },
                    'payment[]': {
                        required: true,
                        minlength: 1
                    }
                },

                messages: { // custom messages for radio buttons and checkboxes
                    'payment[]': {
                        required: "Please select at least one option",
                        minlength: jQuery.validator.format("Please select at least one option")
                    }
                },

                errorPlacement: function (error, element) { // render error placement for each input type
                    if (element.attr("name") == "gender") { // for uniform radio buttons, insert the after the given container
                        error.insertAfter("#form_gender_error");
                    } else if (element.attr("name") == "payment[]") { // for uniform checkboxes, insert the after the given container
                        error.insertAfter("#form_payment_error");
                    } else {
                        error.insertAfter(element); // for other inputs, just perform default behavior
                    }
                },

                invalidHandler: function (event, validator) { //display error alert on form submit
                    success.hide();
                    error.show();
                    Metronic.scrollTo(error, -200);
                },

                highlight: function (element) { // hightlight error inputs
                    $(element)
                        .closest('.form-group').removeClass('has-success').addClass('has-error'); // set error class to the control group
                },

                unhighlight: function (element) { // revert the change done by hightlight
                    $(element)
                        .closest('.form-group').removeClass('has-error'); // set error class to the control group
                },

                success: function (label) {
                    if (label.attr("for") == "gender" || label.attr("for") == "payment[]") { // for checkboxes and radio buttons, no need to show OK icon
                        label
                            .closest('.form-group').removeClass('has-error').addClass('has-success');
                        label.remove(); // remove error label here
                    } else { // display success icon for other inputs
                        label
                            .addClass('valid') // mark the current input as valid and display OK icon
                            .closest('.form-group').removeClass('has-error').addClass('has-success'); // set success class to the control group
                    }
                },

                submitHandler: function (form) {
                    success.show();
                    error.hide();
                    //add here some ajax code to submit your form or just call form.submit() if you want to submit the form without ajax
                }

            });

            var displayConfirm = function () {
                $('#tab4 .form-control-static', form).each(function () {
                    var input = $('[name="' + $(this).attr("data-display") + '"]', form);
                    if (input.is(":radio")) {
                        input = $('[name="' + $(this).attr("data-display") + '"]:checked', form);
                    }
                    if (input.is(":text") || input.is("textarea")) {
                        $(this).html(input.val());
                    } else if (input.is("select")) {
                        $(this).html(input.find('option:selected').text());
                    } else if (input.is(":radio") && input.is(":checked")) {
                        $(this).html(input.attr("data-title"));
                    } else if ($(this).attr("data-display") == 'payment[]') {
                        var payment = [];
                        $('[name="payment[]"]:checked', form).each(function () {
                            payment.push($(this).attr('data-title'));
                        });
                        $(this).html(payment.join("<br>"));
                    }
                });
            }

            var handleTitle = function (tab, navigation, index) {
                var total   = navigation.find('li').length;
                var current = index + 1;
                // set wizard title
                $('.step-title', $('#form_wizard_1')).text('Step ' + (index + 1) + ' of ' + total);
                // set done steps
                jQuery('li', $('#form_wizard_1')).removeClass("done");
                var li_list = navigation.find('li');
                for (var i = 0; i < index; i++) {
                    jQuery(li_list[i]).addClass("done");
                }

                if (current == 1) {
                    $('#form_wizard_1').find('.button-previous').hide();
                    $('#form_wizard_1').find('.button-backindex').show();
                } else if (current == 2) {
                    $('#form_wizard_1').find('.button-previous').hide();
                    $('#form_wizard_1').find('.button-backindex').hide();
                } else {
                    $('#form_wizard_1').find('.button-previous').show();
                    $('#form_wizard_1').find('.button-backindex').hide();
                }

                if (current >= total) {
                    $('#form_wizard_1').find('.button-next').hide();
                    $('#form_wizard_1').find('.button-submit').show();
                    displayConfirm();
                } else {
                    $('#form_wizard_1').find('.button-next').show();
                    $('#form_wizard_1').find('.button-submit').hide();
                }
                Metronic.scrollTo($('.page-title'));
            }

            // default form wizard
            $('#form_wizard_1').bootstrapWizard({
                'nextSelector': '.button-next',
                'previousSelector': '.button-previous',
                onTabClick: function (tab, navigation, index, clickedIndex) {
                    return false;
                    /*
                     success.hide();
                     error.hide();
                     if (form.valid() == false) {
                     return false;
                     }
                     handleTitle(tab, navigation, clickedIndex);
                     */
                },
                onNext: function (tab, navigation, index) {
                    success.hide();
                    error.hide();

                    if (form.valid() == false) {
                        return false;
                    }

                    var current = index + 1;
                    switch (current) {
                        case 1:
                            break;
                        case 2:
                            //break;
                            var name       = $("#project-name").val();
                            var desc       = $("#project-desc").val();
                            var createProj = JSON.parse(window.javaBridge.invoke('createProj', new Array(name, desc)));
                            if (!createProj.success) {
                                return false;
                            }
                            break;
                        case 3:
                            var apis       = [];
                            //有值： {"resultSet":[{"nes2":[{"apipath":"\\internetware\\ddf84bd019bd4ce7817cbf9847d50833\\apis\\a41b643f22234d24b3cc8e947ca550c3","rsphandler":"nes2.handler.Nes2Handler","charset":"UTF-8","api":"a41b643f22234d24b3cc8e947ca550c3","iwcmd":"nes2"},{"apipath":"\\internetware\\ddf84bd019bd4ce7817cbf9847d50833\\apis\\f47052a3360948a8ba4cb2422dd4639e","rsphandler":"nes2.handler.Nes2Handler","charset":"UTF-8","api":"f47052a3360948a8ba4cb2422dd4639e","iwcmd":"nes2"}]}],"success":true}
                            //为空： {"resultSet":[{"news1":[]}],"success":true}
                            var genAPINext = JSON.parse(window.javaBridge.invoke('genAPINext', new Array()));
                            if (!genAPINext.success) {
                                return false;
                            }
                            apis = genAPINext.resultSet;
                            // apis = [{
                            //    "apiName1": [{"api": "1111", "apiPath": "/path/to/api/file"}, {
                            //        "api": "1111",
                            //        "apiPath": "/path/to/api/file",
                            //        "rsphandler":"nes2.handler.Nes2Handler",
                            //    }]
                            // },
                            //    {
                            //        "apiName2": [{"api": "1111", "apiPath": "/path/to/api/file"}, {
                            //            "api": "1111",
                            //            "apiPath": "/path/to/api/file",
                            //            "rsphandler":"nes2.handler.Nes2Handler",
                            //        }]
                            //    },
                            //    {
                            //        "apiName3": [{"api": "1111", "apiPath": "/path/to/api/file"}, {
                            //            "api": "1111",
                            //            "apiPath": "/path/to/api/file",
                            //            "rsphandler":"nes2.handler.Nes2Handler",
                            //        }]
                            //    },
                            //    {
                            //        "apiName4": [{"api": "1111", "apiPath": "/path/to/api/file"}, {
                            //            "api": "1111",
                            //            "apiPath": "/path/to/api/file",
                            //            "rsphandler":"nes2.handler.Nes2Handler",
                            //        }]
                            //    }];
                            setApiSelectionPage(apis, '{}');
                            break;
                        case 4:
                            var apiInfos = [];
                            $("input:radio[class='apiRspHandler']:checked").each(function() {
                                var apiInfo = {
                                    apiname: $(this).attr('name'),
                                    api: $(this).attr('data-name'),
                                    rspHandler: $(this).attr('rspHandler'),
                                    apipath: $(this).attr('data-path'),
                                    charset: $(this).attr('api-charset'),
                                    bscmd: $(this).attr('bs-cmd')
                                }
                                apiInfos.push(apiInfo);
                            });
                            if (apiInfos.length === 0) {
                                $('#small2').modal('show');
                                return false;
                            }
                            var result = {
                                apiInfos: apiInfos
                            };
                            console.log(JSON.stringify(result));
                            window.javaBridge.invoke('saveApiInfos', new Array(JSON.stringify(result)));
                            break;
                        case 5:
                            var preForTest = JSON.parse(window.javaBridge.invoke('preForTest', new Array()));
                            if (!preForTest.success) {
                                return false;
                            }
                            var testItems = preForTest.testItems;
                            if (testItems.length == 0) {
                                $('.test-urls').html('没有可用于测试的链接，请检查项目详情配置中API测试信息是否完善');
                            }
                            else {
                                
                                var urlHtml = '';
                                var testUrlLength = testItems.length;
                                testItems.forEach(function (urlObj, i) {
                                    var apiName = urlObj.apiName,
                                    url = urlObj.url.replace(/"/g, "'"),
                                    paramArr = urlObj.params,
                                    paramHTML = '';
                                    
                                    for (var j = 0; j < paramArr.length; j++) {
                                        paramArr[j].paramValue = paramArr[j].paramValue.replace(/"/g, "'")
                                        paramHTML += '<div class="param-item"><label class="col-md-1 control-label param-name-label" title="' + paramArr[j].paramName + '">' + paramArr[j].paramName + ':</label><div class="col-md-3"><input class="form-control param-input" data-key="' + paramArr[j].paramName + '" type="text" value="' +　paramArr[j].paramValue+ '"></div></div>'
                                    }
                                    if (paramArr.length === 0) {
                                        paramHTML += '<div class="col-md-12">无可配置的参数</div>'
                                    }
                                    
                                    
                                    urlHtml += '<div class="row test-url-wrapper"><label class="col-md-offset-1 col-md-2 control-label test-api-name"><span class="test-api-name-word">' + apiName + '<span class="trangle"></span></span></label><div class="col-md-7"><div class="input-group"><input class="form-control test-url-' + i + ' test-url-input" type="text" placeholder="" value="' + url + '" readonly><span class="input-group-btn"><button class="btn btn-test" type="button" index="' + i + '">测试</button></span></div><div class="param-set hidden param-set-area" data-url="' + url + '">' + paramHTML + '</div></div></div>'
                                });
                                $('.test-urls').html(urlHtml);
                                
                                function setTestUrl(ele) {
                                    var parent = $(ele).parents('.param-set');
                                    var input = $(ele).parents('.test-url-wrapper').find('.test-url-input');
                                    var baseURL = parent.attr('data-url');
                                    var inputArr = parent.find('.param-input');
                                    var paramArr = [];
                                    var addURL = '';
                                    var url = '';
                                    inputArr.each(function(i, item) {
                                        if (item.value !== '') {
                                            paramArr.push({key: $(item).attr('data-key'), value: item.value})
                                        }
                                    })
                                    for (var i = 0; i < paramArr.length; i++) {
                                        if (i == 0 ) {
                                            addURL += paramArr[i].key + '=' + paramArr[i].value.toString();
                                        } else {
                                            addURL += '&' + paramArr[i].key + '=' + paramArr[i].value.toString();
                                        }
                                    }
                                    if (baseURL.indexOf('?') !== -1) {
                                        url = baseURL + addURL
                                    } else {
                                        url = baseURL + '?' + addURL;
                                    }
                                    input.val(url)
                                }
                                
                                $('.param-input').on('change', function(e) {
                                    var ele = e.target;
                                    setTestUrl(ele);
                                })
                                
                                function initTestUrl() {
                                    var eleArr = $('.test-url-wrapper');
                                    eleArr.each(function(i, ele){
                                        if ($(ele).find('.param-name-label')[0]) {
                                            setTestUrl($(ele).find('.param-name-label')[0])
                                        }
                                    })
                                }
                                
                                initTestUrl();
                                
                                $('.test-api-name-word').on('click', function(e) {
                                    $(e.target).parents('.test-url-wrapper').find('.param-set').toggleClass('hidden')
                                })
                            }
                            break;
                    }
                    handleTitle(tab, navigation, index);
                },
                onPrevious: function (tab, navigation, index) {
                    success.hide();
                    error.hide();
                    var current = index + 1;
                    switch (current) {
                        case 1:
                            if (!window.javaBridge.invoke('genAPIPre', new Array())) {
                                return false;
                            }
                            break;
                        case 2:
                            break;
                        case 3:

                            break;
                        case 4:
                            window.javaBridge.invoke('stopReactor', new Array());
                            break;
                        case 5:

                            break;
                    }
                    handleTitle(tab, navigation, index);
                },
                onTabShow: function (tab, navigation, index) {
                    var total    = navigation.find('li').length;
                    var current  = index + 1;
                    var $percent = (current / (total - 1)) * 100;
                    if (current == 3) {
                        $('#form_wizard_1').find('.progress-bar').css({
                            width: '50%'
                        });
                        $(".tab3-list1").html("");
                    } else if (current == 4) {
                        $('#form_wizard_1').find('.progress-bar').css({
                            width: '75%'
                        });
                        var apiList = JSON.parse(window.javaBridge.invoke('loadApi', new Array())).apiList;
                        setApiConfigPage(apiList);
                    } else if (current == 5) {
                        $('#form_wizard_1').find('.progress-bar').css({
                            width: '100%'
                        });
                    } else {
                        $('#form_wizard_1').find('.progress-bar').css({
                            width: $percent + '%'
                        });
                    }
                }
            });

            $('#form_wizard_1').find('.button-previous').hide();
            $('#form_wizard_1 .button-submit').click(function () {
                // if (window.javaBridge.invoke('finish', new Array())) {
                //     self.location = "index.html";
                // }
                window.javaBridge.invoke('finish', new Array());
                self.location = "index.html";
            }).hide();

            //apply validation on select2 dropdown value change, this only needed for chosen dropdown integration.
            $('#country_list', form).change(function () {
                form.validate().element($(this)); //revalidate the chosen dropdown value and show error or success message for the input
            });

            $(".btn-goto").on("click", function () {
                var url = $("#project-url").val();
                if (url) {
                    window.javaBridge.invoke('forward', new Array(url));
                } else {
                    $('#project-url').pulsate({
                        color: "#bf1c56",
                        repeat: false
                    });
                }
            });
            $(document).on("click", ".btn-set-search", function () {
                //var keyword = $("#project-search").val();
                var apiName = $(this).attr('api');
                window.javaBridge.invoke('openSearchView', new Array(apiName));
            });
            $(document).on("click", ".openInEditor", function () {
                var apipath = $(this).attr("data-path");
                console.log("js openApiInEditor:"+apipath);
                window.javaBridge.invoke('openApiInEditor', new Array(apipath));
            });
            $(document).on("click", ".btn-test", function () {
                var index = $(this).attr('index');
                var url   = $('.test-url-' + index).val();
                console.log("test url :" + url);
                window.javaBridge.invoke('test', new Array(url));
            })
            $(document).on("click", ".config-api", function () {
                var apiName    = $(this).attr('apiName');
                var apiId      = $(this).attr('apiId');
                var apiHandler = $(this).attr('apiHandler');
                var apiPath    = $(this).attr('apiPath');
                var pageName   = $(this).html();
                var pageNum;

                if (pageName === '文档') {
                    pageNum = 0;
                } else if (pageName === '逻辑处理块') {
                    pageNum = 1;
                } else if (pageName === '详细配置') {
                    pageNum = 2;
                } else if (pageName === '辅助工具') {
                    pageNum = 3;
                } else {
                    pageNum = -1;
                }
                window.javaBridge.invoke('configApi', new Array(apiName, apiId, apiHandler, apiPath, pageNum));
            })
        }

    };

}();

function setApiSelectionPage(apis, selected) {
    //alert(JSON.stringify(apis));
    var left       = '',
        right      = '',
        apiSet     = JSON.parse(selected);
    apis.forEach(function (api, i) {
        var apiName, apiArr,
            apiHtml = '';
        for (var m in api) {
            apiName = m;
            apiArr  = api[m];
            //if(api[m].length > 0){
                $(".tab3-addApis").show();
            //}
        }
        for (var n = 0; n < apiArr.length; n++) {
            var api        = apiArr[n].api;
            var rspHandler = apiArr[n].rspHandler;
            var apiPath    = apiArr[n].apiPath;
            var charset    = apiArr[n].charset;
            var bsCmd = apiArr[n].bscmd;
            //console.log(apiSet[apiName]);
            console.log("setApiSelectionPage api name: "+api);
            if (apiSet[apiName] == api) {
                apiHtml += '<label><input type="radio" class="apiRspHandler" name="' + apiName + '" class="icheck" data-name="' + api + '" data-path="' + apiPath + '" rspHandler="' + rspHandler + '" api-charset="' + charset + '" bs-cmd="' + bsCmd + '" checked><a href="javascript:;" class="openInEditor" data-path="' + apiPath + '">备选API' + (n + 1) + '</a></label>';
            }
            else {
                apiHtml += '<label><input type="radio" class="apiRspHandler" name="' + apiName + '" class="icheck" data-name="' + api + '" data-path="' + apiPath + '" rspHandler="' + rspHandler + '" api-charset="' + charset + '" bs-cmd="' + bsCmd + '"><a href="javascript:;" class="openInEditor" data-path="' + apiPath + '">备选API' + (n + 1) + '</a></label>';
            }
        }
        if (i % 2 == 0) {
            if (i === 0) {
                left += '<div class="portlet box green"><div class="portlet-title"><div class="caption filter-api-name" title="' + apiName + '">' + apiName + '</div><div class="tools"><a href="javascript:;" class="collapse"   data-original-title="折叠/收起" title="折叠/收起"><i class="fa fa-sort-down"></i></a></div></div><div class="portlet-body form" style="display: block;overflow: hidden;"><div class="form-body"><div class="col-md-12"><div class="form-group"><div class="input-group"><div class="icheck-list ' + apiName + '" style="margin-left: 1em;">' + apiHtml + '</div></div></div></div></div><div class="text-center">若以上不存在符合要求的API，请点击<a href="javascript:;" class="btn-set-search" api="' + apiName + '">查找</a></div></div></div>';
            } else {
                left += '<div class="portlet box green"><div class="portlet-title"><div class="caption filter-api-name" title="' + apiName + '">' + apiName + '</div><div class="tools"><a href="javascript:;" class="expand"   data-original-title="折叠/收起" title="折叠/收起"><i class="fa fa-sort-down"></i></a></div></div><div class="portlet-body form" style="display: none;overflow: hidden;"><div class="form-body"><div class="col-md-12"><div class="form-group"><div class="input-group"><div class="icheck-list ' + apiName + '" style="margin-left: 1em;">' + apiHtml + '</div></div></div></div></div><div class="text-center">若以上不存在符合要求的API，请点击<a href="javascript:;" class="btn-set-search" api="' + apiName + '">查找</a></div></div></div>';
            }
        } else {
            if (i === 1) {
                right += '<div class="portlet box green"><div class="portlet-title"><div class="caption filter-api-name" title="' + apiName + '">' + apiName + '</div><div class="tools"><a href="javascript:;" class="collapse"   data-original-title="折叠/收起" title="折叠/收起"><i class="fa fa-sort-down"></i></a></div></div><div class="portlet-body form" style="display: block;overflow: hidden;"><div class="form-body"><div class="col-md-12"><div class="form-group"><div class="input-group"><div class="icheck-list ' + apiName + '" style="margin-left: 1em;">' + apiHtml + '</div></div></div></div></div><div class="text-center">若以上不存在符合要求的API，请点击<a href="javascript:;" class="btn-set-search" api="' + apiName + '">查找</a></div></div></div>';
            } else {
                right += '<div class="portlet box green"><div class="portlet-title"><div class="caption filter-api-name" title="' + apiName + '">' + apiName + '</div><div class="tools"><a href="javascript:;" class="expand"   data-original-title="折叠/收起" title="折叠/收起"><i class="fa fa-sort-down"></i></a></div></div><div class="portlet-body form" style="display: none;overflow: hidden;"><div class="form-body"><div class="col-md-12"><div class="form-group"><div class="input-group"><div class="icheck-list ' + apiName + '" style="margin-left: 1em;">' + apiHtml + '</div></div></div></div></div><div class="text-center">若以上不存在符合要求的API，请点击<a href="javascript:;" class="btn-set-search" api="' + apiName + '">查找</a></div></div></div>';
            }
        }
    });
    if (left == '') {
        left = '<div class="portlet box green"><div class="portlet-title"><div class="caption filter-api-name"></div><div class="tools"><a href="javascript:;" class="expand"   data-original-title="折叠/收起" title="折叠/收起"><i class="fa fa-sort-down"></i></a></div></div><div class="portlet-body form" style="display: none;overflow: hidden;"><div class="form-body"><div class="col-md-12"><div class="form-group"><div class="input-group"><div class="icheck-list api-default" style="margin-left: 1em;"></div></div></div></div></div><div class="text-center">若以上不存在符合要求的API，请点击<a href="javascript:;" class="btn-set-search" api="api-default">查找</a></div></div>'
    }
    $(".tab3-list2-left").html(left);
    $(".tab3-list2-right").html(right);
    Metronic.initUniform();
}

function setApiConfigPage(apiList) {
        var left  = "";
        var right = "";
        apiList.forEach(function (api, i) {
            var apiName    = api.apiname;
            var apiId      = api.api;
            var apiHandler = api.rspHandler;
            var apiPath    = api.apiPath;
            var bscmd      = api.bscmd;
            if (i % 2 == 0) {
                if (i === 0) {
                    left += '<div class="portlet box green"><div class="portlet-title"><div class="caption set-api-name" title="' + apiName + '">' + apiName + '</div><div class="tools"><a href="javascript:;" class="collapse"   data-original-title="折叠/收起" title="折叠/收起"><i class="fa fa-sort-down"></i></a></div></div><div class="portlet-body form" style="display: block;"><div class="form-body"><div class="form-group"><label class="control-label col-md-5 tab4-iw-cmd">调用名称(bs-cmd)</label><div class="col-md-7"><p class="form-control-static api-name"><input class="form-control" name="iwa" aria-required="true" aria-describedby="iwa-error" aria-invalid="true" apiName="' + apiName + '" id="' + apiId + '" apiId="' + apiId + '" apiHandler="' + apiHandler + '" apiPath="' + apiPath + '" type="text" value="' + bscmd + '"></p></div></div><div class="form-group"><div class="col-md-offset-5 col-md-7 api-setbtngroup"><a href="javascript:;" class="config-api" apiName="' + apiName + '" apiId="' + apiId + '" apiHandler="' + apiHandler + '" apiPath="' + apiPath + '" configType="doc">文档</a><a href="javascript:;" class="config-api" apiName="' + apiName + '" apiId="' + apiId + '" apiHandler="' + apiHandler + '" apiPath="' + apiPath + '" configType="doc">逻辑处理块</a><a href="javascript:;" class="config-api" apiName="' + apiName + '" apiId="' + apiId + '" apiHandler="' + apiHandler + '" apiPath="' + apiPath + '" configType="doc">详细配置</a><a href="javascript:;" class="config-api" apiName="' + apiName + '" apiId="' + apiId + '" apiHandler="' + apiHandler + '" apiPath="' + apiPath + '" configType="doc">辅助工具</a></div></div></div></div></div>';
                } else {
                    left += '<div class="portlet box green"><div class="portlet-title"><div class="caption set-api-name" title="' + apiName + '">' + apiName + '</div><div class="tools"><a href="javascript:;" class="expand"   data-original-title="折叠/收起" title="折叠/收起"><i class="fa fa-sort-down"></i></a></div></div><div class="portlet-body form" style="display: none;"><div class="form-body"><div class="form-group"><label class="control-label col-md-5 tab4-iw-cmd">调用名称(bs-cmd)</label><div class="col-md-7"><p class="form-control-static api-name"><input class="form-control" name="iwa" aria-required="true" aria-describedby="iwa-error" aria-invalid="true" apiName="' + apiName + '" id="' + apiId + '" apiId="' + apiId + '" apiHandler="' + apiHandler + '" apiPath="' + apiPath + '" type="text" value="' + bscmd + '"></p></div></div><div class="form-group"><div class="col-md-offset-5 col-md-7 api-setbtngroup"><a href="javascript:;" class="config-api" apiName="' + apiName + '" apiId="' + apiId + '" apiHandler="' + apiHandler + '" apiPath="' + apiPath + '" configType="doc">文档</a><a href="javascript:;" class="config-api" apiName="' + apiName + '" apiId="' + apiId + '" apiHandler="' + apiHandler + '" apiPath="' + apiPath + '" configType="doc">逻辑处理块</a><a href="javascript:;" class="config-api" apiName="' + apiName + '" apiId="' + apiId + '" apiHandler="' + apiHandler + '" apiPath="' + apiPath + '" configType="doc">详细配置</a><a href="javascript:;" class="config-api" apiName="' + apiName + '" apiId="' + apiId + '" apiHandler="' + apiHandler + '" apiPath="' + apiPath + '" configType="doc">辅助工具</a></div></div></div></div></div>';
                }
            } else {
                if (i === 1) {
                    right += '<div class="portlet box green"><div class="portlet-title"><div class="caption set-api-name" title="' + apiName + '">' + apiName + '</div><div class="tools"><a href="javascript:;" class="collapse"   data-original-title="折叠/收起" title="折叠/收起"><i class="fa fa-sort-down"></i></a></div></div><div class="portlet-body form" style="display: block;"><div class="form-body"><div class="form-group"><label class="control-label col-md-5 tab4-iw-cmd">调用名称(iw-cmd)</label><div class="col-md-7"><p class="form-control-static api-name"><input class="form-control" name="iwa" aria-required="true" aria-describedby="iwa-error" aria-invalid="true" apiName="' + apiName + '" id="' + apiId + '" apiId="' + apiId + '" apiHandler="' + apiHandler + '" apiPath="' + apiPath + '" type="text" value="' + bscmd + '"></p></div></div><div class="form-group"><div class="col-md-offset-5 col-md-7 api-setbtngroup"><a href="javascript:;" class="config-api" apiName="' + apiName + '" apiId="' + apiId + '" apiHandler="' + apiHandler + '" apiPath="' + apiPath + '" configType="doc">文档</a><a href="javascript:;" class="config-api" apiName="' + apiName + '" apiId="' + apiId + '" apiHandler="' + apiHandler + '" apiPath="' + apiPath + '" configType="doc">逻辑处理块</a><a href="javascript:;" class="config-api" apiName="' + apiName + '" apiId="' + apiId + '" apiHandler="' + apiHandler + '" apiPath="' + apiPath + '" configType="doc">详细配置</a><a href="javascript:;" class="config-api" apiName="' + apiName + '" apiId="' + apiId + '" apiHandler="' + apiHandler + '" apiPath="' + apiPath + '" configType="doc">辅助工具</a></div></div></div></div></div>';
                } else {
                    right += '<div class="portlet box green"><div class="portlet-title"><div class="caption set-api-name" title="' + apiName + '">' + apiName + '</div><div class="tools"><a href="javascript:;" class="expand"   data-original-title="折叠/收起" title="折叠/收起"><i class="fa fa-sort-down"></i></a></div></div><div class="portlet-body form" style="display: none;"><div class="form-body"><div class="form-group"><label class="control-label col-md-5 tab4-iw-cmd">调用名称(iw-cmd)</label><div class="col-md-7"><p class="form-control-static api-name"><input class="form-control" name="iwa" aria-required="true" aria-describedby="iwa-error" aria-invalid="true" apiName="' + apiName + '" id="' + apiId + '" apiId="' + apiId + '" apiHandler="' + apiHandler + '" apiPath="' + apiPath + '" type="text" value="' + bscmd + '"></p></div></div><div class="form-group"><div class="col-md-offset-5 col-md-7 api-setbtngroup"><a href="javascript:;" class="config-api" apiName="' + apiName + '" apiId="' + apiId + '" apiHandler="' + apiHandler + '" apiPath="' + apiPath + '" configType="doc">文档</a><a href="javascript:;" class="config-api" apiName="' + apiName + '" apiId="' + apiId + '" apiHandler="' + apiHandler + '" apiPath="' + apiPath + '" configType="doc">逻辑处理块</a><a href="javascript:;" class="config-api" apiName="' + apiName + '" apiId="' + apiId + '" apiHandler="' + apiHandler + '" apiPath="' + apiPath + '" configType="doc">详细配置</a><a href="javascript:;" class="config-api" apiName="' + apiName + '" apiId="' + apiId + '" apiHandler="' + apiHandler + '" apiPath="' + apiPath + '" configType="doc">辅助工具</a></div></div></div></div></div>';
                }
            }
        });
        $(".tab3-list1-left").html(left);
        $(".tab3-list1-right").html(right);
        Metronic.initUniform();
        $(".api-name input").blur(function () {
            var bsCmd   = $(this).val();
            var apiId   = $(this).attr('apiId');
            var apiPath = $(this).attr('apiPath');
            window.javaBridge.invoke('changeBscmd', new Array(apiPath, apiId, bsCmd));
        });
    }