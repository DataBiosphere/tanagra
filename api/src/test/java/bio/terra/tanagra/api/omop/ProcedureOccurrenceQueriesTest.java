package bio.terra.tanagra.api.omop;

import bio.terra.tanagra.api.BaseQueriesTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public abstract class ProcedureOccurrenceQueriesTest extends BaseQueriesTest {
  @Test
  void mammogram() throws IOException {
    allOccurrencesForPrimariesWithACriteria(4_324_693L, "mammogram"); // "Mammography"
  }

  @Override
  protected String getEntityName() {
    return "procedure_occurrence";
  }
}
