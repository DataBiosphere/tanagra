package bio.terra.tanagra.app.controller;

import bio.terra.tanagra.app.configuration.VersionConfiguration;
import bio.terra.tanagra.generated.controller.UnauthenticatedApi;
import bio.terra.tanagra.generated.model.ApiSystemVersionV2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

/** Spring controller for the unauthenticated API methods. */
@Controller
public class UnauthenticatedApiController implements UnauthenticatedApi {
  private static final String GITHUB_COMMIT_URL =
      "https://github.com/DataBiosphere/tanagra/commit/";
  private final ApiSystemVersionV2 currentVersion;

  @Autowired
  public UnauthenticatedApiController(VersionConfiguration versionConfiguration) {
    this.currentVersion =
        new ApiSystemVersionV2()
            .gitTag(versionConfiguration.getGitTag())
            .gitHash(versionConfiguration.getGitHash())
            .github(GITHUB_COMMIT_URL + versionConfiguration.getGitHash())
            .build(versionConfiguration.getBuild());
  }

  @Override
  public ResponseEntity<Void> serviceStatus() {
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Override
  public ResponseEntity<ApiSystemVersionV2> serviceVersion() {
    return new ResponseEntity<>(currentVersion, HttpStatus.OK);
  }
}
