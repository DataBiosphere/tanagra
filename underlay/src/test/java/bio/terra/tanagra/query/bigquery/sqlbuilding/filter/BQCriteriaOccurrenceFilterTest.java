package bio.terra.tanagra.query.bigquery.sqlbuilding.filter;

import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.HierarchyHasAncestorFilter;
import bio.terra.tanagra.api.filter.OccurrenceForPrimaryFilter;
import bio.terra.tanagra.api.filter.PrimaryWithCriteriaFilter;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.api.query.list.ListQueryResult;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.NaryOperator;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.query.bigquery.BQRunnerTest;
import bio.terra.tanagra.query.bigquery.BQTable;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class BQCriteriaOccurrenceFilterTest extends BQRunnerTest {
  @Test
  void primaryWithCriteriaFilterSingleOccurrence() throws IOException {
    CriteriaOccurrence criteriaOccurrence =
        (CriteriaOccurrence) underlay.getEntityGroup("conditionPerson");

    // Only filter on criteria ids.
    EntityFilter criteriaSubFilter =
        new HierarchyHasAncestorFilter(
            underlay,
            criteriaOccurrence.getCriteriaEntity(),
            criteriaOccurrence.getCriteriaEntity().getHierarchy(Hierarchy.DEFAULT_NAME),
            List.of(Literal.forInt64(201_826L), Literal.forInt64(201_254L)));
    PrimaryWithCriteriaFilter primaryWithCriteriaFilter =
        new PrimaryWithCriteriaFilter(
            underlay, criteriaOccurrence, criteriaSubFilter, null, null, null, null);
    AttributeField simpleAttribute =
        new AttributeField(
            underlay,
            underlay.getPrimaryEntity(),
            underlay.getPrimaryEntity().getIdAttribute(),
            false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                primaryWithCriteriaFilter,
                null,
                null));

    List<BQTable> tableNamesToSubstitute = new ArrayList<>();
    criteriaOccurrence
        .getOccurrenceEntities()
        .forEach(
            occurrenceEntity ->
                tableNamesToSubstitute.add(
                    underlay
                        .getIndexSchema()
                        .getEntityMain(occurrenceEntity.getName())
                        .getTablePointer()));
    BQTable primaryEntityTable =
        underlay
            .getIndexSchema()
            .getEntityMain(underlay.getPrimaryEntity().getName())
            .getTablePointer();
    BQTable criteriaEntityTable =
        underlay
            .getIndexSchema()
            .getEntityMain(criteriaOccurrence.getCriteriaEntity().getName())
            .getTablePointer();
    BQTable criteriaAncestorDescendantTable =
        underlay
            .getIndexSchema()
            .getHierarchyAncestorDescendant(
                criteriaOccurrence.getCriteriaEntity().getName(), Hierarchy.DEFAULT_NAME)
            .getTablePointer();
    tableNamesToSubstitute.add(primaryEntityTable);
    tableNamesToSubstitute.add(criteriaEntityTable);
    tableNamesToSubstitute.add(criteriaAncestorDescendantTable);
    assertSqlMatchesWithTableNameOnly(
        "primaryWithCriteriaFilterSingleOccOnlyCriteriaIds",
        listQueryResult.getSql(),
        tableNamesToSubstitute.toArray(new BQTable[0]));

    // Filter on criteria ids + attribute subfilters.
    Entity conditionOccurrence = criteriaOccurrence.getOccurrenceEntities().get(0);
    criteriaSubFilter =
        new AttributeFilter(
            underlay,
            criteriaOccurrence.getCriteriaEntity(),
            criteriaOccurrence.getCriteriaEntity().getIdAttribute(),
            BinaryOperator.EQUALS,
            Literal.forInt64(201_826L));
    AttributeFilter ageAtOccurrenceFilter =
        new AttributeFilter(
            underlay,
            conditionOccurrence,
            conditionOccurrence.getAttribute("age_at_occurrence"),
            NaryOperator.BETWEEN,
            List.of(Literal.forInt64(45L), Literal.forInt64(65L)));
    primaryWithCriteriaFilter =
        new PrimaryWithCriteriaFilter(
            underlay,
            criteriaOccurrence,
            criteriaSubFilter,
            Map.of(conditionOccurrence, List.of(ageAtOccurrenceFilter)),
            null,
            null,
            null);
    listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                primaryWithCriteriaFilter,
                null,
                null));
    assertSqlMatchesWithTableNameOnly(
        "primaryWithCriteriaFilterSingleOccAttributeSubfilter",
        listQueryResult.getSql(),
        tableNamesToSubstitute.toArray(new BQTable[0]));
  }

  @Test
  void primaryWithCriteriaFilterMultipleOccurrences() throws IOException {
    CriteriaOccurrence criteriaOccurrence =
        (CriteriaOccurrence) underlay.getEntityGroup("icd9cmPerson");

    // Only filter on criteria ids.
    EntityFilter criteriaSubFilter =
        new AttributeFilter(
            underlay,
            criteriaOccurrence.getCriteriaEntity(),
            criteriaOccurrence.getCriteriaEntity().getIdAttribute(),
            NaryOperator.IN,
            List.of(Literal.forInt64(44_833_365L), Literal.forInt64(44_832_370L)));
    PrimaryWithCriteriaFilter primaryWithCriteriaFilter =
        new PrimaryWithCriteriaFilter(
            underlay, criteriaOccurrence, criteriaSubFilter, null, null, null, null);
    AttributeField simpleAttribute =
        new AttributeField(
            underlay,
            underlay.getPrimaryEntity(),
            underlay.getPrimaryEntity().getIdAttribute(),
            false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                primaryWithCriteriaFilter,
                null,
                null));

    List<BQTable> tableNamesToSubstitute = new ArrayList<>();
    criteriaOccurrence
        .getOccurrenceEntities()
        .forEach(
            occurrenceEntity ->
                tableNamesToSubstitute.add(
                    underlay
                        .getIndexSchema()
                        .getEntityMain(occurrenceEntity.getName())
                        .getTablePointer()));
    BQTable primaryEntityTable =
        underlay
            .getIndexSchema()
            .getEntityMain(underlay.getPrimaryEntity().getName())
            .getTablePointer();
    BQTable criteriaEntityTable =
        underlay
            .getIndexSchema()
            .getEntityMain(criteriaOccurrence.getCriteriaEntity().getName())
            .getTablePointer();
    BQTable criteriaAncestorDescendantTable =
        underlay
            .getIndexSchema()
            .getHierarchyAncestorDescendant(
                criteriaOccurrence.getCriteriaEntity().getName(), Hierarchy.DEFAULT_NAME)
            .getTablePointer();
    tableNamesToSubstitute.add(primaryEntityTable);
    tableNamesToSubstitute.add(criteriaEntityTable);
    tableNamesToSubstitute.add(criteriaAncestorDescendantTable);
    assertSqlMatchesWithTableNameOnly(
        "primaryWithCriteriaFilterMultipleOccOnlyCriteriaIds",
        listQueryResult.getSql(),
        tableNamesToSubstitute.toArray(new BQTable[0]));

    // Filter on criteria ids + attribute subfilters.
    Map<Entity, List<EntityFilter>> subFiltersPerOccurrenceEntity = new HashMap<>();
    criteriaOccurrence
        .getOccurrenceEntities()
        .forEach(
            occurrenceEntity -> {
              AttributeFilter ageAtOccurrenceFilter =
                  new AttributeFilter(
                      underlay,
                      occurrenceEntity,
                      occurrenceEntity.getAttribute("age_at_occurrence"),
                      NaryOperator.BETWEEN,
                      List.of(Literal.forInt64(45L), Literal.forInt64(65L)));
              subFiltersPerOccurrenceEntity.put(occurrenceEntity, List.of(ageAtOccurrenceFilter));
            });
    criteriaSubFilter =
        new HierarchyHasAncestorFilter(
            underlay,
            criteriaOccurrence.getCriteriaEntity(),
            criteriaOccurrence.getCriteriaEntity().getHierarchy(Hierarchy.DEFAULT_NAME),
            Literal.forInt64(44_833_365L));
    primaryWithCriteriaFilter =
        new PrimaryWithCriteriaFilter(
            underlay,
            criteriaOccurrence,
            criteriaSubFilter,
            subFiltersPerOccurrenceEntity,
            null,
            null,
            null);
    listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                primaryWithCriteriaFilter,
                null,
                null));
    assertSqlMatchesWithTableNameOnly(
        "primaryWithCriteriaFilterMultipleOccAttributeSubfilter",
        listQueryResult.getSql(),
        tableNamesToSubstitute.toArray(new BQTable[0]));
  }

  @Test
  void primaryWithCriteriaFilterSingleOccurrenceWithGroupBy() throws IOException {
    CriteriaOccurrence criteriaOccurrence =
        (CriteriaOccurrence) underlay.getEntityGroup("conditionPerson");

    // Only filter on criteria ids, no group by attributes.
    EntityFilter criteriaSubFilter =
        new AttributeFilter(
            underlay,
            criteriaOccurrence.getCriteriaEntity(),
            criteriaOccurrence.getCriteriaEntity().getIdAttribute(),
            NaryOperator.IN,
            List.of(Literal.forInt64(201_826L), Literal.forInt64(201_254L)));
    PrimaryWithCriteriaFilter primaryWithCriteriaFilter =
        new PrimaryWithCriteriaFilter(
            underlay,
            criteriaOccurrence,
            criteriaSubFilter,
            null,
            null,
            BinaryOperator.GREATER_THAN,
            2);
    AttributeField simpleAttribute =
        new AttributeField(
            underlay,
            underlay.getPrimaryEntity(),
            underlay.getPrimaryEntity().getIdAttribute(),
            false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                primaryWithCriteriaFilter,
                null,
                null));

    List<BQTable> tableNamesToSubstitute = new ArrayList<>();
    criteriaOccurrence
        .getOccurrenceEntities()
        .forEach(
            occurrenceEntity ->
                tableNamesToSubstitute.add(
                    underlay
                        .getIndexSchema()
                        .getEntityMain(occurrenceEntity.getName())
                        .getTablePointer()));
    BQTable primaryEntityTable =
        underlay
            .getIndexSchema()
            .getEntityMain(underlay.getPrimaryEntity().getName())
            .getTablePointer();
    BQTable criteriaEntityTable =
        underlay
            .getIndexSchema()
            .getEntityMain(criteriaOccurrence.getCriteriaEntity().getName())
            .getTablePointer();
    BQTable criteriaAncestorDescendantTable =
        underlay
            .getIndexSchema()
            .getHierarchyAncestorDescendant(
                criteriaOccurrence.getCriteriaEntity().getName(), Hierarchy.DEFAULT_NAME)
            .getTablePointer();
    tableNamesToSubstitute.add(primaryEntityTable);
    tableNamesToSubstitute.add(criteriaEntityTable);
    tableNamesToSubstitute.add(criteriaAncestorDescendantTable);
    assertSqlMatchesWithTableNameOnly(
        "primaryWithCriteriaFilterGroupBySingleOccOnlyCriteriaIds",
        listQueryResult.getSql(),
        tableNamesToSubstitute.toArray(new BQTable[0]));

    // Filter on criteria ids + attribute subfilters, with group by attributes.
    criteriaSubFilter =
        new HierarchyHasAncestorFilter(
            underlay,
            criteriaOccurrence.getCriteriaEntity(),
            criteriaOccurrence.getCriteriaEntity().getHierarchy(Hierarchy.DEFAULT_NAME),
            Literal.forInt64(201_826L));
    Entity conditionOccurrence = criteriaOccurrence.getOccurrenceEntities().get(0);
    AttributeFilter ageAtOccurrenceFilter =
        new AttributeFilter(
            underlay,
            conditionOccurrence,
            conditionOccurrence.getAttribute("age_at_occurrence"),
            NaryOperator.BETWEEN,
            List.of(Literal.forInt64(45L), Literal.forInt64(65L)));
    primaryWithCriteriaFilter =
        new PrimaryWithCriteriaFilter(
            underlay,
            criteriaOccurrence,
            criteriaSubFilter,
            Map.of(conditionOccurrence, List.of(ageAtOccurrenceFilter)),
            Map.of(
                conditionOccurrence,
                List.of(
                    conditionOccurrence.getAttribute("start_date"),
                    conditionOccurrence.getAttribute("condition"))),
            BinaryOperator.EQUALS,
            4);
    listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                primaryWithCriteriaFilter,
                null,
                null));
    assertSqlMatchesWithTableNameOnly(
        "primaryWithCriteriaFilterGroupBySingleOccAttributeSubfilter",
        listQueryResult.getSql(),
        tableNamesToSubstitute.toArray(new BQTable[0]));
  }

  @Test
  void primaryWithCriteriaFilterMultipleOccurrencesWithGroupBy() throws IOException {
    CriteriaOccurrence criteriaOccurrence =
        (CriteriaOccurrence) underlay.getEntityGroup("icd9cmPerson");

    // Only filter on criteria ids, with group by attributes.
    EntityFilter criteriaSubFilter =
        new HierarchyHasAncestorFilter(
            underlay,
            criteriaOccurrence.getCriteriaEntity(),
            criteriaOccurrence.getCriteriaEntity().getHierarchy(Hierarchy.DEFAULT_NAME),
            List.of(Literal.forInt64(44_833_365L), Literal.forInt64(44_832_370L)));
    Entity conditionOccurrence = underlay.getEntity("conditionOccurrence");
    Entity observationOccurrence = underlay.getEntity("observationOccurrence");
    Entity procedureOccurrence = underlay.getEntity("procedureOccurrence");
    Map<Entity, List<Attribute>> groupByAttributesPerOccurrenceEntity =
        Map.of(
            conditionOccurrence,
            List.of(
                conditionOccurrence.getAttribute("start_date"),
                conditionOccurrence.getAttribute("source_criteria_id")),
            observationOccurrence,
            List.of(
                observationOccurrence.getAttribute("date"),
                observationOccurrence.getAttribute("source_criteria_id")),
            procedureOccurrence,
            List.of(
                procedureOccurrence.getAttribute("date"),
                procedureOccurrence.getAttribute("source_criteria_id")));
    PrimaryWithCriteriaFilter primaryWithCriteriaFilter =
        new PrimaryWithCriteriaFilter(
            underlay,
            criteriaOccurrence,
            criteriaSubFilter,
            null,
            groupByAttributesPerOccurrenceEntity,
            BinaryOperator.GREATER_THAN_OR_EQUAL,
            11);
    AttributeField simpleAttribute =
        new AttributeField(
            underlay,
            underlay.getPrimaryEntity(),
            underlay.getPrimaryEntity().getIdAttribute(),
            false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                primaryWithCriteriaFilter,
                null,
                null));

    List<BQTable> tableNamesToSubstitute = new ArrayList<>();
    criteriaOccurrence
        .getOccurrenceEntities()
        .forEach(
            occurrenceEntity ->
                tableNamesToSubstitute.add(
                    underlay
                        .getIndexSchema()
                        .getEntityMain(occurrenceEntity.getName())
                        .getTablePointer()));
    BQTable primaryEntityTable =
        underlay
            .getIndexSchema()
            .getEntityMain(underlay.getPrimaryEntity().getName())
            .getTablePointer();
    BQTable criteriaEntityTable =
        underlay
            .getIndexSchema()
            .getEntityMain(criteriaOccurrence.getCriteriaEntity().getName())
            .getTablePointer();
    BQTable criteriaAncestorDescendantTable =
        underlay
            .getIndexSchema()
            .getHierarchyAncestorDescendant(
                criteriaOccurrence.getCriteriaEntity().getName(), Hierarchy.DEFAULT_NAME)
            .getTablePointer();
    tableNamesToSubstitute.add(primaryEntityTable);
    tableNamesToSubstitute.add(criteriaEntityTable);
    tableNamesToSubstitute.add(criteriaAncestorDescendantTable);
    assertSqlMatchesWithTableNameOnly(
        "primaryWithCriteriaFilterGroupByMultipleOccOnlyCriteriaIds",
        listQueryResult.getSql(),
        tableNamesToSubstitute.toArray(new BQTable[0]));

    // Filter on criteria ids + attribute subfilters, no group by attributes.
    criteriaSubFilter =
        new AttributeFilter(
            underlay,
            criteriaOccurrence.getCriteriaEntity(),
            criteriaOccurrence.getCriteriaEntity().getIdAttribute(),
            BinaryOperator.EQUALS,
            Literal.forInt64(44_833_365L));
    Map<Entity, List<EntityFilter>> subFiltersPerOccurrenceEntity = new HashMap<>();
    criteriaOccurrence
        .getOccurrenceEntities()
        .forEach(
            occurrenceEntity -> {
              AttributeFilter ageAtOccurrenceFilter =
                  new AttributeFilter(
                      underlay,
                      occurrenceEntity,
                      occurrenceEntity.getAttribute("age_at_occurrence"),
                      NaryOperator.BETWEEN,
                      List.of(Literal.forInt64(45L), Literal.forInt64(65L)));
              subFiltersPerOccurrenceEntity.put(occurrenceEntity, List.of(ageAtOccurrenceFilter));
            });
    primaryWithCriteriaFilter =
        new PrimaryWithCriteriaFilter(
            underlay,
            criteriaOccurrence,
            criteriaSubFilter,
            subFiltersPerOccurrenceEntity,
            null,
            BinaryOperator.LESS_THAN,
            14);
    listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                primaryWithCriteriaFilter,
                null,
                null));
    assertSqlMatchesWithTableNameOnly(
        "primaryWithCriteriaFilterGroupByMultipleOccAttributeSubfilter",
        listQueryResult.getSql(),
        tableNamesToSubstitute.toArray(new BQTable[0]));
  }

  @Test
  void occurrenceForPrimaryFilter() throws IOException {
    CriteriaOccurrence criteriaOccurrence =
        (CriteriaOccurrence) underlay.getEntityGroup("conditionPerson");
    Entity conditionOccurrence = criteriaOccurrence.getOccurrenceEntities().get(0);

    // Only primary entity sub-filter.
    EntityFilter conditionFilter =
        new HierarchyHasAncestorFilter(
            underlay,
            criteriaOccurrence.getCriteriaEntity(),
            criteriaOccurrence.getCriteriaEntity().getHierarchy(Hierarchy.DEFAULT_NAME),
            List.of(Literal.forInt64(201_826L), Literal.forInt64(201_254L)));
    PrimaryWithCriteriaFilter primaryWithCriteriaFilter =
        new PrimaryWithCriteriaFilter(
            underlay, criteriaOccurrence, conditionFilter, null, null, null, null);
    AttributeFilter attributeFilter =
        new AttributeFilter(
            underlay,
            underlay.getPrimaryEntity(),
            underlay.getPrimaryEntity().getAttribute("gender"),
            BinaryOperator.EQUALS,
            Literal.forInt64(8_532L));
    BooleanAndOrFilter personFilter =
        new BooleanAndOrFilter(
            BooleanAndOrFilter.LogicalOperator.AND,
            List.of(primaryWithCriteriaFilter, attributeFilter));
    OccurrenceForPrimaryFilter occurrenceForPrimaryFilter =
        new OccurrenceForPrimaryFilter(
            underlay, criteriaOccurrence, conditionOccurrence, personFilter, null);
    AttributeField simpleAttribute =
        new AttributeField(
            underlay, conditionOccurrence, conditionOccurrence.getIdAttribute(), false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                conditionOccurrence,
                List.of(simpleAttribute),
                occurrenceForPrimaryFilter,
                null,
                null));

    List<BQTable> tableNamesToSubstitute = new ArrayList<>();
    criteriaOccurrence
        .getOccurrenceEntities()
        .forEach(
            occurrenceEntity ->
                tableNamesToSubstitute.add(
                    underlay
                        .getIndexSchema()
                        .getEntityMain(occurrenceEntity.getName())
                        .getTablePointer()));
    BQTable primaryEntityTable =
        underlay
            .getIndexSchema()
            .getEntityMain(underlay.getPrimaryEntity().getName())
            .getTablePointer();
    BQTable criteriaEntityTable =
        underlay
            .getIndexSchema()
            .getEntityMain(criteriaOccurrence.getCriteriaEntity().getName())
            .getTablePointer();
    BQTable criteriaAncestorDescendantTable =
        underlay
            .getIndexSchema()
            .getHierarchyAncestorDescendant(
                criteriaOccurrence.getCriteriaEntity().getName(), Hierarchy.DEFAULT_NAME)
            .getTablePointer();
    tableNamesToSubstitute.add(primaryEntityTable);
    tableNamesToSubstitute.add(criteriaEntityTable);
    tableNamesToSubstitute.add(criteriaAncestorDescendantTable);
    assertSqlMatchesWithTableNameOnly(
        "occurrenceForPrimaryFilterWithPrimarySubFilterOnly",
        listQueryResult.getSql(),
        tableNamesToSubstitute.toArray(new BQTable[0]));

    // Only criteria entity sub-filter.
    occurrenceForPrimaryFilter =
        new OccurrenceForPrimaryFilter(
            underlay, criteriaOccurrence, conditionOccurrence, null, conditionFilter);
    listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                conditionOccurrence,
                List.of(simpleAttribute),
                occurrenceForPrimaryFilter,
                null,
                null));
    assertSqlMatchesWithTableNameOnly(
        "occurrenceForPrimaryFilterWithCriteriaSubFilterOnly",
        listQueryResult.getSql(),
        tableNamesToSubstitute.toArray(new BQTable[0]));

    // Both primary and criteria entity sub-filters.
    occurrenceForPrimaryFilter =
        new OccurrenceForPrimaryFilter(
            underlay, criteriaOccurrence, conditionOccurrence, personFilter, conditionFilter);
    listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                conditionOccurrence,
                List.of(simpleAttribute),
                occurrenceForPrimaryFilter,
                null,
                null));
    assertSqlMatchesWithTableNameOnly(
        "occurrenceForPrimaryFilterWithBothSubFilters",
        listQueryResult.getSql(),
        tableNamesToSubstitute.toArray(new BQTable[0]));

    // Neither sub-filter.
    OccurrenceForPrimaryFilter invalidOccurrenceForPrimaryFilter =
        new OccurrenceForPrimaryFilter(
            underlay, criteriaOccurrence, conditionOccurrence, null, null);
    assertThrows(
        InvalidQueryException.class,
        () ->
            bqQueryRunner.run(
                ListQueryRequest.dryRunAgainstIndexData(
                    underlay,
                    conditionOccurrence,
                    List.of(simpleAttribute),
                    invalidOccurrenceForPrimaryFilter,
                    null,
                    null)));
  }
}
