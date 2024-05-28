package bio.terra.tanagra.query.bigquery.translator.filter;

import bio.terra.tanagra.api.filter.TemporalPrimaryFilter;
import bio.terra.tanagra.api.shared.JoinOperator;
import bio.terra.tanagra.api.shared.ReducingOperator;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.filterbuilder.EntityOutput;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.SqlQueryField;
import bio.terra.tanagra.query.sql.translator.ApiFilterTranslator;
import bio.terra.tanagra.query.sql.translator.ApiTranslator;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Relationship;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.EntityGroup;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;

public class BQTemporalPrimaryFilterTranslator extends ApiFilterTranslator {
  private final TemporalPrimaryFilter temporalPrimaryFilter;

  public BQTemporalPrimaryFilterTranslator(
      ApiTranslator apiTranslator, TemporalPrimaryFilter temporalPrimaryFilter) {
    super(apiTranslator);
    this.temporalPrimaryFilter = temporalPrimaryFilter;
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias) {
    // Get the join fields.
    Map<EntityOutput, List<SqlQueryField>> firstConditionJoinFields = new HashMap<>();
    temporalPrimaryFilter
        .getFirstCondition()
        .forEach(
            entityOutput ->
                firstConditionJoinFields.put(
                    entityOutput,
                    getJoinFields(
                        temporalPrimaryFilter.getUnderlay(),
                        entityOutput.getEntity(),
                        temporalPrimaryFilter.getJoinOperator())));
    Map<EntityOutput, List<SqlQueryField>> secondConditionJoinFields = new HashMap<>();
    temporalPrimaryFilter
        .getSecondCondition()
        .forEach(
            entityOutput ->
                secondConditionJoinFields.put(
                    entityOutput,
                    getJoinFields(
                        temporalPrimaryFilter.getUnderlay(),
                        entityOutput.getEntity(),
                        temporalPrimaryFilter.getJoinOperator())));

    // Build the temp table queries for the first and second conditions.
    String firstConditionQuery =
        buildQueryForCondition(
            temporalPrimaryFilter.getUnderlay(),
            temporalPrimaryFilter.getFirstConditionQualifier(),
            firstConditionJoinFields,
            sqlParams);
    String secondConditionQuery =
        buildQueryForCondition(
            temporalPrimaryFilter.getUnderlay(),
            temporalPrimaryFilter.getSecondConditionQualifier(),
            secondConditionJoinFields,
            sqlParams);

    // Join the temp tables for the first and second conditions.
    String joinQuery =
        buildQueryJoiningConditions(
            List.of(SqlField.of(PRIMARY_ENTITY_ID_ALIAS)),
            List.of(),
            firstConditionQuery,
            secondConditionQuery,
            temporalPrimaryFilter.getJoinOperator(),
            temporalPrimaryFilter.getJoinOperatorValue());

    // Wrap in a WHERE clause.
    // [WHERE] primaryEntity.id IN (join query)
    ITEntityMain selectEntityTable =
        temporalPrimaryFilter
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(temporalPrimaryFilter.getUnderlay().getPrimaryEntity().getName());
    Attribute selectIdAttribute =
        temporalPrimaryFilter.getUnderlay().getPrimaryEntity().getIdAttribute();
    SqlField selectIdField =
        attributeSwapFields.containsKey(selectIdAttribute)
            ? attributeSwapFields.get(selectIdAttribute)
            : selectEntityTable.getAttributeValueField(selectIdAttribute.getName());
    return SqlQueryField.of(selectIdField).renderForWhere(tableAlias) + " IN (" + joinQuery + ')';
  }

  private String buildQueryForCondition(
      Underlay underlay,
      @Nullable ReducingOperator reducingOperator,
      Map<EntityOutput, List<SqlQueryField>> joinFields,
      SqlParams sqlParams) {
    // SELECT all the fields needed for the JOIN from each entity output.
    // SELECT primary_id AS primaryEntityId, visit_date AS visitDate,
    //   [RANK() OVER (PARTITION BY primaryEntityId ORDER BY date ASC) AS orderRank]
    // FROM entity [WHERE filter]
    boolean isSingleOccurrence = joinFields.size() == 1;
    List<String> subSelectSqls = new ArrayList<>();
    List<String> selectFieldsFromUnionSql = new ArrayList<>();
    joinFields.forEach(
        (entityOutput, sqlQueryFields) -> {
          ITEntityMain indexTable =
              underlay.getIndexSchema().getEntityMain(entityOutput.getEntity().getName());

          List<String> selectFields = new ArrayList<>();
          sqlQueryFields.stream().map(SqlQueryField::renderForSelect).forEach(selectFields::add);
          if (isSingleOccurrence && reducingOperator != null) {
            SqlQueryField primaryEntityIdField =
                sqlQueryFields.stream()
                    .filter(
                        sqlQueryField -> sqlQueryField.getAlias().equals(PRIMARY_ENTITY_ID_ALIAS))
                    .findFirst()
                    .get();
            SqlQueryField visitDateField =
                sqlQueryFields.stream()
                    .filter(sqlQueryField -> sqlQueryField.getAlias().equals(VISIT_DATE_ALIAS))
                    .findFirst()
                    .get();
            selectFields.add(
                "RANK() OVER (PARTITION BY "
                    + SqlQueryField.of(primaryEntityIdField.getField()).renderForSelect()
                    + " ORDER BY "
                    + SqlQueryField.of(visitDateField.getField()).renderForSelect()
                    + (ReducingOperator.FIRST_MENTION_OF.equals(reducingOperator)
                        ? " ASC"
                        : " DESC")
                    + ") AS "
                    + ORDER_RANK_ALIAS);
          }

          String subSelectSql =
              "SELECT "
                  + selectFields.stream().collect(Collectors.joining(","))
                  + " FROM"
                  + indexTable.getTablePointer().render();
          if (entityOutput.hasDataFeatureFilter()) {
            subSelectSql +=
                " WHERE "
                    + apiTranslator
                        .translator(entityOutput.getDataFeatureFilter())
                        .buildSql(sqlParams, null);
          }
          subSelectSqls.add(subSelectSql);
          if (selectFieldsFromUnionSql.isEmpty()) {
            sqlQueryFields.stream()
                .map(SqlQueryField::getAlias)
                .forEach(selectFieldsFromUnionSql::add);
          }
        });

    // UNION together the SELECT statements for each entity output.
    String unionSql = subSelectSqls.stream().collect(Collectors.joining(" UNION ALL "));

    // SELECT the JOIN fields from the UNION statement.
    if (reducingOperator == null) {
      // ... UNION ALL ...
      return unionSql;
    }

    String innerSelectFromUnionSql;
    if (isSingleOccurrence) {
      innerSelectFromUnionSql = unionSql;
    } else {
      // SELECT includedAttributes,
      //   RANK() OVER (PARTITION BY primaryEntityId ORDER BY date ASC) AS orderRank
      // FROM (... UNION ALL ...)
      String orderRankField =
          "RANK() OVER (PARTITION BY "
              + PRIMARY_ENTITY_ID_ALIAS
              + " ORDER BY "
              + VISIT_DATE_ALIAS
              + (ReducingOperator.FIRST_MENTION_OF.equals(reducingOperator) ? " ASC" : " DESC")
              + ") AS "
              + ORDER_RANK_ALIAS;
      selectFieldsFromUnionSql.add(orderRankField);
      innerSelectFromUnionSql =
          "SELECT "
              + selectFieldsFromUnionSql.stream().collect(Collectors.joining(", "))
              + " FROM ("
              + unionSql
              + ")";
    }

    // Wrap to filter on orderRank.
    // SELECT * FROM (above query) WHERE orderRank=1
    return "SELECT * FROM (" + innerSelectFromUnionSql + ") WHERE " + ORDER_RANK_ALIAS + " = 1";
  }

  private static String buildQueryJoiningConditions(
      List<SqlField> firstConditionSelectFields,
      List<SqlField> secondConditionSelectFields,
      String firstConditionQuery,
      String secondConditionQuery,
      JoinOperator joinOperator,
      @Nullable Integer joinOperatorValue) {
    final String firstConditionAlias = "firstCondition";
    final String secondConditionAlias = "secondCondition";
    List<String> allSelectFields = new ArrayList<>();
    firstConditionSelectFields.forEach(
        sqlField ->
            allSelectFields.add(SqlQueryField.of(sqlField).renderForSelect(firstConditionAlias)));
    secondConditionSelectFields.forEach(
        sqlField ->
            allSelectFields.add(SqlQueryField.of(sqlField).renderForSelect(secondConditionAlias)));

    // SELECT firstCondition.primaryEntityId
    // FROM (temp table query 1) AS firstCondition
    // JOIN (temp table query 2) AS secondCondition
    //   ON secondCondition.primaryEntityId = firstCondition.primaryEntityId
    //   [AND secondCondition.visitId = firstCondition.visitId]
    //   [AND secondCondition.date >= firstCondition + operatorValue]
    String joinSql =
        "SELECT "
            + allSelectFields.stream().collect(Collectors.joining(", "))
            + " FROM ("
            + firstConditionQuery
            + ") AS "
            + firstConditionAlias
            + " JOIN ("
            + secondConditionQuery
            + ") AS "
            + secondConditionAlias
            + " ON ";

    SqlQueryField primaryEntityId = SqlQueryField.of(SqlField.of(PRIMARY_ENTITY_ID_ALIAS));
    SqlQueryField visitDate = SqlQueryField.of(SqlField.of(VISIT_DATE_ALIAS));
    SqlQueryField visitOccurrenceId = SqlQueryField.of(SqlField.of(VISIT_OCCURRENCE_ID_ALIAS));
    switch (joinOperator) {
      case DURING_SAME_ENCOUNTER:
        return joinSql
            + primaryEntityId.renderForSelect(firstConditionAlias)
            + " = "
            + primaryEntityId.renderForSelect(secondConditionAlias)
            + " AND "
            + visitDate.renderForSelect(firstConditionAlias)
            + " = "
            + visitDate.renderForSelect(secondConditionAlias)
            + " AND "
            + visitOccurrenceId.renderForSelect(firstConditionAlias)
            + " = "
            + visitOccurrenceId.renderForSelect(secondConditionAlias);
      case NUM_DAYS_BEFORE:
        // e.g. firstCondition >=2 days before secondCondition.
        // --> secondCondition - firstCondition >= 2.
        return joinSql
            + primaryEntityId.renderForSelect(firstConditionAlias)
            + " = "
            + primaryEntityId.renderForSelect(secondConditionAlias)
            + " AND TIMESTAMP_DIFF("
            + visitDate.renderForSelect(secondConditionAlias)
            + ", "
            + visitDate.renderForSelect(firstConditionAlias)
            + ", DAY) >= "
            + joinOperatorValue;
      case NUM_DAYS_AFTER:
        // e.g. firstCondition >=3 days after secondCondition.
        // --> firstCondition - secondCondition >= 3
        return joinSql
            + primaryEntityId.renderForSelect(firstConditionAlias)
            + " = "
            + primaryEntityId.renderForSelect(secondConditionAlias)
            + " AND TIMESTAMP_DIFF("
            + visitDate.renderForSelect(firstConditionAlias)
            + ", "
            + visitDate.renderForSelect(secondConditionAlias)
            + ", DAY) >= "
            + joinOperatorValue;
      case WITHIN_NUM_DAYS:
        // e.g. firstCondition within 4 days of secondCondition.
        // --> abs(firstCondition - secondCondition) <= 4
        return joinSql
            + primaryEntityId.renderForSelect(firstConditionAlias)
            + " = "
            + primaryEntityId.renderForSelect(secondConditionAlias)
            + " AND ABS(TIMESTAMP_DIFF("
            + visitDate.renderForSelect(firstConditionAlias)
            + ", "
            + visitDate.renderForSelect(secondConditionAlias)
            + ", DAY)) <= "
            + joinOperatorValue;
      default:
        throw new SystemException("Unsupported JoinOperator: " + joinOperator);
    }
  }

  private static List<SqlQueryField> getJoinFields(
      Underlay underlay, Entity entity, JoinOperator joinOperator) {
    switch (joinOperator) {
      case DURING_SAME_ENCOUNTER:
        return List.of(
            getJoinFieldPrimaryEntityId(underlay, entity),
            getJoinFieldVisitDate(underlay, entity),
            getJoinFieldVisitOccurrenceId(underlay, entity));
      case NUM_DAYS_BEFORE:
      case NUM_DAYS_AFTER:
      case WITHIN_NUM_DAYS:
        return List.of(
            getJoinFieldPrimaryEntityId(underlay, entity), getJoinFieldVisitDate(underlay, entity));
      default:
        throw new SystemException("Unsupported JoinOperator: " + joinOperator);
    }
  }

  private static final String PRIMARY_ENTITY_ID_ALIAS = "primaryEntityId";
  private static final String VISIT_DATE_ALIAS = "visitDate";
  private static final String VISIT_OCCURRENCE_ID_ALIAS = "visitOccurrenceId";
  private static final String ORDER_RANK_ALIAS = "orderRank";

  private static SqlQueryField getJoinFieldPrimaryEntityId(Underlay underlay, Entity entity) {
    if (entity.isPrimary()) {
      return getJoinField(underlay, entity, entity.getIdAttribute(), PRIMARY_ENTITY_ID_ALIAS);
    } else {
      Pair<EntityGroup, Relationship> relationshipToPrimary =
          underlay.getRelationship(entity, underlay.getPrimaryEntity());
      if (!relationshipToPrimary.getRight().isForeignKeyAttribute(entity)) {
        throw new InvalidQueryException(
            "Only output entities with a foreign key relationship to the primary entity support temporal queries: "
                + entity.getName());
      }
      return getJoinField(
          underlay,
          entity,
          relationshipToPrimary.getRight().getForeignKeyAttribute(entity),
          PRIMARY_ENTITY_ID_ALIAS);
    }
  }

  private static SqlQueryField getJoinFieldVisitDate(Underlay underlay, Entity entity) {
    return getJoinField(
        underlay, entity, entity.getVisitDateAttributeForTemporalQuery(), VISIT_DATE_ALIAS);
  }

  private static SqlQueryField getJoinFieldVisitOccurrenceId(Underlay underlay, Entity entity) {
    return getJoinField(
        underlay, entity, entity.getVisitIdAttributeForTemporalQuery(), VISIT_OCCURRENCE_ID_ALIAS);
  }

  private static SqlQueryField getJoinField(
      Underlay underlay, Entity entity, Attribute attribute, String alias) {
    ITEntityMain indexMain = underlay.getIndexSchema().getEntityMain(entity.getName());
    return SqlQueryField.of(indexMain.getAttributeValueField(attribute.getName()), alias);
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return attribute.isId();
  }
}
