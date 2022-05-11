package edu.iu.uits.lms.blueprintmanager.controller;

import edu.iu.uits.lms.blueprintmanager.model.BlueprintAssociationModel;
import edu.iu.uits.lms.blueprintmanager.model.BlueprintConfirmationModel;
import edu.iu.uits.lms.blueprintmanager.services.BlueprintConfigurationUpdateException;
import edu.iu.uits.lms.blueprintmanager.services.BlueprintToolService;
import edu.iu.uits.lms.canvas.model.BlueprintMigrationStatus;
import edu.iu.uits.lms.canvas.model.BlueprintUpdateStatus;
import edu.iu.uits.lms.common.session.CourseSessionService;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.controller.OidcTokenAwareController;
import edu.iu.uits.lms.lti.service.OidcTokenUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;

import javax.servlet.http.HttpSession;
import java.util.Locale;

@Controller
@RequestMapping("/app")
@Slf4j
public class BlueprintController extends OidcTokenAwareController {

    private static final String FORM_SESSION_KEY = "form.session.key";

    @Autowired
    private BlueprintToolService blueprintToolService = null;

    @Autowired
    private MessageSource messageSource = null;

    @Autowired
    private CourseSessionService courseSessionService;

    @RequestMapping("/launch")
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public String launch(Model model) {
        OidcAuthenticationToken token = getTokenWithoutContext();
        OidcTokenUtils oidcTokenUtils = new OidcTokenUtils(token);
        String courseId = oidcTokenUtils.getCourseId();

        return main(courseId, model);
    }

    @RequestMapping("/{context}/index")
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public String main (@PathVariable("context") String context, Model model) {
        BlueprintSettings courseSettings = blueprintToolService.getCourseSettings(context);
        return main(context, model, courseSettings);
    }

    private String main (String context, Model model, BlueprintSettings courseSettings) {
        OidcAuthenticationToken token = getValidatedToken(context);
        if (courseSettings == null) {
            courseSettings = blueprintToolService.getCourseSettings(context);
        }

        // some check to see if this is a SIS course
        boolean sisCourse = courseSettings.getCourse().getSisCourseId() != null;
        // if there are statuses returned, then disable the Sync button
        boolean isSyncInProgress = !blueprintToolService.getActiveMigrationStatuses(courseSettings.getCourse().getId()).isEmpty();
        // if the course does not have any associations, disable the Sync button
        boolean hasAssociations = courseSettings.isHasAssociations();

        boolean disableSync = false;
        // if there's a sync in progress or there are no associations, disable the Sync button
        if (isSyncInProgress || !hasAssociations) {
            disableSync = true;
        }

        model.addAttribute("disableSync", disableSync);

        if (sisCourse) {
            model.addAttribute("messageInfoKey", "blueprint.sisCourse.warning");
        } else if (courseSettings.hasEnrollments()) {
            model.addAttribute("messageInfoKey", "blueprint.enrollments.warning");
        } else if (courseSettings.isAlreadyAssociated()) {
            model.addAttribute("messageInfoKey", "blueprint.courseAlreadyAssociated");
        } else {
            // normal mode
        }

        model.addAttribute("blueprintModel", courseSettings.getBlueprintModel());
        return "index";
    }

    @RequestMapping(value = "/{context}/submit", method = RequestMethod.POST, params="action=" + BlueprintConstants.ACTION_SETTINGS_SAVE)
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public String settingsSubmit(@PathVariable("context") String context,
                         @ModelAttribute BlueprintModel blueprintModel,
                         BindingResult bindingResult,
                         Model model) {
        BlueprintSettings courseSettings = null;
        try {
            courseSettings = blueprintToolService.updateCourseSettings(blueprintModel);
            model.addAttribute("submitSuccess", true);
        } catch (BlueprintConfigurationUpdateException e) {
            model.addAttribute("submitFailure", true);

            /*
            The following block of code is actually unused due to the messageInfoKey stuff superseding this error message.
            I'm keeping it around just in case we change the html to actually care about it when there's some other sort of message.
             */
            //Begin useless code
            String message = messageSource.getMessage("settings.failure", null, Locale.getDefault());

            if (e.getFailureMessage() != null) {
                message = messageSource.getMessage("settings.failure.details", new String[]{e.getFailureMessage()}, Locale.getDefault());
            }
            model.addAttribute("submitFailureMessage", message);
            //End useless code
        }

        return main(context, model, courseSettings);
    }

    @RequestMapping(value = "/{context}/submit", method = RequestMethod.POST, params="action=" + BlueprintConstants.ACTION_ASSOCIATE)
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public String associate(@PathVariable("context") String context,
                         @ModelAttribute BlueprintAssociationModel blueprintModel,
                         Model model, HttpSession session) {

        OidcAuthenticationToken token = getValidatedToken(context);
        OidcTokenUtils oidcTokenUtils = new OidcTokenUtils(token);
        String username = oidcTokenUtils.getUserLoginId();

        if (blueprintModel == null || !blueprintModel.isInitialized()) {
            BlueprintAssociationModel blueprintAssociationModel = blueprintToolService.buildAssociationBackingModel(context, username);
            courseSessionService.addAttributeToSession(session, context, FORM_SESSION_KEY, blueprintAssociationModel);
            model.addAttribute("blueprintAssociationModel", blueprintAssociationModel);
        } else {
            courseSessionService.addAttributeToSession(session, context, FORM_SESSION_KEY, blueprintModel);
            model.addAttribute("blueprintAssociationModel", blueprintModel);
        }

        return "associate";
    }

    @RequestMapping(value = "/{context}/submit", method = RequestMethod.POST, params = {"action=" + BlueprintConstants.ACTION_SYNC})
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public String sync(@PathVariable("context") String context,
                       @ModelAttribute BlueprintModel blueprintModel,
                       Model model) {

        OidcAuthenticationToken token = getValidatedToken(context);
        OidcTokenUtils oidcTokenUtils = new OidcTokenUtils(token);
        String username = oidcTokenUtils.getUserLoginId();

        // re-send association information via this call
        BlueprintMigrationStatus migrationStatus = blueprintToolService.performMigration(context, blueprintModel.isCopySettings(), blueprintModel.isSendNotifications(), username, false);
        log.debug("{}", migrationStatus);

        if (migrationStatus.getBlueprintMigration() != null) {
            model.addAttribute("syncButtonSuccess", true);
        } else {
            model.addAttribute("syncButtonFail", true);
        }

        // since this function is triggered on the main page, return to there, too!
        return main(context, model);
    }

    @RequestMapping(value = "/{context}/confirm", method = RequestMethod.POST)
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public String confirm(@PathVariable("context") String context,
                            @ModelAttribute BlueprintAssociationModel blueprintModel,
                            Model model, HttpSession session) {

        BlueprintConfirmationModel blueprintConfirmationModel = blueprintToolService.buildConfirmationBackingModel(context, blueprintModel);
        model.addAttribute("confirmationModel", blueprintConfirmationModel);
        BlueprintAssociationModel bam  = courseSessionService.getAttributeFromSession(session, context, FORM_SESSION_KEY, BlueprintAssociationModel.class);
        bam.mergeModels(blueprintModel);
        courseSessionService.addAttributeToSession(session, context, FORM_SESSION_KEY, bam);

        return "confirm";
    }

    @RequestMapping(value = "/{context}/confirm", method = RequestMethod.POST, params = {"action=" + BlueprintConstants.ACTION_CONFIRM_SUBMIT})
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public String confirmSubmit(@PathVariable("context") String context,
                          @ModelAttribute BlueprintConfirmationModel blueprintModel,
                          Model model, HttpSession session) {

        OidcAuthenticationToken token = getValidatedToken(context);
        OidcTokenUtils oidcTokenUtils = new OidcTokenUtils(token);
        String username = oidcTokenUtils.getUserLoginId();

        BlueprintAssociationModel bam = null;
        BlueprintUpdateStatus updateStatus = blueprintToolService.updateAssociations(blueprintModel);

        if (updateStatus.isSuccess()) {
            BlueprintMigrationStatus migrationStatus = blueprintToolService.performMigration(context, true, false, username, blueprintModel.isPublishAfterSync());
            log.debug("{}", migrationStatus);

            if (migrationStatus.getBlueprintMigration() != null) {

                model.addAttribute("associateSuccess", true);

                if (blueprintModel.isPublishAfterSync()) {
                    model.addAttribute("publishAfterSyncMessage", true);
                }

                courseSessionService.removeAttributeFromSession(session, context, FORM_SESSION_KEY);
            } else {
                model.addAttribute("syncWarningMessage", true);
                bam = courseSessionService.getAttributeFromSession(session, context, FORM_SESSION_KEY, BlueprintAssociationModel.class);
            }
        } else {
            model.addAttribute("associateFailureMessage", updateStatus.getMessage());
            bam = courseSessionService.getAttributeFromSession(session, context, FORM_SESSION_KEY, BlueprintAssociationModel.class);
        }

        return associate(context, bam, model, session);
    }

    @RequestMapping(value = "/{context}/confirm", method = RequestMethod.POST, params = {"action=" + BlueprintConstants.ACTION_CONFIRM_EDIT})
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public String confirmEdit(@PathVariable("context") String context,
                                @ModelAttribute BlueprintConfirmationModel blueprintModel,
                                Model model, HttpSession session) {

        BlueprintAssociationModel bam = courseSessionService.getAttributeFromSession(session, context, FORM_SESSION_KEY, BlueprintAssociationModel.class);
        bam.setInitialized(true);
        bam.setPublishAfterSync(blueprintModel.isPublishAfterSync());
        courseSessionService.removeAttributeFromSession(session, context, FORM_SESSION_KEY);

        return associate(context, bam, model, session);
    }

    @RequestMapping(value = "/{context}/confirm", method = RequestMethod.POST, params = {"action=" + BlueprintConstants.ACTION_CONFIRM_CANCEL})
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public String confirmCancel(@PathVariable("context") String context,
                              Model model, HttpSession session) {

        courseSessionService.removeAttributeFromSession(session, context, FORM_SESSION_KEY);
        return associate(context, null, model, session);
    }
}
