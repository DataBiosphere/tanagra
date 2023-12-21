package bio.terra.tanagra.query2.bigquery.filtertranslator;

import bio.terra.tanagra.api.filter.RelationshipFilter;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query2.sql.SqlFilterTranslator;
import bio.terra.tanagra.query2.sql.SqlParams;
import bio.terra.tanagra.query2.sql.SqlTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.indextable.ITRelationshipIdPairs;
import java.util.ArrayList;
import java.util.List;

public class BQRelationshipFilterTranslator extends SqlFilterTranslator {
  private final RelationshipFilter relationshipFilter;

  public BQRelationshipFilterTranslator(
      SqlTranslator sqlTranslator, RelationshipFilter relationshipFilter) {
    super(sqlTranslator);
    this.relationshipFilter = relationshipFilter;
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias) {
    if (relationshipFilter.isForeignKeyOnSelectTable()) {
      return foreignKeyOnSelectEntity(sqlParams, tableAlias);
    } else if (relationshipFilter.isForeignKeyOnFilterTable()) {
      return foreignKeyOnFilterEntity(sqlParams, tableAlias);
    } else {
      return intermediateTable(sqlParams, tableAlias);
    }
  }

  private String foreignKeyOnSelectEntity(SqlParams sqlParams, String tableAlias) {
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
    if (sqlTranslator
            .translator(relationshipFilter.getSubFilter())
            .isFilterOnAttribute(relationshipFilter.getFilterEntity().getIdAttribute())
        && !relationshipFilter.hasGroupByFilter()) {
      // subFilter(idField=foreignKey)
      return sqlTranslator
          .translator(relationshipFilter.getSubFilter())
          .swapAttributeField(
              relationshipFilter.getFilterEntity().getIdAttribute(), foreignKeyField)
          .buildSql(sqlParams, tableAlias);
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
          sqlTranslator.translator(relationshipFilter.getSubFilter()).buildSql(sqlParams, null);
      if (relationshipFilter.hasGroupByFilter()) {
        throw new InvalidQueryException(
            "A having clause is unsupported for relationships where the foreign key is on the selected entity table.");
      }
      return sqlTranslator.inSelectFilterSql(
          foreignKeyField,
          tableAlias,
          filterEntityIdField,
          filterEntityTable.getTablePointer(),
          inSelectFilterSql,
          sqlParams);
    }
  }

  private String foreignKeyOnFilterEntity(SqlParams sqlParams, String tableAlias) {
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
    ITEntityMain selectEntityTable =
        relationshipFilter
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(relationshipFilter.getSelectEntity().getName());
    FieldPointer selectIdField =
        selectEntityTable.getAttributeValueField(
            relationshipFilter.getSelectEntity().getIdAttribute().getName());
    if (sqlTranslator
            .translator(relationshipFilter.getSubFilter())
            .isFilterOnAttribute(foreignKeyAttribute)
        && !relationshipFilter.hasGroupByFilter()) {
      // subFilter(idField=foreignKey)
      return sqlTranslator
          .translator(relationshipFilter.getSubFilter())
          .swapAttributeField(foreignKeyAttribute, selectIdField)
          .buildSql(sqlParams, tableAlias);
    } else {
      // id IN (SELECT foreignKey FROM filterEntity WHERE subFilter(idField=id) [GROUP BY
      // foreignKey, groupByAttr HAVING groupByOp groupByCount])
      String inSelectFilterSql =
          sqlTranslator.translator(relationshipFilter.getSubFilter()).buildSql(sqlParams, null);
      if (relationshipFilter.hasGroupByFilter()) {
        List<FieldPointer> groupByFields = new ArrayList<>();
        groupByFields.add(foreignKeyField);
        if (relationshipFilter.hasGroupByCountAttribute()) {
          groupByFields.add(
              filterEntityTable.getAttributeValueField(
                  relationshipFilter.getGroupByCountAttribute().getName()));
        }
        inSelectFilterSql +=
            ' '
                + sqlTranslator.havingSql(
                    relationshipFilter.getGroupByCountOperator(),
                    relationshipFilter.getGroupByCountValue(),
                    groupByFields,
                    null,
                    sqlParams);
      }
      return sqlTranslator.inSelectFilterSql(
          selectIdField,
          tableAlias,
          foreignKeyField,
          filterEntityTable.getTablePointer(),
          inSelectFilterSql,
          sqlParams);
    }
  }

  private String intermediateTable(SqlParams sqlParams, String tableAlias) {
    ITEntityMain selectEntityTable =
        relationshipFilter
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(relationshipFilter.getSelectEntity().getName());
    FieldPointer selectIdField =
        selectEntityTable.getAttributeValueField(
            relationshipFilter.getSelectEntity().getIdAttribute().getName());
    ITRelationshipIdPairs idPairsTable =
        relationshipFilter
            .getUnderlay()
            .getIndexSchema()
            .getRelationshipIdPairs(
                relationshipFilter.getEntityGroup().getName(),
                relationshipFilter.getRelationship().getEntityA().getName(),
                relationshipFilter.getRelationship().getEntityB().getName());
    FieldPointer selectIdIntTable =
        idPairsTable.getEntityIdField(relationshipFilter.getSelectEntity().getName());
    FieldPointer filterIdIntTable =
        idPairsTable.getEntityIdField(relationshipFilter.getFilterEntity().getName());
    if (sqlTranslator
            .translator(relationshipFilter.getSubFilter())
            .isFilterOnAttribute(relationshipFilter.getFilterEntity().getIdAttribute())
        && (!relationshipFilter.hasGroupByFilter()
            || !relationshipFilter.hasGroupByCountAttribute())) {
      // id IN (SELECT selectId FROM intermediateTable WHERE subFilter(idField=filterId) [GROUP BY
      // selectId HAVING groupByOp groupByCount])
      String subFilterSql =
          sqlTranslator
              .translator(relationshipFilter.getSubFilter())
              .swapAttributeField(
                  relationshipFilter.getFilterEntity().getIdAttribute(), filterIdIntTable)
              .buildSql(sqlParams, null);
      if (relationshipFilter.hasGroupByFilter()) {
        if (relationshipFilter.hasGroupByCountAttribute()) {
          throw new InvalidQueryException(
              "An additional group by attribute is unsupported for relationships that use an intermediate table.");
        }
        subFilterSql +=
            ' '
                + sqlTranslator.havingSql(
                    relationshipFilter.getGroupByCountOperator(),
                    relationshipFilter.getGroupByCountValue(),
                    List.of(selectIdIntTable),
                    null,
                    sqlParams);
      }
      return sqlTranslator.inSelectFilterSql(
          selectIdField,
          tableAlias,
          selectIdIntTable,
          idPairsTable.getTablePointer(),
          subFilterSql,
          sqlParams);
    } else {
      // id IN (SELECT selectId FROM intermediateTable WHERE filterId IN (SELECT id FROM
      // filterEntity WHERE subFilter(idField=id)) [GROUP BY selectId HAVING groupByOp
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
          sqlTranslator.translator(relationshipFilter.getSubFilter()).buildSql(sqlParams, null);
      String havingSql = "";
      if (relationshipFilter.hasGroupByFilter()) {
        if (relationshipFilter.hasGroupByCountAttribute()) {
          throw new InvalidQueryException(
              "An additional group by attribute is unsupported for relationships that use an intermediate table.");
        }
        havingSql =
            ' '
                + sqlTranslator.havingSql(
                    relationshipFilter.getGroupByCountOperator(),
                    relationshipFilter.getGroupByCountValue(),
                    List.of(selectIdIntTable),
                    null,
                    sqlParams);
      }
      return sqlTranslator.inSelectFilterSql(
          selectIdField,
          tableAlias,
          selectIdIntTable,
          idPairsTable.getTablePointer(),
          sqlTranslator.inSelectFilterSql(
                  filterIdIntTable,
                  null,
                  filterEntityIdField,
                  filterEntityTable.getTablePointer(),
                  subFilterSql,
                  sqlParams)
              + havingSql,
          sqlParams);
    }
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return attribute.isId();
  }
}
