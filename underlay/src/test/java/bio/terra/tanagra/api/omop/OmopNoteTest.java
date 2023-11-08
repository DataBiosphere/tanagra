package bio.terra.tanagra.api.omop;

import bio.terra.tanagra.api.BaseQueriesTest;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

public abstract class OmopNoteTest extends BaseQueriesTest {
  @Test
  void textFilter() throws IOException {
    // filter for "note" entity instances that match the search term "admis"
    // i.e. notes that have a name or synonym that includes "admis"
    textFilter("admis");
  }

  @Test
  void cohort() throws IOException {
    // Cohort of people with >=1 occurrence of note = "Admission Notes".
    singleCriteriaCohort(getEntity(), "admissionNote", 44_814_638L);
  }

  @Test
  void dataset() throws IOException {
    // Note occurrences for cohort of people with >=1 occurrence of note = "Admission Notes"
    allOccurrencesForSingleCriteriaCohort(getEntity(), "outpatient", 44_814_638L);
  }
  //  and note occurrence text includes "snow".

  @Test
  void cohortCount() throws IOException {
    // Count the number of people with >=1 occurrence of note = "Admission Notes", grouped by gender
    // and race.
    countSingleCriteriaCohort(getEntity(), "admissionNote", List.of("gender", "race"), 44_814_638L);
  }

  @Override
  protected String getEntityName() {
    return "note";
  }
}
