package bio.terra.tanagra.query2.bigquery;

import bio.terra.tanagra.api.field.valuedisplay.AttributeField;
import bio.terra.tanagra.api.field.valuedisplay.EntityIdCountField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyIsMemberField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyIsRootField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyNumChildrenField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyPathField;
import bio.terra.tanagra.api.field.valuedisplay.RelatedEntityIdCountField;
import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.HierarchyHasAncestorFilter;
import bio.terra.tanagra.api.filter.HierarchyHasParentFilter;
import bio.terra.tanagra.api.filter.HierarchyIsMemberFilter;
import bio.terra.tanagra.api.filter.HierarchyIsRootFilter;
import bio.terra.tanagra.api.filter.RelationshipFilter;
import bio.terra.tanagra.api.filter.TextSearchFilter;
import bio.terra.tanagra.query.filtervariable.FunctionFilterVariable;
import bio.terra.tanagra.query2.bigquery.fieldtranslator.BQAttributeFieldTranslator;
import bio.terra.tanagra.query2.bigquery.fieldtranslator.BQEntityIdCountFieldTranslator;
import bio.terra.tanagra.query2.bigquery.fieldtranslator.BQHierarchyIsMemberFieldTranslator;
import bio.terra.tanagra.query2.bigquery.fieldtranslator.BQHierarchyIsRootFieldTranslator;
import bio.terra.tanagra.query2.bigquery.fieldtranslator.BQHierarchyNumChildrenFieldTranslator;
import bio.terra.tanagra.query2.bigquery.fieldtranslator.BQHierarchyPathFieldTranslator;
import bio.terra.tanagra.query2.bigquery.fieldtranslator.BQRelatedEntityIdCountFieldTranslator;
import bio.terra.tanagra.query2.bigquery.filtertranslator.BQAttributeFilterTranslator;
import bio.terra.tanagra.query2.bigquery.filtertranslator.BQHierarchyHasAncestorFilterTranslator;
import bio.terra.tanagra.query2.bigquery.filtertranslator.BQHierarchyHasParentFilterTranslator;
import bio.terra.tanagra.query2.bigquery.filtertranslator.BQHierarchyIsMemberFilterTranslator;
import bio.terra.tanagra.query2.bigquery.filtertranslator.BQHierarchyIsRootFilterTranslator;
import bio.terra.tanagra.query2.bigquery.filtertranslator.BQRelationshipFilterTranslator;
import bio.terra.tanagra.query2.bigquery.filtertranslator.BQTextSearchFilterTranslator;
import bio.terra.tanagra.query2.sql.SqlFieldTranslator;
import bio.terra.tanagra.query2.sql.SqlFilterTranslator;
import bio.terra.tanagra.query2.sql.SqlTranslator;

public final class BQTranslator implements SqlTranslator {
  @Override
  public SqlFieldTranslator translator(AttributeField attributeField) {
    return new BQAttributeFieldTranslator(attributeField);
  }

  @Override
  public SqlFieldTranslator translator(EntityIdCountField entityIdCountField) {
    return new BQEntityIdCountFieldTranslator(entityIdCountField);
  }

  @Override
  public SqlFieldTranslator translator(HierarchyIsMemberField hierarchyIsMemberField) {
    return new BQHierarchyIsMemberFieldTranslator(hierarchyIsMemberField);
  }

  @Override
  public SqlFieldTranslator translator(HierarchyIsRootField hierarchyIsRootField) {
    return new BQHierarchyIsRootFieldTranslator(hierarchyIsRootField);
  }

  @Override
  public SqlFieldTranslator translator(HierarchyNumChildrenField hierarchyNumChildrenField) {
    return new BQHierarchyNumChildrenFieldTranslator(hierarchyNumChildrenField);
  }

  @Override
  public SqlFieldTranslator translator(HierarchyPathField hierarchyPathField) {
    return new BQHierarchyPathFieldTranslator(hierarchyPathField);
  }

  @Override
  public SqlFieldTranslator translator(RelatedEntityIdCountField relatedEntityIdCountField) {
    return new BQRelatedEntityIdCountFieldTranslator(relatedEntityIdCountField);
  }

  @Override
  public SqlFilterTranslator translator(AttributeFilter attributeFilter) {
    return new BQAttributeFilterTranslator(this, attributeFilter);
  }

  @Override
  public SqlFilterTranslator translator(HierarchyHasAncestorFilter hierarchyHasAncestorFilter) {
    return new BQHierarchyHasAncestorFilterTranslator(this, hierarchyHasAncestorFilter);
  }

  @Override
  public SqlFilterTranslator translator(HierarchyHasParentFilter hierarchyHasParentFilter) {
    return new BQHierarchyHasParentFilterTranslator(this, hierarchyHasParentFilter);
  }

  @Override
  public SqlFilterTranslator translator(HierarchyIsMemberFilter hierarchyIsMemberFilter) {
    return new BQHierarchyIsMemberFilterTranslator(this, hierarchyIsMemberFilter);
  }

  @Override
  public SqlFilterTranslator translator(HierarchyIsRootFilter hierarchyIsRootFilter) {
    return new BQHierarchyIsRootFilterTranslator(this, hierarchyIsRootFilter);
  }

  @Override
  public SqlFilterTranslator translator(RelationshipFilter relationshipFilter) {
    return new BQRelationshipFilterTranslator(this, relationshipFilter);
  }

  @Override
  public SqlFilterTranslator translator(TextSearchFilter textSearchFilter) {
    return new BQTextSearchFilterTranslator(this, textSearchFilter);
  }

  @SuppressWarnings("PMD.TooFewBranchesForASwitchStatement")
  @Override
  public String functionTemplateSql(FunctionFilterVariable.FunctionTemplate functionTemplate) {
    switch (functionTemplate) {
      case TEXT_FUZZY_MATCH:
        return "bqutil.fn.levenshtein(UPPER("
            + FUNCTION_TEMPLATE_FIELD_VAR_BRACES
            + "), UPPER("
            + FUNCTION_TEMPLATE_VALUES_VAR_BRACES
            + "))<5";
      default:
        return SqlTranslator.super.functionTemplateSql(functionTemplate);
    }
  }
}
