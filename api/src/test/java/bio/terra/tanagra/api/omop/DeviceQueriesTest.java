package bio.terra.tanagra.api.omop;

import bio.terra.tanagra.api.BaseQueriesTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public abstract class DeviceQueriesTest extends BaseQueriesTest {
  @Test
  void textFilter() throws IOException {
    // filter for "device" entity instances that match the search term "hearing aid"
    // i.e. devices that have a name or synonym that includes "hearing aid"
    textFilter("hearing aid");
  }

  @Override
  protected String getEntityName() {
    return "device";
  }
}
