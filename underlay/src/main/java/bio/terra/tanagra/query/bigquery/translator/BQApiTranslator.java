package bio.terra.tanagra.query.bigquery.translator;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.field.CountDistinctField;
import bio.terra.tanagra.api.field.HierarchyIsMemberField;
import bio.terra.tanagra.api.field.HierarchyIsRootField;
import bio.terra.tanagra.api.field.HierarchyNumChildrenField;
import bio.terra.tanagra.api.field.HierarchyPathField;
import bio.terra.tanagra.api.field.RelatedEntityIdCountField;
import bio.terra.tanagra.api.filter.*;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter.LogicalOperator;
import bio.terra.tanagra.api.filter.TextSearchFilter.TextSearchOperator;
import bio.terra.tanagra.api.shared.*;
import bio.terra.tanagra.query.bigquery.translator.field.BQAttributeFieldTranslator;
import bio.terra.tanagra.query.bigquery.translator.field.BQCountDistinctFieldTranslator;
import bio.terra.tanagra.query.bigquery.translator.field.BQHierarchyIsMemberFieldTranslator;
import bio.terra.tanagra.query.bigquery.translator.field.BQHierarchyIsRootFieldTranslator;
import bio.terra.tanagra.query.bigquery.translator.field.BQHierarchyNumChildrenFieldTranslator;
import bio.terra.tanagra.query.bigquery.translator.field.BQHierarchyPathFieldTranslator;
import bio.terra.tanagra.query.bigquery.translator.field.BQRelatedEntityIdCountFieldTranslator;
import bio.terra.tanagra.query.bigquery.translator.filter.BQAttributeFilterTranslator;
import bio.terra.tanagra.query.bigquery.translator.filter.BQHierarchyHasAncestorFilterTranslator;
import bio.terra.tanagra.query.bigquery.translator.filter.BQHierarchyHasParentFilterTranslator;
import bio.terra.tanagra.query.bigquery.translator.filter.BQHierarchyIsLeafFilterTranslator;
import bio.terra.tanagra.query.bigquery.translator.filter.BQHierarchyIsMemberFilterTranslator;
import bio.terra.tanagra.query.bigquery.translator.filter.BQHierarchyIsRootFilterTranslator;
import bio.terra.tanagra.query.bigquery.translator.filter.BQPrimaryWithCriteriaFilterTranslator;
import bio.terra.tanagra.query.bigquery.translator.filter.BQRelationshipFilterTranslator;
import bio.terra.tanagra.query.bigquery.translator.filter.BQTemporalPrimaryFilterTranslator;
import bio.terra.tanagra.query.bigquery.translator.filter.BQTextSearchFilterTranslator;
import bio.terra.tanagra.query.sql.*;
import bio.terra.tanagra.query.sql.translator.ApiFieldTranslator;
import bio.terra.tanagra.query.sql.translator.ApiFilterTranslator;
import bio.terra.tanagra.query.sql.translator.ApiTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import jakarta.annotation.*;
import java.util.*;

public final class BQApiTranslator implements ApiTranslator {
  @Override
  public ApiFieldTranslator translator(AttributeField attributeField) {
    return new BQAttributeFieldTranslator(attributeField);
  }

  @Override
  public ApiFieldTranslator translator(CountDistinctField countDistinctField) {
    return new BQCountDistinctFieldTranslator(countDistinctField);
  }

  @Override
  public ApiFieldTranslator translator(HierarchyIsMemberField hierarchyIsMemberField) {
    return new BQHierarchyIsMemberFieldTranslator(hierarchyIsMemberField);
  }

  @Override
  public ApiFieldTranslator translator(HierarchyIsRootField hierarchyIsRootField) {
    return new BQHierarchyIsRootFieldTranslator(hierarchyIsRootField);
  }

  @Override
  public ApiFieldTranslator translator(HierarchyNumChildrenField hierarchyNumChildrenField) {
    return new BQHierarchyNumChildrenFieldTranslator(hierarchyNumChildrenField);
  }

  @Override
  public ApiFieldTranslator translator(HierarchyPathField hierarchyPathField) {
    return new BQHierarchyPathFieldTranslator(hierarchyPathField);
  }

  @Override
  public ApiFieldTranslator translator(RelatedEntityIdCountField relatedEntityIdCountField) {
    return new BQRelatedEntityIdCountFieldTranslator(relatedEntityIdCountField);
  }

  @Override
  public ApiFilterTranslator translator(
      AttributeFilter attributeFilter, Map<Attribute, SqlField> attributeSwapFields) {
    return new BQAttributeFilterTranslator(this, attributeFilter, attributeSwapFields);
  }

  @Override
  public Optional<ApiFilterTranslator> mergedTranslator(
      List<AttributeFilter> attributeFilters,
      LogicalOperator logicalOperator,
      Map<Attribute, SqlField> attributeSwapFields) {
    return BQAttributeFilterTranslator.canMergeTranslation(attributeFilters)
        ? Optional.of(
            new BQAttributeFilterTranslator(
                this, attributeFilters, logicalOperator, attributeSwapFields))
        : Optional.empty();
  }

  @Override
  public ApiFilterTranslator translator(
      HierarchyHasAncestorFilter hierarchyHasAncestorFilter,
      Map<Attribute, SqlField> attributeSwapFields) {
    return new BQHierarchyHasAncestorFilterTranslator(
        this, hierarchyHasAncestorFilter, attributeSwapFields);
  }

  @Override
  public ApiFilterTranslator translator(
      HierarchyHasParentFilter hierarchyHasParentFilter,
      Map<Attribute, SqlField> attributeSwapFields) {
    return new BQHierarchyHasParentFilterTranslator(
        this, hierarchyHasParentFilter, attributeSwapFields);
  }

  @Override
  public ApiFilterTranslator translator(
      HierarchyIsLeafFilter hierarchyIsLeafFilter, Map<Attribute, SqlField> attributeSwapFields) {
    return new BQHierarchyIsLeafFilterTranslator(this, hierarchyIsLeafFilter, attributeSwapFields);
  }

  @Override
  public ApiFilterTranslator translator(
      HierarchyIsMemberFilter hierarchyIsMemberFilter,
      Map<Attribute, SqlField> attributeSwapFields) {
    return new BQHierarchyIsMemberFilterTranslator(
        this, hierarchyIsMemberFilter, attributeSwapFields);
  }

  @Override
  public ApiFilterTranslator translator(
      HierarchyIsRootFilter hierarchyIsRootFilter, Map<Attribute, SqlField> attributeSwapFields) {
    return new BQHierarchyIsRootFilterTranslator(this, hierarchyIsRootFilter, attributeSwapFields);
  }

  @Override
  public ApiFilterTranslator translator(
      PrimaryWithCriteriaFilter primaryWithCriteriaFilter,
      Map<Attribute, SqlField> attributeSwapFields) {
    return new BQPrimaryWithCriteriaFilterTranslator(
        this, primaryWithCriteriaFilter, attributeSwapFields);
  }

  @Override
  public ApiFilterTranslator translator(
      RelationshipFilter relationshipFilter, Map<Attribute, SqlField> attributeSwapFields) {
    return new BQRelationshipFilterTranslator(this, relationshipFilter, attributeSwapFields);
  }

  @Override
  public ApiFilterTranslator translator(
      TextSearchFilter textSearchFilter, Map<Attribute, SqlField> attributeSwapFields) {
    return new BQTextSearchFilterTranslator(this, textSearchFilter, attributeSwapFields);
  }

  @Override
  public ApiFilterTranslator translator(
      TemporalPrimaryFilter temporalPrimaryFilter, Map<Attribute, SqlField> attributeSwapFields) {
    return new BQTemporalPrimaryFilterTranslator(this, temporalPrimaryFilter, attributeSwapFields);
  }

  @Override
  public String naryFilterOnRepeatedFieldSql(
      SqlField field,
      NaryOperator naryOperator,
      List<Literal> values,
      @Nullable String tableAlias,
      SqlParams sqlParams) {
    String functionTemplate =
        "EXISTS (SELECT * FROM UNNEST("
            + FUNCTION_TEMPLATE_FIELD_VAR_BRACES
            + ") AS flattened WHERE flattened "
            + (NaryOperator.IN.equals(naryOperator) ? "IN" : "NOT IN")
            + " ("
            + FUNCTION_TEMPLATE_VALUES_VAR_BRACES
            + "))";
    return functionWithCommaSeparatedArgsFilterSql(
        field, functionTemplate, values, tableAlias, sqlParams);
  }

  @SuppressWarnings("PMD.TooFewBranchesForASwitchStatement")
  @Override
  public String textSearchOperatorTemplateSql(TextSearchFilter.TextSearchOperator operator) {
    if (TextSearchOperator.FUZZY_MATCH.equals(operator)) {
      return "bqutil.fn.levenshtein(UPPER("
          + FUNCTION_TEMPLATE_FIELD_VAR_BRACES
          + "), UPPER("
          + FUNCTION_TEMPLATE_VALUES_VAR_BRACES
          + "))<5";
    }
    return ApiTranslator.super.textSearchOperatorTemplateSql(operator);
  }
}
