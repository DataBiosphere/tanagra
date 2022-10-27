package bio.terra.tanagra.api.omop;

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

public abstract class ProcedureQueriesTest extends BaseQueryTest {
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
        "sql/" + getSqlDirectoryName() + "/procedure-noFilter.sql");
  }

  @Test
  void textFilter() throws IOException {
    // filter for "procedure" entity instances that match the search term "mammogram"
    // i.e. procedures that have a name or synonym that includes "mammogram"
    TextFilter textFilter =
        new TextFilter.Builder()
            .textSearch(getEntity().getTextSearch())
            .functionTemplate(FunctionFilterVariable.FunctionTemplate.TEXT_EXACT_MATCH)
            .text("mammogram")
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
        "sql/" + getSqlDirectoryName() + "/procedure-textFilter.sql");
  }

  @Test
  void hierarchyRootFilter() throws IOException {
    // filter for "procedure" entity instances that are root nodes in the "standard" hierarchy
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
        "sql/" + getSqlDirectoryName() + "/procedure-hierarchyRootFilter.sql");
  }

  @Test
  void hierarchyParentFilter() throws IOException {
    // filter for "procedure" entity instances that are children of the "procedure" entity
    // instance with concept_id=4179181
    // i.e. give me all the children of "Mumps vaccination"
    HierarchyParentFilter hierarchyParentFilter =
        new HierarchyParentFilter(getEntity().getHierarchy("standard"), new Literal(4_179_181L));

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
        "sql/" + getSqlDirectoryName() + "/procedure-hierarchyParentFilter.sql");
  }

  @Test
  void hierarchyAncestorFilter() throws IOException {
    // filter for "procedure" entity instances that are descendants of the "procedure" entity
    // instance with concept_id=4176720
    // i.e. give me all the descendants of "Viral immunization"
    HierarchyAncestorFilter hierarchyAncestorFilter =
        new HierarchyAncestorFilter(getEntity().getHierarchy("standard"), new Literal(4_176_720L));

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
        "sql/" + getSqlDirectoryName() + "/procedure-hierarchyAncestorFilter.sql");
  }

  @Override
  protected String getEntityName() {
    return "procedure";
  }
}
