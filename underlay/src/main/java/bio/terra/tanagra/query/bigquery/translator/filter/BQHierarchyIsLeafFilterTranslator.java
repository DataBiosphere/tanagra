package bio.terra.tanagra.query.bigquery.translator.filter;

import bio.terra.tanagra.api.filter.BooleanAndOrFilter.LogicalOperator;
import bio.terra.tanagra.api.filter.HierarchyIsLeafFilter;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.translator.ApiFilterTranslator;
import bio.terra.tanagra.query.sql.translator.ApiTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BQHierarchyIsLeafFilterTranslator extends ApiFilterTranslator {
  private final HierarchyIsLeafFilter hierarchyIsLeafFilter;

  public BQHierarchyIsLeafFilterTranslator(
      ApiTranslator apiTranslator,
      HierarchyIsLeafFilter hierarchyIsLeafFilter,
      Map<Attribute, SqlField> attributeSwapFields) {
    super(apiTranslator, attributeSwapFields);
    this.hierarchyIsLeafFilter = hierarchyIsLeafFilter;
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias) {
    ITEntityMain indexTable =
        hierarchyIsLeafFilter
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(hierarchyIsLeafFilter.getEntity().getName());

    // IS_LEAF means num_children=0.
    SqlField numChildrenField =
        indexTable.getHierarchyNumChildrenField(hierarchyIsLeafFilter.getHierarchy().getName());
    return apiTranslator.binaryFilterSql(
        numChildrenField, BinaryOperator.EQUALS, Literal.forInt64(0L), tableAlias, sqlParams);
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return false;
  }

  public static Optional<ApiFilterTranslator> mergedTranslator(
      ApiTranslator apiTranslator,
      List<HierarchyIsLeafFilter> hierarchyIsLeafFilters,
      LogicalOperator logicalOperator,
      Map<Attribute, SqlField> attributeSwapFields) {
    // LogicalOperator.AND is not supported for hierarchy filters
    if (logicalOperator == LogicalOperator.AND) {
      return Optional.empty();
    }

    // hierarchy must be the same
    return hierarchyIsLeafFilters.stream()
                .map(HierarchyIsLeafFilter::getHierarchy)
                .distinct()
                .count()
            == 1
        ? Optional.of(
            new BQHierarchyIsLeafFilterTranslator(
                apiTranslator, hierarchyIsLeafFilters.get(0), attributeSwapFields))
        : Optional.empty();
  }
}
