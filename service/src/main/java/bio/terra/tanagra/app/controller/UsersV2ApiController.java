package bio.terra.tanagra.app.controller;

import bio.terra.tanagra.generated.controller.UsersV2Api;
import bio.terra.tanagra.generated.model.ApiUserProfileV2;
import bio.terra.tanagra.service.auth.UserAuthentication;
import bio.terra.tanagra.service.auth.UserId;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

@Controller
public class UsersV2ApiController implements UsersV2Api {
  @Override
  public ResponseEntity<ApiUserProfileV2> getMe() {
    UserId userId =
        ((UserAuthentication) SecurityContextHolder.getContext().getAuthentication())
            .getPrincipal();
    return ResponseEntity.ok(new ApiUserProfileV2().email(userId.getEmail()));
  }
}
