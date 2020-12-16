package edu.iu.uits.lms.blueprintmanager.services;

import canvas.client.generated.api.AccountsApi;
import canvas.client.generated.api.BlueprintApi;
import canvas.client.generated.api.CoursesApi;
import canvas.client.generated.api.TermsApi;
import canvas.client.generated.model.Account;
import canvas.client.generated.model.BlueprintAssociatedCourse;
import canvas.client.generated.model.BlueprintConfiguration;
import canvas.client.generated.model.BlueprintCourseUpdateStatus;
import canvas.client.generated.model.BlueprintMigration;
import canvas.client.generated.model.BlueprintMigrationStatus;
import canvas.client.generated.model.BlueprintRestriction;
import canvas.client.generated.model.BlueprintUpdateStatus;
import canvas.client.generated.model.CanvasTerm;
import canvas.client.generated.model.Course;
import canvas.client.generated.model.User;
import canvas.helpers.BlueprintHelper;
import canvas.helpers.CanvasConstants;
import canvas.helpers.CanvasDateFormatUtil;
import canvas.helpers.EnrollmentHelper;
import canvas.helpers.TermHelper;
import edu.iu.uits.lms.blueprintmanager.controller.BlueprintModel;
import edu.iu.uits.lms.blueprintmanager.controller.BlueprintSettings;
import edu.iu.uits.lms.blueprintmanager.model.BlueprintAssociationModel;
import edu.iu.uits.lms.blueprintmanager.model.BlueprintConfirmationModel;
import edu.iu.uits.lms.blueprintmanager.model.BlueprintCourseModel;
import edu.iu.uits.lms.blueprintmanager.model.BlueprintTermModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BlueprintToolService {

    private static final String TEMPLATE_ID = "default";

    @Autowired
    private CoursesApi coursesApi;

    @Autowired
    private BlueprintApi blueprintApi = null;

    @Autowired
    private TermsApi termsApi = null;

    @Autowired
    private AccountsApi accountsApi = null;


    public BlueprintSettings getCourseSettings(String courseId) {
        Course course = coursesApi.getCourse(courseId);
        return getCourseSettings(course);
    }

    /**
     *
     * @param course
     * @return
     */
    private BlueprintSettings getCourseSettings(Course course) {
        BlueprintModel blueprintModel = new BlueprintModel();
        blueprintModel.setCourseId(course.getId());
        blueprintModel.setEnableBlueprint(BooleanUtils.toBoolean(course.getBlueprint()));

        BlueprintRestriction blueprintRestriction = course.getBlueprintRestrictions();

        blueprintModel.setBlueprintRestrictions(blueprintRestriction == null ? new BlueprintRestriction() : blueprintRestriction);

        Map<String, BlueprintRestriction> restrictionMap = course.getBlueprintRestrictionsByObjectType();
        blueprintModel.setBlueprintRestrictionsByObjectType(restrictionMap == null ? new HashMap<>() : restrictionMap);

        BlueprintSettings settings = new BlueprintSettings();
        settings.setCourse(course);
        settings.setBlueprintModel(blueprintModel);

        List<String> states = Arrays.asList(EnrollmentHelper.STATE.active.name(), EnrollmentHelper.STATE.invited.name());
        List<String> types = Arrays.asList(EnrollmentHelper.TYPE.student.name(), EnrollmentHelper.TYPE.observer.name());
        List<User> users = coursesApi.getUsersForCourseByType(course.getId(), types, states);

        if (users != null && !users.isEmpty()) {
            settings.hasEnrollments(true);
        }

        List<BlueprintMigration> subscriptions = blueprintApi.getSubscriptions(course.getId(), TEMPLATE_ID);
        settings.setAlreadyAssociated(!subscriptions.isEmpty());

        List<BlueprintAssociatedCourse> bpCourses = blueprintApi.getAssociatedCourses(course.getId(), TEMPLATE_ID);
        settings.setHasAssociations(!bpCourses.isEmpty());

        return settings;
    }

    public BlueprintSettings updateCourseSettings(BlueprintModel blueprintModel) throws BlueprintConfigurationUpdateException {
        boolean originalBlueprintSetting = BooleanUtils.toBoolean(coursesApi.getCourse(blueprintModel.getCourseId()).getBlueprint());

        BlueprintConfiguration bc = new BlueprintConfiguration();
        bc.setEnabled(blueprintModel.isEnableBlueprint());
        bc.setRestrictions(blueprintModel.getBlueprintRestrictions());
        bc.setHasObjectRestrictions(BlueprintModel.RADIO_OPTION.restrictionsByType.equals(blueprintModel.getRadioOption()));
        bc.setObjectRestrictions(blueprintModel.getBlueprintRestrictionsByObjectType());

        List<String> contentTypes = Arrays.stream(BlueprintHelper.CONTENT_TYPE.values())
              .map(Enum::name)
              .collect(Collectors.toList());
        bc.setDefaultRestrictionTypes(contentTypes);
        BlueprintCourseUpdateStatus courseUpdateStatus = blueprintApi.saveBlueprintConfiguration(blueprintModel.getCourseId(), bc);
        Course course = courseUpdateStatus.getCourse();
        if (course == null) {
            String errorMessage = null;
            if (courseUpdateStatus.getErrors() != null && courseUpdateStatus.getErrors().getMasterCourse() != null) {
                errorMessage = courseUpdateStatus.getErrors().getMasterCourse().get(0).getMessage();
            }
            throw new BlueprintConfigurationUpdateException(errorMessage);
        }

        // Only update the term and availability when blueprint is enabled/disabled
        if (originalBlueprintSetting != BooleanUtils.toBoolean(course.getBlueprint())) {
            updateCourseTermAndDates(course);
        }

        return getCourseSettings(course);
    }


    private void updateCourseTermAndDates(Course course) {

        CanvasTerm noExpTerm = termsApi.getTermBySisId(TermHelper.TERM_NO_EXPIRATION);
        if (BooleanUtils.toBoolean(course.getBlueprint())) {
            // verify that the course term is set to "noexp", the course start and end dates are null, and no override
            if (!noExpTerm.getId().equals(course.getEnrollmentTermId()) || course.getEndAt() != null || course.getStartAt() != null || course.getRestrictEnrollmentsToCourseDates()) {
                coursesApi.updateTermAndCourseEndDate(course.getId(), null, noExpTerm.getId(), null, null, false);
            }
        } else {
            // if the course is still set to "noexp", set the term to the current term used by the "Start a new course" form
            // check the override box, and set the end date to one year from today
            if (noExpTerm.getId().equals(course.getEnrollmentTermId())) {
                // set the term to the current term, restrict enrollments to course dates, and set end date to one year from today
                OffsetDateTime endDate = CanvasDateFormatUtil.getCalculatedCourseEndDate();
                CanvasTerm currTerm = termsApi.getCurrentYearTerm();

                coursesApi.updateTermAndCourseEndDate(course.getId(), null, currTerm.getId(), null, endDate, true);
            }
        }
    }

    /**
     * Get all courses that are associated to the given blueprint courseId
     * @param courseId courseId for a blueprint course
     * @param availableCourses
     * @return List of courses
     */
    private List<BlueprintAssociatedCourse> getAssociatedCourses(String courseId, List<Course> availableCourses) {
        List<BlueprintAssociatedCourse> bpCourses = blueprintApi.getAssociatedCourses(courseId, TEMPLATE_ID);

        Set<String> availableCourseIds = availableCourses.stream().map(Course::getId).collect(Collectors.toSet());

        return bpCourses.stream()
                        .filter(c -> availableCourseIds.contains(c.getId()))
                        .collect(Collectors.toList());
    }

    /**
     * Get all of the courses that the current user is teaching
     * @param userId userId of the instructor
     * @param accountId
     * @return List of courses
     */
    protected List<Course> getAvailableCourses(String userId, String accountId) {
        List<Course> courses = coursesApi.getCoursesTaughtBy(userId, false, false, false);
        Map<String, Set<String>> accountIdCache = new HashMap<>();
        List<Course> filteredCourses = new ArrayList<>();
        courses.forEach(c -> {
            Set<String> accountIds = accountIdCache.computeIfAbsent(c.getAccountId(), k -> {
                List<Account> accounts = accountsApi.getParentAccounts(c.getAccountId());
                Set<String> theAccountIds = accounts.stream().map(Account::getId).collect(Collectors.toSet());
                theAccountIds.add(c.getAccountId());
                return theAccountIds;
            });
            if (accountIds.contains(accountId) && !BooleanUtils.toBoolean(c.getBlueprint())) {
                filteredCourses.add(c);
            }
        });
        return filteredCourses;
    }

    /**
     * Extract the terms that are used in the list of courses
     * @param courses List of courses
     * @return List of CanvasTerm objects, sorted by start date
     */
    protected List<CanvasTerm> getPossibleTerms(List<Course> courses) {
        List<CanvasTerm> allTerms = termsApi.getEnrollmentTerms();
        Map<String, CanvasTerm> allTermMap = allTerms.stream().collect(Collectors.toMap(CanvasTerm::getId, term -> term, (a, b) -> b));

        List<String> availableTermsIds = courses.stream().map(Course::getEnrollmentTermId)
                .distinct().collect(Collectors.toList());

        return availableTermsIds.stream()
                .map(allTermMap::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(TermHelper::getStartDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public BlueprintAssociationModel buildAssociationBackingModel(String courseId, String username) {
        BlueprintAssociationModel blueprintAssociationModel = new BlueprintAssociationModel();
        blueprintAssociationModel.setInitialized(true);

        Course blueprintCourse = coursesApi.getCourse(courseId);

        List<Course> availableCourses = getAvailableCourses(username, blueprintCourse.getAccountId());

        Map<String, List<Course>> termCourseMap = availableCourses.stream()
                .collect(Collectors.groupingBy(Course::getEnrollmentTermId));


        List<BlueprintAssociatedCourse> associatedCourses = getAssociatedCourses(courseId, availableCourses);
        List<String> associatedCourseIds = associatedCourses.stream().map(BlueprintAssociatedCourse::getId)
                .distinct().collect(Collectors.toList());

        List<CanvasTerm> availableTerms = getPossibleTerms(availableCourses);

        availableTerms.forEach(term -> {
            List<Course> termCourses = termCourseMap.get(term.getId());
            BlueprintTermModel btm = new BlueprintTermModel(term.getId(), term.getName(),
                    termCourses.stream().map(course -> new BlueprintCourseModel(course.getId(), course.getName(), associatedCourseIds))
                            .collect(Collectors.toList()));

            //Now that we have course, check for any that are selected
            Predicate<BlueprintCourseModel> p1 = c -> c.isOriginallyChecked() || c.isCurrentlyChecked();

            boolean termVisible = btm.getCourses().stream().anyMatch(p1);
            btm.setVisible(termVisible);
            blueprintAssociationModel.addTermModel(btm);

            //Add the term to the dropdown if it is not shown on the page
            if (!termVisible) {
                blueprintAssociationModel.addTermOption(term);
            }
        });

        blueprintAssociationModel.setAssociatedCourses(associatedCourses);


        return blueprintAssociationModel;
    }

    public BlueprintConfirmationModel buildConfirmationBackingModel(String courseId, BlueprintAssociationModel blueprintAssociationModel) {
        BlueprintConfirmationModel bcm = new BlueprintConfirmationModel();
        bcm.setBlueprintCourseId(courseId);
        bcm.setPublishAfterSync(blueprintAssociationModel.isPublishAfterSync());

        List<BlueprintCourseModel> flatCourses = blueprintAssociationModel.getTermModels().stream()
                .map(BlueprintTermModel::getCourses)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        flatCourses.forEach(course -> {
            BlueprintConfirmationModel.CourseConfirmation cc = new BlueprintConfirmationModel.CourseConfirmation(course);
            if (course.isCurrentlyChecked()) {
                bcm.addFinalCourse(cc);
                if (!course.isOriginallyChecked()) {
                    bcm.addAddedCourse(cc);
                }
            } else {
                if (course.isOriginallyChecked()) {
                    bcm.addRemovedCourse(cc);
                }
            }
        });

        return bcm;
    }

    public BlueprintUpdateStatus updateAssociations(BlueprintConfirmationModel model) {
        List<BlueprintConfirmationModel.CourseConfirmation> addedCourses = model.getAddedCourses();
        List<String> addedCourseIds = addedCourses.stream()
                .map(BlueprintConfirmationModel.CourseConfirmation::getCourseId)
                .collect(Collectors.toList());

        List<BlueprintConfirmationModel.CourseConfirmation> removedCourses = model.getRemovedCourses();
        List<String> removedCourseIds = removedCourses.stream()
                .map(BlueprintConfirmationModel.CourseConfirmation::getCourseId)
                .collect(Collectors.toList());

        return blueprintApi.updateAssociatedCourses(model.getBlueprintCourseId(), TEMPLATE_ID,
                addedCourseIds, removedCourseIds);
    }

    public BlueprintMigrationStatus performMigration(String courseId, boolean copySettings, boolean sendNotifications, String username, boolean publishAfterSync) {
        List<BlueprintMigration> filteredMigrations = getActiveMigrationStatuses(courseId);

        if (filteredMigrations.isEmpty()) {
            String asUser = username != null ? CanvasConstants.API_FIELD_SIS_LOGIN_ID + ":" + username : null;
            return blueprintApi.performMigration(courseId, TEMPLATE_ID, copySettings, sendNotifications, asUser, publishAfterSync);
        } else {
            BlueprintMigrationStatus status = new BlueprintMigrationStatus();
            status.setMessage("A sync is currently in progress.  Please wait a few minutes and try to associate these courses again.");
            return status;
        }
    }

    public List<BlueprintMigration> getActiveMigrationStatuses(String courseId) {
        //Check for currently running migrations
        List<BlueprintMigration> blueprintMigrations = blueprintApi.getMigrations(courseId, TEMPLATE_ID);

        List<String> activeStates = Arrays.asList(BlueprintHelper.WORKFLOW_STATE.EXPORTING.getValue(),
              BlueprintHelper.WORKFLOW_STATE.IMPORTS_QUEUED.getValue(), BlueprintHelper.WORKFLOW_STATE.QUEUED.getValue());

        return blueprintMigrations.stream()
                .filter(m -> activeStates.contains(m.getWorkflowState()))
                .collect(Collectors.toList());
    }
}
