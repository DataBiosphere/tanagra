package bio.terra.tanagra.query.bigquery.translator.filter;

import bio.terra.tanagra.api.filter.RelationshipFilter;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.FunctionTemplate;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.translator.ApiFilterTranslator;
import bio.terra.tanagra.query.sql.translator.ApiTranslator;
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

    if (!relationshipFilter.hasSubFilter() && !relationshipFilter.hasGroupByFilter()) {
      // foreignKey IS NOT NULL
      return apiTranslator.functionFilterSql(
          foreignKeyField,
          apiTranslator.functionTemplateSql(FunctionTemplate.IS_NOT_NULL),
          List.of(),
          null,
          new SqlParams());
    } else if (apiTranslator
            .translator(relationshipFilter.getSubFilter())
            .isFilterOnAttribute(relationshipFilter.getFilterEntity().getIdAttribute())
        && !relationshipFilter.hasGroupByFilter()) {
      // subFilter(filterId=>foreignKey)
      return apiTranslator
          .translator(relationshipFilter.getSubFilter())
          .swapAttributeField(
              relationshipFilter.getFilterEntity().getIdAttribute(), foreignKeyField)
          .buildSql(sqlParams, tableAlias);
    } else {
      // foreignKey IN (SELECT id FROM filterEntity [WHERE subFilter] [GROUP BY
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
          relationshipFilter.hasSubFilter()
              ? apiTranslator
                  .translator(relationshipFilter.getSubFilter())
                  .buildSql(sqlParams, null)
              : null;
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
          null,
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
    Attribute selectIdAttribute = relationshipFilter.getSelectEntity().getIdAttribute();
    SqlField selectIdField =
        attributeSwapFields.containsKey(selectIdAttribute)
            ? attributeSwapFields.get(selectIdAttribute)
            : selectEntityTable.getAttributeValueField(selectIdAttribute.getName());

    if (!relationshipFilter.hasSubFilter()
        && !relationshipFilter.hasGroupByFilter()
        && relationshipFilter
            .getEntityGroup()
            .hasRollupCountField(
                relationshipFilter.getSelectEntity().getName(),
                relationshipFilter.getFilterEntity().getName())) {
      // rollupCount > 0
      SqlField selectRollupField =
          selectEntityTable.getEntityGroupCountField(
              relationshipFilter.getEntityGroup().getName(), null);
      return apiTranslator.binaryFilterSql(
          selectRollupField, BinaryOperator.GREATER_THAN, Literal.forInt64(0L), null, sqlParams);
    } else if (relationshipFilter.hasSubFilter()
        && apiTranslator
            .translator(relationshipFilter.getSubFilter())
            .isFilterOnAttribute(foreignKeyAttribute)
        && !relationshipFilter.hasGroupByFilter()) {
      // subFilter(foreignKey=selectId)
      return apiTranslator
          .translator(relationshipFilter.getSubFilter())
          .swapAttributeField(foreignKeyAttribute, selectIdField)
          .buildSql(sqlParams, tableAlias);
    } else {
      // id IN (SELECT foreignKey FROM filterEntity [WHERE subFilter] [GROUP BY
      // foreignKey, groupByAttr HAVING groupByOp groupByCount])
      String inSelectFilterSql = "";
      if (relationshipFilter.hasSubFilter()) {
        inSelectFilterSql =
            apiTranslator.translator(relationshipFilter.getSubFilter()).buildSql(sqlParams, null);
      }
      String inSelectHavingSql = "";
      if (relationshipFilter.hasGroupByFilter()) {
        List<SqlField> groupByFields = new ArrayList<>();
        groupByFields.add(foreignKeyField);
        if (relationshipFilter.hasGroupByCountAttribute()) {
          groupByFields.add(
              filterEntityTable.getAttributeValueField(
                  relationshipFilter.getGroupByCountAttribute().getName()));
        }
        inSelectHavingSql =
            apiTranslator.havingSql(
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
          inSelectFilterSql.isEmpty() ? null : inSelectFilterSql,
          inSelectHavingSql.isEmpty() ? null : inSelectHavingSql,
          sqlParams);
    }
  }

  private String intermediateTable(SqlParams sqlParams, String tableAlias) {
    ITEntityMain selectEntityTable =
        relationshipFilter
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(relationshipFilter.getSelectEntity().getName());
    Attribute selectIdAttribute = relationshipFilter.getSelectEntity().getIdAttribute();
    SqlField selectIdField =
        attributeSwapFields.containsKey(selectIdAttribute)
            ? attributeSwapFields.get(selectIdAttribute)
            : selectEntityTable.getAttributeValueField(selectIdAttribute.getName());
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

    if (!relationshipFilter.hasSubFilter()
        && !relationshipFilter.hasGroupByFilter()
        && relationshipFilter
            .getEntityGroup()
            .hasRollupCountField(
                relationshipFilter.getSelectEntity().getName(),
                relationshipFilter.getFilterEntity().getName())) {
      // rollupCount > 0
      SqlField selectRollupField =
          selectEntityTable.getEntityGroupCountField(
              relationshipFilter.getEntityGroup().getName(), null);
      return apiTranslator.binaryFilterSql(
          selectRollupField, BinaryOperator.GREATER_THAN, Literal.forInt64(0L), null, sqlParams);
    } else if (relationshipFilter.hasSubFilter()
        && apiTranslator
            .translator(relationshipFilter.getSubFilter())
            .isFilterOnAttribute(relationshipFilter.getFilterEntity().getIdAttribute())
        && (!relationshipFilter.hasGroupByFilter()
            || !relationshipFilter.hasGroupByCountAttribute())) {
      // id IN (SELECT selectId FROM intermediateTable WHERE subFilter(id->inttable field)
      // [GROUP BY selectId HAVING groupByOp groupByCount])
      String subFilterSql = "";
      if (relationshipFilter.hasSubFilter()) {
        subFilterSql =
            apiTranslator
                .translator(relationshipFilter.getSubFilter())
                .swapAttributeField(
                    relationshipFilter.getFilterEntity().getIdAttribute(), filterIdIntTable)
                .buildSql(sqlParams, null);
      }
      String havingSql = "";
      if (relationshipFilter.hasGroupByFilter()) {
        if (relationshipFilter.hasGroupByCountAttribute()) {
          throw new InvalidQueryException(
              "An additional group by attribute is unsupported for relationships that use an intermediate table.");
        }
        havingSql =
            apiTranslator.havingSql(
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
          subFilterSql.isEmpty() ? null : subFilterSql,
          havingSql.isEmpty() ? null : havingSql,
          sqlParams);
    } else {
      // id IN (SELECT selectId FROM intermediateTable [WHERE filterId IN (SELECT id FROM
      // filterEntity WHERE subFilter)] [GROUP BY selectId HAVING groupByOp
      // groupByCount])
      String filterIdInSelectSql = "";
      if (relationshipFilter.hasSubFilter()) {
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
        filterIdInSelectSql =
            apiTranslator.inSelectFilterSql(
                filterIdIntTable,
                null,
                filterEntityIdField,
                filterEntityTable.getTablePointer(),
                subFilterSql,
                null,
                sqlParams);
      }

      String havingSql = "";
      if (relationshipFilter.hasGroupByFilter()) {
        if (relationshipFilter.hasGroupByCountAttribute()) {
          throw new InvalidQueryException(
              "An additional group by attribute is unsupported for relationships that use an intermediate table.");
        }
        havingSql =
            apiTranslator.havingSql(
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
          filterIdInSelectSql.isEmpty() ? null : filterIdInSelectSql,
          havingSql.isEmpty() ? null : havingSql,
          sqlParams);
    }
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return attribute.isId();
  }
}
