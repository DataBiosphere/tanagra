package bio.terra.tanagra.api.omop;

import bio.terra.tanagra.api.BaseQueriesTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public abstract class VisitQueriesTest extends BaseQueriesTest {
  @Test
  void textFilter() throws IOException {
    // filter for "visit" entity instances that match the search term "ambul"
    // i.e. visits that have a name or synonym that includes "ambul"
    textFilter("ambul");
  }

  @Override
  protected String getEntityName() {
    return "visit";
  }
}
