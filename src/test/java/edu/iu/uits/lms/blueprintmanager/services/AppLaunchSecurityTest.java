package edu.iu.uits.lms.blueprintmanager.services;

import edu.iu.uits.lms.blueprintmanager.config.ToolConfig;
import edu.iu.uits.lms.blueprintmanager.controller.BlueprintController;
import edu.iu.uits.lms.blueprintmanager.controller.BlueprintModel;
import edu.iu.uits.lms.blueprintmanager.controller.BlueprintSettings;
import edu.iu.uits.lms.canvas.model.Course;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.service.TestUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;

import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BlueprintController.class, properties = {"oauth.tokenprovider.url=http://foo"})
@Import(ToolConfig.class)
@ActiveProfiles("none")
public class AppLaunchSecurityTest {

   @Autowired
   private MockMvc mvc;

   @MockBean
   private BlueprintToolService blueprintToolService;

   @Test
   public void appNoAuthnLaunch() throws Exception {
      //This is a secured endpoint and should not not allow access without authn
      mvc.perform(get("/app/1234/index")
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
   }

   @Test
   public void appAuthnWrongContextLaunch() throws Exception {
      OidcAuthenticationToken token = TestUtils.buildToken("userId",
            "asdf", LTIConstants.INSTRUCTOR_AUTHORITY);

      SecurityContextHolder.getContext().setAuthentication(token);

      //This is a secured endpoint and should not not allow access without authn
      mvc.perform(get("/app/1234/index")
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError())
            .andExpect(MockMvcResultMatchers.view().name ("error"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("error"));
   }

   @Test
   public void appAuthnLaunch() throws Exception {
      OidcAuthenticationToken token = TestUtils.buildToken("userId",
            "1234", LTIConstants.INSTRUCTOR_AUTHORITY);

      SecurityContextHolder.getContext().setAuthentication(token);

      Course course = new Course();
      course.setId("1234");
      course.setBlueprint(true);
      course.setBlueprintRestrictions(null);
      course.setBlueprintRestrictionsByObjectType(null);
      course.setSisCourseId("sis_course_id");

      BlueprintSettings settings = new BlueprintSettings();
      settings.setCourse(course);
      BlueprintModel bm = new BlueprintModel();
//      bm.setBlueprintRestrictionsByObjectType(new HashMap<>());
      settings.setBlueprintModel(new BlueprintModel());

      Mockito.when(blueprintToolService.getCourseSettings(anyString())).thenReturn(settings);

      //This is a secured endpoint and should not not allow access without authn
      mvc.perform(get("/app/1234/index")
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
   }

   @Test
   public void randomUrlNoAuth() throws Exception {
      SecurityContextHolder.getContext().setAuthentication(null);
      //This is a secured endpoint and should not allow access without authn
      mvc.perform(get("/asdf/foobar")
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
   }

   @Test
   public void randomUrlWithAuth() throws Exception {
      OidcAuthenticationToken token = TestUtils.buildToken("userId",
            "1234", LTIConstants.INSTRUCTOR_AUTHORITY);
      SecurityContextHolder.getContext().setAuthentication(token);

      //This is a secured endpoint and should not not allow access without authn
      mvc.perform(get("/asdf/foobar")
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
   }
}
