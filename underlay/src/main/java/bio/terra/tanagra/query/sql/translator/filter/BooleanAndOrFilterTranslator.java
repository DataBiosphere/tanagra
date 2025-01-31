package bio.terra.tanagra.query.sql.translator.filter;

import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.translator.ApiFilterTranslator;
import bio.terra.tanagra.query.sql.translator.ApiTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BooleanAndOrFilterTranslator extends ApiFilterTranslator {
  private final BooleanAndOrFilter booleanAndOrFilter;
  private final List<ApiFilterTranslator> subFilterTranslators;

  public BooleanAndOrFilterTranslator(
      ApiTranslator apiTranslator,
      BooleanAndOrFilter booleanAndOrFilter,
      Map<Attribute, SqlField> attributeSwapFields) {
    super(apiTranslator, attributeSwapFields);
    this.booleanAndOrFilter = booleanAndOrFilter;

    // Get a single merged translator if the sub-filters are mergeable: must be of the same type
    // Additional checks may be needed for individual sub-filter types
    Optional<ApiFilterTranslator> mergedTranslator =
        EntityFilter.areSameFilterType(booleanAndOrFilter.getSubFilters())
            ? apiTranslator.mergedTranslator(
                booleanAndOrFilter.getSubFilters(),
                booleanAndOrFilter.getOperator(),
                attributeSwapFields)
            : Optional.empty();

    this.subFilterTranslators =
        mergedTranslator
            .map(List::of)
            .orElseGet(
                () ->
                    booleanAndOrFilter.getSubFilters().stream()
                        .map(filter -> apiTranslator.translator(filter, attributeSwapFields))
                        .toList());
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias) {
    List<String> subFilterSqls =
        subFilterTranslators.stream()
            .map(subFilterTranslator -> subFilterTranslator.buildSql(sqlParams, tableAlias))
            .toList();
    return apiTranslator.booleanAndOrFilterSql(
        booleanAndOrFilter.getOperator(), subFilterSqls.toArray(new String[0]));
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return subFilterTranslators.parallelStream()
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
