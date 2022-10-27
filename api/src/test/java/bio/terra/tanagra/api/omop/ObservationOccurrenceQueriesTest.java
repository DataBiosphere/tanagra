package bio.terra.tanagra.api.omop;

import bio.terra.tanagra.api.BaseQueriesTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public abstract class ObservationOccurrenceQueriesTest extends BaseQueriesTest {
  @Test
  void vaccineRefusal() throws IOException {
    allOccurrencesForPrimariesWithACriteria(
        43_531_662L, "vaccineRefusal"); // "Vaccine refused by patient"
  }

  @Override
  protected String getEntityName() {
    return "observation_occurrence";
  }
}
