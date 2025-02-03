package bio.terra.tanagra.query.bigquery.translator.filter;

import bio.terra.tanagra.api.filter.BooleanAndOrFilter.LogicalOperator;
import bio.terra.tanagra.api.filter.HierarchyIsMemberFilter;
import bio.terra.tanagra.api.shared.UnaryOperator;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.translator.ApiFilterTranslator;
import bio.terra.tanagra.query.sql.translator.ApiTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BQHierarchyIsMemberFilterTranslator extends ApiFilterTranslator {
  private final HierarchyIsMemberFilter hierarchyIsMemberFilter;

  public BQHierarchyIsMemberFilterTranslator(
      ApiTranslator apiTranslator,
      HierarchyIsMemberFilter hierarchyIsMemberFilter,
      Map<Attribute, SqlField> attributeSwapFields) {
    super(apiTranslator, attributeSwapFields);
    this.hierarchyIsMemberFilter = hierarchyIsMemberFilter;
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias) {
    ITEntityMain indexTable =
        hierarchyIsMemberFilter
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(hierarchyIsMemberFilter.getEntity().getName());

    // IS_MEMBER means path IS NOT NULL.
    SqlField pathField =
        indexTable.getHierarchyPathField(hierarchyIsMemberFilter.getHierarchy().getName());
    return apiTranslator.unaryFilterSql(
        pathField, UnaryOperator.IS_NOT_NULL, tableAlias, sqlParams);
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return false;
  }

  public static Optional<ApiFilterTranslator> mergedTranslator(
      ApiTranslator apiTranslator,
      List<HierarchyIsMemberFilter> hierarchyIsMemberFilters,
      LogicalOperator logicalOperator,
      Map<Attribute, SqlField> attributeSwapFields) {
    // hierarchy must be the same
    return hierarchyIsMemberFilters.stream()
                .map(HierarchyIsMemberFilter::getHierarchy)
                .distinct()
                .count()
            == 1
        ? Optional.of(
            new BQHierarchyIsMemberFilterTranslator(
                apiTranslator, hierarchyIsMemberFilters.get(0), attributeSwapFields))
        : Optional.empty();
  }
}
