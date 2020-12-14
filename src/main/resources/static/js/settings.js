var itemNameList = ['assignment', 'discussion_topic', 'wiki_page', 'attachment', 'quiz'];

jQuery(document).ready(function($) {
    // Do a check on page load
    pageStatusChecks();

    $("#enableBlueprint").click(function() {
        $("#blueprintEnabled").slideToggle("slow");
    });

    $('#gloRadio,#lobtRadio').click(function() {
        if ($('#gloRadio').is(':checked')) {
            $("#gloEnabled").slideDown("slow");
            $("#lobtEnabled").slideUp("slow");
            $("#gloEnabled").find('input[type=checkbox]').prop("disabled", false);
            $("#lobtEnabled").find('input[type=checkbox]').prop("disabled", true);
        } else if ($('#lobtRadio').is(':checked')) {
            $("#gloEnabled").slideUp("slow");
            $("#lobtEnabled").slideDown("slow");
            $("#gloEnabled").find('input[type=checkbox]').prop("disabled", true);
            $("#lobtEnabled").find('input[type=checkbox]').prop("disabled", false);
        }
    });

    //Loop through all item types
    $.each( itemNameList, function( index, val ) {
        $('#' + val + 'TitleItem').click(function() {
            $("#" + val + "Toggler").slideToggle("slow");
            $("#" + val + "Chevron").toggleClass("fa-chevron-right fa-chevron-down");
        });

        var fieldset = $('#' + val + 'Checkboxes input[type="checkbox"]');
        var iconId = $("#" + val + "LockStatus");
        lockStatusCheck(fieldset, iconId);

        $('#' + val + 'Checkboxes input[type="checkbox"]').click(function() {
            lockStatusCheck(fieldset, iconId);
            setLockedAttributeText(val);
        });

        setLockedAttributeText(val);
    });

    $("#enableBlueprint").click(function() {
        enableInputsCheck();
    });

});

function enableInputsCheck() {
    var currentBoxChecked = $("#enableBlueprint").is(":checked");
    $('input[type="radio"], input[type="checkbox"][id!="enableBlueprint"]').prop("disabled", !currentBoxChecked);

}

function pageStatusChecks() {
    var mainBox = $("#enableBlueprint");
    var mainContent = $("#blueprintEnabled");
    var generalRadio = $("#gloRadio");
    var generalRadioContent = $("#gloEnabled");
    var typeRadio = $("#lobtRadio");
    var typeRadioContent = $("#lobtEnabled");

    if (mainBox.is(":checked")) {
        $('input[type="radio"], input[type="checkbox"][id!="enableBlueprint"]').prop("disabled", false);
    } else {
        $('input[type="radio"], input[type="checkbox"][id!="enableBlueprint"]').prop("disabled", true);
        mainContent.hide();
    }

    if (generalRadio.is(":checked")) {
        typeRadioContent.hide();
        typeRadioContent.find('input[type=checkbox]').prop("disabled", true);
        generalRadioContent.find('input[type=checkbox]').prop("disabled", false);
    }

    if (typeRadio.is(":checked")) {
        generalRadioContent.hide();
        generalRadioContent.find('input[type=checkbox]').prop("disabled", true);
        typeRadioContent.find('input[type=checkbox]').prop("disabled", false);
    }
}

function lockStatusCheck(fieldset, iconId) {
    if(fieldset.is(':checked')){
        iconId.removeClass("fa-unlock-alt");
        iconId.addClass("fa-lock");
    } else {
        iconId.removeClass("fa-lock");
        iconId.addClass("fa-unlock-alt");
    }
}

function setLockedAttributeText(itemKey) {
    //Get the labels for all checked checkboxes of this item type
    var checkedLabels = $('#' + itemKey + 'Checkboxes input[type="checkbox"]:checked').map(function () {
        return $("label[for='" + this.id + "']").html();
    }).get();

    var theLabel = $('#' + itemKey + 'LockedAttributes');
    if (checkedLabels.length > 0) {
        theLabel.html(checkedLabels.join(", ").replace(/,(?=[^,]*$)/, ' &'));
    } else {
        theLabel.html($('#lobtEnabled').data('empty_text'));
    }
}