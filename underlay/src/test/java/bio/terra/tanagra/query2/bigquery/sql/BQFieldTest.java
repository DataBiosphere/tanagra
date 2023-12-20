package bio.terra.tanagra.query2.bigquery.sql;

import bio.terra.tanagra.api.field.valuedisplay.AttributeField;
import bio.terra.tanagra.api.field.valuedisplay.EntityIdCountField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyIsMemberField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyIsRootField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyNumChildrenField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyPathField;
import bio.terra.tanagra.api.field.valuedisplay.RelatedEntityIdCountField;
import bio.terra.tanagra.api.field.valuedisplay.ValueDisplayField;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.api.query.list.ListQueryRequest.OrderBy;
import bio.terra.tanagra.api.query.list.ListQueryResult;
import bio.terra.tanagra.query.OrderByDirection;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.query2.bigquery.BQRunnerTest;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.EntityGroup;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

public class BQFieldTest extends BQRunnerTest {

  @Test
  void attributeField() throws IOException {
    Entity entity = underlay.getPrimaryEntity();
    AttributeField simpleAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("year_of_birth"), false, false);
    AttributeField valueDisplayAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("gender"), false, false);
    AttributeField valueDisplayAttributeWithoutDisplay =
        new AttributeField(underlay, entity, entity.getAttribute("race"), true, false);
    AttributeField runtimeCalculatedAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("age"), false, false);
    AttributeField idAttribute =
        new AttributeField(underlay, entity, entity.getIdAttribute(), false, false);

    List<ValueDisplayField> selectAttributes =
        List.of(
            simpleAttribute,
            valueDisplayAttribute,
            valueDisplayAttributeWithoutDisplay,
            runtimeCalculatedAttribute);
    List<OrderBy> orderBys =
        List.of(
            new OrderBy(simpleAttribute, OrderByDirection.ASCENDING),
            new OrderBy(valueDisplayAttribute, OrderByDirection.DESCENDING),
            new OrderBy(runtimeCalculatedAttribute, OrderByDirection.ASCENDING),
            new OrderBy(idAttribute, OrderByDirection.DESCENDING));
    int limit = 35;
    ListQueryResult listQueryResult =
        BQQueryRunner.run(
            new ListQueryRequest(
                underlay, entity, selectAttributes, null, orderBys, limit, null, null, true));

    TablePointer table =
        underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("attributeField", listQueryResult.getSql(), table);
  }

  @Test
  void entityIdCountField() throws IOException {
    Entity entity = underlay.getPrimaryEntity();
    EntityIdCountField entityIdCountField = new EntityIdCountField(underlay, entity);

    List<ValueDisplayField> selectAttributes = List.of(entityIdCountField);
    List<OrderBy> orderBys = List.of(new OrderBy(entityIdCountField, OrderByDirection.DESCENDING));
    ListQueryResult listQueryResult =
        BQQueryRunner.run(
            new ListQueryRequest(
                underlay, entity, selectAttributes, null, orderBys, null, null, null, true));

    TablePointer table =
        underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("entityIdCountField", listQueryResult.getSql(), table);
  }

  @Test
  void hierarchyFields() throws IOException {
    Entity entity = underlay.getEntity("condition");
    Hierarchy hierarchy = entity.getHierarchy(Hierarchy.DEFAULT_NAME);
    HierarchyIsMemberField hierarchyIsMemberField =
        new HierarchyIsMemberField(underlay, entity, hierarchy);
    HierarchyIsRootField hierarchyIsRootField =
        new HierarchyIsRootField(underlay, entity, hierarchy);
    HierarchyNumChildrenField hierarchyNumChildrenField =
        new HierarchyNumChildrenField(underlay, entity, hierarchy);
    HierarchyPathField hierarchyPathField = new HierarchyPathField(underlay, entity, hierarchy);

    List<ValueDisplayField> selectAttributes =
        List.of(
            hierarchyIsMemberField,
            hierarchyIsRootField,
            hierarchyNumChildrenField,
            hierarchyPathField);
    List<OrderBy> orderBys =
        List.of(new OrderBy(hierarchyNumChildrenField, OrderByDirection.DESCENDING));
    ListQueryResult listQueryResult =
        BQQueryRunner.run(
            new ListQueryRequest(
                underlay, entity, selectAttributes, null, orderBys, null, null, null, true));

    TablePointer table =
        underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("hierarchyFields", listQueryResult.getSql(), table);
  }

  @Test
  void relatedEntityIdCountField() throws IOException {
    Entity countForEntity = underlay.getEntity("condition");
    Hierarchy hierarchy = countForEntity.getHierarchy(Hierarchy.DEFAULT_NAME);
    Entity countedEntity = underlay.getPrimaryEntity();
    EntityGroup entityGroup = underlay.getEntityGroup("conditionPerson");
    RelatedEntityIdCountField relatedEntityIdCountFieldNoHier =
        new RelatedEntityIdCountField(underlay, countForEntity, countedEntity, entityGroup, null);
    RelatedEntityIdCountField relatedEntityIdCountFieldWithHier =
        new RelatedEntityIdCountField(
            underlay, countForEntity, countedEntity, entityGroup, hierarchy);

    List<ValueDisplayField> selectAttributes =
        List.of(relatedEntityIdCountFieldNoHier, relatedEntityIdCountFieldWithHier);
    List<OrderBy> orderBys =
        List.of(
            new OrderBy(relatedEntityIdCountFieldNoHier, OrderByDirection.DESCENDING),
            new OrderBy(relatedEntityIdCountFieldWithHier, OrderByDirection.ASCENDING));
    ListQueryResult listQueryResult =
        BQQueryRunner.run(
            new ListQueryRequest(
                underlay,
                countForEntity,
                selectAttributes,
                null,
                orderBys,
                null,
                null,
                null,
                true));

    TablePointer table =
        underlay.getIndexSchema().getEntityMain(countForEntity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("relatedEntityIdCountField", listQueryResult.getSql(), table);
  }
}
