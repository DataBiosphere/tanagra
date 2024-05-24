package bio.terra.tanagra.query.bigquery.translator;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.field.EntityIdCountField;
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
import bio.terra.tanagra.query.bigquery.translator.field.BQAttributeFieldTranslator;
import bio.terra.tanagra.query.bigquery.translator.field.BQEntityIdCountFieldTranslator;
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
import bio.terra.tanagra.query.sql.translator.ApiFieldTranslator;
import bio.terra.tanagra.query.sql.translator.ApiFilterTranslator;
import bio.terra.tanagra.query.sql.translator.ApiTranslator;

public final class BQApiTranslator implements ApiTranslator {
  @Override
  public ApiFieldTranslator translator(AttributeField attributeField) {
    return new BQAttributeFieldTranslator(attributeField);
  }

  @Override
  public ApiFieldTranslator translator(EntityIdCountField entityIdCountField) {
    return new BQEntityIdCountFieldTranslator(entityIdCountField);
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

  @SuppressWarnings("PMD.TooFewBranchesForASwitchStatement")
  @Override
  public String textSearchOperatorTemplateSql(TextSearchFilter.TextSearchOperator operator) {
    switch (operator) {
      case FUZZY_MATCH:
        return "bqutil.fn.levenshtein(UPPER("
            + FUNCTION_TEMPLATE_FIELD_VAR_BRACES
            + "), UPPER("
            + FUNCTION_TEMPLATE_VALUES_VAR_BRACES
            + "))<5";
      default:
        return ApiTranslator.super.textSearchOperatorTemplateSql(operator);
    }
  }
}
