package bio.terra.tanagra.query.bigquery.sqlbuilding.filter;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.RelationshipFilter;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.api.query.list.ListQueryResult;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.UnaryOperator;
import bio.terra.tanagra.query.bigquery.BQQueryRunner;
import bio.terra.tanagra.query.bigquery.BQRunnerTest;
import bio.terra.tanagra.query.bigquery.BQTable;
import bio.terra.tanagra.query.sql.SqlQueryRequest;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.GroupItems;
import bio.terra.tanagra.underlay.serialization.SZService;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

public class BQRelationshipFilterTest extends BQRunnerTest {
  @Test
  void relationshipFilterFKSelect() throws IOException {
    // e.g. SELECT occurrence FILTER ON person (foreign key is on the occurrence table).
    CriteriaOccurrence criteriaOccurrence =
        (CriteriaOccurrence) underlay.getEntityGroup("conditionPerson");
    Entity occurrenceEntity = underlay.getEntity("conditionOccurrence");

    // Sub-filter on filter entity id.
    AttributeFilter idAttributeFilter =
        new AttributeFilter(
            underlay,
            underlay.getPrimaryEntity(),
            underlay.getPrimaryEntity().getIdAttribute(),
            BinaryOperator.EQUALS,
            Literal.forInt64(456L));
    RelationshipFilter relationshipFilter =
        new RelationshipFilter(
            underlay,
            criteriaOccurrence,
            occurrenceEntity,
            criteriaOccurrence.getOccurrencePrimaryRelationship(occurrenceEntity.getName()),
            idAttributeFilter,
            null,
            null,
            null);
    AttributeField simpleAttribute =
        new AttributeField(
            underlay, occurrenceEntity, occurrenceEntity.getAttribute("start_date"), false);
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
    BQTable primaryTable =
        underlay
            .getIndexSchema()
            .getEntityMain(underlay.getPrimaryEntity().getName())
            .getTablePointer();
    assertSqlMatchesWithTableNameOnly(
        "relationshipFilterFKSelectIdFilter",
        listQueryResult.getSql(),
        occurrenceTable,
        primaryTable);

    // Sub-filter not on filter entity id.
    AttributeFilter notIdAttributeFilter =
        new AttributeFilter(
            underlay,
            underlay.getPrimaryEntity(),
            underlay.getPrimaryEntity().getAttribute("gender"),
            BinaryOperator.EQUALS,
            Literal.forInt64(8_207L));
    relationshipFilter =
        new RelationshipFilter(
            underlay,
            criteriaOccurrence,
            occurrenceEntity,
            criteriaOccurrence.getOccurrencePrimaryRelationship(occurrenceEntity.getName()),
            notIdAttributeFilter,
            null,
            null,
            null);
    listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                occurrenceEntity,
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null));
    assertSqlMatchesWithTableNameOnly(
        "relationshipFilterFKSelectNotIdFilter",
        listQueryResult.getSql(),
        occurrenceTable,
        primaryTable);

    // Null sub-filter.
    relationshipFilter =
        new RelationshipFilter(
            underlay,
            criteriaOccurrence,
            occurrenceEntity,
            criteriaOccurrence.getOccurrencePrimaryRelationship(occurrenceEntity.getName()),
            null,
            null,
            null,
            null);
    listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                occurrenceEntity,
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null));
    assertSqlMatchesWithTableNameOnly(
        "relationshipFilterFKSelectNullFilter",
        listQueryResult.getSql(),
        occurrenceTable,
        primaryTable);
  }

  @Test
  void relationshipFilterFKFilter() throws IOException {
    // e.g. SELECT person FILTER ON occurrence (foreign key is on the occurrence table).
    CriteriaOccurrence criteriaOccurrence =
        (CriteriaOccurrence) underlay.getEntityGroup("conditionPerson");
    Entity occurrenceEntity = underlay.getEntity("conditionOccurrence");

    // Sub-filter on filter entity id.
    AttributeFilter attributeFilter =
        new AttributeFilter(
            underlay,
            occurrenceEntity,
            occurrenceEntity.getAttribute("person_id"),
            BinaryOperator.EQUALS,
            Literal.forInt64(15L));
    RelationshipFilter relationshipFilter =
        new RelationshipFilter(
            underlay,
            criteriaOccurrence,
            underlay.getPrimaryEntity(),
            criteriaOccurrence.getOccurrencePrimaryRelationship(occurrenceEntity.getName()),
            attributeFilter,
            null,
            null,
            null);
    AttributeField simpleAttribute =
        new AttributeField(
            underlay,
            underlay.getPrimaryEntity(),
            underlay.getPrimaryEntity().getAttribute("year_of_birth"),
            false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null));
    BQTable occurrenceTable =
        underlay.getIndexSchema().getEntityMain(occurrenceEntity.getName()).getTablePointer();
    BQTable primaryTable =
        underlay
            .getIndexSchema()
            .getEntityMain(underlay.getPrimaryEntity().getName())
            .getTablePointer();
    assertSqlMatchesWithTableNameOnly(
        "relationshipFilterFKFilterIdFilter",
        listQueryResult.getSql(),
        occurrenceTable,
        primaryTable);

    // Sub-filter not on filter entity id.
    attributeFilter =
        new AttributeFilter(
            underlay,
            occurrenceEntity,
            occurrenceEntity.getAttribute("stop_reason"),
            UnaryOperator.IS_NULL);
    relationshipFilter =
        new RelationshipFilter(
            underlay,
            criteriaOccurrence,
            underlay.getPrimaryEntity(),
            criteriaOccurrence.getOccurrencePrimaryRelationship(occurrenceEntity.getName()),
            attributeFilter,
            null,
            null,
            null);
    listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null));
    assertSqlMatchesWithTableNameOnly(
        "relationshipFilterFKFilterNotIdFilter",
        listQueryResult.getSql(),
        occurrenceTable,
        primaryTable);

    // Null sub-filter, without rollup count field.
    relationshipFilter =
        new RelationshipFilter(
            underlay,
            criteriaOccurrence,
            underlay.getPrimaryEntity(),
            criteriaOccurrence.getOccurrencePrimaryRelationship(occurrenceEntity.getName()),
            null,
            null,
            null,
            null);
    listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null));
    assertSqlMatchesWithTableNameOnly(
        "relationshipFilterFKFilterNullFilterWithoutRollupCount",
        listQueryResult.getSql(),
        occurrenceTable,
        primaryTable);

    // Null sub-filter, with rollup count field.
    // The cmssynpuf underlay does not have an example of this type of relationship
    // (i.e. foreign key relationship on the filter entity, and a rollup count field).
    // This is typical for things like vitals (e.g. list of pulse readings for a person,
    // query is to get all persons with at least one pulse reading). So for this part
    // of the test, we use the SD underlay. But since we the GHA does not have
    // credentials to query against an SD dataset, here we just check the generated SQL.
    ConfigReader configReader = ConfigReader.fromJarResources();
    SZService szService = configReader.readService("sd20230331_verily");
    SZUnderlay szUnderlay = configReader.readUnderlay(szService.underlay);
    Underlay underlay = Underlay.fromConfig(szService.bigQuery, szUnderlay, configReader);
    BQQueryRunner bqQueryRunner =
        new BQQueryRunner(szService.bigQuery.queryProjectId, szService.bigQuery.dataLocation);
    GroupItems pulsePerson = (GroupItems) underlay.getEntityGroup("pulsePerson");
    relationshipFilter =
        new RelationshipFilter(
            underlay,
            pulsePerson,
            pulsePerson.getGroupEntity(),
            pulsePerson.getGroupItemsRelationship(),
            null,
            null,
            null,
            null);
    simpleAttribute =
        new AttributeField(
            underlay,
            pulsePerson.getGroupEntity(),
            pulsePerson.getGroupEntity().getIdAttribute(),
            false);
    SqlQueryRequest sqlQueryRequest =
        bqQueryRunner.buildListQuerySqlAgainstIndexData(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                pulsePerson.getGroupEntity(),
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null));
    primaryTable =
        underlay
            .getIndexSchema()
            .getEntityMain(underlay.getPrimaryEntity().getName())
            .getTablePointer();
    assertSqlMatchesWithTableNameOnly(
        "relationshipFilterFKFilterNullFilterWithRollupCount",
        sqlQueryRequest.getSql(),
        primaryTable);
  }

  @Test
  void relationshipFilterIntermediateTable() throws IOException {
    GroupItems groupItems = (GroupItems) underlay.getEntityGroup("brandIngredient");

    // Sub-filter on filter entity id.
    AttributeFilter attributeFilter =
        new AttributeFilter(
            underlay,
            groupItems.getGroupEntity(),
            groupItems.getGroupEntity().getIdAttribute(),
            BinaryOperator.EQUALS,
            Literal.forInt64(15L));
    RelationshipFilter relationshipFilter =
        new RelationshipFilter(
            underlay,
            groupItems,
            groupItems.getItemsEntity(),
            groupItems.getGroupItemsRelationship(),
            attributeFilter,
            null,
            null,
            null);
    AttributeField simpleAttribute =
        new AttributeField(
            underlay,
            groupItems.getItemsEntity(),
            groupItems.getItemsEntity().getAttribute("name"),
            false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                groupItems.getItemsEntity(),
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null));
    BQTable groupTable =
        underlay
            .getIndexSchema()
            .getEntityMain(groupItems.getGroupEntity().getName())
            .getTablePointer();
    BQTable itemsTable =
        underlay
            .getIndexSchema()
            .getEntityMain(groupItems.getItemsEntity().getName())
            .getTablePointer();
    BQTable idPairsTable =
        underlay
            .getIndexSchema()
            .getRelationshipIdPairs(
                groupItems.getName(),
                groupItems.getGroupEntity().getName(),
                groupItems.getItemsEntity().getName())
            .getTablePointer();
    assertSqlMatchesWithTableNameOnly(
        "relationshipFilterIntTableIdFilter",
        listQueryResult.getSql(),
        groupTable,
        itemsTable,
        idPairsTable);

    // Sub-filter not on filter entity id.
    attributeFilter =
        new AttributeFilter(
            underlay,
            groupItems.getGroupEntity(),
            groupItems.getGroupEntity().getAttribute("concept_code"),
            BinaryOperator.EQUALS,
            Literal.forString("161"));
    relationshipFilter =
        new RelationshipFilter(
            underlay,
            groupItems,
            groupItems.getItemsEntity(),
            groupItems.getGroupItemsRelationship(),
            attributeFilter,
            null,
            null,
            null);
    listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                groupItems.getItemsEntity(),
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null));
    assertSqlMatchesWithTableNameOnly(
        "relationshipFilterIntTableNotIdFilter",
        listQueryResult.getSql(),
        groupTable,
        itemsTable,
        idPairsTable);

    // Null sub-filter, without rollup count field.
    relationshipFilter =
        new RelationshipFilter(
            underlay,
            groupItems,
            groupItems.getItemsEntity(),
            groupItems.getGroupItemsRelationship(),
            null,
            null,
            null,
            null);
    listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                groupItems.getItemsEntity(),
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null));
    assertSqlMatchesWithTableNameOnly(
        "relationshipFilterIntTableNullFilterWithoutRollupCount",
        listQueryResult.getSql(),
        groupTable,
        itemsTable,
        idPairsTable);

    // Null sub-filter, with rollup count field.
    simpleAttribute =
        new AttributeField(
            underlay,
            groupItems.getGroupEntity(),
            groupItems.getItemsEntity().getAttribute("name"),
            false);
    relationshipFilter =
        new RelationshipFilter(
            underlay,
            groupItems,
            groupItems.getGroupEntity(),
            groupItems.getGroupItemsRelationship(),
            null,
            null,
            null,
            null);
    listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                groupItems.getGroupEntity(),
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null));
    assertSqlMatchesWithTableNameOnly(
        "relationshipFilterIntTableNullFilterWithRollupCount",
        listQueryResult.getSql(),
        groupTable,
        itemsTable,
        idPairsTable);
  }

  @Test
  void relationshipFilterFKFilterWithGroupBy() throws IOException {
    CriteriaOccurrence criteriaOccurrence =
        (CriteriaOccurrence) underlay.getEntityGroup("conditionPerson");
    Entity occurrenceEntity = underlay.getEntity("conditionOccurrence");

    // Sub-filter on filter entity id.
    AttributeFilter attributeFilter =
        new AttributeFilter(
            underlay,
            occurrenceEntity,
            occurrenceEntity.getAttribute("person_id"),
            BinaryOperator.EQUALS,
            Literal.forInt64(15L));
    RelationshipFilter relationshipFilter =
        new RelationshipFilter(
            underlay,
            criteriaOccurrence,
            underlay.getPrimaryEntity(),
            criteriaOccurrence.getOccurrencePrimaryRelationship(occurrenceEntity.getName()),
            attributeFilter,
            List.of(
                occurrenceEntity.getAttribute("start_date"),
                occurrenceEntity.getAttribute("condition")),
            BinaryOperator.GREATER_THAN,
            1);
    AttributeField simpleAttribute =
        new AttributeField(
            underlay,
            underlay.getPrimaryEntity(),
            underlay.getPrimaryEntity().getAttribute("year_of_birth"),
            false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null));
    BQTable occurrenceTable =
        underlay.getIndexSchema().getEntityMain(occurrenceEntity.getName()).getTablePointer();
    BQTable primaryTable =
        underlay
            .getIndexSchema()
            .getEntityMain(underlay.getPrimaryEntity().getName())
            .getTablePointer();
    assertSqlMatchesWithTableNameOnly(
        "relationshipFilterFKFilterIdFilterWithGroupBy",
        listQueryResult.getSql(),
        occurrenceTable,
        primaryTable);

    // Sub-filter not on filter entity id.
    attributeFilter =
        new AttributeFilter(
            underlay,
            occurrenceEntity,
            occurrenceEntity.getAttribute("stop_reason"),
            UnaryOperator.IS_NULL);
    relationshipFilter =
        new RelationshipFilter(
            underlay,
            criteriaOccurrence,
            underlay.getPrimaryEntity(),
            criteriaOccurrence.getOccurrencePrimaryRelationship(occurrenceEntity.getName()),
            attributeFilter,
            List.of(
                occurrenceEntity.getAttribute("start_date"),
                occurrenceEntity.getAttribute("condition")),
            BinaryOperator.GREATER_THAN,
            1);
    listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null));
    assertSqlMatchesWithTableNameOnly(
        "relationshipFilterFKFilterNotIdFilterWithGroupBy",
        listQueryResult.getSql(),
        occurrenceTable,
        primaryTable);

    // Null sub-filter.
    relationshipFilter =
        new RelationshipFilter(
            underlay,
            criteriaOccurrence,
            underlay.getPrimaryEntity(),
            criteriaOccurrence.getOccurrencePrimaryRelationship(occurrenceEntity.getName()),
            null,
            List.of(
                occurrenceEntity.getAttribute("start_date"),
                occurrenceEntity.getAttribute("condition")),
            BinaryOperator.GREATER_THAN,
            14);
    listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null));
    assertSqlMatchesWithTableNameOnly(
        "relationshipFilterFKFilterNullFilterWithGroupBy",
        listQueryResult.getSql(),
        occurrenceTable,
        primaryTable);
  }

  @Test
  void relationshipFilterIntermediateTableWithGroupBy() throws IOException {
    GroupItems groupItems = (GroupItems) underlay.getEntityGroup("brandIngredient");

    // Sub-filter on filter entity id, group by on filter entity id.
    AttributeFilter attributeFilter =
        new AttributeFilter(
            underlay,
            groupItems.getGroupEntity(),
            groupItems.getGroupEntity().getIdAttribute(),
            BinaryOperator.EQUALS,
            Literal.forInt64(15L));
    RelationshipFilter relationshipFilter =
        new RelationshipFilter(
            underlay,
            groupItems,
            groupItems.getItemsEntity(),
            groupItems.getGroupItemsRelationship(),
            attributeFilter,
            List.of(groupItems.getGroupEntity().getAttribute("vocabulary")),
            BinaryOperator.EQUALS,
            1);
    AttributeField simpleAttribute =
        new AttributeField(
            underlay,
            groupItems.getItemsEntity(),
            groupItems.getItemsEntity().getAttribute("name"),
            false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                groupItems.getItemsEntity(),
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null));
    BQTable groupTable =
        underlay
            .getIndexSchema()
            .getEntityMain(groupItems.getGroupEntity().getName())
            .getTablePointer();
    BQTable itemsTable =
        underlay
            .getIndexSchema()
            .getEntityMain(groupItems.getItemsEntity().getName())
            .getTablePointer();
    BQTable idPairsTable =
        underlay
            .getIndexSchema()
            .getRelationshipIdPairs(
                groupItems.getName(),
                groupItems.getGroupEntity().getName(),
                groupItems.getItemsEntity().getName())
            .getTablePointer();
    assertSqlMatchesWithTableNameOnly(
        "relationshipFilterIntTableIdFilterWithGroupByOnId",
        listQueryResult.getSql(),
        groupTable,
        itemsTable,
        idPairsTable);

    // Sub-filter not on filter entity id.
    attributeFilter =
        new AttributeFilter(
            underlay,
            groupItems.getGroupEntity(),
            groupItems.getGroupEntity().getAttribute("concept_code"),
            BinaryOperator.EQUALS,
            Literal.forString("161"));
    relationshipFilter =
        new RelationshipFilter(
            underlay,
            groupItems,
            groupItems.getItemsEntity(),
            groupItems.getGroupItemsRelationship(),
            attributeFilter,
            List.of(groupItems.getGroupEntity().getAttribute("vocabulary")),
            BinaryOperator.EQUALS,
            1);
    listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                groupItems.getItemsEntity(),
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null));
    assertSqlMatchesWithTableNameOnly(
        "relationshipFilterIntTableNotIdFilterWithGroupBy",
        listQueryResult.getSql(),
        groupTable,
        itemsTable,
        idPairsTable);

    // Null sub-filter.
    relationshipFilter =
        new RelationshipFilter(
            underlay,
            groupItems,
            groupItems.getItemsEntity(),
            groupItems.getGroupItemsRelationship(),
            null,
            null,
            BinaryOperator.EQUALS,
            1);
    listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                groupItems.getItemsEntity(),
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null));
    assertSqlMatchesWithTableNameOnly(
        "relationshipFilterIntTableNullFilterWithGroupBy",
        listQueryResult.getSql(),
        groupTable,
        itemsTable,
        idPairsTable);
  }

  @Test
  void relationshipFilterFKFilterWithSwapFields() throws IOException {
    // e.g. SELECT occurrence FILTER ON (person FILTER ON occurrence).
    CriteriaOccurrence criteriaOccurrence =
        (CriteriaOccurrence) underlay.getEntityGroup("conditionPerson");
    Entity occurrenceEntity = underlay.getEntity("conditionOccurrence");

    AttributeFilter innerOccurrenceFilter =
        new AttributeFilter(
            underlay,
            occurrenceEntity,
            occurrenceEntity.getAttribute("condition"),
            BinaryOperator.EQUALS,
            Literal.forInt64(223_276L));
    RelationshipFilter personFilter =
        new RelationshipFilter(
            underlay,
            criteriaOccurrence,
            underlay.getPrimaryEntity(),
            criteriaOccurrence.getOccurrencePrimaryRelationship(occurrenceEntity.getName()),
            innerOccurrenceFilter,
            null,
            null,
            null);
    RelationshipFilter occurrenceFilter =
        new RelationshipFilter(
            underlay,
            criteriaOccurrence,
            occurrenceEntity,
            criteriaOccurrence.getOccurrencePrimaryRelationship(occurrenceEntity.getName()),
            personFilter,
            null,
            null,
            null);
    AttributeField simpleAttribute =
        new AttributeField(
            underlay, occurrenceEntity, occurrenceEntity.getAttribute("start_date"), false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                occurrenceEntity,
                List.of(simpleAttribute),
                occurrenceFilter,
                null,
                null));
    BQTable occurrenceTable =
        underlay.getIndexSchema().getEntityMain(occurrenceEntity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly(
        "relationshipFilterFKFilterWithSwapFields", listQueryResult.getSql(), occurrenceTable);
  }

  @Test
  void relationshipFilterIntermediateTableWithSwapFields() throws IOException {
    // e.g. SELECT occurrence FILTER ON (ingredient FILTER ON brand).
    CriteriaOccurrence criteriaOccurrence =
        (CriteriaOccurrence) underlay.getEntityGroup("ingredientPerson");
    Entity occurrenceEntity = underlay.getEntity("ingredientOccurrence");
    GroupItems groupItems = (GroupItems) underlay.getEntityGroup("brandIngredient");

    AttributeFilter brandFilter =
        new AttributeFilter(
            underlay,
            groupItems.getGroupEntity(),
            groupItems.getGroupEntity().getIdAttribute(),
            BinaryOperator.EQUALS,
            Literal.forInt64(19_042_336L));
    RelationshipFilter ingredientFilter =
        new RelationshipFilter(
            underlay,
            groupItems,
            groupItems.getItemsEntity(),
            groupItems.getGroupItemsRelationship(),
            brandFilter,
            null,
            null,
            null);
    RelationshipFilter occurrenceFilter =
        new RelationshipFilter(
            underlay,
            criteriaOccurrence,
            occurrenceEntity,
            criteriaOccurrence.getOccurrenceCriteriaRelationship(occurrenceEntity.getName()),
            ingredientFilter,
            null,
            null,
            null);
    AttributeField simpleAttribute =
        new AttributeField(
            underlay, occurrenceEntity, occurrenceEntity.getAttribute("start_date"), false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                occurrenceEntity,
                List.of(simpleAttribute),
                occurrenceFilter,
                null,
                null));
    BQTable occurrenceTable =
        underlay.getIndexSchema().getEntityMain(occurrenceEntity.getName()).getTablePointer();
    BQTable intermediateTable =
        underlay
            .getIndexSchema()
            .getRelationshipIdPairs(
                groupItems.getName(),
                groupItems.getGroupEntity().getName(),
                groupItems.getItemsEntity().getName())
            .getTablePointer();
    assertSqlMatchesWithTableNameOnly(
        "relationshipFilterIntTableWithSwapFields",
        listQueryResult.getSql(),
        occurrenceTable,
        intermediateTable);
  }

  @Test
  void relationshipFilterNestedNullFilterFKFilter() throws IOException {
    // The cmssynpuf underlay does not have an example of this type of relationship
    // (i.e. foreign key relationship on the filter entity).
    // This is typical for things like vitals (e.g. list of pulse readings for a person,
    // query is to get all persons with at least one pulse reading). So for this part
    // of the test, we use the SD underlay. But since we the GHA does not have
    // credentials to query against an SD dataset, here we just check the generated SQL.
    ConfigReader configReader = ConfigReader.fromJarResources();
    SZService szService = configReader.readService("sd20230331_verily");
    SZUnderlay szUnderlay = configReader.readUnderlay(szService.underlay);
    Underlay underlay = Underlay.fromConfig(szService.bigQuery, szUnderlay, configReader);
    BQQueryRunner bqQueryRunner =
        new BQQueryRunner(szService.bigQuery.queryProjectId, szService.bigQuery.dataLocation);

    // e.g. SELECT conditionOccurrence FILTER ON (person FILTER ON HAS ANY bmi).
    CriteriaOccurrence criteriaOccurrence =
        (CriteriaOccurrence) underlay.getEntityGroup("conditionPerson");
    Entity occurrenceEntity = underlay.getEntity("conditionOccurrence");
    GroupItems groupItems = (GroupItems) underlay.getEntityGroup("bmiPerson");

    RelationshipFilter personFilter =
        new RelationshipFilter(
            underlay,
            groupItems,
            underlay.getPrimaryEntity(),
            groupItems.getGroupItemsRelationship(),
            null,
            null,
            null,
            null);
    RelationshipFilter occurrenceFilter =
        new RelationshipFilter(
            underlay,
            criteriaOccurrence,
            occurrenceEntity,
            criteriaOccurrence.getOccurrencePrimaryRelationship(occurrenceEntity.getName()),
            personFilter,
            null,
            null,
            null);
    AttributeField simpleAttribute =
        new AttributeField(
            underlay, occurrenceEntity, occurrenceEntity.getAttribute("start_date"), false);
    SqlQueryRequest sqlQueryRequest =
        bqQueryRunner.buildListQuerySqlAgainstIndexData(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                occurrenceEntity,
                List.of(simpleAttribute),
                occurrenceFilter,
                null,
                null));
    BQTable occurrenceTable =
        underlay.getIndexSchema().getEntityMain(occurrenceEntity.getName()).getTablePointer();
    BQTable itemsTable =
        underlay
            .getIndexSchema()
            .getEntityMain(groupItems.getItemsEntity().getName())
            .getTablePointer();
    assertSqlMatchesWithTableNameOnly(
        "relationshipFilterNestedNullFilterFKFilter",
        sqlQueryRequest.getSql(),
        occurrenceTable,
        itemsTable);
  }
}
