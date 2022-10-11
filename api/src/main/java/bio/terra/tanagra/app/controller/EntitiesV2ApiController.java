package bio.terra.tanagra.app.controller;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.generated.controller.EntitiesV2Api;
import bio.terra.tanagra.generated.model.ApiAttributeV2;
import bio.terra.tanagra.generated.model.ApiAttributeV2DisplayHint;
import bio.terra.tanagra.generated.model.ApiDisplayHintEnumV2;
import bio.terra.tanagra.generated.model.ApiDisplayHintNumericRangeV2;
import bio.terra.tanagra.generated.model.ApiEntityListV2;
import bio.terra.tanagra.generated.model.ApiEntityV2;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.DisplayHint;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.UnderlaysService;
import bio.terra.tanagra.underlay.displayhint.EnumVals;
import bio.terra.tanagra.underlay.displayhint.NumericRange;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
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
    return ResponseEntity.ok(toApiObject(underlaysService.getEntity(underlayName, entityName)));
  }

  private ApiEntityV2 toApiObject(Entity entity) {
    return new ApiEntityV2()
        .name(entity.getName())
        .idAttribute(entity.getIdAttribute().getName())
        .attributes(
            entity.getAttributes().stream().map(a -> toApiObject(a)).collect(Collectors.toList()));
  }

  private ApiAttributeV2 toApiObject(Attribute attribute) {
    return new ApiAttributeV2()
        .name(attribute.getName())
        .type(ApiAttributeV2.TypeEnum.fromValue(attribute.getType().name()))
        .dataType(ApiAttributeV2.DataTypeEnum.fromValue(attribute.getDataType().name()))
        .displayHint(toApiObject(attribute.getDisplayHint()));
  }

  private ApiAttributeV2DisplayHint toApiObject(@Nullable DisplayHint displayHint) {
    if (displayHint == null) {
      return null;
    }

    switch (displayHint.getType()) {
      case ENUM:
        EnumVals enumVals = (EnumVals) displayHint;
        return new ApiAttributeV2DisplayHint()
            .enumHint(
                new ApiDisplayHintEnumV2()
                    .enumHintValues(
                        enumVals.getValueDisplays().stream()
                            .map(vd -> ApiConversionUtils.toApiObject(vd))
                            .collect(Collectors.toList())));
      case RANGE:
        NumericRange numericRange = (NumericRange) displayHint;
        return new ApiAttributeV2DisplayHint()
            .numericRangeHint(
                new ApiDisplayHintNumericRangeV2()
                    .min(numericRange.getMinVal())
                    .max(numericRange.getMaxVal()));
      default:
        throw new SystemException("Unknown display hint type: " + displayHint.getType());
    }
  }
}
