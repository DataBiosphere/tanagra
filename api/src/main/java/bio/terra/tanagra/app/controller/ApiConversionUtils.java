package bio.terra.tanagra.app.controller;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.generated.model.ApiLiteralV2;
import bio.terra.tanagra.generated.model.ApiValueDisplayV2;
import bio.terra.tanagra.underlay.Literal;
import bio.terra.tanagra.underlay.ValueDisplay;

public final class ApiConversionUtils {
  private ApiConversionUtils() {}

  public static ApiValueDisplayV2 toApiObject(ValueDisplay valueDisplay) {
    return new ApiValueDisplayV2()
        .value(toApiObject(valueDisplay.getValue()))
        .display(valueDisplay.getDisplay());
  }

  public static ApiLiteralV2 toApiObject(Literal literal) {
    switch (literal.getDataType()) {
      case INT64:
        return new ApiLiteralV2().int64Val(literal.getInt64Val());
      case STRING:
        return new ApiLiteralV2().stringVal(literal.getStringVal());
      case BOOLEAN:
        return new ApiLiteralV2().boolVal(literal.getBooleanVal());
      default:
        throw new SystemException("Unknown literal data type: " + literal.getDataType());
    }
  }
}
