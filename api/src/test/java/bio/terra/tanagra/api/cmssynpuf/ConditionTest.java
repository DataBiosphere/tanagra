package bio.terra.tanagra.api.cmssynpuf;

import bio.terra.tanagra.api.BaseQueryTest;
import bio.terra.tanagra.api.EntityQueryRequest;
import bio.terra.tanagra.api.entityfilter.HierarchyAncestorFilter;
import bio.terra.tanagra.api.entityfilter.HierarchyParentFilter;
import bio.terra.tanagra.api.entityfilter.HierarchyRootFilter;
import bio.terra.tanagra.api.entityfilter.TextFilter;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.filtervariable.FunctionFilterVariable;
import bio.terra.tanagra.testing.GeneratedSqlUtils;
import bio.terra.tanagra.underlay.Underlay;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class ConditionTest extends BaseQueryTest {
  @Test
  void noFilter() throws IOException {
    EntityQueryRequest entityQueryRequest =
        new EntityQueryRequest.Builder()
            .entity(getEntity())
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(getEntity().getAttributes())
            .limit(DEFAULT_LIMIT)
            .build();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        querysService.buildInstancesQuery(entityQueryRequest).getSql(),
        "sql/cmssynpuf/condition-noFilter.sql");
  }

  @Test
  void textFilter() throws IOException {
    // filter for "condition" entity instances that match the search term "sense of smell absent"
    // i.e. conditions that have a name or synonym that includes "sense of smell absent"
    TextFilter textFilter =
        new TextFilter.Builder()
            .textSearch(getEntity().getTextSearch())
            .functionTemplate(FunctionFilterVariable.FunctionTemplate.TEXT_EXACT_MATCH)
            .text("sense of smell absent")
            .build();

    EntityQueryRequest entityQueryRequest =
        new EntityQueryRequest.Builder()
            .entity(getEntity())
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(getEntity().getAttributes())
            .filter(textFilter)
            .limit(DEFAULT_LIMIT)
            .build();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        querysService.buildInstancesQuery(entityQueryRequest).getSql(),
        "sql/cmssynpuf/condition-textFilter.sql");
  }

  @Test
  void hierarchyRootFilter() throws IOException {
    // filter for "condition" entity instances that are root nodes in the "standard" hierarchy
    HierarchyRootFilter hierarchyRootFilter =
        new HierarchyRootFilter(getEntity().getHierarchy("standard"));

    EntityQueryRequest entityQueryRequest =
        new EntityQueryRequest.Builder()
            .entity(getEntity())
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(getEntity().getAttributes())
            .selectHierarchyFields(getEntity().getHierarchy("standard").getFields())
            .filter(hierarchyRootFilter)
            .limit(DEFAULT_LIMIT)
            .build();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        querysService.buildInstancesQuery(entityQueryRequest).getSql(),
        "sql/cmssynpuf/condition-hierarchyRootFilter.sql");
  }

  @Test
  void hierarchyParentFilter() throws IOException {
    // filter for "condition" entity instances that are children of the "condition" entity
    // instance with concept_id=201826
    // i.e. give me all the children of "Type 2 diabetes mellitus"
    HierarchyParentFilter hierarchyParentFilter =
        new HierarchyParentFilter(getEntity().getHierarchy("standard"), new Literal(201_826L));

    EntityQueryRequest entityQueryRequest =
        new EntityQueryRequest.Builder()
            .entity(getEntity())
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(getEntity().getAttributes())
            .selectHierarchyFields(getEntity().getHierarchy("standard").getFields())
            .filter(hierarchyParentFilter)
            .limit(DEFAULT_LIMIT)
            .build();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        querysService.buildInstancesQuery(entityQueryRequest).getSql(),
        "sql/cmssynpuf/condition-hierarchyParentFilter.sql");
  }

  @Test
  void hierarchyAncestorFilter() throws IOException {
    // filter for "condition" entity instances that are descendants of the "condition" entity
    // instance with concept_id=201826
    // i.e. give me all the descendants of "Type 2 diabetes mellitus"
    HierarchyAncestorFilter hierarchyAncestorFilter =
        new HierarchyAncestorFilter(getEntity().getHierarchy("standard"), new Literal(201_826L));

    EntityQueryRequest entityQueryRequest =
        new EntityQueryRequest.Builder()
            .entity(getEntity())
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(getEntity().getAttributes())
            .selectHierarchyFields(getEntity().getHierarchy("standard").getFields())
            .filter(hierarchyAncestorFilter)
            .limit(DEFAULT_LIMIT)
            .build();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        querysService.buildInstancesQuery(entityQueryRequest).getSql(),
        "sql/cmssynpuf/condition-hierarchyAncestorFilter.sql");
  }

  @Override
  protected String getUnderlayName() {
    return "cms_synpuf";
  }

  @Override
  protected String getEntityName() {
    return "condition";
  }
}
