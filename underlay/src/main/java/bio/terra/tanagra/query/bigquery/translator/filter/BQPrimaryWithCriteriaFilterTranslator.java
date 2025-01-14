package bio.terra.tanagra.query.bigquery.translator.filter;

import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.PrimaryWithCriteriaFilter;
import bio.terra.tanagra.api.filter.RelationshipFilter;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.SqlQueryField;
import bio.terra.tanagra.query.sql.translator.ApiFilterTranslator;
import bio.terra.tanagra.query.sql.translator.ApiTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Relationship;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BQPrimaryWithCriteriaFilterTranslator extends ApiFilterTranslator {
  private final PrimaryWithCriteriaFilter primaryWithCriteriaFilter;

  public BQPrimaryWithCriteriaFilterTranslator(
      ApiTranslator apiTranslator,
      PrimaryWithCriteriaFilter primaryWithCriteriaFilter,
      Map<Attribute, SqlField> attributeSwapFields) {
    super(apiTranslator, attributeSwapFields);
    this.primaryWithCriteriaFilter = primaryWithCriteriaFilter;
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias) {
    // Without GroupBy:
    // WHERE primary.id IN (
    //  SELECT primary_id FROM occurrence WHERE [FILTER ON criteria] AND [sub-filters]
    //  UNION ALL
    //  SELECT primary_id FROM occurrence WHERE [FILTER ON criteria] AND [sub-filters]
    //  UNION ALL
    //  ...
    // )

    // With GroupBy No GroupByAttributes:
    // WHERE primary.id IN (
    //  SELECT primary_id FROM (
    //    SELECT primary_id, group_by_fields FROM occurrence WHERE [FILTER ON criteria] AND
    // [sub-filters]
    //    UNION ALL
    //    SELECT primary_id, group_by_fields FROM occurrence WHERE [FILTER ON criteria] AND
    // [sub-filters]
    //    UNION ALL
    //    ...
    //    )
    //    GROUP BY primary_id
    //    HAVING COUNT(*) group_by_operator group_by_count_val
    // )

    // With GroupBy And GroupByAttributes:
    // WHERE primary.id IN (
    //  SELECT primary_id FROM (
    //    SELECT primary_id, group_by_fields FROM (
    //      SELECT primary_id, group_by_fields FROM occurrence WHERE [FILTER ON criteria] AND
    //        [sub-filters]
    //      UNION ALL
    //      SELECT primary_id, group_by_fields FROM occurrence WHERE [FILTER ON criteria] AND
    //        [sub-filters]
    //      UNION ALL
    //      ...
    //    )
    //    GROUP BY primary_id, group_by_fields
    //  )
    //  GROUP BY primary_id
    //  HAVING COUNT(*) group_by_operator group_by_count_val
    // )

    final String primaryIdFieldAlias = "primary_id";
    final String groupByFieldAliasPrefix = "group_by_";

    List<String> selectSqls = new ArrayList<>();
    CriteriaOccurrence criteriaOccurrence = primaryWithCriteriaFilter.getCriteriaOccurrence();
    primaryWithCriteriaFilter.getCriteriaOccurrence().getOccurrenceEntities().stream()
        .sorted(
            Comparator.comparing(
                Entity::getName)) // Sort by name so the generated SQL is deterministic.
        .forEach(
            occurrenceEntity -> {
              Relationship occurrencePrimaryRelationship =
                  criteriaOccurrence.getOccurrencePrimaryRelationship(occurrenceEntity.getName());
              if (!occurrencePrimaryRelationship.isForeignKeyAttribute(occurrenceEntity)) {
                throw new UnsupportedOperationException(
                    "Only criteria-occurrence entity groups that have the occurrence-primary relationship as a foreign key on the occurrence table are currently supported.");
              }
              ITEntityMain occurrenceEntityTable =
                  primaryWithCriteriaFilter
                      .getUnderlay()
                      .getIndexSchema()
                      .getEntityMain(occurrenceEntity.getName());
              Attribute fkAttributeToPrimary =
                  occurrencePrimaryRelationship.getForeignKeyAttribute(occurrenceEntity);
              SqlField fkPrimaryIdField =
                  occurrenceEntityTable.getAttributeValueField(fkAttributeToPrimary.getName());

              List<SqlQueryField> selectQueryFields = new ArrayList<>();
              selectQueryFields.add(SqlQueryField.of(fkPrimaryIdField, primaryIdFieldAlias));

              if (primaryWithCriteriaFilter.hasGroupByModifier()) {
                List<Attribute> groupByAttributes =
                    primaryWithCriteriaFilter.getGroupByAttributes(occurrenceEntity);
                for (int i = 0; i < groupByAttributes.size(); i++) {
                  SqlField groupByAttrField =
                      occurrenceEntityTable.getAttributeValueField(
                          groupByAttributes.get(i).getName());
                  selectQueryFields.add(
                      SqlQueryField.of(groupByAttrField, groupByFieldAliasPrefix + i));
                }
              }

              // FILTER ON occurrence: WHERE FILTER ON criteria
              RelationshipFilter occurrenceCriteriaFilter =
                  new RelationshipFilter(
                      primaryWithCriteriaFilter.getUnderlay(),
                      criteriaOccurrence,
                      occurrenceEntity,
                      criteriaOccurrence.getOccurrenceCriteriaRelationship(
                          occurrenceEntity.getName()),
                      primaryWithCriteriaFilter.getCriteriaSubFilter(),
                      null,
                      null,
                      null);

              EntityFilter allOccurrenceFilters;
              if (primaryWithCriteriaFilter.hasSubFilters(occurrenceEntity)) {
                // ALL FILTERS ON occurrence: WHERE [FILTER ON criteria] AND [sub-filters]
                List<EntityFilter> allOccurrenceSubFilters = new ArrayList<>();
                allOccurrenceSubFilters.add(occurrenceCriteriaFilter);
                allOccurrenceSubFilters.addAll(
                    primaryWithCriteriaFilter.getSubFilters(occurrenceEntity));
                allOccurrenceFilters =
                    new BooleanAndOrFilter(
                        BooleanAndOrFilter.LogicalOperator.AND, allOccurrenceSubFilters);

              } else {
                // ALL FILTERS ON occurrence: WHERE [FILTER ON criteria]
                allOccurrenceFilters = occurrenceCriteriaFilter;
              }

              selectSqls.add(
                  "SELECT "
                      + selectQueryFields.stream()
                          .map(SqlQueryField::renderForSelect)
                          .collect(Collectors.joining(","))
                      + " FROM "
                      + occurrenceEntityTable.getTablePointer().render()
                      + " WHERE "
                      + apiTranslator
                          .translator(allOccurrenceFilters, attributeSwapFields)
                          .buildSql(sqlParams, null));
            });

    ITEntityMain selectEntityTable =
        primaryWithCriteriaFilter
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(criteriaOccurrence.getPrimaryEntity().getName());
    Attribute selectIdAttribute = criteriaOccurrence.getPrimaryEntity().getIdAttribute();
    SqlField selectIdField =
        attributeSwapFields.containsKey(selectIdAttribute)
            ? attributeSwapFields.get(selectIdAttribute)
            : selectEntityTable.getAttributeValueField(selectIdAttribute.getName());

    if (!primaryWithCriteriaFilter.hasGroupByModifier()) {
      return SqlQueryField.of(selectIdField).renderForWhere(tableAlias)
          + " IN ("
          + String.join(" UNION ALL ", selectSqls)
          + ')';
    }

    if (!primaryWithCriteriaFilter.hasGroupByAttributes()) {
      return SqlQueryField.of(selectIdField).renderForWhere(tableAlias)
          + " IN (SELECT "
          + primaryIdFieldAlias
          + " FROM ("
          + String.join(" UNION ALL ", selectSqls)
          + ") GROUP BY "
          + primaryIdFieldAlias
          + " HAVING COUNT(*) "
          + apiTranslator.binaryOperatorSql(primaryWithCriteriaFilter.getGroupByCountOperator())
          + " @"
          + sqlParams.addParam(
              "groupByCountValue",
              Literal.forInt64(Long.valueOf(primaryWithCriteriaFilter.getGroupByCountValue())))
          + ')';
    }

    List<String> groupByFieldsSql = new ArrayList<>();
    for (int i = 0; i < primaryWithCriteriaFilter.getNumGroupByAttributes(); i++) {
      groupByFieldsSql.add(groupByFieldAliasPrefix + i);
    }
    String groupByFieldsSqlJoined = String.join(", ", groupByFieldsSql);
    return SqlQueryField.of(selectIdField).renderForWhere(tableAlias)
        + " IN (SELECT "
        + primaryIdFieldAlias
        + " FROM (SELECT "
        + primaryIdFieldAlias
        + ", "
        + groupByFieldsSqlJoined
        + " FROM ("
        + String.join(" UNION ALL ", selectSqls)
        + ") GROUP BY "
        + primaryIdFieldAlias
        + ", "
        + groupByFieldsSqlJoined
        + ") GROUP BY "
        + primaryIdFieldAlias
        + " HAVING COUNT(*) "
        + apiTranslator.binaryOperatorSql(primaryWithCriteriaFilter.getGroupByCountOperator())
        + " @"
        + sqlParams.addParam(
            "groupByCountValue",
            Literal.forInt64(Long.valueOf(primaryWithCriteriaFilter.getGroupByCountValue())))
        + ')';
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return attribute.isId();
  }
}
