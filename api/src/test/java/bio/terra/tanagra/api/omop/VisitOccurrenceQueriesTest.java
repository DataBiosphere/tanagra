package bio.terra.tanagra.api.omop;

import bio.terra.tanagra.api.BaseQueriesTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public abstract class VisitOccurrenceQueriesTest extends BaseQueriesTest {
  @Test
  void outpatient() throws IOException {
    allOccurrencesForPrimariesWithACriteria(9_202L, "outpatient"); // "Outpatient Visit"
  }

  @Override
  protected String getEntityName() {
    return "visit_occurrence";
  }
}
