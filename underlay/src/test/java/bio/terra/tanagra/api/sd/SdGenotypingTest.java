package bio.terra.tanagra.api.sd;

import bio.terra.tanagra.api.BaseQueriesTest;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

public class SdGenotypingTest extends BaseQueriesTest {
  @Override
  protected String getServiceConfigName() {
    return "sd020230331_verily";
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
  void hierarchyRootFilter() throws IOException {
    // filter for "genotyping" entity instances that are root nodes in the "default" hierarchy
    hierarchyRootFilter("default");
  }

  @Test
  void hierarchyMemberFilter() throws IOException {
    // filter for "genotyping" entity instances that are members of the "default" hierarchy
    hierarchyMemberFilter("default");
  }

  @Test
  void hierarchyParentFilter() throws IOException {
    // filter for "genotyping" entity instances that are children of the "genotyping" entity
    // instance with id=101
    // i.e. give me all the genotyping platforms that are "GWAS Platforms"
    hierarchyParentFilter("default", 101L, "gwasPlatforms");
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
