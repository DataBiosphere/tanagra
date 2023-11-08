package bio.terra.tanagra.api.omop;

import bio.terra.tanagra.api.BaseQueriesTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public abstract class OmopBrandTest extends BaseQueriesTest {
  @Test
  void textFilter() throws IOException {
    // filter for "brand" entity instances that match the search term "paracetamol"
    // i.e. brands that have a name or synonym that includes "paracetamol"
    textFilter("paracetamol");
  }

  @Override
  protected String getEntityName() {
    return "brand";
  }
}
