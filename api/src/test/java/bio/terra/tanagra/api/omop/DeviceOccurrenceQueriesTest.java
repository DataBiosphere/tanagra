package bio.terra.tanagra.api.omop;

import bio.terra.tanagra.api.BaseQueriesTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public abstract class DeviceOccurrenceQueriesTest extends BaseQueriesTest {
  @Test
  void longLegCast() throws IOException {
    allOccurrencesForPrimariesWithACriteria(4_038_664L, "longLegCast"); // "Long leg cast"
  }

  @Override
  protected String getEntityName() {
    return "device_occurrence";
  }
}
