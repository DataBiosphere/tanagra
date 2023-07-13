package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.READ;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.UNDERLAY;

import bio.terra.tanagra.app.auth.SpringAuthentication;
import bio.terra.tanagra.generated.controller.UnderlaysV2Api;
import bio.terra.tanagra.generated.model.ApiUnderlayListV2;
import bio.terra.tanagra.generated.model.ApiUnderlayV2;
import bio.terra.tanagra.service.AccessControlService;
import bio.terra.tanagra.service.UnderlaysService;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.utils.ToApiConversionUtils;
import bio.terra.tanagra.underlay.Underlay;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class UnderlaysV2ApiController implements UnderlaysV2Api {
  private final UnderlaysService underlaysService;
  private final AccessControlService accessControlService;

  @Autowired
  public UnderlaysV2ApiController(
      UnderlaysService underlaysService, AccessControlService accessControlService) {
    this.underlaysService = underlaysService;
    this.accessControlService = accessControlService;
  }

  @Override
  public ResponseEntity<ApiUnderlayListV2> listUnderlays() {
    ResourceCollection authorizedUnderlayNames =
        accessControlService.listAuthorizedResources(
            SpringAuthentication.getCurrentUser(), Permissions.forActions(UNDERLAY, READ));
    List<Underlay> authorizedUnderlays = underlaysService.listUnderlays(authorizedUnderlayNames);
    ApiUnderlayListV2 apiUnderlays = new ApiUnderlayListV2();
    authorizedUnderlays.stream()
        .forEach(
            underlay -> apiUnderlays.addUnderlaysItem(ToApiConversionUtils.toApiObject(underlay)));
    return ResponseEntity.ok(apiUnderlays);
  }

  @Override
  public ResponseEntity<ApiUnderlayV2> getUnderlay(String underlayName) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(UNDERLAY, READ),
        ResourceId.forUnderlay(underlayName));
    return ResponseEntity.ok(
        ToApiConversionUtils.toApiObject(underlaysService.getUnderlay(underlayName)));
  }
}
