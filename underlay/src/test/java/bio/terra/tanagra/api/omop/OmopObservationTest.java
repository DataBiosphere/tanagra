package bio.terra.tanagra.api.omop;

import bio.terra.tanagra.api.BaseQueriesTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public abstract class OmopObservationTest extends BaseQueriesTest {
  @Test
  void textFilter() throws IOException {
    // filter for "observation" entity instances that match the search term "smoke"
    // i.e. observations that have a name or synonym that includes "smoke"
    textFilter("smoke");
  }

  @Test
  void cohort() throws IOException {
    // Cohort of people with >=1 occurrence of observation = "Vaccine refused by patient".
    singleCriteriaCohort(getEntity(), "vaccineRefusal", 43_531_662L);
  }

  @Test
  void dataset() throws IOException {
    // Observation occurrences for cohort of people with >=1 occurrence of observation = "Vaccine
    // refused by patient".
    allOccurrencesForSingleCriteriaCohort(getEntity(), "vaccineRefusal", 43_531_662L);
  }

  @Override
  protected String getEntityName() {
    return "observation";
  }
}
