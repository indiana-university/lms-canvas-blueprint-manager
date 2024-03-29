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
var summaryNone = "<li id='summaryNone'>None</li>";

jQuery(document).ready(function($) {
   checkboxEventRegistration();

    //Reset button handler
   $("#btnReset").click(function() {
      $('.summaryList').empty();

      $("input[type=checkbox].hidden_checkbox").each(function () {
         var original = $(this);
         var id = original.attr("id");
         var index = id.indexOf("_");
         var courseId = id.substring(index+1);
         var actual = $("#" + courseId);
         if (original.is(":checked")) {
             actual.prop('checked', 'checked');
             var courseName = $("#" + courseId + "_courseName").val();
             var termName = $("#" + courseId + "_termName").val();

             $('.summaryList').append('<li id="accociated_' + courseId + '">' + courseName + ' (' + termName + ')</li>');
          } else {
             actual.prop('checked', '');
          }
      });

      checkDirtyForm();
   });

   //Term dropdown handler
   $('#select-term').on('change', function() {
       var selectObj = $(this);
       var termId = selectObj.val();
       $("#term_" + termId).show();
       $("#term_" + termId).prop('hidden', '');
       $("#term_" + termId).removeClass("hidden_term");
       $("#term_" + termId + "_visible").val(true);
       selectObj.find(":selected").remove();
       location.href = "#term_" + termId;
       $("#" + termId + "_termName").focus();
   });

   checkDirtyForm();

});


function checkboxEventRegistration() {
    $('.bp_checkboxes :checkbox').not('#publishAfterSync').change('click', function(event) {
        event.stopPropagation();
        event.preventDefault();

        var currentBox = $(this);
        var li = currentBox.parent();

        if (currentBox.is(":checked")) {
            var courseName = $('label[for=' + currentBox.attr('id') + '] span.courseName').text();

            var myElementName = currentBox.attr('name');

            var index = myElementName.lastIndexOf(".");
            var termElementName = myElementName.substring(0, index+1) + "termName";
            var termName = $('input[name="' + termElementName + '"]').val();
            $('.summaryList').append($("<li>", {
                id: 'accociated_' + currentBox.attr('id'),
                class: 'sectionHighlight',
                text: courseName + ' (' + termName + ')'
                })
            );

            $('.summaryList #summaryNone').remove();
        } else {
            $('.summaryList #accociated_'+currentBox.attr('id')).remove();
            if ($('.summaryList li').length == 0) {
                $('.summaryList').append(summaryNone);
            }

        }
        checkDirtyForm();

    });
}

function checkDirtyForm() {
    var dirty = false;
    $("input[type=checkbox].currentlyChecked").each(function () {
       var courseId = this.id;
       var currentlyChecked = $(this).is(":checked");
       var originallyChecked = $("#originallyChecked_" + courseId).is(":checked")

       if (currentlyChecked != originallyChecked) {
            dirty = true;
            return false;
       }
    });

    if (dirty) {
        $("#btnContinue").prop('disabled', '');
        $("#btnContinue").attr('aria-disabled', 'false');
    } else {
        $("#btnContinue").prop('disabled', 'disabled');
        $("#btnContinue").attr('aria-disabled', 'true');
    }
}
