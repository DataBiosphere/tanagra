package bio.terra.tanagra.api.omop;

import bio.terra.tanagra.api.BaseQueriesTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public abstract class OmopVisitTest extends BaseQueriesTest {
  @Test
  void textFilter() throws IOException {
    // filter for "visit" entity instances that match the search term "ambul"
    // i.e. visits that have a name or synonym that includes "ambul"
    textFilter("ambul");
  }

  @Test
  void cohort() throws IOException {
    // Cohort of people with >=1 occurrence of visit = "Outpatient Visit".
    singleCriteriaCohort(getEntity(), "outpatient", 9_202L);
  }

  @Test
  void dataset() throws IOException {
    // Visit occurrences for cohort of people with >=1 occurrence of visit = "Outpatient Visit".
    allOccurrencesForSingleCriteriaCohort(getEntity(), "outpatient", 9_202L);
  }

  @Override
  protected String getEntityName() {
    return "visit";
  }
}
