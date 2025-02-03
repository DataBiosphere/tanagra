package bio.terra.tanagra.query.bigquery.translator.filter;

import bio.terra.tanagra.api.filter.BooleanAndOrFilter.LogicalOperator;
import bio.terra.tanagra.api.filter.HierarchyHasAncestorFilter;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.NaryOperator;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.translator.ApiFilterTranslator;
import bio.terra.tanagra.query.sql.translator.ApiTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.indextable.ITHierarchyAncestorDescendant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BQHierarchyHasAncestorFilterTranslator extends ApiFilterTranslator {
  private final List<HierarchyHasAncestorFilter> hierarchyHasAncestorFilters;

  public BQHierarchyHasAncestorFilterTranslator(
      ApiTranslator apiTranslator,
      HierarchyHasAncestorFilter hierarchyHasAncestorFilter,
      Map<Attribute, SqlField> attributeSwapFields) {
    super(apiTranslator, attributeSwapFields);
    this.hierarchyHasAncestorFilters = List.of(hierarchyHasAncestorFilter);
  }

  public BQHierarchyHasAncestorFilterTranslator(
      ApiTranslator apiTranslator,
      List<HierarchyHasAncestorFilter> hierarchyHasAncestorFilters,
      Map<Attribute, SqlField> attributeSwapFields) {
    super(apiTranslator, attributeSwapFields);
    this.hierarchyHasAncestorFilters = hierarchyHasAncestorFilters;
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias) {
    //  entity.id IN (SELECT ancestorId UNION ALL SELECT descendant FROM ancestorDescendantTable
    // FILTER ON ancestorId)

    // List is used only when they are mergeable (private constructor). See mergedTranslator
    HierarchyHasAncestorFilter singleFilter = hierarchyHasAncestorFilters.get(0);
    List<Literal> ancestorIds =
        hierarchyHasAncestorFilters.stream()
            .flatMap(filter -> filter.getAncestorIds().stream())
            .toList();

    ITHierarchyAncestorDescendant ancestorDescendantTable =
        singleFilter
            .getUnderlay()
            .getIndexSchema()
            .getHierarchyAncestorDescendant(
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

    // FILTER ON ancestorId = [WHERE ancestor IN (ancestorIds)] or [WHERE ancestor = ancestorId]
    String ancestorIdFilterSql =
        ancestorIds.size() > 1
            ? apiTranslator.naryFilterSql(
                ancestorDescendantTable.getAncestorField(),
                NaryOperator.IN,
                ancestorIds,
                null,
                sqlParams)
            : apiTranslator.binaryFilterSql(
                ancestorDescendantTable.getAncestorField(),
                BinaryOperator.EQUALS,
                ancestorIds.get(0),
                null,
                sqlParams);

    return apiTranslator.inSelectFilterSql(
        idField,
        tableAlias,
        ancestorDescendantTable.getDescendantField(),
        ancestorDescendantTable.getTablePointer(),
        ancestorIdFilterSql,
        null,
        false,
        sqlParams,
        ancestorIds.toArray(new Literal[0]));
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return attribute.isId();
  }

  public static Optional<ApiFilterTranslator> mergedTranslator(
      ApiTranslator apiTranslator,
      List<HierarchyHasAncestorFilter> hierarchyHasAncestorFilters,
      LogicalOperator logicalOperator,
      Map<Attribute, SqlField> attributeSwapFields) {
    // hierarchy must be the same
    return hierarchyHasAncestorFilters.stream()
                .map(HierarchyHasAncestorFilter::getHierarchy)
                .distinct()
                .count()
            == 1
        ? Optional.of(
            new BQHierarchyHasAncestorFilterTranslator(
                apiTranslator, hierarchyHasAncestorFilters, attributeSwapFields))
        : Optional.empty();
  }
}
