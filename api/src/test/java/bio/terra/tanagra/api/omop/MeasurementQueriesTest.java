package bio.terra.tanagra.api.omop;

import bio.terra.tanagra.api.BaseQueryTest;
import bio.terra.tanagra.api.EntityQueryRequest;
import bio.terra.tanagra.api.entityfilter.AttributeFilter;
import bio.terra.tanagra.api.entityfilter.BooleanAndOrFilter;
import bio.terra.tanagra.api.entityfilter.HierarchyAncestorFilter;
import bio.terra.tanagra.api.entityfilter.HierarchyParentFilter;
import bio.terra.tanagra.api.entityfilter.HierarchyRootFilter;
import bio.terra.tanagra.api.entityfilter.TextFilter;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable;
import bio.terra.tanagra.query.filtervariable.FunctionFilterVariable;
import bio.terra.tanagra.testing.GeneratedSqlUtils;
import bio.terra.tanagra.underlay.Underlay;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

public abstract class MeasurementQueriesTest extends BaseQueryTest {
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
        "sql/" + getSqlDirectoryName() + "/measurement-noFilter.sql");
  }

  @Test
  void textFilter() throws IOException {
    // filter for "measurement" entity instances that match the search term "hematocrit"
    // i.e. measurements that have a name or synonym that includes "hematocrit"
    TextFilter textFilter =
        new TextFilter.Builder()
            .textSearch(getEntity().getTextSearch())
            .functionTemplate(FunctionFilterVariable.FunctionTemplate.TEXT_EXACT_MATCH)
            .text("hematocrit")
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
        "sql/" + getSqlDirectoryName() + "/measurement-textFilter.sql");
  }

  @Test
  void hierarchyRootFilterLOINC() throws IOException {
    // filter for "measurement" entity instances that are root nodes in the "standard" hierarchy
    HierarchyRootFilter hierarchyRootFilter =
        new HierarchyRootFilter(getEntity().getHierarchy("standard"));

    // filter for "measurement" entity instances that have the "LOINC" vocabulary
    AttributeFilter vocabularyFilter =
        new AttributeFilter(
            getEntity().getAttribute("vocabulary"),
            BinaryFilterVariable.BinaryOperator.EQUALS,
            new Literal("LOINC"));

    // filter for is_root AND vocabulary='LOINC'
    BooleanAndOrFilter rootAndLoinc =
        new BooleanAndOrFilter(
            BooleanAndOrFilterVariable.LogicalOperator.AND,
            List.of(hierarchyRootFilter, vocabularyFilter));

    EntityQueryRequest entityQueryRequest =
        new EntityQueryRequest.Builder()
            .entity(getEntity())
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(getEntity().getAttributes())
            .selectHierarchyFields(getEntity().getHierarchy("standard").getFields())
            .filter(rootAndLoinc)
            .limit(DEFAULT_LIMIT)
            .build();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        querysService.buildInstancesQuery(entityQueryRequest).getSql(),
        "sql/" + getSqlDirectoryName() + "/measurement-hierarchyRootFilterLOINC.sql");
  }

  @Test
  void hierarchyRootFilterSNOMED() throws IOException {
    // filter for "measurement" entity instances that are root nodes in the "standard" hierarchy
    HierarchyRootFilter hierarchyRootFilter =
        new HierarchyRootFilter(getEntity().getHierarchy("standard"));

    // filter for "measurement" entity instances that have the "SNOMED" vocabulary
    AttributeFilter vocabularyFilter =
        new AttributeFilter(
            getEntity().getAttribute("vocabulary"),
            BinaryFilterVariable.BinaryOperator.EQUALS,
            new Literal("SNOMED"));

    // filter for is_root AND vocabulary='SNOMED'
    BooleanAndOrFilter rootAndLoinc =
        new BooleanAndOrFilter(
            BooleanAndOrFilterVariable.LogicalOperator.AND,
            List.of(hierarchyRootFilter, vocabularyFilter));

    EntityQueryRequest entityQueryRequest =
        new EntityQueryRequest.Builder()
            .entity(getEntity())
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(getEntity().getAttributes())
            .selectHierarchyFields(getEntity().getHierarchy("standard").getFields())
            .filter(rootAndLoinc)
            .limit(DEFAULT_LIMIT)
            .build();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        querysService.buildInstancesQuery(entityQueryRequest).getSql(),
        "sql/" + getSqlDirectoryName() + "/measurement-hierarchyRootFilterSNOMED.sql");
  }

  @Test
  void hierarchyParentFilter() throws IOException {
    // filter for "measurement" entity instances that are children of the "measurement" entity
    // instance with concept_id=37072239
    // i.e. give me all the children of "Glucose tolerance 2 hours panel | Serum or Plasma |
    // Challenge Bank Panels"
    HierarchyParentFilter hierarchyParentFilter =
        new HierarchyParentFilter(getEntity().getHierarchy("standard"), new Literal(37_072_239L));

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
        "sql/" + getSqlDirectoryName() + "/measurement-hierarchyParentFilter.sql");
  }

  @Test
  void hierarchyAncestorFilter() throws IOException {
    // filter for "measurement" entity instances that are descendants of the "measurement" entity
    // instance with concept_id=37048668
    // i.e. give me all the descendants of "Glucose tolerance 2 hours panel"
    HierarchyAncestorFilter hierarchyAncestorFilter =
        new HierarchyAncestorFilter(getEntity().getHierarchy("standard"), new Literal(37_048_668L));

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
        "sql/" + getSqlDirectoryName() + "/measurement-hierarchyAncestorFilter.sql");
  }

  @Override
  protected String getEntityName() {
    return "measurement";
  }
}
