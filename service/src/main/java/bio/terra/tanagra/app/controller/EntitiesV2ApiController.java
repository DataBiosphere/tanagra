package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.READ;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.UNDERLAY;

import bio.terra.tanagra.app.auth.SpringAuthentication;
import bio.terra.tanagra.generated.controller.EntitiesV2Api;
import bio.terra.tanagra.generated.model.ApiEntityListV2;
import bio.terra.tanagra.generated.model.ApiEntityV2;
import bio.terra.tanagra.service.AccessControlService;
import bio.terra.tanagra.service.UnderlaysService;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.utils.ToApiConversionUtils;
import bio.terra.tanagra.underlay.Entity;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class EntitiesV2ApiController implements EntitiesV2Api {
  private final UnderlaysService underlaysService;
  private final AccessControlService accessControlService;

  @Autowired
  public EntitiesV2ApiController(
      UnderlaysService underlaysService, AccessControlService accessControlService) {
    this.underlaysService = underlaysService;
    this.accessControlService = accessControlService;
  }

  @Override
  public ResponseEntity<ApiEntityListV2> listEntities(String underlayName) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        READ,
        UNDERLAY,
        ResourceId.forUnderlay(underlayName));
    ApiEntityListV2 apiEntities = new ApiEntityListV2();
    List<Entity> entities = underlaysService.listEntities(underlayName);
    entities.stream().forEach(entity -> apiEntities.addEntitiesItem(toApiObject(entity)));
    return ResponseEntity.ok(apiEntities);
  }

  @Override
  public ResponseEntity<ApiEntityV2> getEntity(String underlayName, String entityName) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        READ,
        UNDERLAY,
        ResourceId.forUnderlay(underlayName));
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
