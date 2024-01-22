package bio.terra.tanagra.query.bigquery.translator.filter;

import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.PrimaryWithCriteriaFilter;
import bio.terra.tanagra.api.filter.RelationshipFilter;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.NaryOperator;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.SqlQueryField;
import bio.terra.tanagra.query.sql.translator.ApiFilterTranslator;
import bio.terra.tanagra.query.sql.translator.ApiTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Relationship;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BQPrimaryWithCriteriaFilterTranslator extends ApiFilterTranslator {
  private final PrimaryWithCriteriaFilter primaryWithCriteriaFilter;

  public BQPrimaryWithCriteriaFilterTranslator(
      ApiTranslator apiTranslator, PrimaryWithCriteriaFilter primaryWithCriteriaFilter) {
    super(apiTranslator);
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

    // With GroupBy:
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
    //    GROUP BY primary_id, group_by_fields
    //    HAVING COUNT(*) group_by_operator group_by_count_val
    // )

    final String primaryIdFieldAlias = "primary_id";
    final String groupByFieldAliasPrefix = "group_by_";

    List<String> selectSqls = new ArrayList<>();
    CriteriaOccurrence criteriaOccurrence = primaryWithCriteriaFilter.getCriteriaOccurrence();
    primaryWithCriteriaFilter.getCriteriaOccurrence().getOccurrenceEntities().stream()
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

              AttributeFilter criteriaFilter;
              if (primaryWithCriteriaFilter.getCriteriaIds().size() == 1) {
                // FILTER ON criteria: WHERE criteria.id = 123
                criteriaFilter =
                    new AttributeFilter(
                        primaryWithCriteriaFilter.getUnderlay(),
                        criteriaOccurrence.getCriteriaEntity(),
                        criteriaOccurrence.getCriteriaEntity().getIdAttribute(),
                        BinaryOperator.EQUALS,
                        primaryWithCriteriaFilter.getCriteriaIds().get(0));
              } else {
                // FILTER ON criteria: WHERE criteria.id IN (123, 456)
                criteriaFilter =
                    new AttributeFilter(
                        primaryWithCriteriaFilter.getUnderlay(),
                        criteriaOccurrence.getCriteriaEntity(),
                        criteriaOccurrence.getCriteriaEntity().getIdAttribute(),
                        NaryOperator.IN,
                        primaryWithCriteriaFilter.getCriteriaIds());
              }

              // FILTER ON occurrence: WHERE FILTER ON criteria
              RelationshipFilter occurrenceCriteriaFilter =
                  new RelationshipFilter(
                      primaryWithCriteriaFilter.getUnderlay(),
                      criteriaOccurrence,
                      occurrenceEntity,
                      criteriaOccurrence.getOccurrenceCriteriaRelationship(
                          occurrenceEntity.getName()),
                      criteriaFilter,
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
                          .map(selectQueryField -> selectQueryField.renderForSelect())
                          .collect(Collectors.joining(","))
                      + " FROM "
                      + occurrenceEntityTable.getTablePointer().render()
                      + " WHERE "
                      + apiTranslator.translator(allOccurrenceFilters).buildSql(sqlParams, null));
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

    if (primaryWithCriteriaFilter.hasGroupByModifier()) {
      List<String> groupByFields = new ArrayList<>();
      groupByFields.add(primaryIdFieldAlias);
      for (int i = 0; i < primaryWithCriteriaFilter.getNumGroupByAttributes(); i++) {
        groupByFields.add(groupByFieldAliasPrefix + i);
      }

      return SqlQueryField.of(selectIdField).renderForWhere(tableAlias)
          + " IN (SELECT "
          + primaryIdFieldAlias
          + " FROM ("
          + selectSqls.stream().collect(Collectors.joining(" UNION ALL "))
          + ") GROUP BY "
          + groupByFields.stream().collect(Collectors.joining(","))
          + " HAVING COUNT(*) "
          + apiTranslator.binaryOperatorSql(primaryWithCriteriaFilter.getGroupByCountOperator())
          + " @"
          + sqlParams.addParam(
              "groupByCountValue",
              Literal.forInt64(Long.valueOf(primaryWithCriteriaFilter.getGroupByCountValue())))
          + ')';
    } else {
      return SqlQueryField.of(selectIdField).renderForWhere(tableAlias)
          + " IN ("
          + selectSqls.stream().collect(Collectors.joining(" UNION ALL "))
          + ')';
    }
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return attribute.isId();
  }
}
