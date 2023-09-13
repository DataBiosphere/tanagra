package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.QUERY_COUNTS;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.UNDERLAY;

import bio.terra.tanagra.api.query.EntityHintRequest;
import bio.terra.tanagra.api.query.EntityHintResult;
import bio.terra.tanagra.app.authentication.SpringAuthentication;
import bio.terra.tanagra.app.controller.objmapping.FromApiUtils;
import bio.terra.tanagra.app.controller.objmapping.ToApiUtils;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.generated.controller.HintsV2Api;
import bio.terra.tanagra.generated.model.ApiDisplayHintEnumV2;
import bio.terra.tanagra.generated.model.ApiDisplayHintEnumV2EnumHintValues;
import bio.terra.tanagra.generated.model.ApiDisplayHintListV2;
import bio.terra.tanagra.generated.model.ApiDisplayHintNumericRangeV2;
import bio.terra.tanagra.generated.model.ApiDisplayHintV2;
import bio.terra.tanagra.generated.model.ApiDisplayHintV2DisplayHint;
import bio.terra.tanagra.generated.model.ApiHintQueryV2;
import bio.terra.tanagra.service.accesscontrol.AccessControlService;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.query.QueryRunner;
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
public class HintsV2ApiController implements HintsV2Api {
  private final UnderlayService underlayService;
  private final AccessControlService accessControlService;

  @Autowired
  public HintsV2ApiController(
      UnderlayService underlayService, AccessControlService accessControlService) {
    this.underlayService = underlayService;
    this.accessControlService = accessControlService;
  }

  @Override
  public ResponseEntity<ApiDisplayHintListV2> queryHints(
      String underlayName, String entityName, ApiHintQueryV2 body) {
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
              underlayService
                  .getRelationship(
                      entity.getUnderlay().getEntityGroups().values(), entity, relatedEntity)
                  .getEntityGroup());
    } // else {} Return display hints computed across all entity instances (e.g. enum values for
    // person.gender).
    EntityHintResult entityHintResult = QueryRunner.listEntityHints(entityHintRequest.build());
    return ResponseEntity.ok(toApiObject(entityHintResult));
  }

  private ApiDisplayHintListV2 toApiObject(EntityHintResult entityHintResult) {
    return new ApiDisplayHintListV2()
        .sql(SqlFormatter.format(entityHintResult.getSql()))
        .displayHints(
            entityHintResult.getHintMap().entrySet().stream()
                .map(
                    attrHint -> {
                      Attribute attr = attrHint.getKey();
                      DisplayHint hint = attrHint.getValue();
                      return new ApiDisplayHintV2()
                          .attribute(ToApiUtils.toApiObject(attr))
                          .displayHint(hint == null ? null : toApiObject(hint));
                    })
                .collect(Collectors.toList()));
  }

  private ApiDisplayHintV2DisplayHint toApiObject(DisplayHint displayHint) {
    switch (displayHint.getType()) {
      case ENUM:
        EnumVals enumVals = (EnumVals) displayHint;
        return new ApiDisplayHintV2DisplayHint()
            .enumHint(
                new ApiDisplayHintEnumV2()
                    .enumHintValues(
                        enumVals.getEnumValsList().stream()
                            .map(
                                ev ->
                                    new ApiDisplayHintEnumV2EnumHintValues()
                                        .enumVal(ToApiUtils.toApiObject(ev.getValueDisplay()))
                                        .count(Math.toIntExact(ev.getCount())))
                            .collect(Collectors.toList())));
      case RANGE:
        NumericRange numericRange = (NumericRange) displayHint;
        return new ApiDisplayHintV2DisplayHint()
            .numericRangeHint(
                new ApiDisplayHintNumericRangeV2()
                    .min(numericRange.getMinVal())
                    .max(numericRange.getMaxVal()));
      default:
        throw new SystemException("Unknown display hint type: " + displayHint.getType());
    }
  }
}
