package edu.iu.uits.lms.blueprintmanager.services;

import com.nimbusds.jose.shaded.json.JSONObject;
import edu.iu.uits.lms.blueprintmanager.config.ToolConfig;
import edu.iu.uits.lms.blueprintmanager.controller.BlueprintController;
import edu.iu.uits.lms.blueprintmanager.controller.BlueprintModel;
import edu.iu.uits.lms.blueprintmanager.controller.BlueprintSettings;
import edu.iu.uits.lms.blueprintmanager.model.BlueprintAssociationModel;
import edu.iu.uits.lms.canvas.model.BlueprintMigration;
import edu.iu.uits.lms.canvas.model.BlueprintMigrationStatus;
import edu.iu.uits.lms.canvas.model.BlueprintRestriction;
import edu.iu.uits.lms.canvas.model.BlueprintUpdateStatus;
import edu.iu.uits.lms.canvas.model.Course;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.service.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(controllers = BlueprintController.class, properties = {"oauth.tokenprovider.url=http://foo", "logging.level.org.springframework.security=DEBUG"})
@Import(ToolConfig.class)
public class BlueprintControllerTest {

    @MockBean
    private BlueprintToolService blueprintToolService;

    @MockBean
    private MessageSource messageSource;

    @Autowired
    private MockMvc mockMvc;

    private static final String ID = "asdf";

    @BeforeEach
    public void setup() {
        //setup lti token

        Map<String, Object> extraAttributes = new HashMap<>();

        JSONObject customMap = new JSONObject();
        customMap.put(LTIConstants.CUSTOM_CANVAS_COURSE_ID_KEY, ID);
        customMap.put(LTIConstants.CUSTOM_CANVAS_USER_LOGIN_ID_KEY, "user1");

        OidcAuthenticationToken token = TestUtils.buildToken("userId", LTIConstants.INSTRUCTOR_AUTHORITY,
              extraAttributes, customMap);

        SecurityContextHolder.getContext().setAuthentication(token);
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

        BlueprintModel model = new BlueprintModel();
        model.setCourseId(ID);
        model.setBlueprintRestrictions(new BlueprintRestriction());
        settings.setBlueprintModel(model);
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

        ResultActions mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.post("/app/{ID}/submit?action=save", ID).with(csrf()));

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
        ResultActions mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.post("/app/{ID}/submit?action=save", ID).with(csrf()));

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
        Mockito.when(blueprintToolService.buildAssociationBackingModel(any(), any())).thenReturn(new BlueprintAssociationModel());

        ResultActions mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.post("/app/{ID}/confirm?action=submit", ID).with(csrf()));

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
        Mockito.when(blueprintToolService.buildAssociationBackingModel(any(), any())).thenReturn(new BlueprintAssociationModel());

        ResultActions mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.post("/app/{ID}/confirm?action=submit", ID).with(csrf()));

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
        Mockito.when(blueprintToolService.buildAssociationBackingModel(any(), any())).thenReturn(new BlueprintAssociationModel());

        ResultActions mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.post("/app/{ID}/confirm?action=submit", ID).with(csrf()));

        mockMvcAction.andExpect(MockMvcResultMatchers.status().isOk());
        mockMvcAction.andExpect(MockMvcResultMatchers.view().name("associate"));
        mockMvcAction.andExpect(MockMvcResultMatchers.model().attributeExists ("syncWarningMessage"));
        mockMvcAction.andExpect(MockMvcResultMatchers.model().attributeDoesNotExist("associateSuccess"));
    }
}
