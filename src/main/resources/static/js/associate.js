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
   });

    // this controls the toggle dropdowns
    $(".toggleGroup").click(function() {
        var toggleButton = $(this).find('button:first');
        toggleButton.attr("aria-expanded", toggleButton.attr('aria-expanded')=='true' ? 'false' : 'true');
        
        var toggleImg = toggleButton.find('span:first')
        toggleImg.toggleClass("fa-chevron-right fa-chevron-down");

        var toggleSection = $(this).parent().find('.toggler:first');
        toggleSection.slideToggle("slow");
        toggleSection.attr("aria-hidden", toggleSection.attr('aria-hidden')=='true' ? 'false' : 'true');
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