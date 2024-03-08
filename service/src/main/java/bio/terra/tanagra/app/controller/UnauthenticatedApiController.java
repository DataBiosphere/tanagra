package bio.terra.tanagra.app.controller;

import bio.terra.tanagra.app.configuration.VersionConfiguration;
import bio.terra.tanagra.generated.controller.UnauthenticatedApi;
import bio.terra.tanagra.generated.model.ApiSystemVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

/** Spring controller for the unauthenticated API methods. */
@Controller
public class UnauthenticatedApiController implements UnauthenticatedApi {
  private final ApiSystemVersion currentVersion;

  @Autowired
  public UnauthenticatedApiController(VersionConfiguration versionConfiguration) {
    this.currentVersion =
        new ApiSystemVersion()
            .gitTag(versionConfiguration.getGitTag())
            .gitHash(versionConfiguration.getGitHash())
            .github(versionConfiguration.getGithubUrl())
            .build(versionConfiguration.getBuild());
  }

  @Override
  public ResponseEntity<Void> serviceStatus() {
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Override
  public ResponseEntity<ApiSystemVersion> serviceVersion() {
    return new ResponseEntity<>(currentVersion, HttpStatus.OK);
  }
}
