package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.READ;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.UNDERLAY;

import bio.terra.tanagra.app.authentication.SpringAuthentication;
import bio.terra.tanagra.app.controller.objmapping.ToApiUtils;
import bio.terra.tanagra.generated.controller.EntitiesApi;
import bio.terra.tanagra.generated.model.ApiEntity;
import bio.terra.tanagra.generated.model.ApiEntityList;
import bio.terra.tanagra.service.accesscontrol.AccessControlService;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.query.UnderlayService;
import bio.terra.tanagra.underlay.Entity;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class EntitiesApiController implements EntitiesApi {
  private final UnderlayService underlayService;
  private final AccessControlService accessControlService;

  @Autowired
  public EntitiesApiController(
      UnderlayService underlayService, AccessControlService accessControlService) {
    this.underlayService = underlayService;
    this.accessControlService = accessControlService;
  }

  @Override
  public ResponseEntity<ApiEntityList> listEntities(String underlayName) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(UNDERLAY, READ),
        ResourceId.forUnderlay(underlayName));
    ApiEntityList apiEntities = new ApiEntityList();
    List<Entity> entities = underlayService.listEntities(underlayName);
    entities.stream().forEach(entity -> apiEntities.addEntitiesItem(toApiObject(entity)));
    return ResponseEntity.ok(apiEntities);
  }

  @Override
  public ResponseEntity<ApiEntity> getEntity(String underlayName, String entityName) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(UNDERLAY, READ),
        ResourceId.forUnderlay(underlayName));
    Entity entity = underlayService.getEntity(underlayName, entityName);
    return ResponseEntity.ok(toApiObject(entity));
  }

  private ApiEntity toApiObject(Entity entity) {
    return new ApiEntity()
        .name(entity.getName())
        .idAttribute(entity.getIdAttribute().getName())
        .attributes(
            entity.getAttributes().stream()
                .map(a -> ToApiUtils.toApiObject(a))
                .collect(Collectors.toList()));
  }
}
