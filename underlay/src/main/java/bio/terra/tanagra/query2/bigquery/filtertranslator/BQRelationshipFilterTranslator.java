package bio.terra.tanagra.query2.bigquery.filtertranslator;

import bio.terra.tanagra.api.filter.RelationshipFilter;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.query2.sql.ApiFilterTranslator;
import bio.terra.tanagra.query2.sql.ApiTranslator;
import bio.terra.tanagra.query2.sql.SqlField;
import bio.terra.tanagra.query2.sql.SqlParams;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.indextable.ITRelationshipIdPairs;
import java.util.ArrayList;
import java.util.List;

public class BQRelationshipFilterTranslator extends ApiFilterTranslator {
  private final RelationshipFilter relationshipFilter;

  public BQRelationshipFilterTranslator(
      ApiTranslator apiTranslator, RelationshipFilter relationshipFilter) {
    super(apiTranslator);
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
    SqlField foreignKeyField =
        relationshipFilter
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(relationshipFilter.getSelectEntity().getName())
            .getAttributeValueField(foreignKeyAttribute.getName());
    if (apiTranslator
            .translator(relationshipFilter.getSubFilter())
            .isFilterOnAttribute(relationshipFilter.getFilterEntity().getIdAttribute())
        && !relationshipFilter.hasGroupByFilter()) {
      // subFilter(idField=foreignKey)
      return apiTranslator
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
      SqlField filterEntityIdField =
          filterEntityTable.getAttributeValueField(
              relationshipFilter.getFilterEntity().getIdAttribute().getName());
      String inSelectFilterSql =
          apiTranslator.translator(relationshipFilter.getSubFilter()).buildSql(sqlParams, null);
      if (relationshipFilter.hasGroupByFilter()) {
        throw new InvalidQueryException(
            "A having clause is unsupported for relationships where the foreign key is on the selected entity table.");
      }
      return apiTranslator.inSelectFilterSql(
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
    SqlField foreignKeyField =
        filterEntityTable.getAttributeValueField(foreignKeyAttribute.getName());
    ITEntityMain selectEntityTable =
        relationshipFilter
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(relationshipFilter.getSelectEntity().getName());
    SqlField selectIdField =
        selectEntityTable.getAttributeValueField(
            relationshipFilter.getSelectEntity().getIdAttribute().getName());
    if (apiTranslator
            .translator(relationshipFilter.getSubFilter())
            .isFilterOnAttribute(foreignKeyAttribute)
        && !relationshipFilter.hasGroupByFilter()) {
      // subFilter(idField=foreignKey)
      return apiTranslator
          .translator(relationshipFilter.getSubFilter())
          .swapAttributeField(foreignKeyAttribute, selectIdField)
          .buildSql(sqlParams, tableAlias);
    } else {
      // id IN (SELECT foreignKey FROM filterEntity WHERE subFilter(idField=id) [GROUP BY
      // foreignKey, groupByAttr HAVING groupByOp groupByCount])
      String inSelectFilterSql =
          apiTranslator.translator(relationshipFilter.getSubFilter()).buildSql(sqlParams, null);
      if (relationshipFilter.hasGroupByFilter()) {
        List<SqlField> groupByFields = new ArrayList<>();
        groupByFields.add(foreignKeyField);
        if (relationshipFilter.hasGroupByCountAttribute()) {
          groupByFields.add(
              filterEntityTable.getAttributeValueField(
                  relationshipFilter.getGroupByCountAttribute().getName()));
        }
        inSelectFilterSql +=
            ' '
                + apiTranslator.havingSql(
                    relationshipFilter.getGroupByCountOperator(),
                    relationshipFilter.getGroupByCountValue(),
                    groupByFields,
                    null,
                    sqlParams);
      }
      return apiTranslator.inSelectFilterSql(
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
    SqlField selectIdField =
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
    SqlField selectIdIntTable =
        idPairsTable.getEntityIdField(relationshipFilter.getSelectEntity().getName());
    SqlField filterIdIntTable =
        idPairsTable.getEntityIdField(relationshipFilter.getFilterEntity().getName());
    if (apiTranslator
            .translator(relationshipFilter.getSubFilter())
            .isFilterOnAttribute(relationshipFilter.getFilterEntity().getIdAttribute())
        && (!relationshipFilter.hasGroupByFilter()
            || !relationshipFilter.hasGroupByCountAttribute())) {
      // id IN (SELECT selectId FROM intermediateTable WHERE subFilter(idField=filterId) [GROUP BY
      // selectId HAVING groupByOp groupByCount])
      String subFilterSql =
          apiTranslator
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
                + apiTranslator.havingSql(
                    relationshipFilter.getGroupByCountOperator(),
                    relationshipFilter.getGroupByCountValue(),
                    List.of(selectIdIntTable),
                    null,
                    sqlParams);
      }
      return apiTranslator.inSelectFilterSql(
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
      SqlField filterEntityIdField =
          filterEntityTable.getAttributeValueField(
              relationshipFilter.getFilterEntity().getIdAttribute().getName());
      String subFilterSql =
          apiTranslator.translator(relationshipFilter.getSubFilter()).buildSql(sqlParams, null);
      String havingSql = "";
      if (relationshipFilter.hasGroupByFilter()) {
        if (relationshipFilter.hasGroupByCountAttribute()) {
          throw new InvalidQueryException(
              "An additional group by attribute is unsupported for relationships that use an intermediate table.");
        }
        havingSql =
            ' '
                + apiTranslator.havingSql(
                    relationshipFilter.getGroupByCountOperator(),
                    relationshipFilter.getGroupByCountValue(),
                    List.of(selectIdIntTable),
                    null,
                    sqlParams);
      }
      return apiTranslator.inSelectFilterSql(
          selectIdField,
          tableAlias,
          selectIdIntTable,
          idPairsTable.getTablePointer(),
          apiTranslator.inSelectFilterSql(
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
