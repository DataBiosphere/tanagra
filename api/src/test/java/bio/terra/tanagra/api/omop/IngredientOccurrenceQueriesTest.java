package bio.terra.tanagra.api.omop;

import bio.terra.tanagra.api.BaseQueriesTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public abstract class IngredientOccurrenceQueriesTest extends BaseQueriesTest {
  @Test
  void ibuprofen() throws IOException {
    allOccurrencesForPrimariesWithACriteria(1_177_480L, "ibuprofen"); // "Ibuprofen"
  }

  @Override
  protected String getEntityName() {
    return "ingredient_occurrence";
  }
}
