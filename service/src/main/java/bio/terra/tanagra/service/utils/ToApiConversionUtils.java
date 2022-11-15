package bio.terra.tanagra.service.utils;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.generated.model.ApiAttributeV2;
import bio.terra.tanagra.generated.model.ApiDataTypeV2;
import bio.terra.tanagra.generated.model.ApiLiteralV2;
import bio.terra.tanagra.generated.model.ApiLiteralV2ValueUnion;
import bio.terra.tanagra.generated.model.ApiValueDisplayV2;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.ValueDisplay;

public final class ToApiConversionUtils {
  private ToApiConversionUtils() {}

  public static ApiAttributeV2 toApiObject(Attribute attribute) {
    return new ApiAttributeV2()
        .name(attribute.getName())
        .type(ApiAttributeV2.TypeEnum.fromValue(attribute.getType().name()))
        .dataType(ApiDataTypeV2.fromValue(attribute.getDataType().name()));
  }

  public static ApiValueDisplayV2 toApiObject(ValueDisplay valueDisplay) {
    return new ApiValueDisplayV2()
        .value(toApiObject(valueDisplay.getValue()))
        .display(valueDisplay.getDisplay());
  }

  public static ApiLiteralV2 toApiObject(Literal literal) {
    ApiLiteralV2 apiLiteral =
        new ApiLiteralV2().dataType(ApiDataTypeV2.fromValue(literal.getDataType().name()));
    switch (literal.getDataType()) {
      case INT64:
        return apiLiteral.valueUnion(new ApiLiteralV2ValueUnion().int64Val(literal.getInt64Val()));
      case STRING:
        return apiLiteral.valueUnion(
            new ApiLiteralV2ValueUnion().stringVal(literal.getStringVal()));
      case BOOLEAN:
        return apiLiteral.valueUnion(new ApiLiteralV2ValueUnion().boolVal(literal.getBooleanVal()));
      case DATE:
        return apiLiteral.valueUnion(
            new ApiLiteralV2ValueUnion().dateVal(literal.getDateValAsString()));
      default:
        throw new SystemException("Unknown literal data type: " + literal.getDataType());
    }
  }
}
