package bio.terra.tanagra.app.controller;

import bio.terra.tanagra.api.UnderlaysService;
import bio.terra.tanagra.api.utils.ToApiConversionUtils;
import bio.terra.tanagra.generated.controller.EntitiesV2Api;
import bio.terra.tanagra.generated.model.ApiEntityListV2;
import bio.terra.tanagra.generated.model.ApiEntityV2;
import bio.terra.tanagra.underlay.Entity;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class EntitiesV2ApiController implements EntitiesV2Api {
  private final UnderlaysService underlaysService;

  @Autowired
  public EntitiesV2ApiController(UnderlaysService underlaysService) {
    this.underlaysService = underlaysService;
  }

  @Override
  public ResponseEntity<ApiEntityListV2> listEntitiesV2(String underlayName) {
    return ResponseEntity.ok(
        new ApiEntityListV2()
            .entities(
                underlaysService.getUnderlay(underlayName).getEntities().values().stream()
                    .map(e -> toApiObject(e))
                    .collect(Collectors.toList())));
  }

  @Override
  public ResponseEntity<ApiEntityV2> getEntityV2(String underlayName, String entityName) {
    Entity entity = underlaysService.getEntity(underlayName, entityName);
    return ResponseEntity.ok(toApiObject(entity));
  }

  private ApiEntityV2 toApiObject(Entity entity) {
    return new ApiEntityV2()
        .name(entity.getName())
        .idAttribute(entity.getIdAttribute().getName())
        .attributes(
            entity.getAttributes().stream()
                .map(a -> ToApiConversionUtils.toApiObject(a))
                .collect(Collectors.toList()));
  }
}
