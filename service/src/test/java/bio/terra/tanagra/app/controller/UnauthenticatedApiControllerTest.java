package bio.terra.tanagra.app.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bio.terra.tanagra.generated.model.ApiSystemVersion;
import bio.terra.tanagra.testing.BaseSpringUnitTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
public class UnauthenticatedApiControllerTest extends BaseSpringUnitTest {
  @Autowired private MockMvc mvc;

  @Test
  public void serviceStatusTest() {
    assertDoesNotThrow(
        () -> this.mvc.perform(get("/status")).andExpect(status().isOk()).andReturn());
  }

  @Test
  public void serviceVersionTest() throws Exception {
    MockHttpServletResponse response =
        this.mvc.perform(get("/version")).andExpect(status().isOk()).andReturn().getResponse();

    ApiSystemVersion version =
        new ObjectMapper().readValue(response.getContentAsString(), ApiSystemVersion.class);
    assertNotNull(version);
    assertNotNull(version.getGitTag());
    assertNotNull(version.getGitHash());
    assertNotNull(version.getGithub());
    assertNotNull(version.getBuild());
  }
}
