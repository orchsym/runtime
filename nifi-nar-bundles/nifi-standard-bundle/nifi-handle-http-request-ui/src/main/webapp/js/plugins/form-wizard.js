$().ready(function () {
    $("#submit_form1").validate({
        doNotHideMessage: true, //this option enables to show the error/success messages on tab switch.
        errorElement: 'span', //default input error message container
        errorClass: 'help-block help-block-error', // default input error message class
        focusInvalid: false, // do not focus the last invalid input
        rules: {
            //account
            name: {
                minlength: 2,
                required: true,
                displayname: true,
                apidisplayname: true,
                javareservedword: true
            },
            iwCmd: {
                minlength: 2,
                required: true,
                iwa: true,
                iwCmd: true,
                javareservedword: true
            },
            displayName: {
                minlength: 2,
                required: true,
                displayname: true,
                apidisplayname: true,
                javareservedword: true
            },
            // requestPathPattern: {
                // required: true,
                // xpathPattern: true
            // }
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
    })
})