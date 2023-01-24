package bio.terra.tanagra.api.sdd;

import bio.terra.tanagra.api.BaseQueriesTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class SddSnpTest extends BaseQueriesTest {
  @Override
  protected String getUnderlayName() {
    return "sdd";
  }

  @Override
  protected String getEntityName() {
    return "snp";
  }

  @Test
  void textFilter() throws IOException {
    // "snp" entity instances that match the search term "RS1292"
    textFilter("id", "RS1292");
  }

  @Test
  void singleRelationshipCohort() throws IOException {
    // Cohort of people with >=1 relationship of snp = "RS12925749"
    singleRelationshipCohort("id", "RS12925749");
  }
}
