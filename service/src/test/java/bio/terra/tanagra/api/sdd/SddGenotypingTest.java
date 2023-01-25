package bio.terra.tanagra.api.sdd;

import bio.terra.tanagra.api.BaseQueriesTest;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

public class SddGenotypingTest extends BaseQueriesTest {
  @Override
  protected String getUnderlayName() {
    return "sdd";
  }

  @Override
  protected String getEntityName() {
    return "genotyping";
  }

  @Test
  void textFilter() throws IOException {
    // genotyping entity instances that match the search term "Illumina"
    textFilter("name", "Illumina");
  }

  @Test
  void relationshipCohort() throws IOException {
    // Cohort of people with >=1 relationship of genotyping = "Illumina 5M"
    relationshipCohort("name", "Illumina 5M");
  }

  @Test
  void countRelationshipCohort() throws IOException {
    // Count the number of people with >=1 relationship of genotyping = "Illumina 5M", grouped by
    // gender and race.
    countRelationshipCohort(List.of("gender", "race"), "name", "Illumina 5M");
  }
}
