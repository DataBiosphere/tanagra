package bio.terra.tanagra.query.bigquery.sqlbuilding;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.field.CountDistinctField;
import bio.terra.tanagra.api.field.HierarchyIsMemberField;
import bio.terra.tanagra.api.field.HierarchyIsRootField;
import bio.terra.tanagra.api.field.HierarchyNumChildrenField;
import bio.terra.tanagra.api.field.HierarchyPathField;
import bio.terra.tanagra.api.field.RelatedEntityIdCountField;
import bio.terra.tanagra.api.field.ValueDisplayField;
import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.api.query.list.ListQueryResult;
import bio.terra.tanagra.api.query.list.OrderBy;
import bio.terra.tanagra.api.shared.OrderByDirection;
import bio.terra.tanagra.api.shared.UnaryOperator;
import bio.terra.tanagra.query.bigquery.BQQueryRunner;
import bio.terra.tanagra.query.bigquery.BQRunnerTest;
import bio.terra.tanagra.query.bigquery.BQTable;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
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
        new AttributeField(underlay, entity, entity.getAttribute("year_of_birth"), false);
    AttributeField valueDisplayAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("gender"), false);
    AttributeField valueDisplayAttributeWithoutDisplay =
        new AttributeField(underlay, entity, entity.getAttribute("race"), true);
    AttributeField runtimeCalculatedAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("age"), false);
    AttributeField idAttribute =
        new AttributeField(underlay, entity, entity.getIdAttribute(), false);

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
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay, entity, selectAttributes, null, orderBys, limit));

    BQTable table = underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("attributeField", listQueryResult.getSql(), table);
  }

  @Test
  void attributeFieldAgainstSourceData() throws IOException {
    Entity entity = underlay.getPrimaryEntity();
    AttributeField simpleAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("year_of_birth"), false);
    AttributeField valueDisplayAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("gender"), false);
    AttributeField valueDisplayAttributeWithoutDisplay =
        new AttributeField(underlay, entity, entity.getAttribute("race"), true);
    AttributeField runtimeCalculatedAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("age"), false);
    AttributeField idAttribute =
        new AttributeField(underlay, entity, entity.getIdAttribute(), false);

    // We don't have an example of an attribute with the display field in the same table, yet.
    // So create an artificial attribute just for this test.
    Attribute ethnicityAttribute = entity.getAttribute("ethnicity");
    AttributeField noDisplayJoinAttribute =
        new AttributeField(
            underlay,
            entity,
            new Attribute(
                "ethnicityNoDisplayJoin",
                ethnicityAttribute.getDataType(),
                ethnicityAttribute.isValueDisplay(),
                ethnicityAttribute.isId(),
                ethnicityAttribute.getRuntimeSqlFunctionWrapper(),
                ethnicityAttribute.getRuntimeDataType(),
                ethnicityAttribute.isComputeDisplayHint(),
                ethnicityAttribute.isSuppressedForExport(),
                ethnicityAttribute.isVisitDateForTemporalQuery(),
                ethnicityAttribute.isVisitIdForTemporalQuery(),
                new Attribute.SourceQuery(
                    "person_source_value", null, "ethnicity_concept_id", null)),
            false);

    // We don't have an example of a suppressed attribute, yet.
    // So create an artificial attribute just for this test.
    Attribute genderAttribute = entity.getAttribute("gender");
    AttributeField suppressedAttribute =
        new AttributeField(
            underlay,
            entity,
            new Attribute(
                "genderSuppressed",
                genderAttribute.getDataType(),
                genderAttribute.isValueDisplay(),
                genderAttribute.isId(),
                genderAttribute.getRuntimeSqlFunctionWrapper(),
                genderAttribute.getRuntimeDataType(),
                genderAttribute.isComputeDisplayHint(),
                true,
                genderAttribute.isVisitDateForTemporalQuery(),
                genderAttribute.isVisitIdForTemporalQuery(),
                new Attribute.SourceQuery(
                    genderAttribute.getSourceQuery().getValueFieldName(),
                    genderAttribute.getSourceQuery().getDisplayFieldTable(),
                    genderAttribute.getSourceQuery().getDisplayFieldName(),
                    genderAttribute.getSourceQuery().getDisplayFieldTableJoinFieldName())),
            false);

    List<ValueDisplayField> selectAttributes =
        List.of(
            simpleAttribute,
            valueDisplayAttribute,
            valueDisplayAttributeWithoutDisplay,
            runtimeCalculatedAttribute,
            idAttribute,
            suppressedAttribute,
            noDisplayJoinAttribute);
    EntityFilter filter =
        new AttributeFilter(
            underlay,
            entity,
            entity.getAttribute("person_source_value"),
            UnaryOperator.IS_NOT_NULL);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstSourceData(underlay, entity, selectAttributes, filter));

    BQTable sourceTable = BQQueryRunner.fromFullTablePath(entity.getSourceQueryTableName());
    BQTable displayJoinTable =
        BQQueryRunner.fromFullTablePath(
            entity.getAttribute("gender").getSourceQuery().getDisplayFieldTable());
    BQTable indexTable =
        underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly(
        "attributeFieldAgainstSourceData",
        listQueryResult.getSql(),
        sourceTable,
        displayJoinTable,
        indexTable);
  }

  @Test
  void entityIdCountField() throws IOException {
    Entity entity = underlay.getPrimaryEntity();
    CountDistinctField countDistinctField =
        new CountDistinctField(underlay, entity, entity.getIdAttribute());

    List<ValueDisplayField> selectAttributes = List.of(countDistinctField);
    List<OrderBy> orderBys = List.of(new OrderBy(countDistinctField, OrderByDirection.DESCENDING));
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay, entity, selectAttributes, null, orderBys, null));

    BQTable table = underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
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
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay, entity, selectAttributes, null, orderBys, null));

    BQTable table = underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
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
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay, countForEntity, selectAttributes, null, orderBys, null));

    BQTable table =
        underlay.getIndexSchema().getEntityMain(countForEntity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("relatedEntityIdCountField", listQueryResult.getSql(), table);
  }
}
