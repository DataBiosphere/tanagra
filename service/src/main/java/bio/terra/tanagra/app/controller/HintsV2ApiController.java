package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.QUERY_COUNTS;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.UNDERLAY;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.generated.controller.HintsV2Api;
import bio.terra.tanagra.generated.model.ApiDisplayHintEnumV2;
import bio.terra.tanagra.generated.model.ApiDisplayHintEnumV2EnumHintValues;
import bio.terra.tanagra.generated.model.ApiDisplayHintListV2;
import bio.terra.tanagra.generated.model.ApiDisplayHintNumericRangeV2;
import bio.terra.tanagra.generated.model.ApiDisplayHintV2;
import bio.terra.tanagra.generated.model.ApiDisplayHintV2DisplayHint;
import bio.terra.tanagra.generated.model.ApiHintQueryV2;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.QueryRequest;
import bio.terra.tanagra.service.AccessControlService;
import bio.terra.tanagra.service.FromApiConversionService;
import bio.terra.tanagra.service.ToApiConversionUtils;
import bio.terra.tanagra.service.UnderlaysService;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.instances.QuerysService;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.DisplayHint;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.displayhint.EnumVals;
import bio.terra.tanagra.underlay.displayhint.NumericRange;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class HintsV2ApiController implements HintsV2Api {
  private final UnderlaysService underlaysService;
  private final QuerysService querysService;
  private final AccessControlService accessControlService;

  @Autowired
  public HintsV2ApiController(
      UnderlaysService underlaysService,
      QuerysService querysService,
      AccessControlService accessControlService) {
    this.underlaysService = underlaysService;
    this.querysService = querysService;
    this.accessControlService = accessControlService;
  }

  @Override
  public ResponseEntity<ApiDisplayHintListV2> queryHints(
      String underlayName, String entityName, ApiHintQueryV2 body) {
    accessControlService.throwIfUnauthorized(
        null, QUERY_COUNTS, UNDERLAY, new ResourceId(underlayName));
    Entity entity = underlaysService.getEntity(underlayName, entityName);

    if (body == null || body.getRelatedEntity() == null) {
      // Return display hints computed across all entity instances (e.g. enum values for
      // person.gender).
      Map<String, DisplayHint> displayHints = new HashMap<>();
      entity.getAttributes().stream()
          .forEach(
              attr -> {
                if (attr.getDisplayHint() != null) {
                  displayHints.put(attr.getName(), attr.getDisplayHint());
                }
              });
      // Currently, these display hints are stored in the underlay config files, so no SQL query is
      // necessary to look them up.
      return ResponseEntity.ok(toApiObject(entity, displayHints, null));
    } else {
      // Return display hints for entity instances that are related to an instance of another entity
      // (e.g. numeric range for measurement_occurrence.value_numeric, computed across
      // measurement_occurrence instances that are related to measurement=BodyHeight).
      Underlay underlay = underlaysService.getUnderlay(underlayName);
      Entity relatedEntity =
          underlaysService.getEntity(underlayName, body.getRelatedEntity().getName());
      Literal relatedEntityId =
          FromApiConversionService.fromApiObject(body.getRelatedEntity().getId());
      QueryRequest queryRequest =
          querysService.buildDisplayHintsQuery(
              underlay, entity, Underlay.MappingType.INDEX, relatedEntity, relatedEntityId);
      Map<String, DisplayHint> displayHints =
          querysService.runDisplayHintsQuery(
              entity.getMapping(Underlay.MappingType.INDEX).getTablePointer().getDataPointer(),
              queryRequest);
      return ResponseEntity.ok(toApiObject(entity, displayHints, queryRequest.getSql()));
    }
  }

  private ApiDisplayHintListV2 toApiObject(
      Entity entity, Map<String, DisplayHint> displayHints, String sql) {
    return new ApiDisplayHintListV2()
        .sql(sql)
        .displayHints(
            displayHints.entrySet().stream()
                .map(
                    attrHint -> {
                      Attribute attr = entity.getAttribute(attrHint.getKey());
                      DisplayHint hint = attrHint.getValue();
                      return new ApiDisplayHintV2()
                          .attribute(ToApiConversionUtils.toApiObject(attr))
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
                                        .enumVal(
                                            ToApiConversionUtils.toApiObject(ev.getValueDisplay()))
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
