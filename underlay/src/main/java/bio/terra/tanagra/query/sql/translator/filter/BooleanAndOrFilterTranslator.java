package bio.terra.tanagra.query.sql.translator.filter;

import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.translator.ApiFilterTranslator;
import bio.terra.tanagra.query.sql.translator.ApiTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import java.util.List;
import java.util.stream.Collectors;

public class BooleanAndOrFilterTranslator extends ApiFilterTranslator {
  private final BooleanAndOrFilter booleanAndOrFilter;
  private final List<ApiFilterTranslator> subFilterTranslators;

  public BooleanAndOrFilterTranslator(
      ApiTranslator apiTranslator, BooleanAndOrFilter booleanAndOrFilter) {
    super(apiTranslator);
    this.booleanAndOrFilter = booleanAndOrFilter;
    this.subFilterTranslators =
        booleanAndOrFilter.getSubFilters().stream()
            .map(apiTranslator::translator)
            .collect(Collectors.toList());
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias) {
    List<String> subFilterSqls =
        subFilterTranslators.stream()
            .map(subFilterTranslator -> subFilterTranslator.buildSql(sqlParams, tableAlias))
            .collect(Collectors.toList());
    return apiTranslator.booleanAndOrFilterSql(
        booleanAndOrFilter.getOperator(), subFilterSqls.toArray(new String[0]));
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return subFilterTranslators
        .parallelStream()
        .filter(subFilterTranslator -> !subFilterTranslator.isFilterOnAttribute(attribute))
        .findAny()
        .isEmpty();
  }

  @Override
  public ApiFilterTranslator swapAttributeField(Attribute attribute, SqlField swappedField) {
    subFilterTranslators.forEach(
        subFilterTranslator -> subFilterTranslator.swapAttributeField(attribute, swappedField));
    return this;
  }
}
