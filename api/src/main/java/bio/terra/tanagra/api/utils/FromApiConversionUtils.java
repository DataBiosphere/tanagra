package bio.terra.tanagra.api.utils;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.generated.model.ApiAttributeFilterV2;
import bio.terra.tanagra.generated.model.ApiLiteralV2;
import bio.terra.tanagra.generated.model.ApiQueryV2IncludeHierarchyFields;
import bio.terra.tanagra.generated.model.ApiTextFilterV2;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.FunctionFilterVariable;
import bio.terra.tanagra.underlay.HierarchyField;
import bio.terra.tanagra.underlay.Literal;
import bio.terra.tanagra.underlay.hierarchyfield.IsMember;
import bio.terra.tanagra.underlay.hierarchyfield.IsRoot;
import bio.terra.tanagra.underlay.hierarchyfield.NumChildren;
import bio.terra.tanagra.underlay.hierarchyfield.Path;

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

  public static BinaryFilterVariable.BinaryOperator fromApiObject(
      ApiAttributeFilterV2.OperatorEnum apiOperator) {
    return BinaryFilterVariable.BinaryOperator.valueOf(apiOperator.name());
  }

  public static FunctionFilterVariable.FunctionTemplate fromApiObject(
      ApiTextFilterV2.MatchTypeEnum apiMatchType) {
    switch (apiMatchType) {
      case EXACT_MATCH:
        return FunctionFilterVariable.FunctionTemplate.TEXT_EXACT_MATCH;
      case FUZZY_MATCH:
        return FunctionFilterVariable.FunctionTemplate.TEXT_FUZZY_MATCH;
      default:
        throw new SystemException("Unknown API text match type: " + apiMatchType.name());
    }
  }

  public static HierarchyField fromApiObject(
      String hierarchyName, ApiQueryV2IncludeHierarchyFields.FieldsEnum apiHierarchyField) {
    switch (apiHierarchyField) {
      case IS_MEMBER:
        return new IsMember(hierarchyName);
      case IS_ROOT:
        return new IsRoot(hierarchyName);
      case PATH:
        return new Path(hierarchyName);
      case NUM_CHILDREN:
        return new NumChildren(hierarchyName);
      default:
        throw new SystemException("Unknown API hierarchy field: " + apiHierarchyField);
    }
  }
}
