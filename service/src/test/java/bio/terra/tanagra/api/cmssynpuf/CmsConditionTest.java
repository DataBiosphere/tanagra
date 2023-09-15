package bio.terra.tanagra.api.cmssynpuf;

import bio.terra.tanagra.api.omop.OmopConditionTest;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable;
import bio.terra.tanagra.underlay.Entity;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

public class CmsConditionTest extends OmopConditionTest {
  @Override
  protected String getUnderlayName() {
    return "cms_synpuf";
  }

  @Test
  void mariko() throws IOException {
    Entity occurrenceEntity =
        getCriteriaOccurrenceEntityGroup(getEntity()).getOccurrenceEntities().get(0);

    // Cohort of people with > 1 occurrence date of condition = "Type 2 diabetes mellitus".
    singleCriteriaCohort(
        getEntity(),
        "diabetes-numOccurrenceDates",
        List.of(201_826L),
        BooleanAndOrFilterVariable.LogicalOperator.AND,
        /*groupByCountAttribute=*/ occurrenceEntity.getAttribute("condition"),
        /*groupByCountOperator=*/ BinaryFilterVariable.BinaryOperator.GREATER_THAN,
        /*groupByCountValue=*/ 1);
  }
}
