package bio.terra.tanagra.app.controller;

import bio.terra.tanagra.app.authentication.SpringAuthentication;
import bio.terra.tanagra.generated.controller.UsersApi;
import bio.terra.tanagra.generated.model.ApiUserProfile;
import bio.terra.tanagra.service.authentication.UserId;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class UsersApiController implements UsersApi {
  @Override
  public ResponseEntity<ApiUserProfile> getMe() {
    UserId userId = SpringAuthentication.getCurrentUser();
    return ResponseEntity.ok(
        new ApiUserProfile().email(userId.getEmail()).subjectId(userId.getSubject()));
  }
}
