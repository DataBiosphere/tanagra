package bio.terra.tanagra.api.omop;

import bio.terra.tanagra.api.BaseQueriesTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public abstract class OmopProcedureTest extends BaseQueriesTest {
  @Test
  void textFilter() throws IOException {
    // filter for "procedure" entity instances that match the search term "mammogram"
    // i.e. procedures that have a name or synonym that includes "mammogram"
    textFilter("mammogram");
  }

  @Test
  void hierarchyRootFilter() throws IOException {
    // filter for "procedure" entity instances that are root nodes in the "standard" hierarchy
    hierarchyRootFilter("standard");
  }

  @Test
  void hierarchyMemberFilter() throws IOException {
    // filter for "procedure" entity instances that are members of the "standard" hierarchy
    hierarchyMemberFilter("standard");
  }

  @Test
  void hierarchyParentFilter() throws IOException {
    // filter for "procedure" entity instances that are children of the "procedure" entity
    // instance with concept_id=4179181
    // i.e. give me all the children of "Mumps vaccination"
    hierarchyParentFilter("standard", 4_179_181L, "mumpsVaccination");
  }

  @Test
  void hierarchyAncestorFilter() throws IOException {
    // filter for "procedure" entity instances that are descendants of the "procedure" entity
    // instance with concept_id=4176720
    // i.e. give me all the descendants of "Viral immunization"
    hierarchyAncestorFilter("standard", 4_176_720L, "viralImmunization");
  }

  @Test
  void cohort() throws IOException {
    // Cohort of people with >=1 occurrence of procedure = "Mammography".
    singleCriteriaCohort(getEntity(), "mammogram", 4_324_693L);
  }

  @Test
  void dataset() throws IOException {
    // Procedure occurrences for cohort of people with >=1 occurrence of procedure = "Mammography".
    allOccurrencesForSingleCriteriaCohort(getEntity(), "mammogram", 4_324_693L);
  }

  @Override
  protected String getEntityName() {
    return "procedure";
  }
}
