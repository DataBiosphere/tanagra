package bio.terra.tanagra.app.controller;

import bio.terra.tanagra.app.auth.SpringAuthentication;
import bio.terra.tanagra.generated.controller.UsersV2Api;
import bio.terra.tanagra.generated.model.ApiUserProfileV2;
import bio.terra.tanagra.service.auth.UserId;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class UsersV2ApiController implements UsersV2Api {
  @Override
  public ResponseEntity<ApiUserProfileV2> getMe() {
    UserId userId = SpringAuthentication.getCurrentUser();
    return ResponseEntity.ok(
        new ApiUserProfileV2().email(userId.getEmail()).subjectId(userId.getSubject()));
  }
}
