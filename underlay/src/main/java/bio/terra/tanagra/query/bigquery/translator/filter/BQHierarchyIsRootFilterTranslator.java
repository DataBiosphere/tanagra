package bio.terra.tanagra.query.bigquery.translator.filter;

import bio.terra.tanagra.api.filter.BooleanAndOrFilter.LogicalOperator;
import bio.terra.tanagra.api.filter.HierarchyIsRootFilter;
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

public class BQHierarchyIsRootFilterTranslator extends ApiFilterTranslator {
  private final HierarchyIsRootFilter hierarchyIsRootFilter;

  public BQHierarchyIsRootFilterTranslator(
      ApiTranslator apiTranslator,
      HierarchyIsRootFilter hierarchyIsRootFilter,
      Map<Attribute, SqlField> attributeSwapFields) {
    super(apiTranslator, attributeSwapFields);
    this.hierarchyIsRootFilter = hierarchyIsRootFilter;
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias) {
    ITEntityMain indexTable =
        hierarchyIsRootFilter
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(hierarchyIsRootFilter.getEntity().getName());

    // IS_ROOT means path=''.
    SqlField pathField =
        indexTable.getHierarchyPathField(hierarchyIsRootFilter.getHierarchy().getName());
    return apiTranslator.unaryFilterSql(
        pathField, UnaryOperator.IS_EMPTY_STRING, tableAlias, sqlParams);
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return false;
  }

  public static Optional<ApiFilterTranslator> mergedTranslator(
      ApiTranslator apiTranslator,
      List<HierarchyIsRootFilter> hierarchyIsRootFilters,
      LogicalOperator logicalOperator,
      Map<Attribute, SqlField> attributeSwapFields) {
    // LogicalOperator.AND is not supported for hierarchy filters
    if (logicalOperator == LogicalOperator.AND) {
      return Optional.empty();
    }

    // hierarchy must be the same
    return hierarchyIsRootFilters.stream()
                .map(HierarchyIsRootFilter::getHierarchy)
                .distinct()
                .count()
            == 1
        ? Optional.of(
            new BQHierarchyIsRootFilterTranslator(
                apiTranslator, hierarchyIsRootFilters.get(0), attributeSwapFields))
        : Optional.empty();
  }
}
