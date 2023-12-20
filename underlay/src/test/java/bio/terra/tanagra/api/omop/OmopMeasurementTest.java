package bio.terra.tanagra.api.omop;

import bio.terra.tanagra.api.BaseQueriesTest;
import bio.terra.tanagra.api.field.valuedisplay.AttributeField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyIsMemberField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyIsRootField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyNumChildrenField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyPathField;
import bio.terra.tanagra.api.field.valuedisplay.ValueDisplayField;
import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.HierarchyIsRootFilter;
import bio.terra.tanagra.api.query.EntityQueryRunner;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable;
import bio.terra.tanagra.testing.GeneratedSqlUtils;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import java.io.IOException;
import java.util.ArrayList;
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
    // Select all attributes and hierarchy fields.
    Hierarchy hierarchy = getEntity().getHierarchy("default");
    List<ValueDisplayField> selectFields = new ArrayList<>();
    getEntity().getAttributes().stream()
        .forEach(
            attribute ->
                selectFields.add(
                    new AttributeField(getUnderlay(), getEntity(), attribute, false, false)));
    selectFields.add(new HierarchyPathField(getUnderlay(), getEntity(), hierarchy));
    selectFields.add(new HierarchyNumChildrenField(getUnderlay(), getEntity(), hierarchy));
    selectFields.add(new HierarchyIsRootField(getUnderlay(), getEntity(), hierarchy));
    selectFields.add(new HierarchyIsMemberField(getUnderlay(), getEntity(), hierarchy));

    // filter for "measurement" entity instances that are root nodes in the "default" hierarchy
    HierarchyIsRootFilter hierarchyIsRootFilter =
        new HierarchyIsRootFilter(getUnderlay(), getEntity(), hierarchy);

    // filter for "measurement" entity instances that have the "LOINC" vocabulary
    AttributeFilter vocabularyFilter =
        new AttributeFilter(
            getUnderlay(),
            getEntity(),
            getEntity().getAttribute("vocabulary"),
            BinaryFilterVariable.BinaryOperator.EQUALS,
            new Literal("LOINC"));

    // filter for is_root AND vocabulary='LOINC'
    BooleanAndOrFilter rootAndLoinc =
        new BooleanAndOrFilter(
            BooleanAndOrFilterVariable.LogicalOperator.AND,
            List.of(hierarchyIsRootFilter, vocabularyFilter));

    ListQueryRequest listQueryRequest =
        new ListQueryRequest(
            getUnderlay(),
            getEntity(),
            selectFields,
            rootAndLoinc,
            null,
            DEFAULT_LIMIT,
            null,
            null,
            false);
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        EntityQueryRunner.buildQueryRequest(listQueryRequest).getSql(),
        "sql/" + getSqlDirectoryName() + "/measurement-hierarchyRootFilterLOINC.sql");
  }

  @Test
  void hierarchyRootFilterSnomed() throws IOException {
    // Select all attributes and hierarchy fields.
    Hierarchy hierarchy = getEntity().getHierarchy("default");
    List<ValueDisplayField> selectFields = new ArrayList<>();
    getEntity().getAttributes().stream()
        .forEach(
            attribute ->
                selectFields.add(
                    new AttributeField(getUnderlay(), getEntity(), attribute, false, false)));
    selectFields.add(new HierarchyPathField(getUnderlay(), getEntity(), hierarchy));
    selectFields.add(new HierarchyNumChildrenField(getUnderlay(), getEntity(), hierarchy));
    selectFields.add(new HierarchyIsRootField(getUnderlay(), getEntity(), hierarchy));
    selectFields.add(new HierarchyIsMemberField(getUnderlay(), getEntity(), hierarchy));

    // filter for "measurement" entity instances that are root nodes in the "default" hierarchy
    HierarchyIsRootFilter hierarchyIsRootFilter =
        new HierarchyIsRootFilter(getUnderlay(), getEntity(), hierarchy);

    // filter for "measurement" entity instances that have the "SNOMED" vocabulary
    AttributeFilter vocabularyFilter =
        new AttributeFilter(
            getUnderlay(),
            getEntity(),
            getEntity().getAttribute("vocabulary"),
            BinaryFilterVariable.BinaryOperator.EQUALS,
            new Literal("SNOMED"));

    // filter for is_root AND vocabulary='SNOMED'
    BooleanAndOrFilter rootAndLoinc =
        new BooleanAndOrFilter(
            BooleanAndOrFilterVariable.LogicalOperator.AND,
            List.of(hierarchyIsRootFilter, vocabularyFilter));

    ListQueryRequest listQueryRequest =
        new ListQueryRequest(
            getUnderlay(),
            getEntity(),
            selectFields,
            rootAndLoinc,
            null,
            DEFAULT_LIMIT,
            null,
            null,
            false);
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        EntityQueryRunner.buildQueryRequest(listQueryRequest).getSql(),
        "sql/" + getSqlDirectoryName() + "/measurement-hierarchyRootFilterSNOMED.sql");
  }

  @Test
  void hierarchyParentFilter() throws IOException {
    // filter for "measurement" entity instances that are children of the "measurement" entity
    // instance with concept_id=37072239
    // i.e. give me all the children of "Glucose tolerance 2 hours panel | Serum or Plasma |
    // Challenge Bank Panels"
    hierarchyParentFilter("default", 37_072_239L, "glucoseTolerance");
  }

  @Test
  void hierarchyAncestorFilter() throws IOException {
    // filter for "measurement" entity instances that are descendants of the "measurement" entity
    // instance with concept_id=37048668
    // i.e. give me all the descendants of "Glucose tolerance 2 hours panel"
    hierarchyAncestorFilter("default", 37_048_668L, "glucoseTolerance");
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
    return "measurementLoinc";
  }
}
