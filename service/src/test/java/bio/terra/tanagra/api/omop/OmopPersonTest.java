package bio.terra.tanagra.api.omop;

import bio.terra.tanagra.api.BaseQueriesTest;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

public abstract class OmopPersonTest extends BaseQueriesTest {
  @Test
  void countAll() throws IOException {
    // Count the number of people grouped by gender and race.
    count(getEntity(), "all", List.of());
  }

  @Test
  void breakdown() throws IOException {
    // Count the number of people grouped by gender and race.
    count(getEntity(), "genderRace", List.of("gender", "race"));
  }

  @Override
  protected String getEntityName() {
    return "person";
  }
}
