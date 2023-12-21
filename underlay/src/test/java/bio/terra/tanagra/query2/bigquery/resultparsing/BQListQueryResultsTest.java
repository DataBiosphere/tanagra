package bio.terra.tanagra.query2.bigquery.resultparsing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import bio.terra.tanagra.api.field.valuedisplay.AttributeField;
import bio.terra.tanagra.api.field.valuedisplay.EntityIdCountField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyIsMemberField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyIsRootField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyNumChildrenField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyPathField;
import bio.terra.tanagra.api.field.valuedisplay.RelatedEntityIdCountField;
import bio.terra.tanagra.api.field.valuedisplay.ValueDisplayField;
import bio.terra.tanagra.api.query.ValueDisplay;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.api.query.list.ListQueryResult;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.OrderByDirection;
import bio.terra.tanagra.query2.bigquery.BQRunnerTest;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.EntityGroup;
import java.util.List;
import org.junit.jupiter.api.Test;

public class BQListQueryResultsTest extends BQRunnerTest {
  @Test
  void attributeField() {
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
    List<ListQueryRequest.OrderBy> orderBys =
        List.of(new ListQueryRequest.OrderBy(idAttribute, OrderByDirection.DESCENDING));
    int limit = 5;
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay, entity, selectAttributes, null, orderBys, limit, null, null, false));

    // Make sure we got the right number of results back.
    assertEquals(limit, listQueryResult.getListInstances().size());

    // Check each of the selected fields.
    listQueryResult.getListInstances().stream()
        .forEach(
            listInstance -> {
              ValueDisplay yearOfBirth = listInstance.getEntityFieldValue(simpleAttribute);
              assertNotNull(yearOfBirth);
              assertEquals(Literal.DataType.INT64, yearOfBirth.getValue().getDataType());
              assertNotNull(yearOfBirth.getValue().getInt64Val());
              assertNull(yearOfBirth.getDisplay());

              ValueDisplay gender = listInstance.getEntityFieldValue(valueDisplayAttribute);
              assertNotNull(gender);
              assertEquals(Literal.DataType.INT64, gender.getValue().getDataType());
              assertNotNull(gender.getValue().getInt64Val());
              assertNotNull(gender.getDisplay());

              ValueDisplay race =
                  listInstance.getEntityFieldValue(valueDisplayAttributeWithoutDisplay);
              assertNotNull(race);
              assertEquals(Literal.DataType.INT64, race.getValue().getDataType());
              assertNotNull(race.getValue().getInt64Val());
              assertNull(race.getDisplay());

              ValueDisplay age = listInstance.getEntityFieldValue(runtimeCalculatedAttribute);
              assertNotNull(age);
              assertEquals(Literal.DataType.INT64, age.getValue().getDataType());
              assertNotNull(age.getValue().getInt64Val());
              assertNull(age.getDisplay());
            });
  }

  @Test
  void entityIdCountField() {
    Entity entity = underlay.getPrimaryEntity();
    EntityIdCountField entityIdCountField = new EntityIdCountField(underlay, entity);

    List<ValueDisplayField> selectAttributes = List.of(entityIdCountField);
    List<ListQueryRequest.OrderBy> orderBys =
        List.of(new ListQueryRequest.OrderBy(entityIdCountField, OrderByDirection.DESCENDING));
    int limit = 11;
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay, entity, selectAttributes, null, orderBys, limit, null, null, false));

    // Make sure we got the right number of results back.
    assertEquals(1, listQueryResult.getListInstances().size());

    // Check each of the selected fields.
    listQueryResult.getListInstances().stream()
        .forEach(
            listInstance -> {
              ValueDisplay entityIdCount = listInstance.getEntityFieldValue(entityIdCountField);
              assertNotNull(entityIdCount);
              assertEquals(Literal.DataType.INT64, entityIdCount.getValue().getDataType());
              assertNotNull(entityIdCount.getValue().getInt64Val());
              assertNull(entityIdCount.getDisplay());
            });
  }

  @Test
  void hierarchyFields() {
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
    List<ListQueryRequest.OrderBy> orderBys =
        List.of(
            new ListQueryRequest.OrderBy(hierarchyNumChildrenField, OrderByDirection.DESCENDING));
    int limit = 9;
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay, entity, selectAttributes, null, orderBys, limit, null, null, false));

    // Make sure we got the right number of results back.
    assertEquals(limit, listQueryResult.getListInstances().size());

    // Check each of the selected fields.
    listQueryResult.getListInstances().stream()
        .forEach(
            listInstance -> {
              ValueDisplay isMember = listInstance.getEntityFieldValue(hierarchyIsMemberField);
              assertNotNull(isMember);
              assertEquals(Literal.DataType.BOOLEAN, isMember.getValue().getDataType());
              assertNotNull(isMember.getValue().getBooleanVal());
              assertNull(isMember.getDisplay());

              ValueDisplay isRoot = listInstance.getEntityFieldValue(hierarchyIsRootField);
              assertNotNull(isRoot);
              assertEquals(Literal.DataType.BOOLEAN, isRoot.getValue().getDataType());
              assertNotNull(isRoot.getValue().getBooleanVal());
              assertNull(isRoot.getDisplay());

              ValueDisplay numChildren =
                  listInstance.getEntityFieldValue(hierarchyNumChildrenField);
              assertNotNull(numChildren);
              assertEquals(Literal.DataType.INT64, numChildren.getValue().getDataType());
              assertNotNull(numChildren.getValue().getInt64Val());
              assertNull(numChildren.getDisplay());

              ValueDisplay path = listInstance.getEntityFieldValue(hierarchyPathField);
              assertNotNull(path);
              assertEquals(Literal.DataType.STRING, path.getValue().getDataType());
              assertNotNull(path.getValue().getStringVal());
              assertNull(path.getDisplay());
            });
  }

  @Test
  void relatedEntityIdCountField() {
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
    List<ListQueryRequest.OrderBy> orderBys =
        List.of(
            new ListQueryRequest.OrderBy(
                relatedEntityIdCountFieldNoHier, OrderByDirection.DESCENDING),
            new ListQueryRequest.OrderBy(
                relatedEntityIdCountFieldWithHier, OrderByDirection.ASCENDING));
    int limit = 14;
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            new ListQueryRequest(
                underlay,
                countForEntity,
                selectAttributes,
                null,
                orderBys,
                limit,
                null,
                null,
                false));

    // Make sure we got the right number of results back.
    assertEquals(limit, listQueryResult.getListInstances().size());

    // Check each of the selected fields.
    listQueryResult.getListInstances().stream()
        .forEach(
            listInstance -> {
              ValueDisplay countNoHier =
                  listInstance.getEntityFieldValue(relatedEntityIdCountFieldNoHier);
              assertNotNull(countNoHier);
              assertEquals(Literal.DataType.INT64, countNoHier.getValue().getDataType());
              assertNotNull(countNoHier.getValue().getInt64Val());
              assertNull(countNoHier.getDisplay());

              ValueDisplay countWithHier =
                  listInstance.getEntityFieldValue(relatedEntityIdCountFieldWithHier);
              assertNotNull(countWithHier);
              assertEquals(Literal.DataType.INT64, countWithHier.getValue().getDataType());
              assertNotNull(countWithHier.getValue().getInt64Val());
              assertNull(countWithHier.getDisplay());
            });
  }
}
