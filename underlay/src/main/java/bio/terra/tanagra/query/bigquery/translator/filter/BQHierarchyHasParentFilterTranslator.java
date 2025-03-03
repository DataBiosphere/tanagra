package bio.terra.tanagra.query.bigquery.translator.filter;

import bio.terra.tanagra.api.filter.BooleanAndOrFilter.LogicalOperator;
import bio.terra.tanagra.api.filter.HierarchyHasParentFilter;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.NaryOperator;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.translator.ApiFilterTranslator;
import bio.terra.tanagra.query.sql.translator.ApiTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.indextable.ITHierarchyChildParent;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BQHierarchyHasParentFilterTranslator extends ApiFilterTranslator {
  private final List<HierarchyHasParentFilter> hierarchyHasParentFilters;

  public BQHierarchyHasParentFilterTranslator(
      ApiTranslator apiTranslator,
      HierarchyHasParentFilter hierarchyHasParentFilter,
      Map<Attribute, SqlField> attributeSwapFields) {
    super(apiTranslator, attributeSwapFields);
    this.hierarchyHasParentFilters = List.of(hierarchyHasParentFilter);
  }

  public BQHierarchyHasParentFilterTranslator(
      ApiTranslator apiTranslator,
      List<HierarchyHasParentFilter> hierarchyHasParentFilters,
      Map<Attribute, SqlField> attributeSwapFields) {
    super(apiTranslator, attributeSwapFields);
    this.hierarchyHasParentFilters = hierarchyHasParentFilters;
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias) {
    //  entity.id IN (SELECT child FROM childParentTable FILTER ON parentId)

    // List is used only when they are mergeable (private constructor). See mergedTranslator
    HierarchyHasParentFilter singleFilter = hierarchyHasParentFilters.get(0);
    List<Literal> parentIds =
        hierarchyHasParentFilters.stream()
            .flatMap(filter -> filter.getParentIds().stream())
            .toList();

    ITHierarchyChildParent childParentIndexTable =
        singleFilter
            .getUnderlay()
            .getIndexSchema()
            .getHierarchyChildParent(
                singleFilter.getEntity().getName(), singleFilter.getHierarchy().getName());
    Attribute idAttribute = singleFilter.getEntity().getIdAttribute();
    SqlField idField =
        attributeSwapFields.containsKey(idAttribute)
            ? attributeSwapFields.get(idAttribute)
            : singleFilter
                .getUnderlay()
                .getIndexSchema()
                .getEntityMain(singleFilter.getEntity().getName())
                .getAttributeValueField(idAttribute.getName());

    // FILTER ON parentId = [WHERE parent IN (parentIds)] or [WHERE parent = parentId]
    String parentIdFilterSql =
        parentIds.size() > 1
            ? apiTranslator.naryFilterSql(
                childParentIndexTable.getParentField(), NaryOperator.IN, parentIds, null, sqlParams)
            : apiTranslator.binaryFilterSql(
                childParentIndexTable.getParentField(),
                BinaryOperator.EQUALS,
                parentIds.get(0),
                null,
                sqlParams);

    return apiTranslator.inSelectFilterSql(
        idField,
        tableAlias,
        childParentIndexTable.getChildField(),
        childParentIndexTable.getTablePointer(),
        parentIdFilterSql,
        null,
        false,
        sqlParams);
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return attribute.isId();
  }

  public static Optional<ApiFilterTranslator> mergedTranslator(
      ApiTranslator apiTranslator,
      List<HierarchyHasParentFilter> hierarchyHasParentFilters,
      LogicalOperator logicalOperator,
      Map<Attribute, SqlField> attributeSwapFields) {
    // LogicalOperator.AND is not supported for hierarchy filters
    if (logicalOperator == LogicalOperator.AND) {
      return Optional.empty();
    }

    // hierarchy must be the same
    return hierarchyHasParentFilters.stream()
                .map(HierarchyHasParentFilter::getHierarchy)
                .distinct()
                .count()
            == 1
        ? Optional.of(
            new BQHierarchyHasParentFilterTranslator(
                apiTranslator, hierarchyHasParentFilters, attributeSwapFields))
        : Optional.empty();
  }
}
