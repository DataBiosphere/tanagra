package bio.terra.tanagra.query2.bigquery;

import static bio.terra.tanagra.query2.sql.SqlGeneration.FUNCTION_TEMPLATE_FIELD_VAR_BRACES;
import static bio.terra.tanagra.query2.sql.SqlGeneration.FUNCTION_TEMPLATE_VALUES_VAR_BRACES;

import bio.terra.tanagra.api.field.valuedisplay.AttributeField;
import bio.terra.tanagra.api.field.valuedisplay.EntityIdCountField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyIsMemberField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyIsRootField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyNumChildrenField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyPathField;
import bio.terra.tanagra.api.field.valuedisplay.RelatedEntityIdCountField;
import bio.terra.tanagra.api.field.valuedisplay.ValueDisplayField;
import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.BooleanNotFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.HierarchyHasAncestorFilter;
import bio.terra.tanagra.api.filter.HierarchyHasParentFilter;
import bio.terra.tanagra.api.filter.HierarchyIsMemberFilter;
import bio.terra.tanagra.api.filter.HierarchyIsRootFilter;
import bio.terra.tanagra.api.filter.RelationshipFilter;
import bio.terra.tanagra.api.filter.TextSearchFilter;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.filtervariable.FunctionFilterVariable;
import bio.terra.tanagra.query2.bigquery.fieldtranslator.BQAttributeFieldTranslator;
import bio.terra.tanagra.query2.bigquery.fieldtranslator.BQEntityIdCountFieldTranslator;
import bio.terra.tanagra.query2.bigquery.fieldtranslator.BQHierarchyIsMemberFieldTranslator;
import bio.terra.tanagra.query2.bigquery.fieldtranslator.BQHierarchyIsRootFieldTranslator;
import bio.terra.tanagra.query2.bigquery.fieldtranslator.BQHierarchyNumChildrenFieldTranslator;
import bio.terra.tanagra.query2.bigquery.fieldtranslator.BQHierarchyPathFieldTranslator;
import bio.terra.tanagra.query2.bigquery.fieldtranslator.BQRelatedEntityIdCountFieldTranslator;
import bio.terra.tanagra.query2.bigquery.filtertranslator.BQAttributeFilterTranslator;
import bio.terra.tanagra.query2.bigquery.filtertranslator.BQBooleanAndOrFilterTranslator;
import bio.terra.tanagra.query2.bigquery.filtertranslator.BQBooleanNotFilterTranslator;
import bio.terra.tanagra.query2.bigquery.filtertranslator.BQHierarchyHasAncestorFilterTranslator;
import bio.terra.tanagra.query2.bigquery.filtertranslator.BQHierarchyHasParentFilterTranslator;
import bio.terra.tanagra.query2.bigquery.filtertranslator.BQHierarchyIsMemberFilterTranslator;
import bio.terra.tanagra.query2.bigquery.filtertranslator.BQHierarchyIsRootFilterTranslator;
import bio.terra.tanagra.query2.bigquery.filtertranslator.BQRelationshipFilterTranslator;
import bio.terra.tanagra.query2.bigquery.filtertranslator.BQTextSearchFilterTranslator;
import bio.terra.tanagra.query2.sql.SqlFieldTranslator;
import bio.terra.tanagra.query2.sql.SqlFilterTranslator;

public final class BQTranslator {
  private BQTranslator() {}

  public static SqlFieldTranslator translator(ValueDisplayField valueDisplayField) {
    if (valueDisplayField instanceof AttributeField) {
      return new BQAttributeFieldTranslator((AttributeField) valueDisplayField);
    } else if (valueDisplayField instanceof EntityIdCountField) {
      return new BQEntityIdCountFieldTranslator((EntityIdCountField) valueDisplayField);
    } else if (valueDisplayField instanceof HierarchyIsMemberField) {
      return new BQHierarchyIsMemberFieldTranslator((HierarchyIsMemberField) valueDisplayField);
    } else if (valueDisplayField instanceof HierarchyIsRootField) {
      return new BQHierarchyIsRootFieldTranslator((HierarchyIsRootField) valueDisplayField);
    } else if (valueDisplayField instanceof HierarchyNumChildrenField) {
      return new BQHierarchyNumChildrenFieldTranslator(
          (HierarchyNumChildrenField) valueDisplayField);
    } else if (valueDisplayField instanceof HierarchyPathField) {
      return new BQHierarchyPathFieldTranslator((HierarchyPathField) valueDisplayField);
    } else if (valueDisplayField instanceof RelatedEntityIdCountField) {
      return new BQRelatedEntityIdCountFieldTranslator(
          (RelatedEntityIdCountField) valueDisplayField);
    } else {
      throw new InvalidQueryException("Unsupported field for BigQuery runner");
    }
  }

  public static SqlFilterTranslator translator(EntityFilter entityFilter) {
    if (entityFilter instanceof AttributeFilter) {
      return new BQAttributeFilterTranslator((AttributeFilter) entityFilter);
    } else if (entityFilter instanceof BooleanAndOrFilter) {
      return new BQBooleanAndOrFilterTranslator((BooleanAndOrFilter) entityFilter);
    } else if (entityFilter instanceof BooleanNotFilter) {
      return new BQBooleanNotFilterTranslator((BooleanNotFilter) entityFilter);
    } else if (entityFilter instanceof HierarchyHasAncestorFilter) {
      return new BQHierarchyHasAncestorFilterTranslator((HierarchyHasAncestorFilter) entityFilter);
    } else if (entityFilter instanceof HierarchyHasParentFilter) {
      return new BQHierarchyHasParentFilterTranslator((HierarchyHasParentFilter) entityFilter);
    } else if (entityFilter instanceof HierarchyIsMemberFilter) {
      return new BQHierarchyIsMemberFilterTranslator((HierarchyIsMemberFilter) entityFilter);
    } else if (entityFilter instanceof HierarchyIsRootFilter) {
      return new BQHierarchyIsRootFilterTranslator((HierarchyIsRootFilter) entityFilter);
    } else if (entityFilter instanceof RelationshipFilter) {
      return new BQRelationshipFilterTranslator((RelationshipFilter) entityFilter);
    } else if (entityFilter instanceof TextSearchFilter) {
      return new BQTextSearchFilterTranslator((TextSearchFilter) entityFilter);
    } else {
      throw new InvalidQueryException("Unsupported filter for BigQuery runner");
    }
  }

  public static String functionTemplateSql(
      FunctionFilterVariable.FunctionTemplate functionTemplate) {
    switch (functionTemplate) {
      case TEXT_EXACT_MATCH:
        return "REGEXP_CONTAINS(UPPER("
            + FUNCTION_TEMPLATE_FIELD_VAR_BRACES
            + "), UPPER("
            + FUNCTION_TEMPLATE_VALUES_VAR_BRACES
            + "))";
      case TEXT_FUZZY_MATCH:
        return "bqutil.fn.levenshtein(UPPER("
            + FUNCTION_TEMPLATE_FIELD_VAR_BRACES
            + "), UPPER("
            + FUNCTION_TEMPLATE_VALUES_VAR_BRACES
            + "))<5";
      case IN:
        return FUNCTION_TEMPLATE_FIELD_VAR_BRACES
            + " IN ("
            + FUNCTION_TEMPLATE_VALUES_VAR_BRACES
            + ")";
      case NOT_IN:
        return FUNCTION_TEMPLATE_FIELD_VAR_BRACES
            + " NOT IN ("
            + FUNCTION_TEMPLATE_VALUES_VAR_BRACES
            + ")";
      case IS_NULL:
        return FUNCTION_TEMPLATE_FIELD_VAR_BRACES + " IS NULL";
      case IS_NOT_NULL:
        return FUNCTION_TEMPLATE_FIELD_VAR_BRACES + " IS NOT NULL";
      case IS_EMPTY_STRING:
        return FUNCTION_TEMPLATE_FIELD_VAR_BRACES + " = ''";
      default:
        throw new SystemException("Unknown function template: " + functionTemplate);
    }
  }
}
