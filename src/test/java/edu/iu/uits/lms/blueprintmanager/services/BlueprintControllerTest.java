package edu.iu.uits.lms.blueprintmanager.services;

import canvas.client.generated.model.BlueprintMigration;
import canvas.client.generated.model.BlueprintMigrationStatus;
import canvas.client.generated.model.BlueprintUpdateStatus;
import canvas.client.generated.model.Course;
import edu.iu.uits.lms.blueprintmanager.controller.BlueprintController;
import edu.iu.uits.lms.blueprintmanager.controller.BlueprintSettings;
import edu.iu.uits.lms.common.session.CourseSessionService;
import edu.iu.uits.lms.lti.security.LtiAuthenticationToken;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.View;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.mockito.Matchers.any;

public class BlueprintControllerTest {

    @Autowired
    @InjectMocks
    private BlueprintController blueprintController;

    @Autowired
    @Mock
    private BlueprintToolService blueprintToolService;

    @Autowired
    @Mock
    private CourseSessionService courseSessionService;

    @Autowired
    @Mock
    private MessageSource messageSource;

    private MockMvc mockMvc;

    @Mock
    private View view;

    private static final String ID = "asdf";

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        //setup lti token
        List authoritiesList = Arrays.asList(new SimpleGrantedAuthority("Instructor"),
                new SimpleGrantedAuthority("ROLE_LTI_INSTRUCTOR"));

        final LtiAuthenticationToken ltiAuthenticationToken = new LtiAuthenticationToken("user1",
                ID,
                "test.uits.iu.edu",
                authoritiesList,
                "lms_lti_blueprint");

        SecurityContextHolder.getContext().setAuthentication(ltiAuthenticationToken);

        mockMvc = MockMvcBuilders
                .standaloneSetup(blueprintController)
                .setSingleView(view)
                .build();
    }

    private Course baseCourse(String id) {
        Course course = new Course();
        course.setId(id);
        course.setBlueprint(true);
        course.setBlueprintRestrictions(null);
        course.setBlueprintRestrictionsByObjectType(null);
        return course;
    }

    @Test
    public void testGoodLoad() throws Exception {
        Course course = baseCourse(ID);
        course.setSisCourseId("sis_course_id");

        BlueprintSettings settings = new BlueprintSettings();
        settings.setCourse(course);

        Mockito.when(blueprintToolService.getCourseSettings(ID)).thenReturn(settings);

        ResultActions mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.get("/app/{ID}/index", ID));

        mockMvcAction.andExpect(MockMvcResultMatchers.status().isOk());
        mockMvcAction.andExpect(MockMvcResultMatchers.view().name("index"));
        mockMvcAction.andExpect(MockMvcResultMatchers.model().attributeExists ("messageInfoKey"));
        mockMvcAction.andExpect(MockMvcResultMatchers.model().attribute("messageInfoKey", "blueprint.sisCourse.warning"));

    }

    @Test
    public void testBadLoad() throws Exception {
        Course course = baseCourse(ID);

        BlueprintSettings settings = new BlueprintSettings();
        settings.setCourse(course);
        settings.setAlreadyAssociated(true);
        Mockito.when(blueprintToolService.getCourseSettings(ID)).thenReturn(settings);

        ResultActions mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.get("/app/{ID}/index", ID));

        mockMvcAction.andExpect(MockMvcResultMatchers.status().isOk());
        mockMvcAction.andExpect(MockMvcResultMatchers.view().name("index"));
        mockMvcAction.andExpect(MockMvcResultMatchers.model().attributeExists ("messageInfoKey"));
        mockMvcAction.andExpect(MockMvcResultMatchers.model().attribute("messageInfoKey", "blueprint.courseAlreadyAssociated"));

    }

    @Test
    public void testLoadSisCourse() throws Exception {
        Course course = baseCourse(ID);
        course.setSisCourseId("sis_course_id");

        BlueprintSettings settings = new BlueprintSettings();
        settings.setCourse(course);
        Mockito.when(blueprintToolService.getCourseSettings(ID)).thenReturn(settings);

        ResultActions mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.get("/app/{ID}/index", ID));

        mockMvcAction.andExpect(MockMvcResultMatchers.status().isOk());
        mockMvcAction.andExpect(MockMvcResultMatchers.view().name("index"));
        mockMvcAction.andExpect(MockMvcResultMatchers.model().attributeExists ("messageInfoKey"));
        mockMvcAction.andExpect(MockMvcResultMatchers.model().attribute("messageInfoKey", "blueprint.sisCourse.warning"));
    }

    @Test
    public void testLoadCourseWithStudentEnrollments() throws Exception {
        Course course = baseCourse(ID);

        BlueprintSettings settings = new BlueprintSettings();
        settings.setCourse(course);
        settings.hasEnrollments(true);
        Mockito.when(blueprintToolService.getCourseSettings(ID)).thenReturn(settings);

        ResultActions mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.get("/app/{ID}/index", ID));

        mockMvcAction.andExpect(MockMvcResultMatchers.status().isOk());
        mockMvcAction.andExpect(MockMvcResultMatchers.view().name("index"));
        mockMvcAction.andExpect(MockMvcResultMatchers.model().attributeExists ("messageInfoKey"));
        mockMvcAction.andExpect(MockMvcResultMatchers.model().attribute("messageInfoKey", "blueprint.enrollments.warning"));
    }

    @Test
    public void testLoadCourseWithInstructorEnrollments() throws Exception {
        Course course = baseCourse(ID);

        BlueprintSettings settings = new BlueprintSettings();
        settings.setCourse(course);
        settings.hasEnrollments(false);
        Mockito.when(blueprintToolService.getCourseSettings(ID)).thenReturn(settings);

        ResultActions mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.get("/app/{ID}/index", ID));

        mockMvcAction.andExpect(MockMvcResultMatchers.status().isOk());
        mockMvcAction.andExpect(MockMvcResultMatchers.view().name("index"));
        mockMvcAction.andExpect(MockMvcResultMatchers.model().attributeDoesNotExist("messageInfoKey"));
    }

    @Test
    public void testUpdateSuccess() throws Exception {
        Course course = baseCourse(ID);
        course.setSisCourseId("sis_course_id");

        BlueprintSettings settings = new BlueprintSettings();
        settings.setCourse(course);
        Mockito.when(blueprintToolService.getCourseSettings(ID)).thenReturn(settings);
        Mockito.when(blueprintToolService.updateCourseSettings(any())).thenReturn(settings);

        ResultActions mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.post("/app/{ID}/submit?action=save", ID));

        mockMvcAction.andExpect(MockMvcResultMatchers.status().isOk());
        mockMvcAction.andExpect(MockMvcResultMatchers.view().name("index"));
        mockMvcAction.andExpect(MockMvcResultMatchers.model().attributeExists ("submitSuccess"));
        mockMvcAction.andExpect(MockMvcResultMatchers.model().attributeDoesNotExist("submitFailure"));
    }

    @Test
    public void testUpdateFailure() throws Exception {
        Course course = baseCourse(ID);
        course.setSisCourseId("sis_course_id");

        BlueprintSettings settings = new BlueprintSettings();
        settings.setCourse(course);
        Mockito.when(blueprintToolService.getCourseSettings(ID)).thenReturn(settings);
        Mockito.when(messageSource.getMessage("settings.failure", null, Locale.getDefault())).thenReturn("Foobar");

        Mockito.doThrow(BlueprintConfigurationUpdateException.class).when(blueprintToolService).updateCourseSettings(any());
        ResultActions mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.post("/app/{ID}/submit?action=save", ID));

        mockMvcAction.andExpect(MockMvcResultMatchers.status().isOk());
        mockMvcAction.andExpect(MockMvcResultMatchers.view().name("index"));
        mockMvcAction.andExpect(MockMvcResultMatchers.model().attributeExists ("submitFailure"));
        mockMvcAction.andExpect(MockMvcResultMatchers.model().attributeExists ("submitFailureMessage"));
        mockMvcAction.andExpect(MockMvcResultMatchers.model().attributeDoesNotExist("submitSuccess"));
    }

    @Test
    public void testAssociateFailure() throws Exception {
        BlueprintUpdateStatus updateStatus = new BlueprintUpdateStatus();
        updateStatus.setSuccess(false);
        updateStatus.setMessage("boom!");

        Mockito.when(blueprintToolService.updateAssociations(any())).thenReturn(updateStatus);

        ResultActions mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.post("/app/{ID}/confirm?action=submit", ID));

        mockMvcAction.andExpect(MockMvcResultMatchers.status().isOk());
        mockMvcAction.andExpect(MockMvcResultMatchers.view().name("associate"));
        mockMvcAction.andExpect(MockMvcResultMatchers.model().attributeExists ("associateFailureMessage"));
        mockMvcAction.andExpect(MockMvcResultMatchers.model().attributeDoesNotExist("associateSuccess"));
    }

    @Test
    public void testAssociateSuccessSyncSuccess() throws Exception {
        BlueprintUpdateStatus updateStatus = new BlueprintUpdateStatus();
        updateStatus.setSuccess(true);
        Mockito.when(blueprintToolService.updateAssociations(any())).thenReturn(updateStatus);

        BlueprintMigrationStatus migrationStatus = new BlueprintMigrationStatus();
        BlueprintMigration blueprintMigration = new BlueprintMigration();
        blueprintMigration.setId("1");
        migrationStatus.setBlueprintMigration(blueprintMigration);
        Mockito.when(blueprintToolService.performMigration(any(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.anyString(), Mockito.anyBoolean())).thenReturn(migrationStatus);


        ResultActions mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.post("/app/{ID}/confirm?action=submit", ID));

        mockMvcAction.andExpect(MockMvcResultMatchers.status().isOk());
        mockMvcAction.andExpect(MockMvcResultMatchers.view().name("associate"));
        mockMvcAction.andExpect(MockMvcResultMatchers.model().attributeExists ("associateSuccess"));
        mockMvcAction.andExpect(MockMvcResultMatchers.model().attributeDoesNotExist("associateFailureMessage"));
    }

    @Test
    public void testAssociateSuccessSyncFailure() throws Exception {
        BlueprintUpdateStatus updateStatus = new BlueprintUpdateStatus();
        updateStatus.setSuccess(true);
        Mockito.when(blueprintToolService.updateAssociations(any())).thenReturn(updateStatus);

        BlueprintMigrationStatus migrationStatus = new BlueprintMigrationStatus();
        migrationStatus.setMessage("boom!");
        Mockito.when(blueprintToolService.performMigration(any(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.anyString(), Mockito.anyBoolean())).thenReturn(migrationStatus);

        ResultActions mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.post("/app/{ID}/confirm?action=submit", ID));

        mockMvcAction.andExpect(MockMvcResultMatchers.status().isOk());
        mockMvcAction.andExpect(MockMvcResultMatchers.view().name("associate"));
        mockMvcAction.andExpect(MockMvcResultMatchers.model().attributeExists ("syncWarningMessage"));
        mockMvcAction.andExpect(MockMvcResultMatchers.model().attributeDoesNotExist("associateSuccess"));
    }
}
