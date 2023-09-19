package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.QUERY_COUNTS;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.UNDERLAY;

import bio.terra.tanagra.api.query.EntityHintRequest;
import bio.terra.tanagra.api.query.EntityHintResult;
import bio.terra.tanagra.app.authentication.SpringAuthentication;
import bio.terra.tanagra.app.controller.objmapping.FromApiUtils;
import bio.terra.tanagra.app.controller.objmapping.ToApiUtils;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.generated.controller.HintsApi;
import bio.terra.tanagra.generated.model.ApiDisplayHint;
import bio.terra.tanagra.generated.model.ApiDisplayHintDisplayHint;
import bio.terra.tanagra.generated.model.ApiDisplayHintEnum;
import bio.terra.tanagra.generated.model.ApiDisplayHintEnumEnumHintValues;
import bio.terra.tanagra.generated.model.ApiDisplayHintList;
import bio.terra.tanagra.generated.model.ApiDisplayHintNumericRange;
import bio.terra.tanagra.generated.model.ApiHintQuery;
import bio.terra.tanagra.service.accesscontrol.AccessControlService;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.query.UnderlayService;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.DisplayHint;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.displayhint.EnumVals;
import bio.terra.tanagra.underlay.displayhint.NumericRange;
import bio.terra.tanagra.utils.SqlFormatter;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class HintsApiController implements HintsApi {
  private final UnderlayService underlayService;
  private final AccessControlService accessControlService;

  @Autowired
  public HintsApiController(
      UnderlayService underlayService, AccessControlService accessControlService) {
    this.underlayService = underlayService;
    this.accessControlService = accessControlService;
  }

  @Override
  public ResponseEntity<ApiDisplayHintList> queryHints(
      String underlayName, String entityName, ApiHintQuery body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(UNDERLAY, QUERY_COUNTS),
        ResourceId.forUnderlay(underlayName));
    Entity entity = underlayService.getEntity(underlayName, entityName);

    EntityHintRequest.Builder entityHintRequest = new EntityHintRequest.Builder().entity(entity);
    if (body != null && body.getRelatedEntity() != null) {
      // Return display hints for entity instances that are related to an instance of another entity
      // (e.g. numeric range for measurement_occurrence.value_numeric, computed across
      // measurement_occurrence instances that are related to measurement=BodyHeight).
      Entity relatedEntity =
          underlayService.getEntity(underlayName, body.getRelatedEntity().getName());
      entityHintRequest
          .relatedEntity(relatedEntity)
          .relatedEntityId(FromApiUtils.fromApiObject(body.getRelatedEntity().getId()))
          .entityGroup(
              FromApiUtils.getRelationship(
                      entity.getUnderlay().getEntityGroups().values(), entity, relatedEntity)
                  .getEntityGroup());
    } // else {} Return display hints computed across all entity instances (e.g. enum values for
    // person.gender).
    EntityHintResult entityHintResult = underlayService.listEntityHints(entityHintRequest.build());
    return ResponseEntity.ok(toApiObject(entityHintResult));
  }

  private ApiDisplayHintList toApiObject(EntityHintResult entityHintResult) {
    return new ApiDisplayHintList()
        .sql(SqlFormatter.format(entityHintResult.getSql()))
        .displayHints(
            entityHintResult.getHintMap().entrySet().stream()
                .map(
                    attrHint -> {
                      Attribute attr = attrHint.getKey();
                      DisplayHint hint = attrHint.getValue();
                      return new ApiDisplayHint()
                          .attribute(ToApiUtils.toApiObject(attr))
                          .displayHint(hint == null ? null : toApiObject(hint));
                    })
                .collect(Collectors.toList()));
  }

  private ApiDisplayHintDisplayHint toApiObject(DisplayHint displayHint) {
    switch (displayHint.getType()) {
      case ENUM:
        EnumVals enumVals = (EnumVals) displayHint;
        return new ApiDisplayHintDisplayHint()
            .enumHint(
                new ApiDisplayHintEnum()
                    .enumHintValues(
                        enumVals.getEnumValsList().stream()
                            .map(
                                ev ->
                                    new ApiDisplayHintEnumEnumHintValues()
                                        .enumVal(ToApiUtils.toApiObject(ev.getValueDisplay()))
                                        .count(Math.toIntExact(ev.getCount())))
                            .collect(Collectors.toList())));
      case RANGE:
        NumericRange numericRange = (NumericRange) displayHint;
        return new ApiDisplayHintDisplayHint()
            .numericRangeHint(
                new ApiDisplayHintNumericRange()
                    .min(numericRange.getMinVal())
                    .max(numericRange.getMaxVal()));
      default:
        throw new SystemException("Unknown display hint type: " + displayHint.getType());
    }
  }
}
