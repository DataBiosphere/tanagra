package bio.terra.tanagra.query.bigquery.sqlbuilding.filter;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.HierarchyHasAncestorFilter;
import bio.terra.tanagra.api.filter.OccurrenceForPrimaryFilter;
import bio.terra.tanagra.api.filter.TemporalPrimaryFilter;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.api.query.list.ListQueryResult;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.JoinOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.ReducingOperator;
import bio.terra.tanagra.filterbuilder.EntityOutput;
import bio.terra.tanagra.query.bigquery.BQRunnerTest;
import bio.terra.tanagra.query.bigquery.BQTable;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

public class BQTemporalPrimaryFilterTest extends BQRunnerTest {
  @Test
  void temporalPrimaryFilterSingleOutput() throws IOException {
    Entity conditionOccurrence = underlay.getEntity("conditionOccurrence");
    CriteriaOccurrence conditionPerson =
        (CriteriaOccurrence) underlay.getEntityGroup("conditionPerson");

    EntityFilter criteriaSubFilterCondition1 =
        new HierarchyHasAncestorFilter(
            underlay,
            conditionPerson.getCriteriaEntity(),
            conditionPerson.getCriteriaEntity().getHierarchy(Hierarchy.DEFAULT_NAME),
            List.of(Literal.forInt64(201_826L)));
    OccurrenceForPrimaryFilter occurrenceFilterCondition1 =
        new OccurrenceForPrimaryFilter(
            underlay, conditionPerson, conditionOccurrence, null, criteriaSubFilterCondition1);
    List<EntityOutput> temporalCondition1 =
        List.of(EntityOutput.filtered(conditionOccurrence, occurrenceFilterCondition1));

    EntityFilter criteriaSubFilterCondition2 =
        new HierarchyHasAncestorFilter(
            underlay,
            conditionPerson.getCriteriaEntity(),
            conditionPerson.getCriteriaEntity().getHierarchy(Hierarchy.DEFAULT_NAME),
            List.of(Literal.forInt64(201_254L)));
    OccurrenceForPrimaryFilter occurrenceFilterCondition2 =
        new OccurrenceForPrimaryFilter(
            underlay, conditionPerson, conditionOccurrence, null, criteriaSubFilterCondition2);
    List<EntityOutput> temporalCondition2 =
        List.of(EntityOutput.filtered(conditionOccurrence, occurrenceFilterCondition2));

    Entity procedureOccurrence = underlay.getEntity("procedureOccurrence");
    CriteriaOccurrence procedurePerson =
        (CriteriaOccurrence) underlay.getEntityGroup("procedurePerson");

    EntityFilter criteriaSubFilterProcedure1 =
        new HierarchyHasAncestorFilter(
            underlay,
            procedurePerson.getCriteriaEntity(),
            procedurePerson.getCriteriaEntity().getHierarchy(Hierarchy.DEFAULT_NAME),
            List.of(Literal.forInt64(4_042_673L)));
    OccurrenceForPrimaryFilter occurrenceFilterProcedure1 =
        new OccurrenceForPrimaryFilter(
            underlay, procedurePerson, procedureOccurrence, null, criteriaSubFilterProcedure1);
    List<EntityOutput> temporalProcedure1 =
        List.of(EntityOutput.filtered(procedureOccurrence, occurrenceFilterProcedure1));

    EntityFilter criteriaSubFilterProcedure2 =
        new HierarchyHasAncestorFilter(
            underlay,
            procedurePerson.getCriteriaEntity(),
            procedurePerson.getCriteriaEntity().getHierarchy(Hierarchy.DEFAULT_NAME),
            List.of(Literal.forInt64(4_043_201L)));
    OccurrenceForPrimaryFilter occurrenceFilterProcedure2 =
        new OccurrenceForPrimaryFilter(
            underlay, procedurePerson, procedureOccurrence, null, criteriaSubFilterProcedure2);
    List<EntityOutput> temporalProcedure2 =
        List.of(EntityOutput.filtered(procedureOccurrence, occurrenceFilterProcedure2));

    // ANY, single output entity1 / ANY, single output entity1 / SAME_VISIT
    TemporalPrimaryFilter temporalPrimaryFilter =
        new TemporalPrimaryFilter(
            underlay,
            null,
            temporalCondition1,
            JoinOperator.DURING_SAME_ENCOUNTER,
            null,
            null,
            temporalCondition2);
    AttributeField simpleAttribute =
        new AttributeField(
            underlay,
            underlay.getPrimaryEntity(),
            underlay.getPrimaryEntity().getAttribute("id"),
            false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                temporalPrimaryFilter,
                null,
                null));
    BQTable personTable =
        underlay
            .getIndexSchema()
            .getEntityMain(underlay.getPrimaryEntity().getName())
            .getTablePointer();
    BQTable conditionOccurrenceTable =
        underlay.getIndexSchema().getEntityMain(conditionOccurrence.getName()).getTablePointer();
    BQTable conditionAncestorDescendantTable =
        underlay
            .getIndexSchema()
            .getHierarchyAncestorDescendant(
                conditionPerson.getCriteriaEntity().getName(), Hierarchy.DEFAULT_NAME)
            .getTablePointer();
    assertSqlMatchesWithTableNameOnly(
        "temporalPrimaryFilterSingleOutputAnyAnySameVisit",
        listQueryResult.getSql(),
        personTable,
        conditionOccurrenceTable,
        conditionAncestorDescendantTable);

    // FIRST, single output entity1 / FIRST, single output entity2 / NUM_DAYS_BEFORE
    temporalPrimaryFilter =
        new TemporalPrimaryFilter(
            underlay,
            ReducingOperator.FIRST_MENTION_OF,
            temporalCondition1,
            JoinOperator.NUM_DAYS_BEFORE,
            4,
            ReducingOperator.FIRST_MENTION_OF,
            temporalProcedure1);
    listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                temporalPrimaryFilter,
                null,
                null));
    BQTable procedureOccurrenceTable =
        underlay.getIndexSchema().getEntityMain(procedureOccurrence.getName()).getTablePointer();
    BQTable procedureAncestorDescendantTable =
        underlay
            .getIndexSchema()
            .getHierarchyAncestorDescendant(
                procedurePerson.getCriteriaEntity().getName(), Hierarchy.DEFAULT_NAME)
            .getTablePointer();
    assertSqlMatchesWithTableNameOnly(
        "temporalPrimaryFilterSingleOutputFirstFirstDaysBefore",
        listQueryResult.getSql(),
        personTable,
        conditionOccurrenceTable,
        conditionAncestorDescendantTable,
        procedureOccurrenceTable,
        procedureAncestorDescendantTable);

    // LAST, single output entity2 / LAST, single output entity1 / NUM_DAYS_AFTER
    temporalPrimaryFilter =
        new TemporalPrimaryFilter(
            underlay,
            ReducingOperator.LAST_MENTION_OF,
            temporalProcedure2,
            JoinOperator.NUM_DAYS_AFTER,
            2,
            ReducingOperator.LAST_MENTION_OF,
            temporalCondition2);
    listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                temporalPrimaryFilter,
                null,
                null));
    assertSqlMatchesWithTableNameOnly(
        "temporalPrimaryFilterSingleOutputLastLastDaysAfter",
        listQueryResult.getSql(),
        personTable,
        conditionOccurrenceTable,
        conditionAncestorDescendantTable,
        procedureOccurrenceTable,
        procedureAncestorDescendantTable);

    // FIRST, single output entity2 / LAST, single output, entity2 / WITHIN_NUM_DAYS
    temporalPrimaryFilter =
        new TemporalPrimaryFilter(
            underlay,
            ReducingOperator.FIRST_MENTION_OF,
            temporalProcedure1,
            JoinOperator.WITHIN_NUM_DAYS,
            3,
            ReducingOperator.LAST_MENTION_OF,
            temporalProcedure2);
    listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                temporalPrimaryFilter,
                null,
                null));
    assertSqlMatchesWithTableNameOnly(
        "temporalPrimaryFilterSingleOutputFirstLastWithinDays",
        listQueryResult.getSql(),
        personTable,
        procedureOccurrenceTable,
        procedureAncestorDescendantTable);
  }

  @Test
  void temporalPrimaryFilterSingleOutputAttrModifiers() throws IOException {
    Entity conditionOccurrence = underlay.getEntity("conditionOccurrence");
    CriteriaOccurrence conditionPerson =
        (CriteriaOccurrence) underlay.getEntityGroup("conditionPerson");
    EntityFilter criteriaSubFilterCondition1 =
        new HierarchyHasAncestorFilter(
            underlay,
            conditionPerson.getCriteriaEntity(),
            conditionPerson.getCriteriaEntity().getHierarchy(Hierarchy.DEFAULT_NAME),
            List.of(Literal.forInt64(201_826L)));
    OccurrenceForPrimaryFilter occurrenceFilterCondition1 =
        new OccurrenceForPrimaryFilter(
            underlay, conditionPerson, conditionOccurrence, null, criteriaSubFilterCondition1);
    AttributeFilter attrModifierFilterCondition1 =
        new AttributeFilter(
            underlay,
            conditionOccurrence,
            conditionOccurrence.getAttribute("age_at_occurrence"),
            BinaryOperator.EQUALS,
            Literal.forInt64(65L));
    List<EntityOutput> temporalCondition1 =
        List.of(
            EntityOutput.filtered(
                conditionOccurrence,
                new BooleanAndOrFilter(
                    BooleanAndOrFilter.LogicalOperator.AND,
                    List.of(occurrenceFilterCondition1, attrModifierFilterCondition1))));

    Entity procedureOccurrence = underlay.getEntity("procedureOccurrence");
    CriteriaOccurrence procedurePerson =
        (CriteriaOccurrence) underlay.getEntityGroup("procedurePerson");
    EntityFilter criteriaSubFilterProcedure1 =
        new HierarchyHasAncestorFilter(
            underlay,
            procedurePerson.getCriteriaEntity(),
            procedurePerson.getCriteriaEntity().getHierarchy(Hierarchy.DEFAULT_NAME),
            List.of(Literal.forInt64(4_042_673L)));
    OccurrenceForPrimaryFilter occurrenceFilterProcedure1 =
        new OccurrenceForPrimaryFilter(
            underlay, procedurePerson, procedureOccurrence, null, criteriaSubFilterProcedure1);
    AttributeFilter attrModifierFilterProcedure1 =
        new AttributeFilter(
            underlay,
            procedureOccurrence,
            procedureOccurrence.getAttribute("age_at_occurrence"),
            BinaryOperator.EQUALS,
            Literal.forInt64(80L));
    List<EntityOutput> temporalProcedure1 =
        List.of(
            EntityOutput.filtered(
                procedureOccurrence,
                new BooleanAndOrFilter(
                    BooleanAndOrFilter.LogicalOperator.AND,
                    List.of(occurrenceFilterProcedure1, attrModifierFilterProcedure1))));

    // FIRST, single output entity1 / FIRST, single output entity2 / NUM_DAYS_BEFORE
    TemporalPrimaryFilter temporalPrimaryFilter =
        new TemporalPrimaryFilter(
            underlay,
            ReducingOperator.FIRST_MENTION_OF,
            temporalCondition1,
            JoinOperator.NUM_DAYS_BEFORE,
            4,
            ReducingOperator.FIRST_MENTION_OF,
            temporalProcedure1);
    AttributeField simpleAttribute =
        new AttributeField(
            underlay,
            underlay.getPrimaryEntity(),
            underlay.getPrimaryEntity().getAttribute("id"),
            false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                temporalPrimaryFilter,
                null,
                null));

    BQTable personTable =
        underlay
            .getIndexSchema()
            .getEntityMain(underlay.getPrimaryEntity().getName())
            .getTablePointer();
    BQTable conditionOccurrenceTable =
        underlay.getIndexSchema().getEntityMain(conditionOccurrence.getName()).getTablePointer();
    BQTable conditionAncestorDescendantTable =
        underlay
            .getIndexSchema()
            .getHierarchyAncestorDescendant(
                conditionPerson.getCriteriaEntity().getName(), Hierarchy.DEFAULT_NAME)
            .getTablePointer();
    BQTable procedureOccurrenceTable =
        underlay.getIndexSchema().getEntityMain(procedureOccurrence.getName()).getTablePointer();
    BQTable procedureAncestorDescendantTable =
        underlay
            .getIndexSchema()
            .getHierarchyAncestorDescendant(
                procedurePerson.getCriteriaEntity().getName(), Hierarchy.DEFAULT_NAME)
            .getTablePointer();
    assertSqlMatchesWithTableNameOnly(
        "temporalPrimaryFilterSingleOutputWithAttrModifiersFirstFirstDaysBefore",
        listQueryResult.getSql(),
        personTable,
        conditionOccurrenceTable,
        conditionAncestorDescendantTable,
        procedureOccurrenceTable,
        procedureAncestorDescendantTable);
  }

  @Test
  void temporalPrimaryFilterMultipleOutputs() throws IOException {
    Entity conditionOccurrence = underlay.getEntity("conditionOccurrence");
    CriteriaOccurrence conditionPerson =
        (CriteriaOccurrence) underlay.getEntityGroup("conditionPerson");

    EntityFilter criteriaSubFilterCondition1 =
        new HierarchyHasAncestorFilter(
            underlay,
            conditionPerson.getCriteriaEntity(),
            conditionPerson.getCriteriaEntity().getHierarchy(Hierarchy.DEFAULT_NAME),
            List.of(Literal.forInt64(201_826L)));
    OccurrenceForPrimaryFilter occurrenceFilterCondition1 =
        new OccurrenceForPrimaryFilter(
            underlay, conditionPerson, conditionOccurrence, null, criteriaSubFilterCondition1);

    EntityFilter criteriaSubFilterCondition2 =
        new HierarchyHasAncestorFilter(
            underlay,
            conditionPerson.getCriteriaEntity(),
            conditionPerson.getCriteriaEntity().getHierarchy(Hierarchy.DEFAULT_NAME),
            List.of(Literal.forInt64(201_254L)));
    OccurrenceForPrimaryFilter occurrenceFilterCondition2 =
        new OccurrenceForPrimaryFilter(
            underlay, conditionPerson, conditionOccurrence, null, criteriaSubFilterCondition2);

    Entity procedureOccurrence = underlay.getEntity("procedureOccurrence");
    CriteriaOccurrence procedurePerson =
        (CriteriaOccurrence) underlay.getEntityGroup("procedurePerson");

    EntityFilter criteriaSubFilterProcedure1 =
        new HierarchyHasAncestorFilter(
            underlay,
            procedurePerson.getCriteriaEntity(),
            procedurePerson.getCriteriaEntity().getHierarchy(Hierarchy.DEFAULT_NAME),
            List.of(Literal.forInt64(4_042_673L)));
    OccurrenceForPrimaryFilter occurrenceFilterProcedure1 =
        new OccurrenceForPrimaryFilter(
            underlay, procedurePerson, procedureOccurrence, null, criteriaSubFilterProcedure1);

    EntityFilter criteriaSubFilterProcedure2 =
        new HierarchyHasAncestorFilter(
            underlay,
            procedurePerson.getCriteriaEntity(),
            procedurePerson.getCriteriaEntity().getHierarchy(Hierarchy.DEFAULT_NAME),
            List.of(Literal.forInt64(4_043_201L)));
    OccurrenceForPrimaryFilter occurrenceFilterProcedure2 =
        new OccurrenceForPrimaryFilter(
            underlay, procedurePerson, procedureOccurrence, null, criteriaSubFilterProcedure2);

    // ANY, two outputs entity1+2 / ANY, two outputs entity1+2 / SAME_VISIT
    List<EntityOutput> temporalOutputsC1P1 =
        List.of(
            EntityOutput.filtered(conditionOccurrence, occurrenceFilterCondition1),
            EntityOutput.filtered(procedureOccurrence, occurrenceFilterProcedure1));
    List<EntityOutput> temporalOutputsC2P2 =
        List.of(
            EntityOutput.filtered(conditionOccurrence, occurrenceFilterCondition2),
            EntityOutput.filtered(procedureOccurrence, occurrenceFilterProcedure2));
    TemporalPrimaryFilter temporalPrimaryFilter =
        new TemporalPrimaryFilter(
            underlay,
            null,
            temporalOutputsC1P1,
            JoinOperator.DURING_SAME_ENCOUNTER,
            null,
            null,
            temporalOutputsC2P2);
    AttributeField simpleAttribute =
        new AttributeField(
            underlay,
            underlay.getPrimaryEntity(),
            underlay.getPrimaryEntity().getAttribute("id"),
            false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                temporalPrimaryFilter,
                null,
                null));
    BQTable personTable =
        underlay
            .getIndexSchema()
            .getEntityMain(underlay.getPrimaryEntity().getName())
            .getTablePointer();
    BQTable conditionOccurrenceTable =
        underlay.getIndexSchema().getEntityMain(conditionOccurrence.getName()).getTablePointer();
    BQTable conditionAncestorDescendantTable =
        underlay
            .getIndexSchema()
            .getHierarchyAncestorDescendant(
                conditionPerson.getCriteriaEntity().getName(), Hierarchy.DEFAULT_NAME)
            .getTablePointer();
    BQTable procedureOccurrenceTable =
        underlay.getIndexSchema().getEntityMain(procedureOccurrence.getName()).getTablePointer();
    BQTable procedureAncestorDescendantTable =
        underlay
            .getIndexSchema()
            .getHierarchyAncestorDescendant(
                procedurePerson.getCriteriaEntity().getName(), Hierarchy.DEFAULT_NAME)
            .getTablePointer();
    assertSqlMatchesWithTableNameOnly(
        "temporalPrimaryFilterMultipleOutputAnyAnySameVisit",
        listQueryResult.getSql(),
        personTable,
        conditionOccurrenceTable,
        conditionAncestorDescendantTable,
        procedureOccurrenceTable,
        procedureAncestorDescendantTable);

    // FIRST, two outputs entity1+1 / LAST, two outputs entity1+1 / WITHIN_NUM_DAYS
    List<EntityOutput> temporalOutputsC1C2 =
        List.of(
            EntityOutput.filtered(conditionOccurrence, occurrenceFilterCondition1),
            EntityOutput.filtered(conditionOccurrence, occurrenceFilterCondition2));
    temporalPrimaryFilter =
        new TemporalPrimaryFilter(
            underlay,
            ReducingOperator.FIRST_MENTION_OF,
            temporalOutputsC1C2,
            JoinOperator.WITHIN_NUM_DAYS,
            90,
            ReducingOperator.LAST_MENTION_OF,
            temporalOutputsC1C2);
    listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                temporalPrimaryFilter,
                null,
                null));
    assertSqlMatchesWithTableNameOnly(
        "temporalPrimaryFilterMultipleOutputFirstLastWithinDays",
        listQueryResult.getSql(),
        personTable,
        conditionOccurrenceTable,
        conditionAncestorDescendantTable);

    // LAST, two outputs entity2+2 / LAST, two outputs entity1+1 / NUM_DAYS_BEFORE
    List<EntityOutput> temporalOutputsP1P2 =
        List.of(
            EntityOutput.filtered(procedureOccurrence, occurrenceFilterProcedure1),
            EntityOutput.filtered(procedureOccurrence, occurrenceFilterProcedure2));
    temporalPrimaryFilter =
        new TemporalPrimaryFilter(
            underlay,
            ReducingOperator.LAST_MENTION_OF,
            temporalOutputsP1P2,
            JoinOperator.NUM_DAYS_BEFORE,
            30,
            ReducingOperator.LAST_MENTION_OF,
            temporalOutputsC1C2);
    listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                temporalPrimaryFilter,
                null,
                null));
    assertSqlMatchesWithTableNameOnly(
        "temporalPrimaryFilterMultipleOutputLastLastDaysBefore",
        listQueryResult.getSql(),
        personTable,
        conditionOccurrenceTable,
        conditionAncestorDescendantTable,
        procedureOccurrenceTable,
        procedureAncestorDescendantTable);

    // LAST, two outputs entity2+1 / ANY, two outputs entity1+2 / NUM_DAYS_AFTER
    List<EntityOutput> temporalOutputsP1C1 =
        List.of(
            EntityOutput.filtered(conditionOccurrence, occurrenceFilterCondition1),
            EntityOutput.filtered(procedureOccurrence, occurrenceFilterProcedure1));
    temporalPrimaryFilter =
        new TemporalPrimaryFilter(
            underlay,
            ReducingOperator.LAST_MENTION_OF,
            temporalOutputsP1C1,
            JoinOperator.NUM_DAYS_AFTER,
            60,
            null,
            temporalOutputsC2P2);
    listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                temporalPrimaryFilter,
                null,
                null));
    assertSqlMatchesWithTableNameOnly(
        "temporalPrimaryFilterMultipleOutputLastAnyDaysAfter",
        listQueryResult.getSql(),
        personTable,
        conditionOccurrenceTable,
        conditionAncestorDescendantTable,
        procedureOccurrenceTable,
        procedureAncestorDescendantTable);
  }
}
