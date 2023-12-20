package bio.terra.tanagra.query2.bigquery;

import bio.terra.tanagra.api.field.valuedisplay.AttributeField;
import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.BooleanNotFilter;
import bio.terra.tanagra.api.filter.HierarchyHasAncestorFilter;
import bio.terra.tanagra.api.filter.HierarchyHasParentFilter;
import bio.terra.tanagra.api.filter.HierarchyIsMemberFilter;
import bio.terra.tanagra.api.filter.HierarchyIsRootFilter;
import bio.terra.tanagra.api.filter.RelationshipFilter;
import bio.terra.tanagra.api.filter.TextSearchFilter;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.api.query.list.ListQueryResult;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable;
import bio.terra.tanagra.query.filtervariable.FunctionFilterVariable;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.GroupItems;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

public class BQFilterTest extends BQRunnerTest {
  @Test
  void attributeFilter() throws IOException {
    // Filter with binary operator.
    Entity entity = underlay.getPrimaryEntity();
    Attribute attribute = entity.getAttribute("year_of_birth");
    AttributeFilter attributeFilter =
        new AttributeFilter(
            underlay,
            entity,
            attribute,
            BinaryFilterVariable.BinaryOperator.NOT_EQUALS,
            new Literal(1956));
    AttributeField simpleAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("year_of_birth"), false, false);
    ListQueryResult listQueryResult =
        BQQueryRunner.run(
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
    TablePointer table =
        underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("attributeFilterBinary", listQueryResult.getSql(), table);

    // Filter with function template.
    attribute = entity.getAttribute("age");
    attributeFilter =
        new AttributeFilter(
            underlay,
            entity,
            attribute,
            FunctionFilterVariable.FunctionTemplate.NOT_IN,
            List.of(new Literal(18), new Literal(19)));
    listQueryResult =
        BQQueryRunner.run(
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
    assertSqlMatchesWithTableNameOnly("attributeFilterFunction", listQueryResult.getSql(), table);
  }

  @Test
  void booleanAndOrFilter() throws IOException {
    Entity entity = underlay.getEntity("condition");
    Attribute attribute = entity.getAttribute("concept_code");
    AttributeFilter attributeFilter =
        new AttributeFilter(
            underlay,
            entity,
            attribute,
            BinaryFilterVariable.BinaryOperator.NOT_EQUALS,
            new Literal("1956"));
    TextSearchFilter textSearchFilter =
        new TextSearchFilter(
            underlay,
            entity,
            FunctionFilterVariable.FunctionTemplate.TEXT_EXACT_MATCH,
            "44054006",
            null);
    BooleanAndOrFilter booleanAndOrFilter =
        new BooleanAndOrFilter(
            BooleanAndOrFilterVariable.LogicalOperator.AND,
            List.of(attributeFilter, textSearchFilter));
    AttributeField simpleAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("name"), false, false);
    ListQueryResult listQueryResult =
        BQQueryRunner.run(
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
    TablePointer table =
        underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("booleanAndOrFilter", listQueryResult.getSql(), table);
  }

  @Test
  void booleanNotFilter() throws IOException {
    Entity entity = underlay.getPrimaryEntity();
    Attribute attribute = entity.getAttribute("year_of_birth");
    AttributeFilter attributeFilter =
        new AttributeFilter(
            underlay,
            entity,
            attribute,
            BinaryFilterVariable.BinaryOperator.NOT_EQUALS,
            new Literal(1956));
    BooleanNotFilter booleanNotFilter = new BooleanNotFilter(attributeFilter);
    AttributeField simpleAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("year_of_birth"), false, false);
    ListQueryResult listQueryResult =
        BQQueryRunner.run(
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
    TablePointer table =
        underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("booleanNotFilter", listQueryResult.getSql(), table);
  }

  @Test
  void hierarchyHasAncestorFilter() throws IOException {
    Entity entity = underlay.getEntity("condition");
    HierarchyHasAncestorFilter hierarchyHasAncestorFilter =
        new HierarchyHasAncestorFilter(
            underlay, entity, entity.getHierarchy(Hierarchy.DEFAULT_NAME), new Literal(201_826L));
    AttributeField simpleAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("name"), false, false);
    ListQueryResult listQueryResult =
        BQQueryRunner.run(
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
    TablePointer entityMainTable =
        underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    TablePointer ancestorDescendantTable =
        underlay
            .getIndexSchema()
            .getHierarchyAncestorDescendant(entity.getName(), Hierarchy.DEFAULT_NAME)
            .getTablePointer();
    assertSqlMatchesWithTableNameOnly(
        "hierarchyHasAncestorFilter",
        listQueryResult.getSql(),
        entityMainTable,
        ancestorDescendantTable);
  }

  @Test
  void hierarchyHasParentFilter() throws IOException {
    Entity entity = underlay.getEntity("condition");
    HierarchyHasParentFilter hierarchyHasParentFilter =
        new HierarchyHasParentFilter(
            underlay, entity, entity.getHierarchy(Hierarchy.DEFAULT_NAME), new Literal(201_826L));
    AttributeField simpleAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("name"), false, false);
    ListQueryResult listQueryResult =
        BQQueryRunner.run(
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
    TablePointer entityMainTable =
        underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    TablePointer childParentTable =
        underlay
            .getIndexSchema()
            .getHierarchyChildParent(entity.getName(), Hierarchy.DEFAULT_NAME)
            .getTablePointer();
    assertSqlMatchesWithTableNameOnly(
        "hierarchyHasParentFilter", listQueryResult.getSql(), entityMainTable, childParentTable);
  }

  @Test
  void hierarchyIsMemberFilter() throws IOException {
    Entity entity = underlay.getEntity("condition");
    HierarchyIsMemberFilter hierarchyIsMemberFilter =
        new HierarchyIsMemberFilter(underlay, entity, entity.getHierarchy(Hierarchy.DEFAULT_NAME));
    AttributeField simpleAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("name"), false, false);
    ListQueryResult listQueryResult =
        BQQueryRunner.run(
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
    TablePointer table =
        underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
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
        BQQueryRunner.run(
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
    TablePointer table =
        underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
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
            BinaryFilterVariable.BinaryOperator.EQUALS,
            new Literal(456L));
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
        BQQueryRunner.run(
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
    TablePointer occurrenceTable =
        underlay.getIndexSchema().getEntityMain(occurrenceEntity.getName()).getTablePointer();
    TablePointer primaryTable =
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
            BinaryFilterVariable.BinaryOperator.EQUALS,
            new Literal(8_207L));
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
        BQQueryRunner.run(
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
            BinaryFilterVariable.BinaryOperator.EQUALS,
            new Literal(15));
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
        BQQueryRunner.run(
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
    TablePointer occurrenceTable =
        underlay.getIndexSchema().getEntityMain(occurrenceEntity.getName()).getTablePointer();
    TablePointer primaryTable =
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
            FunctionFilterVariable.FunctionTemplate.IS_NULL,
            List.of());
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
        BQQueryRunner.run(
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
            BinaryFilterVariable.BinaryOperator.EQUALS,
            new Literal(15));
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
        BQQueryRunner.run(
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
    TablePointer groupTable =
        underlay
            .getIndexSchema()
            .getEntityMain(groupItems.getGroupEntity().getName())
            .getTablePointer();
    TablePointer itemsTable =
        underlay
            .getIndexSchema()
            .getEntityMain(groupItems.getItemsEntity().getName())
            .getTablePointer();
    TablePointer idPairsTable =
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
            BinaryFilterVariable.BinaryOperator.EQUALS,
            new Literal("161"));
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
        BQQueryRunner.run(
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
            BinaryFilterVariable.BinaryOperator.EQUALS,
            new Literal(15));
    RelationshipFilter relationshipFilter =
        new RelationshipFilter(
            underlay,
            criteriaOccurrence,
            underlay.getPrimaryEntity(),
            criteriaOccurrence.getOccurrencePrimaryRelationship(occurrenceEntity.getName()),
            attributeFilter,
            occurrenceEntity.getAttribute("start_date"),
            BinaryFilterVariable.BinaryOperator.GREATER_THAN,
            1);
    AttributeField simpleAttribute =
        new AttributeField(
            underlay,
            underlay.getPrimaryEntity(),
            underlay.getPrimaryEntity().getAttribute("year_of_birth"),
            false,
            false);
    ListQueryResult listQueryResult =
        BQQueryRunner.run(
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
    TablePointer occurrenceTable =
        underlay.getIndexSchema().getEntityMain(occurrenceEntity.getName()).getTablePointer();
    TablePointer primaryTable =
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
            FunctionFilterVariable.FunctionTemplate.IS_NULL,
            List.of());
    relationshipFilter =
        new RelationshipFilter(
            underlay,
            criteriaOccurrence,
            underlay.getPrimaryEntity(),
            criteriaOccurrence.getOccurrencePrimaryRelationship(occurrenceEntity.getName()),
            attributeFilter,
            occurrenceEntity.getAttribute("start_date"),
            BinaryFilterVariable.BinaryOperator.GREATER_THAN,
            1);
    listQueryResult =
        BQQueryRunner.run(
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
            BinaryFilterVariable.BinaryOperator.EQUALS,
            new Literal(15));
    RelationshipFilter relationshipFilter =
        new RelationshipFilter(
            underlay,
            groupItems,
            groupItems.getItemsEntity(),
            groupItems.getGroupItemsRelationship(),
            attributeFilter,
            null,
            BinaryFilterVariable.BinaryOperator.EQUALS,
            1);
    AttributeField simpleAttribute =
        new AttributeField(
            underlay,
            groupItems.getItemsEntity(),
            groupItems.getItemsEntity().getAttribute("name"),
            false,
            false);
    ListQueryResult listQueryResult =
        BQQueryRunner.run(
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
    TablePointer groupTable =
        underlay
            .getIndexSchema()
            .getEntityMain(groupItems.getGroupEntity().getName())
            .getTablePointer();
    TablePointer itemsTable =
        underlay
            .getIndexSchema()
            .getEntityMain(groupItems.getItemsEntity().getName())
            .getTablePointer();
    TablePointer idPairsTable =
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
            BinaryFilterVariable.BinaryOperator.EQUALS,
            new Literal("161"));
    relationshipFilter =
        new RelationshipFilter(
            underlay,
            groupItems,
            groupItems.getItemsEntity(),
            groupItems.getGroupItemsRelationship(),
            attributeFilter,
            null,
            BinaryFilterVariable.BinaryOperator.EQUALS,
            1);
    listQueryResult =
        BQQueryRunner.run(
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
  }

  @Test
  void textSearchFilter() throws IOException {
    Entity entity = underlay.getEntity("condition");
    TextSearchFilter textSearchFilter =
        new TextSearchFilter(
            underlay,
            entity,
            FunctionFilterVariable.FunctionTemplate.TEXT_EXACT_MATCH,
            "diabetes",
            null);
    AttributeField simpleAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("name"), false, false);
    ListQueryResult listQueryResult =
        BQQueryRunner.run(
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
    TablePointer table =
        underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("textSearchFilterIndex", listQueryResult.getSql(), table);

    textSearchFilter =
        new TextSearchFilter(
            underlay,
            entity,
            FunctionFilterVariable.FunctionTemplate.TEXT_EXACT_MATCH,
            "diabetes",
            entity.getAttribute("name"));
    listQueryResult =
        BQQueryRunner.run(
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
}
