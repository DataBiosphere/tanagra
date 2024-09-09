package bio.terra.tanagra.query.bigquery.translator;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.field.CountDistinctField;
import bio.terra.tanagra.api.field.HierarchyIsMemberField;
import bio.terra.tanagra.api.field.HierarchyIsRootField;
import bio.terra.tanagra.api.field.HierarchyNumChildrenField;
import bio.terra.tanagra.api.field.HierarchyPathField;
import bio.terra.tanagra.api.field.RelatedEntityIdCountField;
import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.HierarchyHasAncestorFilter;
import bio.terra.tanagra.api.filter.HierarchyHasParentFilter;
import bio.terra.tanagra.api.filter.HierarchyIsMemberFilter;
import bio.terra.tanagra.api.filter.HierarchyIsRootFilter;
import bio.terra.tanagra.api.filter.PrimaryWithCriteriaFilter;
import bio.terra.tanagra.api.filter.RelationshipFilter;
import bio.terra.tanagra.api.filter.TemporalPrimaryFilter;
import bio.terra.tanagra.api.filter.TextSearchFilter;
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
  public ApiFilterTranslator translator(AttributeFilter attributeFilter) {
    return new BQAttributeFilterTranslator(this, attributeFilter);
  }

  @Override
  public ApiFilterTranslator translator(HierarchyHasAncestorFilter hierarchyHasAncestorFilter) {
    return new BQHierarchyHasAncestorFilterTranslator(this, hierarchyHasAncestorFilter);
  }

  @Override
  public ApiFilterTranslator translator(HierarchyHasParentFilter hierarchyHasParentFilter) {
    return new BQHierarchyHasParentFilterTranslator(this, hierarchyHasParentFilter);
  }

  @Override
  public ApiFilterTranslator translator(HierarchyIsMemberFilter hierarchyIsMemberFilter) {
    return new BQHierarchyIsMemberFilterTranslator(this, hierarchyIsMemberFilter);
  }

  @Override
  public ApiFilterTranslator translator(HierarchyIsRootFilter hierarchyIsRootFilter) {
    return new BQHierarchyIsRootFilterTranslator(this, hierarchyIsRootFilter);
  }

  @Override
  public ApiFilterTranslator translator(PrimaryWithCriteriaFilter primaryWithCriteriaFilter) {
    return new BQPrimaryWithCriteriaFilterTranslator(this, primaryWithCriteriaFilter);
  }

  @Override
  public ApiFilterTranslator translator(RelationshipFilter relationshipFilter) {
    return new BQRelationshipFilterTranslator(this, relationshipFilter);
  }

  @Override
  public ApiFilterTranslator translator(TextSearchFilter textSearchFilter) {
    return new BQTextSearchFilterTranslator(this, textSearchFilter);
  }

  @Override
  public ApiFilterTranslator translator(TemporalPrimaryFilter temporalPrimaryFilter) {
    return new BQTemporalPrimaryFilterTranslator(this, temporalPrimaryFilter);
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
