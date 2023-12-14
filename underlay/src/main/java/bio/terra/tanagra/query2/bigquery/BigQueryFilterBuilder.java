package bio.terra.tanagra.query2.bigquery;

import static bio.terra.tanagra.query2.sql.SqlGeneration.FUNCTION_TEMPLATE_FIELD_VAR_BRACES;
import static bio.terra.tanagra.query2.sql.SqlGeneration.FUNCTION_TEMPLATE_VALUES_VAR_BRACES;
import static bio.terra.tanagra.query2.sql.SqlGeneration.havingSql;
import static bio.terra.tanagra.query2.sql.SqlGeneration.inSelectFilterSql;

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

public final class BigQueryFilterBuilder {
  private final Underlay underlay;
  private final SqlParams sqlParams;

  public BigQueryFilterBuilder(Underlay underlay, SqlParams sqlParams) {
    this.underlay = underlay;
    this.sqlParams = sqlParams;
  }

  public String buildFilterSql(EntityFilter entityFilter, String tableAlias, FieldPointer idField) {
    if (entityFilter instanceof AttributeFilter) {
      return attributeFilterSql((AttributeFilter) entityFilter, tableAlias, idField);
    } else if (entityFilter instanceof BooleanAndOrFilter) {
      return booleanAndOrFilterSql((BooleanAndOrFilter) entityFilter, tableAlias, idField);
    } else if (entityFilter instanceof BooleanNotFilter) {
      return booleanNotFilterSql((BooleanNotFilter) entityFilter, tableAlias, idField);
    } else if (entityFilter instanceof HierarchyHasAncestorFilter) {
      return hierarchyHasAncestorFilterSql(
          (HierarchyHasAncestorFilter) entityFilter, tableAlias, idField);
    } else if (entityFilter instanceof HierarchyHasParentFilter) {
      return hierarchyHasParentFilterSql(
          (HierarchyHasParentFilter) entityFilter, tableAlias, idField);
    } else if (entityFilter instanceof HierarchyIsMemberFilter) {
      return hierarchyIsMemberFilterSql((HierarchyIsMemberFilter) entityFilter, tableAlias);
    } else if (entityFilter instanceof HierarchyIsRootFilter) {
      return hierarchyIsRootFilterSql((HierarchyIsRootFilter) entityFilter, tableAlias);
    } else if (entityFilter instanceof RelationshipFilter) {
      RelationshipFilter relationshipFilter = (RelationshipFilter) entityFilter;
      if (relationshipFilter.isForeignKeyOnSelectTable()) {
        return relationshipFilterFKSelectSql(relationshipFilter, tableAlias, idField);
      } else if (relationshipFilter.isForeignKeyOnFilterTable()) {
        return relationshipFilterFKFilterSql(relationshipFilter, tableAlias, idField);
      } else {
        return relationshipFilterIntermediateTableSql(relationshipFilter, tableAlias, idField);
      }
    } else if (entityFilter instanceof TextSearchFilter) {
      return textFilterSql((TextSearchFilter) entityFilter, tableAlias, idField);
    } else {
      throw new SystemException(
          "Unsupported filter type: " + entityFilter.getClass().getSimpleName());
    }
  }

  private String attributeFilterSql(
      AttributeFilter attributeFilter, String tableAlias, FieldPointer idField) {
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

  private String booleanAndOrFilterSql(
      BooleanAndOrFilter booleanAndOrFilter, String tableAlias, FieldPointer idField) {
    List<String> subFilterSqls =
        booleanAndOrFilter.getSubFilters().stream()
            .map(subFilter -> buildFilterSql(subFilter, tableAlias, idField))
            .collect(Collectors.toList());
    return SqlGeneration.booleanAndOrFilterSql(
        booleanAndOrFilter.getOperator(), subFilterSqls.toArray(new String[0]));
  }

  private String booleanNotFilterSql(
      BooleanNotFilter booleanNotFilter, String tableAlias, FieldPointer idField) {
    return SqlGeneration.booleanNotFilterSql(
        buildFilterSql(booleanNotFilter.getSubFilter(), tableAlias, idField));
  }

  private String textFilterSql(
      TextSearchFilter textSearchFilter, String tableAlias, FieldPointer idField) {
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

  private String hierarchyHasAncestorFilterSql(
      HierarchyHasAncestorFilter hierarchyHasAncestorFilter,
      String tableAlias,
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

  private String hierarchyHasParentFilterSql(
      HierarchyHasParentFilter hierarchyHasParentFilter, String tableAlias, FieldPointer idField) {
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

  private String hierarchyIsMemberFilterSql(
      HierarchyIsMemberFilter hierarchyIsMemberFilter, String tableAlias) {
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

  private String hierarchyIsRootFilterSql(
      HierarchyIsRootFilter hierarchyIsMemberFilter, String tableAlias) {
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

  private String relationshipFilterFKSelectSql(
      RelationshipFilter relationshipFilter, String tableAlias, FieldPointer idField) {
    Attribute foreignKeyAttribute =
        relationshipFilter
            .getRelationship()
            .getForeignKeyAttribute(relationshipFilter.getSelectEntity());
    FieldPointer foreignKeyField =
        underlay
            .getIndexSchema()
            .getEntityMain(relationshipFilter.getSelectEntity().getName())
            .getAttributeValueField(foreignKeyAttribute.getName());
    if (isFilterOnAttribute(
            relationshipFilter.getSubFilter(),
            relationshipFilter.getFilterEntity().getIdAttribute())
        && !relationshipFilter.hasGroupByFilter()) {
      // subFilter(idField=foreignKey)
      return buildFilterSql(relationshipFilter.getSubFilter(), tableAlias, foreignKeyField);
    } else {
      // foreignKey IN (SELECT id FROM filterEntity WHERE subFilter(idField=id) [GROUP BY
      // groupByAttr HAVING groupByOp groupByCount])
      ITEntityMain filterEntityTable =
          underlay.getIndexSchema().getEntityMain(relationshipFilter.getFilterEntity().getName());
      FieldPointer filterEntityIdField =
          filterEntityTable.getAttributeValueField(
              relationshipFilter.getFilterEntity().getIdAttribute().getName());
      String inSelectFilterSql =
          buildFilterSql(relationshipFilter.getSubFilter(), null, filterEntityIdField);
      if (relationshipFilter.hasGroupByFilter()) {
        FieldPointer groupByField =
            filterEntityTable.getAttributeValueField(
                relationshipFilter.getGroupByCountAttribute().getName());
        inSelectFilterSql +=
            ' '
                + SqlGeneration.havingSql(
                    relationshipFilter.getGroupByCountOperator(),
                    relationshipFilter.getGroupByCountValue(),
                    groupByField,
                    null,
                    sqlParams);
      }
      return inSelectFilterSql(
          foreignKeyField,
          tableAlias,
          filterEntityIdField,
          filterEntityTable.getTablePointer(),
          inSelectFilterSql,
          sqlParams);
    }
  }

  private String relationshipFilterFKFilterSql(
      RelationshipFilter relationshipFilter, String tableAlias, FieldPointer idField) {
    Attribute foreignKeyAttribute =
        relationshipFilter
            .getRelationship()
            .getForeignKeyAttribute(relationshipFilter.getFilterEntity());
    ITEntityMain filterEntityTable =
        underlay.getIndexSchema().getEntityMain(relationshipFilter.getFilterEntity().getName());
    FieldPointer foreignKeyField =
        filterEntityTable.getAttributeValueField(foreignKeyAttribute.getName());
    if (isFilterOnAttribute(relationshipFilter.getSubFilter(), foreignKeyAttribute)
        && !relationshipFilter.hasGroupByFilter()) {
      // subFilter(idField=foreignKey)
      return buildFilterSql(relationshipFilter.getSubFilter(), tableAlias, foreignKeyField);
    } else {
      // id IN (SELECT foreignKey FROM filterEntity WHERE subFilter(idField=id) [GROUP BY
      // groupByAttr HAVING groupByOp groupByCount])
      FieldPointer filterEntityIdField =
          filterEntityTable.getAttributeValueField(
              relationshipFilter.getFilterEntity().getIdAttribute().getName());
      String inSelectFilterSql =
          buildFilterSql(relationshipFilter.getSubFilter(), null, filterEntityIdField);
      if (relationshipFilter.hasGroupByFilter()) {
        FieldPointer groupByField =
            filterEntityTable.getAttributeValueField(
                relationshipFilter.getGroupByCountAttribute().getName());
        inSelectFilterSql +=
            ' '
                + SqlGeneration.havingSql(
                    relationshipFilter.getGroupByCountOperator(),
                    relationshipFilter.getGroupByCountValue(),
                    groupByField,
                    null,
                    sqlParams);
      }
      return inSelectFilterSql(
          idField,
          tableAlias,
          foreignKeyField,
          filterEntityTable.getTablePointer(),
          inSelectFilterSql,
          sqlParams);
    }
  }

  private String relationshipFilterIntermediateTableSql(
      RelationshipFilter relationshipFilter, String tableAlias, FieldPointer idField) {
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
    if (isFilterOnAttribute(
            relationshipFilter.getSubFilter(),
            relationshipFilter.getFilterEntity().getIdAttribute())
        && (!relationshipFilter.hasGroupByFilter()
            || relationshipFilter.getGroupByCountAttribute().isId())) {
      // id IN (SELECT selectId FROM intermediateTable WHERE subFilter(idField=filterId) [GROUP BY
      // groupByAttr HAVING groupByOp groupByCount])
      String subFilterSql = buildFilterSql(relationshipFilter.getSubFilter(), null, filterId);
      if (relationshipFilter.hasGroupByFilter()) {
        subFilterSql +=
            ' '
                + havingSql(
                    relationshipFilter.getGroupByCountOperator(),
                    relationshipFilter.getGroupByCountValue(),
                    filterId,
                    null,
                    sqlParams);
      }
      return inSelectFilterSql(
          idField, tableAlias, selectId, idPairsTable.getTablePointer(), subFilterSql, sqlParams);
    } else {
      // id IN (SELECT selectId FROM intermediateTable WHERE filterId IN (SELECT id FROM
      // filterEntity WHERE subFilter(idField=id) [GROUP BY groupByAttr HAVING groupByOp
      // groupByCount])
      ITEntityMain filterEntityTable =
          underlay.getIndexSchema().getEntityMain(relationshipFilter.getFilterEntity().getName());
      FieldPointer filterEntityIdField =
          filterEntityTable.getAttributeValueField(
              relationshipFilter.getFilterEntity().getIdAttribute().getName());
      String subFilterSql =
          buildFilterSql(relationshipFilter.getSubFilter(), null, filterEntityIdField);
      if (relationshipFilter.hasGroupByFilter()) {
        FieldPointer groupByField =
            filterEntityTable.getAttributeValueField(
                relationshipFilter.getGroupByCountAttribute().getName());
        subFilterSql +=
            ' '
                + havingSql(
                    relationshipFilter.getGroupByCountOperator(),
                    relationshipFilter.getGroupByCountValue(),
                    groupByField,
                    null,
                    sqlParams);
      }
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
              subFilterSql,
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
