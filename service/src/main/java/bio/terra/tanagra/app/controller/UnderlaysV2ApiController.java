package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.api.accesscontrol.Action.READ;
import static bio.terra.tanagra.api.accesscontrol.ResourceType.UNDERLAY;

import bio.terra.tanagra.api.AccessControlService;
import bio.terra.tanagra.api.UnderlaysService;
import bio.terra.tanagra.api.accesscontrol.ResourceId;
import bio.terra.tanagra.api.accesscontrol.ResourceIdCollection;
import bio.terra.tanagra.generated.controller.UnderlaysV2Api;
import bio.terra.tanagra.generated.model.ApiUnderlayListV2;
import bio.terra.tanagra.generated.model.ApiUnderlayV2;
import bio.terra.tanagra.underlay.Underlay;
import java.util.List;
import java.util.stream.Collectors;
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
  public ResponseEntity<ApiUnderlayListV2> listUnderlaysV2() {
    ResourceIdCollection authorizedUnderlayNames = accessControlService.listResourceIds(UNDERLAY);
    List<Underlay> authorizedUnderlays;
    if (authorizedUnderlayNames.isAllResourceIds()) {
      authorizedUnderlays = underlaysService.getUnderlays();
    } else {
      authorizedUnderlays =
          underlaysService.getUnderlays(
              authorizedUnderlayNames.getResourceIds().stream()
                  .map(ResourceId::getId)
                  .collect(Collectors.toList()));
    }

    return ResponseEntity.ok(
        new ApiUnderlayListV2()
            .underlays(
                authorizedUnderlays.stream()
                    .map(u -> toApiObject(u))
                    .collect(Collectors.toList())));
  }

  @Override
  public ResponseEntity<ApiUnderlayV2> getUnderlayV2(String underlayName) {
    accessControlService.throwIfUnauthorized(null, READ, UNDERLAY, new ResourceId(underlayName));
    return ResponseEntity.ok(toApiObject(underlaysService.getUnderlay(underlayName)));
  }

  private ApiUnderlayV2 toApiObject(Underlay underlay) {
    return new ApiUnderlayV2()
        .name(underlay.getName())
        // TODO: Add display name to underlay config files.
        .displayName(underlay.getName())
        .primaryEntity(underlay.getPrimaryEntity().getName())
        .uiConfiguration(underlay.getUIConfig());
  }
}
