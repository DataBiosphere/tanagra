package bio.terra.tanagra.query.bigquery.sqlbuilding.filter;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.filter.HierarchyHasAncestorFilter;
import bio.terra.tanagra.api.filter.HierarchyHasParentFilter;
import bio.terra.tanagra.api.filter.HierarchyIsMemberFilter;
import bio.terra.tanagra.api.filter.HierarchyIsRootFilter;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.api.query.list.ListQueryResult;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.query.bigquery.BQRunnerTest;
import bio.terra.tanagra.query.bigquery.BQTable;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

public class BQHierarchyFilterTest extends BQRunnerTest {
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
        new AttributeField(underlay, entity, entity.getAttribute("name"), false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                entity,
                List.of(simpleAttribute),
                hierarchyHasAncestorFilter,
                null,
                null));
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
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                entity,
                List.of(simpleAttribute),
                hierarchyHasAncestorFilter,
                null,
                null));
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
        new AttributeField(underlay, entity, entity.getAttribute("name"), false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay, entity, List.of(simpleAttribute), hierarchyHasParentFilter, null, null));
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
            ListQueryRequest.dryRunAgainstIndexData(
                underlay, entity, List.of(simpleAttribute), hierarchyHasParentFilter, null, null));
    assertSqlMatchesWithTableNameOnly(
        "hierarchyHasParentFilterIn", listQueryResult.getSql(), entityMainTable, childParentTable);
  }

  @Test
  void hierarchyIsMemberFilter() throws IOException {
    Entity entity = underlay.getEntity("condition");
    HierarchyIsMemberFilter hierarchyIsMemberFilter =
        new HierarchyIsMemberFilter(underlay, entity, entity.getHierarchy(Hierarchy.DEFAULT_NAME));
    AttributeField simpleAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("name"), false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay, entity, List.of(simpleAttribute), hierarchyIsMemberFilter, null, null));
    BQTable table = underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("hierarchyIsMemberFilter", listQueryResult.getSql(), table);
  }

  @Test
  void hierarchyIsRootFilter() throws IOException {
    Entity entity = underlay.getEntity("condition");
    HierarchyIsRootFilter hierarchyIsRootFilter =
        new HierarchyIsRootFilter(underlay, entity, entity.getHierarchy(Hierarchy.DEFAULT_NAME));
    AttributeField simpleAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("name"), false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay, entity, List.of(simpleAttribute), hierarchyIsRootFilter, null, null));
    BQTable table = underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("hierarchyIsRootFilter", listQueryResult.getSql(), table);
  }
}
