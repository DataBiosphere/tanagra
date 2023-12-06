package bio.terra.tanagra.service.filterbuilder.impl;

import bio.terra.tanagra.generated.model.ApiAttributeFilter;
import bio.terra.tanagra.generated.model.ApiBinaryOperator;
import bio.terra.tanagra.generated.model.ApiBooleanLogicFilter;
import bio.terra.tanagra.generated.model.ApiFilter;
import bio.terra.tanagra.generated.model.ApiFilterFilterUnion;
import bio.terra.tanagra.generated.model.ApiLiteral;
import bio.terra.tanagra.generated.model.ApiLiteralList;
import bio.terra.tanagra.service.filterbuilder.FilterBuilder;
import bio.terra.tanagra.service.filterbuilder.FilterBuilderInput;
import java.util.List;
import java.util.stream.Collectors;

public class Attribute extends FilterBuilder {
  // Selection data keys.
  private static final String ATTRIBUTE = "attribute";
  private static final String VALUES = "values";
  private static final String RANGE_MIN = "range_min";
  private static final String RANGE_MAX = "range_max";

  @Override
  public ApiFilter buildFilter(FilterBuilderInput input) {
    String attribute = input.getPluginDataSingleString(ATTRIBUTE);
    ApiLiteral rangeMin = input.getPluginDataSingleLiteral(RANGE_MIN);
    ApiLiteral rangeMax = input.getPluginDataSingleLiteral(RANGE_MAX);
    ApiLiteralList values = input.getPluginData(VALUES);

    if (rangeMin != null) {
      ApiFilter rangeMinFilter =
          new ApiFilter()
              .filterType(ApiFilter.FilterTypeEnum.ATTRIBUTE)
              .filterUnion(
                  new ApiFilterFilterUnion()
                      .attributeFilter(
                          new ApiAttributeFilter()
                              .attribute(attribute)
                              .operator(ApiBinaryOperator.GREATER_THAN_OR_EQUAL)
                              .value(rangeMin)));
      ApiFilter rangeMaxFilter =
          new ApiFilter()
              .filterType(ApiFilter.FilterTypeEnum.ATTRIBUTE)
              .filterUnion(
                  new ApiFilterFilterUnion()
                      .attributeFilter(
                          new ApiAttributeFilter()
                              .attribute(attribute)
                              .operator(ApiBinaryOperator.LESS_THAN_OR_EQUAL)
                              .value(rangeMax)));
      return new ApiFilter()
          .filterType(ApiFilter.FilterTypeEnum.BOOLEAN_LOGIC)
          .filterUnion(
              new ApiFilterFilterUnion()
                  .booleanLogicFilter(
                      new ApiBooleanLogicFilter()
                          .operator(ApiBooleanLogicFilter.OperatorEnum.AND)
                          .addSubfiltersItem(rangeMinFilter)
                          .addSubfiltersItem(rangeMaxFilter)));
    } else {
      List<ApiFilter> subFilters =
          values.stream()
              .map(
                  apiLiteral ->
                      new ApiFilter()
                          .filterType(ApiFilter.FilterTypeEnum.ATTRIBUTE)
                          .filterUnion(
                              new ApiFilterFilterUnion()
                                  .attributeFilter(
                                      new ApiAttributeFilter()
                                          .attribute(attribute)
                                          .operator(ApiBinaryOperator.EQUALS)
                                          .value(values.get(0)))))
              .collect(Collectors.toList());
      if (values.size() == 1) {
        return subFilters.get(0);
      } else {
        return new ApiFilter()
            .filterType(ApiFilter.FilterTypeEnum.BOOLEAN_LOGIC)
            .filterUnion(
                new ApiFilterFilterUnion()
                    .booleanLogicFilter(
                        new ApiBooleanLogicFilter()
                            .operator(ApiBooleanLogicFilter.OperatorEnum.OR)
                            .subfilters(subFilters)));
      }
    }
  }
}
