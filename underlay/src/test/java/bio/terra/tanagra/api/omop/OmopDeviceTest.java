package bio.terra.tanagra.api.omop;

import bio.terra.tanagra.api.BaseQueriesTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public abstract class OmopDeviceTest extends BaseQueriesTest {
  @Test
  void textFilter() throws IOException {
    // filter for "device" entity instances that match the search term "hearing aid"
    // i.e. devices that have a name or synonym that includes "hearing aid"
    textFilter("hearing aid");
  }

  @Test
  void cohort() throws IOException {
    // Cohort of people with >=1 occurrence of device = "Long leg cast".
    singleCriteriaCohort(getEntity(), "longLegCast", 4_038_664L);
  }

  @Test
  void dataset() throws IOException {
    // Device occurrences for cohort of people with >=1 occurrence of device = "Long leg cast".
    allOccurrencesForSingleCriteriaCohort(getEntity(), "longLegCast", 4_038_664L);
  }

  @Override
  protected String getEntityName() {
    return "device";
  }
}
