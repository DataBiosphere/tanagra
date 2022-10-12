package bio.terra.tanagra.api.utils;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.generated.model.ApiAttributeFilterV2;
import bio.terra.tanagra.generated.model.ApiLiteralV2;
import bio.terra.tanagra.underlay.Literal;
import bio.terra.tanagra.underlay.TableFilter;

public final class FromApiConversionUtils {
  private FromApiConversionUtils() {}

  public static Literal fromApiObject(ApiLiteralV2 apiLiteral) {
    switch (apiLiteral.getDataType()) {
      case INT64:
        return new Literal(apiLiteral.getValueUnion().getInt64Val());
      case STRING:
        return new Literal(apiLiteral.getValueUnion().getStringVal());
      case BOOLEAN:
        return new Literal(apiLiteral.getValueUnion().isBoolVal());
      default:
        throw new SystemException("Unknown API data type: " + apiLiteral.getDataType());
    }
  }

  public static TableFilter.BinaryOperator fromApiObject(
      ApiAttributeFilterV2.OperatorEnum apiOperator) {
    return TableFilter.BinaryOperator.valueOf(apiOperator.name());
  }
}
