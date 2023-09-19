package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.READ;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.UNDERLAY;

import bio.terra.tanagra.app.authentication.SpringAuthentication;
import bio.terra.tanagra.app.controller.objmapping.ToApiUtils;
import bio.terra.tanagra.generated.controller.UnderlaysApi;
import bio.terra.tanagra.generated.model.ApiUnderlay;
import bio.terra.tanagra.generated.model.ApiUnderlayList;
import bio.terra.tanagra.service.accesscontrol.AccessControlService;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.query.UnderlayService;
import bio.terra.tanagra.underlay.Underlay;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class UnderlaysApiController implements UnderlaysApi {
  private final UnderlayService underlayService;
  private final AccessControlService accessControlService;

  @Autowired
  public UnderlaysApiController(
      UnderlayService underlayService, AccessControlService accessControlService) {
    this.underlayService = underlayService;
    this.accessControlService = accessControlService;
  }

  @Override
  public ResponseEntity<ApiUnderlayList> listUnderlays() {
    ResourceCollection authorizedUnderlayNames =
        accessControlService.listAuthorizedResources(
            SpringAuthentication.getCurrentUser(), Permissions.forActions(UNDERLAY, READ));
    List<Underlay> authorizedUnderlays = underlayService.listUnderlays(authorizedUnderlayNames);
    ApiUnderlayList apiUnderlays = new ApiUnderlayList();
    authorizedUnderlays.stream()
        .forEach(underlay -> apiUnderlays.addUnderlaysItem(ToApiUtils.toApiObject(underlay)));
    return ResponseEntity.ok(apiUnderlays);
  }

  @Override
  public ResponseEntity<ApiUnderlay> getUnderlay(String underlayName) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(UNDERLAY, READ),
        ResourceId.forUnderlay(underlayName));
    return ResponseEntity.ok(ToApiUtils.toApiObject(underlayService.getUnderlay(underlayName)));
  }
}
