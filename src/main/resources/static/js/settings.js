/*-
 * #%L
 * blueprint-manager
 * %%
 * Copyright (C) 2015 - 2022 Indiana University
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Indiana University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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
            var toggleSection = $("#" + val + "Toggler");
            toggleSection.slideToggle("slow");  
            toggleSection.attr("aria-hidden", toggleSection.attr('aria-hidden')=='true' ? 'false' : 'true');
                   
            var toggleButton = $("#" + val + "Chevron");
            toggleButton.toggleClass("fa-chevron-right fa-chevron-down");
            toggleButton.attr("aria-expanded", toggleButton.attr('aria-expanded')=='true' ? 'false' : 'true');
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
