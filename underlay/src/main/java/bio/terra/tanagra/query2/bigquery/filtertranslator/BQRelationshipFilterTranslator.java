package bio.terra.tanagra.query2.bigquery.filtertranslator;

import static bio.terra.tanagra.query2.sql.SqlGeneration.havingSql;
import static bio.terra.tanagra.query2.sql.SqlGeneration.inSelectFilterSql;

import bio.terra.tanagra.api.filter.RelationshipFilter;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query2.bigquery.BQTranslator;
import bio.terra.tanagra.query2.sql.SqlFilterTranslator;
import bio.terra.tanagra.query2.sql.SqlGeneration;
import bio.terra.tanagra.query2.sql.SqlParams;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.indextable.ITRelationshipIdPairs;

public class BQRelationshipFilterTranslator implements SqlFilterTranslator {
  private final RelationshipFilter relationshipFilter;

  public BQRelationshipFilterTranslator(RelationshipFilter relationshipFilter) {
    this.relationshipFilter = relationshipFilter;
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias, FieldPointer idField) {
    if (relationshipFilter.isForeignKeyOnSelectTable()) {
      return relationshipFilterFKSelectSql(sqlParams, tableAlias);
    } else if (relationshipFilter.isForeignKeyOnFilterTable()) {
      return relationshipFilterFKFilterSql(sqlParams, tableAlias, idField);
    } else {
      return relationshipFilterIntermediateTableSql(sqlParams, tableAlias, idField);
    }
  }

  private String relationshipFilterFKSelectSql(SqlParams sqlParams, String tableAlias) {
    Attribute foreignKeyAttribute =
        relationshipFilter
            .getRelationship()
            .getForeignKeyAttribute(relationshipFilter.getSelectEntity());
    FieldPointer foreignKeyField =
        relationshipFilter
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(relationshipFilter.getSelectEntity().getName())
            .getAttributeValueField(foreignKeyAttribute.getName());
    if (BQTranslator.translator(relationshipFilter.getSubFilter())
            .isFilterOnAttribute(relationshipFilter.getFilterEntity().getIdAttribute())
        && !relationshipFilter.hasGroupByFilter()) {
      // subFilter(idField=foreignKey)
      return BQTranslator.translator(relationshipFilter.getSubFilter())
          .buildSql(sqlParams, tableAlias, foreignKeyField);
    } else {
      // foreignKey IN (SELECT id FROM filterEntity WHERE subFilter(idField=id) [GROUP BY
      // groupByAttr HAVING groupByOp groupByCount])
      ITEntityMain filterEntityTable =
          relationshipFilter
              .getUnderlay()
              .getIndexSchema()
              .getEntityMain(relationshipFilter.getFilterEntity().getName());
      FieldPointer filterEntityIdField =
          filterEntityTable.getAttributeValueField(
              relationshipFilter.getFilterEntity().getIdAttribute().getName());
      String inSelectFilterSql =
          BQTranslator.translator(relationshipFilter.getSubFilter())
              .buildSql(sqlParams, null, filterEntityIdField);
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
      SqlParams sqlParams, String tableAlias, FieldPointer idField) {
    Attribute foreignKeyAttribute =
        relationshipFilter
            .getRelationship()
            .getForeignKeyAttribute(relationshipFilter.getFilterEntity());
    ITEntityMain filterEntityTable =
        relationshipFilter
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(relationshipFilter.getFilterEntity().getName());
    FieldPointer foreignKeyField =
        filterEntityTable.getAttributeValueField(foreignKeyAttribute.getName());
    if (BQTranslator.translator(relationshipFilter.getSubFilter())
            .isFilterOnAttribute(foreignKeyAttribute)
        && !relationshipFilter.hasGroupByFilter()) {
      // subFilter(idField=foreignKey)
      return BQTranslator.translator(relationshipFilter.getSubFilter())
          .buildSql(sqlParams, tableAlias, foreignKeyField);
    } else {
      // id IN (SELECT foreignKey FROM filterEntity WHERE subFilter(idField=id) [GROUP BY
      // groupByAttr HAVING groupByOp groupByCount])
      FieldPointer filterEntityIdField =
          filterEntityTable.getAttributeValueField(
              relationshipFilter.getFilterEntity().getIdAttribute().getName());
      String inSelectFilterSql =
          BQTranslator.translator(relationshipFilter.getSubFilter())
              .buildSql(sqlParams, null, filterEntityIdField);
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
      SqlParams sqlParams, String tableAlias, FieldPointer idField) {
    ITRelationshipIdPairs idPairsTable =
        relationshipFilter
            .getUnderlay()
            .getIndexSchema()
            .getRelationshipIdPairs(
                relationshipFilter.getEntityGroup().getName(),
                relationshipFilter.getRelationship().getEntityA().getName(),
                relationshipFilter.getRelationship().getEntityB().getName());
    FieldPointer selectId =
        idPairsTable.getEntityIdField(relationshipFilter.getSelectEntity().getName());
    FieldPointer filterId =
        idPairsTable.getEntityIdField(relationshipFilter.getFilterEntity().getName());
    if (BQTranslator.translator(relationshipFilter.getSubFilter())
            .isFilterOnAttribute(relationshipFilter.getFilterEntity().getIdAttribute())
        && (!relationshipFilter.hasGroupByFilter()
            || relationshipFilter.getGroupByCountAttribute().isId())) {
      // id IN (SELECT selectId FROM intermediateTable WHERE subFilter(idField=filterId) [GROUP BY
      // groupByAttr HAVING groupByOp groupByCount])
      String subFilterSql =
          BQTranslator.translator(relationshipFilter.getSubFilter())
              .buildSql(sqlParams, null, filterId);
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
          relationshipFilter
              .getUnderlay()
              .getIndexSchema()
              .getEntityMain(relationshipFilter.getFilterEntity().getName());
      FieldPointer filterEntityIdField =
          filterEntityTable.getAttributeValueField(
              relationshipFilter.getFilterEntity().getIdAttribute().getName());
      String subFilterSql =
          BQTranslator.translator(relationshipFilter.getSubFilter())
              .buildSql(sqlParams, null, filterEntityIdField);
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

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return attribute.isId();
  }
}
