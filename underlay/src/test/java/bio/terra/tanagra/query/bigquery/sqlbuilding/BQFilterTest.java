package bio.terra.tanagra.query.bigquery.sqlbuilding;

import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.BooleanNotFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.GroupHasItemsFilter;
import bio.terra.tanagra.api.filter.HierarchyHasAncestorFilter;
import bio.terra.tanagra.api.filter.HierarchyHasParentFilter;
import bio.terra.tanagra.api.filter.HierarchyIsMemberFilter;
import bio.terra.tanagra.api.filter.HierarchyIsRootFilter;
import bio.terra.tanagra.api.filter.ItemInGroupFilter;
import bio.terra.tanagra.api.filter.OccurrenceForPrimaryFilter;
import bio.terra.tanagra.api.filter.PrimaryWithCriteriaFilter;
import bio.terra.tanagra.api.filter.RelationshipFilter;
import bio.terra.tanagra.api.filter.TextSearchFilter;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.api.query.list.ListQueryResult;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.NaryOperator;
import bio.terra.tanagra.api.shared.UnaryOperator;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.query.bigquery.BQQueryRunner;
import bio.terra.tanagra.query.bigquery.BQRunnerTest;
import bio.terra.tanagra.query.bigquery.BQTable;
import bio.terra.tanagra.query.sql.SqlQueryRequest;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.GroupItems;
import bio.terra.tanagra.underlay.serialization.SZService;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class BQFilterTest extends BQRunnerTest {
  @Test
  void attributeFilter() throws IOException {
    // Filter with unary operator.
    Entity entity = underlay.getPrimaryEntity();
    Attribute attribute = entity.getAttribute("ethnicity");
    AttributeFilter attributeFilter =
        new AttributeFilter(underlay, entity, attribute, UnaryOperator.IS_NOT_NULL);
    AttributeField simpleAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("year_of_birth"), false, false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                entity,
                List.of(simpleAttribute),
                attributeFilter,
                null,
                null,
                null,
                null,
                true));
    BQTable table = underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("attributeFilterUnary", listQueryResult.getSql(), table);

    // Filter with binary operator.
    attribute = entity.getAttribute("year_of_birth");
    attributeFilter =
        new AttributeFilter(
            underlay, entity, attribute, BinaryOperator.NOT_EQUALS, Literal.forInt64(1_956L));
    listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                entity,
                List.of(simpleAttribute),
                attributeFilter,
                null,
                null,
                null,
                null,
                true));
    assertSqlMatchesWithTableNameOnly("attributeFilterBinary", listQueryResult.getSql(), table);

    // Filter with n-ary operator IN.
    attribute = entity.getAttribute("age");
    attributeFilter =
        new AttributeFilter(
            underlay,
            entity,
            attribute,
            NaryOperator.IN,
            List.of(Literal.forInt64(18L), Literal.forInt64(19L), Literal.forInt64(20L)));
    listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                entity,
                List.of(simpleAttribute),
                attributeFilter,
                null,
                null,
                null,
                null,
                true));
    assertSqlMatchesWithTableNameOnly("attributeFilterNaryIn", listQueryResult.getSql(), table);

    // Filter with n-ary operator BETWEEN.
    attribute = entity.getAttribute("age");
    attributeFilter =
        new AttributeFilter(
            underlay,
            entity,
            attribute,
            NaryOperator.BETWEEN,
            List.of(Literal.forInt64(45L), Literal.forInt64(65L)));
    listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                entity,
                List.of(simpleAttribute),
                attributeFilter,
                null,
                null,
                null,
                null,
                true));
    assertSqlMatchesWithTableNameOnly(
        "attributeFilterNaryBetween", listQueryResult.getSql(), table);
  }

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
        new AttributeField(underlay, entity, entity.getAttribute("name"), false, false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                entity,
                List.of(simpleAttribute),
                booleanAndOrFilter,
                null,
                null,
                null,
                null,
                true));
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
        new AttributeField(underlay, entity, entity.getAttribute("year_of_birth"), false, false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                entity,
                List.of(simpleAttribute),
                booleanNotFilter,
                null,
                null,
                null,
                null,
                true));
    BQTable table = underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("booleanNotFilter", listQueryResult.getSql(), table);
  }

  @Test
  void hierarchyHasAncestorFilter() throws IOException {
    Entity entity = underlay.getEntity("condition");
    HierarchyHasAncestorFilter hierarchyHasAncestorFilter =
        new HierarchyHasAncestorFilter(
            underlay,
            entity,
            entity.getHierarchy(Hierarchy.DEFAULT_NAME),
            Literal.forInt64(201_826L));
    AttributeField simpleAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("name"), false, false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                entity,
                List.of(simpleAttribute),
                hierarchyHasAncestorFilter,
                null,
                null,
                null,
                null,
                true));
    BQTable entityMainTable =
        underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    BQTable ancestorDescendantTable =
        underlay
            .getIndexSchema()
            .getHierarchyAncestorDescendant(entity.getName(), Hierarchy.DEFAULT_NAME)
            .getTablePointer();
    assertSqlMatchesWithTableNameOnly(
        "hierarchyHasAncestorFilterEquals",
        listQueryResult.getSql(),
        entityMainTable,
        ancestorDescendantTable);

    hierarchyHasAncestorFilter =
        new HierarchyHasAncestorFilter(
            underlay,
            entity,
            entity.getHierarchy(Hierarchy.DEFAULT_NAME),
            List.of(Literal.forInt64(201_826L), Literal.forInt64(201_254L)));
    listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                entity,
                List.of(simpleAttribute),
                hierarchyHasAncestorFilter,
                null,
                null,
                null,
                null,
                true));
    assertSqlMatchesWithTableNameOnly(
        "hierarchyHasAncestorFilterIn",
        listQueryResult.getSql(),
        entityMainTable,
        ancestorDescendantTable);
  }

  @Test
  void hierarchyHasParentFilter() throws IOException {
    Entity entity = underlay.getEntity("condition");
    HierarchyHasParentFilter hierarchyHasParentFilter =
        new HierarchyHasParentFilter(
            underlay,
            entity,
            entity.getHierarchy(Hierarchy.DEFAULT_NAME),
            Literal.forInt64(201_826L));
    AttributeField simpleAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("name"), false, false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                entity,
                List.of(simpleAttribute),
                hierarchyHasParentFilter,
                null,
                null,
                null,
                null,
                true));
    BQTable entityMainTable =
        underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    BQTable childParentTable =
        underlay
            .getIndexSchema()
            .getHierarchyChildParent(entity.getName(), Hierarchy.DEFAULT_NAME)
            .getTablePointer();
    assertSqlMatchesWithTableNameOnly(
        "hierarchyHasParentFilterEquals",
        listQueryResult.getSql(),
        entityMainTable,
        childParentTable);

    hierarchyHasParentFilter =
        new HierarchyHasParentFilter(
            underlay,
            entity,
            entity.getHierarchy(Hierarchy.DEFAULT_NAME),
            List.of(Literal.forInt64(201_826L), Literal.forInt64(201_254L)));
    listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                entity,
                List.of(simpleAttribute),
                hierarchyHasParentFilter,
                null,
                null,
                null,
                null,
                true));
    assertSqlMatchesWithTableNameOnly(
        "hierarchyHasParentFilterIn", listQueryResult.getSql(), entityMainTable, childParentTable);
  }

  @Test
  void hierarchyIsMemberFilter() throws IOException {
    Entity entity = underlay.getEntity("condition");
    HierarchyIsMemberFilter hierarchyIsMemberFilter =
        new HierarchyIsMemberFilter(underlay, entity, entity.getHierarchy(Hierarchy.DEFAULT_NAME));
    AttributeField simpleAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("name"), false, false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                entity,
                List.of(simpleAttribute),
                hierarchyIsMemberFilter,
                null,
                null,
                null,
                null,
                true));
    BQTable table = underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("hierarchyIsMemberFilter", listQueryResult.getSql(), table);
  }

  @Test
  void hierarchyIsRootFilter() throws IOException {
    Entity entity = underlay.getEntity("condition");
    HierarchyIsRootFilter hierarchyIsRootFilter =
        new HierarchyIsRootFilter(underlay, entity, entity.getHierarchy(Hierarchy.DEFAULT_NAME));
    AttributeField simpleAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("name"), false, false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                entity,
                List.of(simpleAttribute),
                hierarchyIsRootFilter,
                null,
                null,
                null,
                null,
                true));
    BQTable table = underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("hierarchyIsRootFilter", listQueryResult.getSql(), table);
  }

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
            underlay, occurrenceEntity, occurrenceEntity.getAttribute("start_date"), false, false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                occurrenceEntity,
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null,
                null,
                null,
                true));
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
            new ListQueryRequest(
                underlay,
                occurrenceEntity,
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null,
                null,
                null,
                true));
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
            new ListQueryRequest(
                underlay,
                occurrenceEntity,
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null,
                null,
                null,
                true));
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
            false,
            false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null,
                null,
                null,
                true));
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
            new ListQueryRequest(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null,
                null,
                null,
                true));
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
            new ListQueryRequest(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null,
                null,
                null,
                true));
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
            false,
            false);
    SqlQueryRequest sqlQueryRequest =
        bqQueryRunner.buildListQuerySql(
            new ListQueryRequest(
                underlay,
                pulsePerson.getGroupEntity(),
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null,
                null,
                null,
                true));
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
            false,
            false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                groupItems.getItemsEntity(),
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null,
                null,
                null,
                true));
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
            new ListQueryRequest(
                underlay,
                groupItems.getItemsEntity(),
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null,
                null,
                null,
                true));
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
            new ListQueryRequest(
                underlay,
                groupItems.getItemsEntity(),
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null,
                null,
                null,
                true));
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
            false,
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
            new ListQueryRequest(
                underlay,
                groupItems.getGroupEntity(),
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null,
                null,
                null,
                true));
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
            occurrenceEntity.getAttribute("start_date"),
            BinaryOperator.GREATER_THAN,
            1);
    AttributeField simpleAttribute =
        new AttributeField(
            underlay,
            underlay.getPrimaryEntity(),
            underlay.getPrimaryEntity().getAttribute("year_of_birth"),
            false,
            false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null,
                null,
                null,
                true));
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
            occurrenceEntity.getAttribute("start_date"),
            BinaryOperator.GREATER_THAN,
            1);
    listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null,
                null,
                null,
                true));
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
            occurrenceEntity.getAttribute("start_date"),
            BinaryOperator.GREATER_THAN,
            14);
    listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null,
                null,
                null,
                true));
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
            null,
            BinaryOperator.EQUALS,
            1);
    AttributeField simpleAttribute =
        new AttributeField(
            underlay,
            groupItems.getItemsEntity(),
            groupItems.getItemsEntity().getAttribute("name"),
            false,
            false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                groupItems.getItemsEntity(),
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null,
                null,
                null,
                true));
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
            null,
            BinaryOperator.EQUALS,
            1);
    listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                groupItems.getItemsEntity(),
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null,
                null,
                null,
                true));
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
            new ListQueryRequest(
                underlay,
                groupItems.getItemsEntity(),
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null,
                null,
                null,
                true));
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
            underlay, occurrenceEntity, occurrenceEntity.getAttribute("start_date"), false, false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                occurrenceEntity,
                List.of(simpleAttribute),
                occurrenceFilter,
                null,
                null,
                null,
                null,
                true));
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
            underlay, occurrenceEntity, occurrenceEntity.getAttribute("start_date"), false, false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                occurrenceEntity,
                List.of(simpleAttribute),
                occurrenceFilter,
                null,
                null,
                null,
                null,
                true));
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
            underlay, occurrenceEntity, occurrenceEntity.getAttribute("start_date"), false, false);
    SqlQueryRequest sqlQueryRequest =
        bqQueryRunner.buildListQuerySql(
            new ListQueryRequest(
                underlay,
                occurrenceEntity,
                List.of(simpleAttribute),
                occurrenceFilter,
                null,
                null,
                null,
                null,
                true));
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
            underlay, occurrenceEntity, occurrenceEntity.getAttribute("person_id"), false, false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                occurrenceEntity,
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null,
                null,
                null,
                true));
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
            underlay, occurrenceEntity, occurrenceEntity.getAttribute("person_id"), false, false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                occurrenceEntity,
                List.of(simpleAttribute),
                relationshipFilter,
                null,
                null,
                null,
                null,
                true));
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

  @Test
  void textSearchFilter() throws IOException {
    Entity entity = underlay.getEntity("condition");
    TextSearchFilter textSearchFilter =
        new TextSearchFilter(
            underlay, entity, TextSearchFilter.TextSearchOperator.EXACT_MATCH, "diabetes", null);
    AttributeField simpleAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("name"), false, false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                entity,
                List.of(simpleAttribute),
                textSearchFilter,
                null,
                null,
                null,
                null,
                true));
    BQTable table = underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("textSearchFilterIndex", listQueryResult.getSql(), table);

    textSearchFilter =
        new TextSearchFilter(
            underlay,
            entity,
            TextSearchFilter.TextSearchOperator.EXACT_MATCH,
            "diabetes",
            entity.getAttribute("name"));
    listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                entity,
                List.of(simpleAttribute),
                textSearchFilter,
                null,
                null,
                null,
                null,
                true));
    assertSqlMatchesWithTableNameOnly("textSearchFilterAttribute", listQueryResult.getSql(), table);
  }

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
            false,
            false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                primaryWithCriteriaFilter,
                null,
                null,
                null,
                null,
                true));

    List<BQTable> tableNamesToSubstitute = new ArrayList<>();
    criteriaOccurrence.getOccurrenceEntities().stream()
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
            new ListQueryRequest(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                primaryWithCriteriaFilter,
                null,
                null,
                null,
                null,
                true));
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
            false,
            false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                primaryWithCriteriaFilter,
                null,
                null,
                null,
                null,
                true));

    List<BQTable> tableNamesToSubstitute = new ArrayList<>();
    criteriaOccurrence.getOccurrenceEntities().stream()
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
    criteriaOccurrence.getOccurrenceEntities().stream()
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
            new ListQueryRequest(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                primaryWithCriteriaFilter,
                null,
                null,
                null,
                null,
                true));
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
            false,
            false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                primaryWithCriteriaFilter,
                null,
                null,
                null,
                null,
                true));

    List<BQTable> tableNamesToSubstitute = new ArrayList<>();
    criteriaOccurrence.getOccurrenceEntities().stream()
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
            Map.of(conditionOccurrence, List.of(conditionOccurrence.getAttribute("start_date"))),
            BinaryOperator.EQUALS,
            4);
    listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                primaryWithCriteriaFilter,
                null,
                null,
                null,
                null,
                true));
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
                conditionOccurrence.getAttribute("age_at_occurrence"),
                conditionOccurrence.getAttribute("start_date")),
            observationOccurrence,
            List.of(
                observationOccurrence.getAttribute("age_at_occurrence"),
                observationOccurrence.getAttribute("date")),
            procedureOccurrence,
            List.of(
                procedureOccurrence.getAttribute("age_at_occurrence"),
                procedureOccurrence.getAttribute("date")));
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
            false,
            false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                primaryWithCriteriaFilter,
                null,
                null,
                null,
                null,
                true));

    List<BQTable> tableNamesToSubstitute = new ArrayList<>();
    criteriaOccurrence.getOccurrenceEntities().stream()
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
    criteriaOccurrence.getOccurrenceEntities().stream()
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
            new ListQueryRequest(
                underlay,
                underlay.getPrimaryEntity(),
                List.of(simpleAttribute),
                primaryWithCriteriaFilter,
                null,
                null,
                null,
                null,
                true));
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
            underlay, conditionOccurrence, conditionOccurrence.getIdAttribute(), false, false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                conditionOccurrence,
                List.of(simpleAttribute),
                occurrenceForPrimaryFilter,
                null,
                null,
                null,
                null,
                true));

    List<BQTable> tableNamesToSubstitute = new ArrayList<>();
    criteriaOccurrence.getOccurrenceEntities().stream()
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
            new ListQueryRequest(
                underlay,
                conditionOccurrence,
                List.of(simpleAttribute),
                occurrenceForPrimaryFilter,
                null,
                null,
                null,
                null,
                true));
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
            new ListQueryRequest(
                underlay,
                conditionOccurrence,
                List.of(simpleAttribute),
                occurrenceForPrimaryFilter,
                null,
                null,
                null,
                null,
                true));
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
                new ListQueryRequest(
                    underlay,
                    conditionOccurrence,
                    List.of(simpleAttribute),
                    invalidOccurrenceForPrimaryFilter,
                    null,
                    null,
                    null,
                    null,
                    true)));
  }

  @Test
  void itemInGroupFilter() throws IOException {
    GroupItems groupItems = (GroupItems) underlay.getEntityGroup("brandIngredient");

    // No group by.
    EntityFilter groupSubFilter =
        new AttributeFilter(
            underlay,
            groupItems.getGroupEntity(),
            groupItems.getGroupEntity().getIdAttribute(),
            BinaryOperator.EQUALS,
            Literal.forInt64(19_042_336L));
    ItemInGroupFilter itemInGroupFilter =
        new ItemInGroupFilter(underlay, groupItems, groupSubFilter, null, null, null);
    AttributeField simpleAttribute =
        new AttributeField(
            underlay,
            groupItems.getItemsEntity(),
            groupItems.getItemsEntity().getAttribute("name"),
            false,
            false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                groupItems.getItemsEntity(),
                List.of(simpleAttribute),
                itemInGroupFilter,
                null,
                null,
                null,
                null,
                true));

    BQTable groupEntityTable =
        underlay
            .getIndexSchema()
            .getEntityMain(groupItems.getGroupEntity().getName())
            .getTablePointer();
    BQTable itemsEntityTable =
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
        "itemInGroup", listQueryResult.getSql(), groupEntityTable, itemsEntityTable, idPairsTable);

    // With group by, no attribute.
    itemInGroupFilter =
        new ItemInGroupFilter(
            underlay, groupItems, groupSubFilter, null, BinaryOperator.GREATER_THAN, 2);
    listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                groupItems.getItemsEntity(),
                List.of(simpleAttribute),
                itemInGroupFilter,
                null,
                null,
                null,
                null,
                true));
    assertSqlMatchesWithTableNameOnly(
        "itemInGroupWithGroupBy",
        listQueryResult.getSql(),
        groupEntityTable,
        itemsEntityTable,
        idPairsTable);

    // With group by attribute.
    itemInGroupFilter =
        new ItemInGroupFilter(
            underlay,
            groupItems,
            groupSubFilter,
            groupItems.getItemsEntity().getAttribute("vocabulary"),
            BinaryOperator.GREATER_THAN,
            2);
    listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                groupItems.getItemsEntity(),
                List.of(simpleAttribute),
                itemInGroupFilter,
                null,
                null,
                null,
                null,
                true));
    assertSqlMatchesWithTableNameOnly(
        "itemInGroupWithGroupByAttribute",
        listQueryResult.getSql(),
        groupEntityTable,
        itemsEntityTable,
        idPairsTable);
  }

  @Test
  void groupHasItemsFilter() throws IOException {
    // Intermediate table.
    GroupItems groupItems = (GroupItems) underlay.getEntityGroup("brandIngredient");
    GroupHasItemsFilter groupHasItemsFilter = new GroupHasItemsFilter(underlay, groupItems);
    AttributeField simpleAttribute =
        new AttributeField(
            underlay,
            groupItems.getGroupEntity(),
            groupItems.getGroupEntity().getAttribute("name"),
            false,
            false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                groupItems.getGroupEntity(),
                List.of(simpleAttribute),
                groupHasItemsFilter,
                null,
                null,
                null,
                null,
                true));

    BQTable groupEntityTable =
        underlay
            .getIndexSchema()
            .getEntityMain(groupItems.getGroupEntity().getName())
            .getTablePointer();
    BQTable itemsEntityTable =
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
        "groupHasItemsIntTable",
        listQueryResult.getSql(),
        groupEntityTable,
        itemsEntityTable,
        idPairsTable);

    // Foreign key on items entity table.
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

    groupItems = (GroupItems) underlay.getEntityGroup("pulsePerson");
    groupHasItemsFilter = new GroupHasItemsFilter(underlay, groupItems);
    simpleAttribute =
        new AttributeField(
            underlay,
            groupItems.getGroupEntity(),
            groupItems.getGroupEntity().getIdAttribute(),
            false,
            false);
    SqlQueryRequest sqlQueryRequest =
        bqQueryRunner.buildListQuerySql(
            new ListQueryRequest(
                underlay,
                groupItems.getGroupEntity(),
                List.of(simpleAttribute),
                groupHasItemsFilter,
                null,
                null,
                null,
                null,
                true));

    groupEntityTable =
        underlay
            .getIndexSchema()
            .getEntityMain(groupItems.getGroupEntity().getName())
            .getTablePointer();
    itemsEntityTable =
        underlay
            .getIndexSchema()
            .getEntityMain(groupItems.getItemsEntity().getName())
            .getTablePointer();
    assertSqlMatchesWithTableNameOnly(
        "groupHasItemsFKItemsTable", sqlQueryRequest.getSql(), groupEntityTable, itemsEntityTable);
  }
}
