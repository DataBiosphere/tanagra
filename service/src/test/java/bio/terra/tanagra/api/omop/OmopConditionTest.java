package bio.terra.tanagra.api.omop;

import bio.terra.tanagra.api.BaseQueriesTest;
import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

public abstract class OmopConditionTest extends BaseQueriesTest {
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
  void hierarchyMemberFilter() throws IOException {
    // filter for "condition" entity instances that are members of the "standard" hierarchy
    hierarchyMemberFilter("standard");
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

  @Test
  void cohort() throws IOException {
    // Cohort of people with >=1 occurrence of condition = "Type 2 diabetes mellitus".
    singleCriteriaCohort(getEntity(), "diabetes", 201_826L);
  }

  @Test
  void dataset() throws IOException {
    // Condition occurrences for cohort of people with >=1 occurrence of condition = "Type 2
    // diabetes mellitus".
    allOccurrencesForSingleCriteriaCohort(getEntity(), "diabetes", 201_826L);
  }

  @Test
  void datasetTwoCriteria() throws IOException {
    // Condition occurrences for cohort of people with >=1 occurrence of condition = "Type 2
    // diabetes mellitus" AND >=1 occurrence of condition = "Sepsis".
    allOccurrencesForSingleCriteriaCohort(
        getEntity(),
        "diabetesAndSepsis",
        List.of(201_826L, 132_797L),
        BooleanAndOrFilterVariable.LogicalOperator.AND);
  }

  @Test
  void cohortCount() throws IOException {
    // Count the number of people with >=1 occurrence of condition = "Type 2 diabetes mellitus",
    // grouped by gender and race.
    countSingleCriteriaCohort(getEntity(), "diabetes", List.of("gender", "race"), 201_826L);
  }

  @Override
  protected String getEntityName() {
    return "condition";
  }
}
