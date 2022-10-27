package bio.terra.tanagra.api.omop;

import bio.terra.tanagra.api.BaseQueriesTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public abstract class ConditionQueriesTest extends BaseQueriesTest {
  @Test
  void textFilter() throws IOException {
    // filter for "condition" entity instances that match the search term "sense of smell absent"
    // i.e. conditions that have a name or synonym that includes "sense of smell absent"
    textFilter("sense of smell absent");
  }

  @Test
  void hierarchyRootFilter() throws IOException {
    // filter for "condition" entity instances that are root nodes in the "standard" hierarchy
    hierarchyRootFilter("standard");
  }

  @Test
  void hierarchyParentFilter() throws IOException {
    // filter for "condition" entity instances that are children of the "condition" entity
    // instance with concept_id=201826
    // i.e. give me all the children of "Type 2 diabetes mellitus"
    hierarchyParentFilter("standard", 201_826L, "diabetes");
  }

  @Test
  void hierarchyAncestorFilter() throws IOException {
    // filter for "condition" entity instances that are descendants of the "condition" entity
    // instance with concept_id=201826
    // i.e. give me all the descendants of "Type 2 diabetes mellitus"
    hierarchyAncestorFilter("standard", 201_826L, "diabetes");
  }

  @Override
  protected String getEntityName() {
    return "condition";
  }
}
