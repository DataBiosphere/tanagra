package bio.terra.tanagra.query.bigquery.resultparsing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.field.HierarchyIsMemberField;
import bio.terra.tanagra.api.field.HierarchyIsRootField;
import bio.terra.tanagra.api.field.HierarchyNumChildrenField;
import bio.terra.tanagra.api.field.HierarchyPathField;
import bio.terra.tanagra.api.field.RelatedEntityIdCountField;
import bio.terra.tanagra.api.field.ValueDisplayField;
import bio.terra.tanagra.api.query.count.CountQueryRequest;
import bio.terra.tanagra.api.query.count.CountQueryResult;
import bio.terra.tanagra.api.query.hint.HintInstance;
import bio.terra.tanagra.api.query.hint.HintQueryResult;
import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.ValueDisplay;
import bio.terra.tanagra.query.bigquery.BQRunnerTest;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.EntityGroup;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class BQCountQueryResultsTest extends BQRunnerTest {
  @Test
  void attributeField() {
    Entity entity = underlay.getPrimaryEntity();
    AttributeField simpleAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("year_of_birth"), false);
    AttributeField valueDisplayAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("gender"), false);
    AttributeField valueDisplayAttributeWithoutDisplay =
        new AttributeField(underlay, entity, entity.getAttribute("race"), true);
    AttributeField runtimeCalculatedAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("age"), false);

    List<ValueDisplayField> groupBys =
        List.of(
            simpleAttribute,
            valueDisplayAttribute,
            valueDisplayAttributeWithoutDisplay,
            runtimeCalculatedAttribute);
    HintQueryResult entityLevelHints =
        new HintQueryResult(
            "",
            List.of(
                new HintInstance(
                    entity.getAttribute("gender"),
                    Map.of(
                        new ValueDisplay(Literal.forInt64(8_507L), "MALE"),
                        111L,
                        new ValueDisplay(Literal.forInt64(8_532L), "FEMALE"),
                        222L))));
    CountQueryResult countQueryResult =
        bqQueryRunner.run(
            new CountQueryRequest(
                underlay, entity, groupBys, null, null, null, entityLevelHints, false));

    // Make sure we got some results back.
    assertFalse(countQueryResult.getCountInstances().isEmpty());

    // Check each of the group by fields.
    countQueryResult
        .getCountInstances()
        .forEach(
            countInstance -> {
              ValueDisplay yearOfBirth = countInstance.getEntityFieldValue(simpleAttribute);
              assertNotNull(yearOfBirth);
              assertTrue(
                  yearOfBirth.getValue().isNull()
                      || DataType.INT64.equals(yearOfBirth.getValue().getDataType()));
              assertNotNull(yearOfBirth.getValue().getInt64Val());
              assertNull(yearOfBirth.getDisplay());

              ValueDisplay gender = countInstance.getEntityFieldValue(valueDisplayAttribute);
              assertNotNull(gender);
              assertTrue(
                  gender.getValue().isNull()
                      || DataType.INT64.equals(gender.getValue().getDataType()));
              assertNotNull(gender.getValue().getInt64Val());
              assertNotNull(gender.getDisplay());

              ValueDisplay race =
                  countInstance.getEntityFieldValue(valueDisplayAttributeWithoutDisplay);
              assertNotNull(race);
              assertTrue(
                  race.getValue().isNull() || DataType.INT64.equals(race.getValue().getDataType()));
              assertNotNull(race.getValue().getInt64Val());
              assertNull(race.getDisplay());

              ValueDisplay age = countInstance.getEntityFieldValue(runtimeCalculatedAttribute);
              assertNotNull(age);
              assertTrue(
                  age.getValue().isNull() || DataType.INT64.equals(age.getValue().getDataType()));
              assertNotNull(age.getValue().getInt64Val());
              assertNull(age.getDisplay());
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

    List<ValueDisplayField> groupBys =
        List.of(
            hierarchyIsMemberField,
            hierarchyIsRootField,
            hierarchyNumChildrenField,
            hierarchyPathField);
    CountQueryResult countQueryResult =
        bqQueryRunner.run(
            new CountQueryRequest(underlay, entity, groupBys, null, null, null, null, false));

    // Make sure we got some results back.
    assertFalse(countQueryResult.getCountInstances().isEmpty());

    // Check each of the group by fields.
    countQueryResult
        .getCountInstances()
        .forEach(
            countInstance -> {
              ValueDisplay isMember = countInstance.getEntityFieldValue(hierarchyIsMemberField);
              assertNotNull(isMember);
              assertEquals(DataType.BOOLEAN, isMember.getValue().getDataType());
              assertNotNull(isMember.getValue().getBooleanVal());
              assertNull(isMember.getDisplay());

              ValueDisplay isRoot = countInstance.getEntityFieldValue(hierarchyIsRootField);
              assertNotNull(isRoot);
              assertEquals(DataType.BOOLEAN, isRoot.getValue().getDataType());
              assertNotNull(isRoot.getValue().getBooleanVal());
              assertNull(isRoot.getDisplay());

              ValueDisplay numChildren =
                  countInstance.getEntityFieldValue(hierarchyNumChildrenField);
              assertNotNull(numChildren);
              assertEquals(DataType.INT64, numChildren.getValue().getDataType());
              assertNotNull(numChildren.getValue().getInt64Val());
              assertNull(numChildren.getDisplay());

              ValueDisplay path = countInstance.getEntityFieldValue(hierarchyPathField);
              assertNotNull(path);
              assertTrue(
                  path.getValue().isNull()
                      || path.getValue().getDataType().equals(DataType.STRING));
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

    List<ValueDisplayField> groupBys =
        List.of(relatedEntityIdCountFieldNoHier, relatedEntityIdCountFieldWithHier);
    CountQueryResult countQueryResult =
        bqQueryRunner.run(
            new CountQueryRequest(
                underlay, countForEntity, groupBys, null, null, null, null, false));

    // Make sure we got some results back.
    assertFalse(countQueryResult.getCountInstances().isEmpty());

    // Check each of the group by fields.
    countQueryResult
        .getCountInstances()
        .forEach(
            countInstance -> {
              ValueDisplay countNoHier =
                  countInstance.getEntityFieldValue(relatedEntityIdCountFieldNoHier);
              assertNotNull(countNoHier);
              assertEquals(DataType.INT64, countNoHier.getValue().getDataType());
              assertNotNull(countNoHier.getValue().getInt64Val());
              assertNull(countNoHier.getDisplay());

              ValueDisplay countWithHier =
                  countInstance.getEntityFieldValue(relatedEntityIdCountFieldWithHier);
              assertNotNull(countWithHier);
              assertEquals(DataType.INT64, countWithHier.getValue().getDataType());
              assertNotNull(countWithHier.getValue().getInt64Val());
              assertNull(countWithHier.getDisplay());
            });
  }
}
