package bio.terra.tanagra.app.controller;

import bio.terra.tanagra.api.UnderlaysService;
import bio.terra.tanagra.generated.controller.UnderlaysV2Api;
import bio.terra.tanagra.generated.model.ApiUnderlayListV2;
import bio.terra.tanagra.generated.model.ApiUnderlayV2;
import bio.terra.tanagra.underlay.Underlay;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class UnderlaysV2ApiController implements UnderlaysV2Api {
  private final UnderlaysService underlaysService;

  @Autowired
  public UnderlaysV2ApiController(UnderlaysService underlaysService) {
    this.underlaysService = underlaysService;
  }

  @Override
  public ResponseEntity<ApiUnderlayListV2> listUnderlaysV2() {
    return ResponseEntity.ok(
        new ApiUnderlayListV2()
            .underlays(
                underlaysService.getUnderlays().stream()
                    .map(u -> toApiObject(u))
                    .collect(Collectors.toList())));
  }

  @Override
  public ResponseEntity<ApiUnderlayV2> getUnderlayV2(String underlayName) {
    return ResponseEntity.ok(toApiObject(underlaysService.getUnderlay(underlayName)));
  }

  private ApiUnderlayV2 toApiObject(Underlay underlay) {
    return new ApiUnderlayV2()
        .name(underlay.getName())
        // TODO: Add display name to underlay config files.
        .displayName(underlay.getName())
        .primaryEntity(underlay.getPrimaryEntity().getName());
  }
}
