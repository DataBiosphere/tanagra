package bio.terra.tanagra.api.utils;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.generated.model.ApiDataTypeV2;
import bio.terra.tanagra.generated.model.ApiLiteralV2;
import bio.terra.tanagra.generated.model.ApiLiteralV2ValueUnion;
import bio.terra.tanagra.generated.model.ApiValueDisplayV2;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.underlay.ValueDisplay;

public final class ToApiConversionUtils {
  private ToApiConversionUtils() {}

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
