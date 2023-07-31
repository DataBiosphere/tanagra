package bio.terra.tanagra.api.sdd;

import bio.terra.tanagra.api.BaseQueriesTest;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

public class SddSnpTest extends BaseQueriesTest {
  @Override
  protected String getUnderlayName() {
    return "sdd_refresh0323";
  }

  @Override
  protected String getEntityName() {
    return "snp";
  }

  @Test
  void textFilter() throws IOException {
    // snp entity instances that match the search term "RS1292"
    textFilter("name", "RS1292");
  }

  @Test
  void relationshipCohort() throws IOException {
    // Cohort of people with >=1 relationship of snp = "RS12925749"
    relationshipCohort("name", "RS12925749");
  }

  @Test
  void countRelationshipCohort() throws IOException {
    // Count the number of people with >=1 relationship of snp = "RS12925749", grouped by gender and
    // race.
    countRelationshipCohort(List.of("gender", "race"), "name", "RS12925749");
  }
}
