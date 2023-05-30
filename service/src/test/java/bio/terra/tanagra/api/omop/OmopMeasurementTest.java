package bio.terra.tanagra.api.omop;

import bio.terra.tanagra.api.BaseQueriesTest;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable;
import bio.terra.tanagra.service.instances.EntityQueryRequest;
import bio.terra.tanagra.service.instances.filter.AttributeFilter;
import bio.terra.tanagra.service.instances.filter.BooleanAndOrFilter;
import bio.terra.tanagra.service.instances.filter.HierarchyRootFilter;
import bio.terra.tanagra.testing.GeneratedSqlUtils;
import bio.terra.tanagra.underlay.Underlay;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

public abstract class OmopMeasurementTest extends BaseQueriesTest {
  @Test
  void textFilter() throws IOException {
    // filter for "measurement" entity instances that match the search term "hematocrit"
    // i.e. measurements that have a name or synonym that includes "hematocrit"
    textFilter("hematocrit");
  }

  @Test
  void hierarchyRootFilterLoinc() throws IOException {
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
        entityQueryRequest.buildInstancesQuery().getSql(),
        "sql/" + getSqlDirectoryName() + "/measurement-hierarchyRootFilterLOINC.sql");
  }

  @Test
  void hierarchyRootFilterSnomed() throws IOException {
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
        entityQueryRequest.buildInstancesQuery().getSql(),
        "sql/" + getSqlDirectoryName() + "/measurement-hierarchyRootFilterSNOMED.sql");
  }

  @Test
  void hierarchyParentFilter() throws IOException {
    // filter for "measurement" entity instances that are children of the "measurement" entity
    // instance with concept_id=37072239
    // i.e. give me all the children of "Glucose tolerance 2 hours panel | Serum or Plasma |
    // Challenge Bank Panels"
    hierarchyParentFilter("standard", 37_072_239L, "glucoseTolerance");
  }

  @Test
  void hierarchyAncestorFilter() throws IOException {
    // filter for "measurement" entity instances that are descendants of the "measurement" entity
    // instance with concept_id=37048668
    // i.e. give me all the descendants of "Glucose tolerance 2 hours panel"
    hierarchyAncestorFilter("standard", 37_048_668L, "glucoseTolerance");
  }

  @Test
  void cohort() throws IOException {
    // Cohort of people with >=1 occurrence of measurement = "Hematocrit [Volume Fraction] of
    // Blood".
    singleCriteriaCohort(getEntity(), "hematocrit", 3_009_542L);
  }

  @Test
  void dataset() throws IOException {
    // Measurement occurrences for cohort of people with >=1 occurrence of measurement = "Hematocrit
    // [Volume Fraction] of Blood".
    allOccurrencesForSingleCriteriaCohort(getEntity(), "hematocrit", 3_009_542L);
  }

  @Override
  protected String getEntityName() {
    return "measurement";
  }
}
