package bio.terra.tanagra.query2.bigquery;

import static bio.terra.tanagra.query2.sql.SqlGeneration.FUNCTION_TEMPLATE_FIELD_VAR_BRACES;
import static bio.terra.tanagra.query2.sql.SqlGeneration.FUNCTION_TEMPLATE_VALUES_VAR_BRACES;
import static bio.terra.tanagra.query2.sql.SqlGeneration.inSelectFilterSql;

import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.BooleanNotFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.HierarchyHasAncestorFilter;
import bio.terra.tanagra.api.filter.HierarchyHasParentFilter;
import bio.terra.tanagra.api.filter.HierarchyIsMemberFilter;
import bio.terra.tanagra.api.filter.HierarchyIsRootFilter;
import bio.terra.tanagra.api.filter.ItemIsInGroupFilter;
import bio.terra.tanagra.api.filter.OccurrenceForPrimaryFilter;
import bio.terra.tanagra.api.filter.PrimaryForCriteriaFilter;
import bio.terra.tanagra.api.filter.RelationshipFilter;
import bio.terra.tanagra.api.filter.TextSearchFilter;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.FunctionFilterVariable;
import bio.terra.tanagra.query2.sql.SqlGeneration;
import bio.terra.tanagra.query2.sql.SqlParams;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.indextable.ITHierarchyAncestorDescendant;
import bio.terra.tanagra.underlay.indextable.ITHierarchyChildParent;
import bio.terra.tanagra.underlay.indextable.ITRelationshipIdPairs;
import java.util.List;
import java.util.stream.Collectors;

public final class BigQueryFilterSqlUtils {
  private BigQueryFilterSqlUtils() {}

  public static String buildFilterSql(
      Underlay underlay,
      EntityFilter entityFilter,
      String tableAlias,
      SqlParams sqlParams,
      FieldPointer idField) {
    if (entityFilter instanceof AttributeFilter) {
      return attributeFilterSql(
          underlay, (AttributeFilter) entityFilter, tableAlias, sqlParams, idField);
    } else if (entityFilter instanceof BooleanAndOrFilter) {
      return booleanAndOrFilterSql(
          underlay, (BooleanAndOrFilter) entityFilter, tableAlias, sqlParams, idField);
    } else if (entityFilter instanceof BooleanNotFilter) {
      return booleanNotFilterSql(
          underlay, (BooleanNotFilter) entityFilter, tableAlias, sqlParams, idField);
    } else if (entityFilter instanceof HierarchyHasAncestorFilter) {
      return hierarchyHasAncestorFilterSql(
          underlay, (HierarchyHasAncestorFilter) entityFilter, tableAlias, sqlParams, idField);
    } else if (entityFilter instanceof HierarchyHasParentFilter) {
      return hierarchyHasParentFilterSql(
          underlay, (HierarchyHasParentFilter) entityFilter, tableAlias, sqlParams, idField);
    } else if (entityFilter instanceof HierarchyIsMemberFilter) {
      return hierarchyIsMemberFilterSql(
          underlay, (HierarchyIsMemberFilter) entityFilter, tableAlias, sqlParams);
    } else if (entityFilter instanceof HierarchyIsRootFilter) {
      return hierarchyIsRootFilterSql(
          underlay, (HierarchyIsRootFilter) entityFilter, tableAlias, sqlParams);
    } else if (entityFilter instanceof RelationshipFilter) {
      RelationshipFilter relationshipFilter = (RelationshipFilter) entityFilter;
      if (relationshipFilter.isForeignKeyOnSelectTable()) {
        return relationshipFilterFKSelectSql(
            underlay, relationshipFilter, tableAlias, sqlParams, idField);
      } else if (relationshipFilter.isForeignKeyOnFilterTable()) {
        return relationshipFilterFKFilterSql(
            underlay, relationshipFilter, tableAlias, sqlParams, idField);
      } else {
        return relationshipFilterIntermediateTableSql(
            underlay, relationshipFilter, tableAlias, sqlParams, idField);
      }
    } else if (entityFilter instanceof TextSearchFilter) {
      return textFilterSql(
          underlay, (TextSearchFilter) entityFilter, tableAlias, sqlParams, idField);
    } else {
      throw new SystemException(
          "Unsupported filter type: " + entityFilter.getClass().getSimpleName());
    }
  }

  private static String attributeFilterSql(
      Underlay underlay,
      AttributeFilter attributeFilter,
      String tableAlias,
      SqlParams sqlParams,
      FieldPointer idField) {
    ITEntityMain indexTable =
        underlay.getIndexSchema().getEntityMain(attributeFilter.getEntity().getName());

    Attribute attribute = attributeFilter.getAttribute();
    FieldPointer valueField =
        attribute.isId() ? idField : indexTable.getAttributeValueField(attribute.getName());
    if (attribute.hasRuntimeSqlFunctionWrapper()) {
      valueField =
          valueField
              .toBuilder()
              .sqlFunctionWrapper(attribute.getRuntimeSqlFunctionWrapper())
              .build();
    }
    return attributeFilter.hasFunctionTemplate()
        ? SqlGeneration.functionFilterSql(
            valueField,
            functionTemplateSql(attributeFilter.getFunctionTemplate()),
            attributeFilter.getValues(),
            tableAlias,
            sqlParams)
        : SqlGeneration.binaryFilterSql(
            valueField,
            attributeFilter.getOperator(),
            attributeFilter.getValues().get(0),
            tableAlias,
            sqlParams);
  }

  private static String booleanAndOrFilterSql(
      Underlay underlay,
      BooleanAndOrFilter booleanAndOrFilter,
      String tableAlias,
      SqlParams sqlParams,
      FieldPointer idField) {
    List<String> subFilterSqls =
        booleanAndOrFilter.getSubFilters().stream()
            .map(subFilter -> buildFilterSql(underlay, subFilter, tableAlias, sqlParams, idField))
            .collect(Collectors.toList());
    return SqlGeneration.booleanAndOrFilterSql(
        booleanAndOrFilter.getOperator(), subFilterSqls.toArray(new String[0]));
  }

  private static String booleanNotFilterSql(
      Underlay underlay,
      BooleanNotFilter booleanNotFilter,
      String tableAlias,
      SqlParams sqlParams,
      FieldPointer idField) {
    return SqlGeneration.booleanNotFilterSql(
        buildFilterSql(underlay, booleanNotFilter.getSubFilter(), tableAlias, sqlParams, idField));
  }

  private static String textFilterSql(
      Underlay underlay,
      TextSearchFilter textSearchFilter,
      String tableAlias,
      SqlParams sqlParams,
      FieldPointer idField) {
    ITEntityMain indexTable =
        underlay.getIndexSchema().getEntityMain(textSearchFilter.getEntity().getName());
    FieldPointer textSearchField;
    if (textSearchFilter.isForSpecificAttribute()) {
      // Search only on the specified attribute.
      Attribute attribute = textSearchFilter.getAttribute();
      textSearchField =
          attribute.isId() ? idField : indexTable.getAttributeValueField(attribute.getName());
      if (attribute.hasRuntimeSqlFunctionWrapper()) {
        textSearchField =
            textSearchField
                .toBuilder()
                .sqlFunctionWrapper(attribute.getRuntimeSqlFunctionWrapper())
                .build();
      }
    } else {
      // Search the text index specified in the underlay config.
      textSearchField = indexTable.getTextSearchField();
    }
    return SqlGeneration.functionFilterSql(
        textSearchField,
        functionTemplateSql(textSearchFilter.getFunctionTemplate()),
        List.of(new Literal(textSearchFilter.getText())),
        tableAlias,
        sqlParams);
  }

  private static String hierarchyHasAncestorFilterSql(
      Underlay underlay,
      HierarchyHasAncestorFilter hierarchyHasAncestorFilter,
      String tableAlias,
      SqlParams sqlParams,
      FieldPointer idField) {
    //  entity.id IN (SELECT ancestorId UNION ALL SELECT descendant FROM ancestorDescendantTable
    // WHERE ancestor=ancestorId)
    ITHierarchyAncestorDescendant ancestorDescendantTable =
        underlay
            .getIndexSchema()
            .getHierarchyAncestorDescendant(
                hierarchyHasAncestorFilter.getEntity().getName(),
                hierarchyHasAncestorFilter.getHierarchy().getName());
    ITEntityMain entityMainIndexTable =
        underlay.getIndexSchema().getEntityMain(hierarchyHasAncestorFilter.getEntity().getName());
    return inSelectFilterSql(
        idField,
        tableAlias,
        ancestorDescendantTable.getDescendantField(),
        ancestorDescendantTable.getTablePointer(),
        SqlGeneration.binaryFilterSql(
            ancestorDescendantTable.getAncestorField(),
            BinaryFilterVariable.BinaryOperator.EQUALS,
            hierarchyHasAncestorFilter.getAncestorId(),
            null,
            sqlParams),
        sqlParams,
        hierarchyHasAncestorFilter.getAncestorId());
  }

  private static String hierarchyHasParentFilterSql(
      Underlay underlay,
      HierarchyHasParentFilter hierarchyHasParentFilter,
      String tableAlias,
      SqlParams sqlParams,
      FieldPointer idField) {
    //  entity.id IN (SELECT child FROM childParentTable WHERE parent=parentId)
    ITHierarchyChildParent childParentIndexTable =
        underlay
            .getIndexSchema()
            .getHierarchyChildParent(
                hierarchyHasParentFilter.getEntity().getName(),
                hierarchyHasParentFilter.getHierarchy().getName());
    return inSelectFilterSql(
        idField,
        tableAlias,
        childParentIndexTable.getChildField(),
        childParentIndexTable.getTablePointer(),
        SqlGeneration.binaryFilterSql(
            childParentIndexTable.getParentField(),
            BinaryFilterVariable.BinaryOperator.EQUALS,
            hierarchyHasParentFilter.getParentId(),
            null,
            sqlParams),
        sqlParams);
  }

  private static String hierarchyIsMemberFilterSql(
      Underlay underlay,
      HierarchyIsMemberFilter hierarchyIsMemberFilter,
      String tableAlias,
      SqlParams sqlParams) {
    ITEntityMain indexTable =
        underlay.getIndexSchema().getEntityMain(hierarchyIsMemberFilter.getEntity().getName());

    // IS_MEMBER means path IS NOT NULL.
    FieldPointer pathField =
        indexTable.getHierarchyPathField(hierarchyIsMemberFilter.getHierarchy().getName());
    return SqlGeneration.functionFilterSql(
        pathField,
        functionTemplateSql(FunctionFilterVariable.FunctionTemplate.IS_NOT_NULL),
        List.of(),
        tableAlias,
        sqlParams);
  }

  private static String hierarchyIsRootFilterSql(
      Underlay underlay,
      HierarchyIsRootFilter hierarchyIsMemberFilter,
      String tableAlias,
      SqlParams sqlParams) {
    ITEntityMain indexTable =
        underlay.getIndexSchema().getEntityMain(hierarchyIsMemberFilter.getEntity().getName());

    // IS_ROOT means path=''.
    FieldPointer pathField =
        indexTable.getHierarchyPathField(hierarchyIsMemberFilter.getHierarchy().getName());
    return SqlGeneration.functionFilterSql(
        pathField,
        functionTemplateSql(FunctionFilterVariable.FunctionTemplate.IS_EMPTY_STRING),
        List.of(),
        tableAlias,
        sqlParams);
  }

  private static String functionTemplateSql(
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

  private static String relationshipFilterFKSelectSql(
      Underlay underlay,
      RelationshipFilter relationshipFilter,
      String tableAlias,
      SqlParams sqlParams,
      FieldPointer idField) {
    Attribute foreignKeyAttribute =
        relationshipFilter
            .getRelationship()
            .getForeignKeyAttribute(relationshipFilter.getSelectEntity());
    FieldPointer foreignKeyField =
        underlay
            .getIndexSchema()
            .getEntityMain(relationshipFilter.getSelectEntity().getName())
            .getAttributeValueField(foreignKeyAttribute.getName());
    if (isFilterOnAttribute(relationshipFilter.getSubFilter(), relationshipFilter.getFilterEntity().getIdAttribute())) {
      // subFilter(idField=foreignKey)
      return buildFilterSql(
          underlay, relationshipFilter.getSubFilter(), tableAlias, sqlParams, foreignKeyField);
    } else {
      // foreignKey IN (SELECT id FROM filterEntity WHERE subFilter(idField=id))
      ITEntityMain filterEntityTable =
          underlay.getIndexSchema().getEntityMain(relationshipFilter.getFilterEntity().getName());
      FieldPointer filterEntityIdField =
          filterEntityTable.getAttributeValueField(
              relationshipFilter.getFilterEntity().getIdAttribute().getName());
      return inSelectFilterSql(
          foreignKeyField,
          tableAlias,
          filterEntityIdField,
          filterEntityTable.getTablePointer(),
          buildFilterSql(
              underlay, relationshipFilter.getSubFilter(), null, sqlParams, filterEntityIdField),
          sqlParams);
    }
  }

  private static String relationshipFilterFKFilterSql(
      Underlay underlay,
      RelationshipFilter relationshipFilter,
      String tableAlias,
      SqlParams sqlParams,
      FieldPointer idField) {
    Attribute foreignKeyAttribute =
            relationshipFilter
                    .getRelationship()
                    .getForeignKeyAttribute(relationshipFilter.getFilterEntity());
    ITEntityMain filterEntityTable =
            underlay.getIndexSchema().getEntityMain(relationshipFilter.getFilterEntity().getName());
    FieldPointer foreignKeyField =
            filterEntityTable.getAttributeValueField(foreignKeyAttribute.getName());
    if (isFilterOnAttribute(relationshipFilter.getSubFilter(), foreignKeyAttribute)) {
      // subFilter(idField=foreignKey)
      return buildFilterSql(underlay, relationshipFilter.getSubFilter(), tableAlias, sqlParams, foreignKeyField);
    } else {
      // id IN (SELECT foreignKey FROM filterEntity WHERE subFilter(idField=id))
      FieldPointer filterEntityIdField =
              filterEntityTable.getAttributeValueField(
                      relationshipFilter.getFilterEntity().getIdAttribute().getName());
      return inSelectFilterSql(
              idField,
              tableAlias,
              foreignKeyField,
              filterEntityTable.getTablePointer(),
              buildFilterSql(
                      underlay, relationshipFilter.getSubFilter(), null, sqlParams, filterEntityIdField),
              sqlParams);
    }
  }

  private static String relationshipFilterIntermediateTableSql(
      Underlay underlay,
      RelationshipFilter relationshipFilter,
      String tableAlias,
      SqlParams sqlParams,
      FieldPointer idField) {
    ITRelationshipIdPairs idPairsTable =
        underlay
            .getIndexSchema()
            .getRelationshipIdPairs(
                relationshipFilter.getEntityGroup().getName(),
                relationshipFilter.getRelationship().getEntityA().getName(),
                relationshipFilter.getRelationship().getEntityB().getName());
    FieldPointer selectId =
        idPairsTable.getEntityIdField(relationshipFilter.getSelectEntity().getName());
    FieldPointer filterId =
        idPairsTable.getEntityIdField(relationshipFilter.getFilterEntity().getName());
    if (isFilterOnAttribute(relationshipFilter.getSubFilter(), relationshipFilter.getFilterEntity().getIdAttribute())) {
      // id IN (SELECT selectId FROM intermediateTable WHERE subFilter(idField=filterId))
      return inSelectFilterSql(
          idField,
          tableAlias,
          selectId,
          idPairsTable.getTablePointer(),
          buildFilterSql(underlay, relationshipFilter.getSubFilter(), null, sqlParams, filterId),
          sqlParams);
    } else {
      // id IN (SELECT selectId FROM intermediateTable WHERE filterId IN (SELECT id FROM
      // filterEntity WHERE subFilter(idField=id))
      ITEntityMain filterEntityTable =
          underlay.getIndexSchema().getEntityMain(relationshipFilter.getFilterEntity().getName());
      FieldPointer filterEntityIdField =
          filterEntityTable.getAttributeValueField(
              relationshipFilter.getFilterEntity().getIdAttribute().getName());
      return inSelectFilterSql(
          idField,
          tableAlias,
          selectId,
          idPairsTable.getTablePointer(),
          inSelectFilterSql(
              filterId,
              null,
              filterEntityIdField,
              filterEntityTable.getTablePointer(),
              buildFilterSql(
                  underlay, relationshipFilter.getSubFilter(), null, sqlParams, filterId),
              sqlParams),
          sqlParams);
    }
  }

  private static boolean isFilterOnAttribute(EntityFilter entityFilter, Attribute attribute) {
    if (entityFilter instanceof AttributeFilter) {
      return ((AttributeFilter) entityFilter).getAttribute().equals(attribute);
    } else if (entityFilter instanceof BooleanAndOrFilter) {
      return ((BooleanAndOrFilter) entityFilter)
              .getSubFilters()
              .parallelStream()
              .filter(subFilter -> !isFilterOnAttribute(subFilter, attribute))
              .findAny()
              .isEmpty();
    } else if (entityFilter instanceof BooleanNotFilter) {
      return isFilterOnAttribute(((BooleanNotFilter) entityFilter).getSubFilter(), attribute);
    } else if (entityFilter instanceof HierarchyHasAncestorFilter) {
      return attribute.isId();
    } else if (entityFilter instanceof HierarchyHasParentFilter) {
      return attribute.isId();
    } else if (entityFilter instanceof HierarchyIsMemberFilter) {
      return false;
    } else if (entityFilter instanceof HierarchyIsRootFilter) {
      return false;
    } else if (entityFilter instanceof RelationshipFilter) {
      return attribute.isId();
    } else if (entityFilter instanceof TextSearchFilter) {
      return ((TextSearchFilter) entityFilter).isForSpecificAttribute()
              && ((TextSearchFilter) entityFilter).getAttribute().equals(attribute);
    } else {
      throw new SystemException(
              "Unsupported filter type: " + entityFilter.getClass().getSimpleName());
    }
  }
}
