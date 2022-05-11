package edu.iu.uits.lms.blueprintmanager.services;

import edu.iu.uits.lms.blueprintmanager.controller.BlueprintModel;
import edu.iu.uits.lms.blueprintmanager.controller.BlueprintSettings;
import edu.iu.uits.lms.blueprintmanager.model.BlueprintAssociationModel;
import edu.iu.uits.lms.blueprintmanager.model.BlueprintConfirmationModel;
import edu.iu.uits.lms.blueprintmanager.model.BlueprintCourseModel;
import edu.iu.uits.lms.blueprintmanager.model.BlueprintTermModel;
import edu.iu.uits.lms.canvas.helpers.CanvasDateFormatUtil;
import edu.iu.uits.lms.canvas.helpers.EnrollmentHelper;
import edu.iu.uits.lms.canvas.helpers.TermHelper;
import edu.iu.uits.lms.canvas.model.Account;
import edu.iu.uits.lms.canvas.model.BlueprintCourseUpdateStatus;
import edu.iu.uits.lms.canvas.model.CanvasTerm;
import edu.iu.uits.lms.canvas.model.Course;
import edu.iu.uits.lms.canvas.model.User;
import edu.iu.uits.lms.canvas.services.AccountService;
import edu.iu.uits.lms.canvas.services.BlueprintService;
import edu.iu.uits.lms.canvas.services.CourseService;
import edu.iu.uits.lms.canvas.services.TermService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;

@Slf4j
public class BlueprintToolServiceImplTest {

    @Autowired
    @InjectMocks
    private BlueprintToolService blueprintToolService;

    @Autowired
    @Mock
    private CourseService coursesApi;

    @Autowired
    @Mock
    private TermService termsApi;

    @Autowired
    @Mock
    private AccountService accountsApi;

    @Autowired
    @Mock
    private BlueprintService blueprintApi;

    private static final String ID = "asdf";

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    private Course baseCourse(EnrollmentHelper.TYPE type) {
        Course course = new Course();
        course.setId(ID);
        course.setBlueprint(true);
        course.setBlueprintRestrictions(null);
        course.setBlueprintRestrictionsByObjectType(null);
        return course;
    }

    @Test
    public void testGetCourseSettingsWithNoEnrollments() {
        Course course = baseCourse(null);

        Mockito.when(coursesApi.getCourse(ID)).thenReturn(course);
        Mockito.when(coursesApi.getUsersForCourseByType(anyString(), anyList(), anyList()))
                .thenReturn(null);

        BlueprintSettings courseSettings = blueprintToolService.getCourseSettings(ID);
        Assertions.assertFalse(courseSettings.hasEnrollments());
    }

    @Test
    public void testGetCourseSettingsWithStudents() {
        Course course = baseCourse(EnrollmentHelper.TYPE.student);

        Mockito.when(coursesApi.getCourse(ID)).thenReturn(course);

        List<User> users = new ArrayList<>();
        User user = new User();
        user.setId("1");
        users.add(user);

        Mockito.when(coursesApi.getUsersForCourseByType(anyString(), anyList(), anyList()))
                .thenReturn(users);

        BlueprintSettings courseSettings = blueprintToolService.getCourseSettings(ID);
        Assertions.assertTrue(courseSettings.hasEnrollments());
    }

    @Test
    public void testGetCourseSettingsWithInstructors() {
        Course course = baseCourse(EnrollmentHelper.TYPE.teacher);

        Mockito.when(coursesApi.getCourse(ID)).thenReturn(course);

        BlueprintSettings courseSettings = blueprintToolService.getCourseSettings(ID);
        Assertions.assertFalse(courseSettings.hasEnrollments());
    }

    @Test
    public void testGoodUpdate() {
        Mockito.when(coursesApi.getCourse(ID)).thenReturn(baseCourse(null));

        Course course = baseCourse(EnrollmentHelper.TYPE.student);
        BlueprintCourseUpdateStatus blueprintCourseUpdateStatus = new BlueprintCourseUpdateStatus();
        blueprintCourseUpdateStatus.setCourse(course);

        Mockito.when(blueprintApi.saveBlueprintConfiguration(anyString(), any()))
                .thenReturn(blueprintCourseUpdateStatus);
        BlueprintModel blueprintModel = new BlueprintModel();
        blueprintModel.setCourseId(ID);

        CanvasTerm noExpTerm = new CanvasTerm();
        noExpTerm.setSisTermId("noexp");
        noExpTerm.setId("1234");
        Mockito.when(termsApi.getTermBySisId(TermHelper.TERM_NO_EXPIRATION)).thenReturn(noExpTerm);

        BlueprintSettings settings = blueprintToolService.updateCourseSettings(blueprintModel);
        Assertions.assertNotNull(settings);
    }

    @Test
    public void testBadUpdate() {
        Mockito.when(coursesApi.getCourse(ID)).thenReturn(baseCourse(null));
        Mockito.doThrow(BlueprintConfigurationUpdateException.class)
                .when(blueprintApi).saveBlueprintConfiguration(anyString(), any());

        BlueprintModel blueprintModel = new BlueprintModel();
        blueprintModel.setCourseId(ID);

        BlueprintConfigurationUpdateException t = Assertions.assertThrows(BlueprintConfigurationUpdateException.class, () ->
              blueprintToolService.updateCourseSettings(blueprintModel));
    }

    @Test
    public void testConfirmationBackingModel() {
        BlueprintAssociationModel bam = new BlueprintAssociationModel();
        List<BlueprintCourseModel> courseModels = new ArrayList<>();
        courseModels.add(new BlueprintCourseModel("1", "course1", "term1", false, false));
        courseModels.add(new BlueprintCourseModel("2", "course2", "term1", true, false));
        courseModels.add(new BlueprintCourseModel("3", "course3", "term1", false, true));
        courseModels.add(new BlueprintCourseModel("4", "course4", "term1", true, true));
        bam.addTermModel(new BlueprintTermModel("1", "term1", courseModels));

        List<BlueprintCourseModel> courseModels2 = new ArrayList<>();
        courseModels2.add(new BlueprintCourseModel("5", "course5", "term2", false, false));
        courseModels2.add(new BlueprintCourseModel("6", "course6", "term2", true, false));
        courseModels2.add(new BlueprintCourseModel("7", "course7", "term2", false, true));
        courseModels2.add(new BlueprintCourseModel("8", "course8", "term2", true, true));
        bam.addTermModel(new BlueprintTermModel("2", "term2", courseModels2));

        BlueprintConfirmationModel bcm = blueprintToolService.buildConfirmationBackingModel("bp1", bam);

        Assertions.assertEquals("bp1", bcm.getBlueprintCourseId());
        Assertions.assertEquals(2, bcm.getAddedCourses().size());
        Assertions.assertEquals(2, bcm.getRemovedCourses().size());
        Assertions.assertEquals(4, bcm.getFinalCourses().size());

    }

    @Test
    public void testGetPossibleTerms() {
        List<CanvasTerm> terms = new ArrayList<>();
        CanvasTerm ct1 = new CanvasTerm();
        ct1.setId("t1");
        ct1.setName("Term 1");
        ct1.setStartAt("2016-01-01T05:00:00Z");
        terms.add(ct1);

        CanvasTerm ct2 = new CanvasTerm();
        ct2.setId("t2");
        ct2.setName("Term 2");
        terms.add(ct2);

        CanvasTerm ct3 = new CanvasTerm();
        ct3.setId("t3");
        ct3.setName("Term 3");
        ct3.setStartAt("2017-01-01T05:00:00Z");
        terms.add(ct3);

        CanvasTerm ct4 = new CanvasTerm();
        ct4.setId("t4");
        ct4.setName("Term 4");
        ct4.setStartAt("2015-01-01T05:00:00Z");
        terms.add(ct4);

        Mockito.when(termsApi.getEnrollmentTerms()).thenReturn(terms);

        List<Course> courses = new ArrayList<>();
        Course c1 = new Course();
        c1.setId("c1");
        c1.setEnrollmentTermId("t1");
        courses.add(c1);

        Course c2 = new Course();
        c2.setId("c2");
        c2.setEnrollmentTermId("t2");
        courses.add(c2);

        Course c3 = new Course();
        c3.setId("c3");
        c3.setEnrollmentTermId("t3");
        courses.add(c3);

        List<CanvasTerm> possibleTerms = blueprintToolService.getPossibleTerms(courses);
        Assertions.assertNotNull(possibleTerms);
        Assertions.assertEquals(3, possibleTerms.size());

        Assertions.assertEquals("t3", possibleTerms.get(0).getId());
        Assertions.assertEquals("t1", possibleTerms.get(1).getId());
        Assertions.assertEquals("t2", possibleTerms.get(2).getId());
    }

    @Test
    public void testGetAvailableCourses() {
        final String userId = "asdf";
        final String accountId = "123";

        //Root account
        Account a1 = new Account();
        a1.setId("root");
        a1.setParentAccountId(null);

        //First child under root
        Account a2 = new Account();
        a2.setId("123");
        a2.setParentAccountId("root");

        //Grand child under root
        Account a3 = new Account();
        a3.setId("456");
        a3.setParentAccountId("123");

        //great grand child under root
        Account a4 = new Account();
        a4.setId("789");
        a4.setParentAccountId("456");

        //Second child under root
        Account a5 = new Account();
        a5.setId("abc");
        a5.setParentAccountId("root");

        /*
        Account structure:
                  root
                 /    \
               a2     a5
              /
            a3
           /
         a4

         */

        List<Course> courses = new ArrayList<>();
        Course c1 = new Course();
        c1.setId("c1");
        c1.setAccountId(a4.getId());
        courses.add(c1);

        Course c2 = new Course();
        c2.setId("c2");
        c2.setAccountId(a3.getId());
        courses.add(c2);

        Course c3 = new Course();
        c3.setId("c3");
        c3.setAccountId(a5.getId());
        courses.add(c3);

        Course c4 = new Course();
        c4.setId("c4");
        c4.setAccountId(a3.getId());
        c4.setBlueprint(true);
        courses.add(c4);

        /*

        Course Structure:
                  root
                 /    \
               -       c3
              /
            c2,c4
           /
         c1

        */

        List<Account> accountList1 = Arrays.asList(a1, a2, a3, a4);
        List<Account> accountList2 = Arrays.asList(a1, a2, a3);
        List<Account> accountList3 = Arrays.asList(a1, a2);
        List<Account> accountList4 = Arrays.asList(a1, a5);

        Mockito.when(coursesApi.getCoursesTaughtBy(userId, false, false, false)).thenReturn(courses);
        Mockito.when(accountsApi.getParentAccounts("789")).thenReturn(accountList1);
        Mockito.when(accountsApi.getParentAccounts("456")).thenReturn(accountList2);
        Mockito.when(accountsApi.getParentAccounts("123")).thenReturn(accountList3);
        Mockito.when(accountsApi.getParentAccounts("abc")).thenReturn(accountList4);

        List<Course> availableCourses = blueprintToolService.getAvailableCourses(userId, accountId);
        Assertions.assertEquals(2, availableCourses.size());

    }

    @Test
    public void fooTest() {
        OffsetDateTime endDate = CanvasDateFormatUtil.getCalculatedCourseEndDate();
        log.debug("End Date: {}", endDate);
        Assertions.assertNotNull(endDate);
    }
}
