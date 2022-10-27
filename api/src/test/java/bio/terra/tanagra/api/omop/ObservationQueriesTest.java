package bio.terra.tanagra.api.omop;

import bio.terra.tanagra.api.BaseQueriesTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public abstract class ObservationQueriesTest extends BaseQueriesTest {
  @Test
  void textFilter() throws IOException {
    // filter for "observation" entity instances that match the search term "smoke"
    // i.e. observations that have a name or synonym that includes "smoke"
    textFilter("smoke");
  }

  @Override
  protected String getEntityName() {
    return "observation";
  }
}
