package bio.terra.tanagra.query.bigquery.sqlbuilding.filter;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.BooleanNotFilter;
import bio.terra.tanagra.api.filter.HierarchyHasAncestorFilter;
import bio.terra.tanagra.api.filter.RelationshipFilter;
import bio.terra.tanagra.api.filter.TextSearchFilter;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.api.query.list.ListQueryResult;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.query.bigquery.BQRunnerTest;
import bio.terra.tanagra.query.bigquery.BQTable;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

public class BQBooleanLogicFilterTest extends BQRunnerTest {
  @Test
  void booleanAndOrFilter() throws IOException {
    Entity entity = underlay.getEntity("condition");
    Attribute attribute = entity.getAttribute("concept_code");
    AttributeFilter attributeFilter =
        new AttributeFilter(
            underlay, entity, attribute, BinaryOperator.NOT_EQUALS, Literal.forString("1956"));
    TextSearchFilter textSearchFilter =
        new TextSearchFilter(
            underlay, entity, TextSearchFilter.TextSearchOperator.EXACT_MATCH, "44054006", null);
    BooleanAndOrFilter booleanAndOrFilter =
        new BooleanAndOrFilter(
            BooleanAndOrFilter.LogicalOperator.AND, List.of(attributeFilter, textSearchFilter));
    AttributeField simpleAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("name"), false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay, entity, List.of(simpleAttribute), booleanAndOrFilter, null, null));
    BQTable table = underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("booleanAndOrFilter", listQueryResult.getSql(), table);
  }

  @Test
  void booleanNotFilter() throws IOException {
    Entity entity = underlay.getPrimaryEntity();
    Attribute attribute = entity.getAttribute("year_of_birth");
    AttributeFilter attributeFilter =
        new AttributeFilter(
            underlay, entity, attribute, BinaryOperator.NOT_EQUALS, Literal.forInt64(1_956L));
    BooleanNotFilter booleanNotFilter = new BooleanNotFilter(attributeFilter);
    AttributeField simpleAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("year_of_birth"), false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay, entity, List.of(simpleAttribute), booleanNotFilter, null, null));
    BQTable table = underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("booleanNotFilter", listQueryResult.getSql(), table);
  }

  @Test
  void booleanAndOrFilterWithSwapFields() throws IOException {
    // e.g. SELECT occurrence BOOLEAN LOGIC FILTER ON condition (foreign key is on the occurrence
    // table).
    CriteriaOccurrence criteriaOccurrence =
        (CriteriaOccurrence) underlay.getEntityGroup("conditionPerson");
    Entity occurrenceEntity = underlay.getEntity("conditionOccurrence");

    HierarchyHasAncestorFilter hasAncestorFilter1 =
        new HierarchyHasAncestorFilter(
            underlay,
            criteriaOccurrence.getCriteriaEntity(),
            criteriaOccurrence.getCriteriaEntity().getHierarchy(Hierarchy.DEFAULT_NAME),
            Literal.forInt64(123L));
    HierarchyHasAncestorFilter hasAncestorFilter2 =
        new HierarchyHasAncestorFilter(
            underlay,
            criteriaOccurrence.getCriteriaEntity(),
            criteriaOccurrence.getCriteriaEntity().getHierarchy(Hierarchy.DEFAULT_NAME),
            Literal.forInt64(456L));
    BooleanAndOrFilter hasAncestorFilter1or2 =
        new BooleanAndOrFilter(
            BooleanAndOrFilter.LogicalOperator.OR, List.of(hasAncestorFilter1, hasAncestorFilter2));
    RelationshipFilter relationshipFilter =
        new RelationshipFilter(
            underlay,
            criteriaOccurrence,
            occurrenceEntity,
            criteriaOccurrence.getOccurrenceCriteriaRelationship(occurrenceEntity.getName()),
            hasAncestorFilter1or2,
            null,
            null,
            null);
    AttributeField simpleAttribute =
        new AttributeField(
            underlay, occurrenceEntity, occurrenceEntity.getAttribute("person_id"), false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                occurrenceEntity,
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null));
    BQTable occurrenceTable =
        underlay.getIndexSchema().getEntityMain(occurrenceEntity.getName()).getTablePointer();
    BQTable criteriaTable =
        underlay
            .getIndexSchema()
            .getEntityMain(criteriaOccurrence.getCriteriaEntity().getName())
            .getTablePointer();
    BQTable criteriaAncestorDescendantTable =
        underlay
            .getIndexSchema()
            .getHierarchyAncestorDescendant(
                criteriaOccurrence.getCriteriaEntity().getName(),
                criteriaOccurrence
                    .getCriteriaEntity()
                    .getHierarchy(Hierarchy.DEFAULT_NAME)
                    .getName())
            .getTablePointer();
    assertSqlMatchesWithTableNameOnly(
        "booleanAndOrFilterWithSwapFields",
        listQueryResult.getSql(),
        occurrenceTable,
        criteriaTable,
        criteriaAncestorDescendantTable);
  }

  @Test
  void booleanNotFilterWithSwapFields() throws IOException {
    // e.g. SELECT occurrence BOOLEAN LOGIC FILTER ON condition (foreign key is on the occurrence
    // table).
    CriteriaOccurrence criteriaOccurrence =
        (CriteriaOccurrence) underlay.getEntityGroup("conditionPerson");
    Entity occurrenceEntity = underlay.getEntity("conditionOccurrence");

    HierarchyHasAncestorFilter hasAncestorFilter1 =
        new HierarchyHasAncestorFilter(
            underlay,
            criteriaOccurrence.getCriteriaEntity(),
            criteriaOccurrence.getCriteriaEntity().getHierarchy(Hierarchy.DEFAULT_NAME),
            Literal.forInt64(123L));
    BooleanNotFilter notHasAncestorFilter1 = new BooleanNotFilter(hasAncestorFilter1);
    RelationshipFilter relationshipFilter =
        new RelationshipFilter(
            underlay,
            criteriaOccurrence,
            occurrenceEntity,
            criteriaOccurrence.getOccurrenceCriteriaRelationship(occurrenceEntity.getName()),
            notHasAncestorFilter1,
            null,
            null,
            null);
    AttributeField simpleAttribute =
        new AttributeField(
            underlay, occurrenceEntity, occurrenceEntity.getAttribute("person_id"), false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                occurrenceEntity,
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null));
    BQTable occurrenceTable =
        underlay.getIndexSchema().getEntityMain(occurrenceEntity.getName()).getTablePointer();
    BQTable criteriaTable =
        underlay
            .getIndexSchema()
            .getEntityMain(criteriaOccurrence.getCriteriaEntity().getName())
            .getTablePointer();
    BQTable criteriaAncestorDescendantTable =
        underlay
            .getIndexSchema()
            .getHierarchyAncestorDescendant(
                criteriaOccurrence.getCriteriaEntity().getName(),
                criteriaOccurrence
                    .getCriteriaEntity()
                    .getHierarchy(Hierarchy.DEFAULT_NAME)
                    .getName())
            .getTablePointer();
    assertSqlMatchesWithTableNameOnly(
        "booleanNotFilterWithSwapFields",
        listQueryResult.getSql(),
        occurrenceTable,
        criteriaTable,
        criteriaAncestorDescendantTable);
  }
}
