package bio.terra.tanagra.api.omop;

import bio.terra.tanagra.api.BaseQueriesTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public abstract class MeasurementOccurrenceQueriesTest extends BaseQueriesTest {
  @Test
  void hematocrit() throws IOException {
    allOccurrencesForPrimariesWithACriteria(
        3_009_542L, "hematocrit"); // "Hematocrit [Volume Fraction] of Blood"
  }

  @Override
  protected String getEntityName() {
    return "measurement_occurrence";
  }
}
