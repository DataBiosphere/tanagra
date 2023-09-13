package bio.terra.tanagra.service;

import static bio.terra.tanagra.service.CriteriaValues.CONDITION_EQ_DIABETES;
import static bio.terra.tanagra.service.CriteriaValues.ETHNICITY_EQ_JAPANESE;
import static bio.terra.tanagra.service.CriteriaValues.GENDER_EQ_WOMAN;
import static bio.terra.tanagra.service.CriteriaValues.PROCEDURE_EQ_AMPUTATION;

import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable;
import bio.terra.tanagra.service.artifact.model.CohortRevision;
import java.util.List;

public final class CriteriaGroupSectionValues {
  private CriteriaGroupSectionValues() {}

  public static final CohortRevision.CriteriaGroup CRITERIA_GROUP_1 =
      CohortRevision.CriteriaGroup.builder()
          .displayName("group 1")
          .criteria(List.of(GENDER_EQ_WOMAN.getValue(), ETHNICITY_EQ_JAPANESE.getValue()))
          .entity(GENDER_EQ_WOMAN.getKey())
          .build();
  public static final CohortRevision.CriteriaGroup CRITERIA_GROUP_2 =
      CohortRevision.CriteriaGroup.builder()
          .displayName("group 2")
          .criteria(List.of(CONDITION_EQ_DIABETES.getValue()))
          .entity(CONDITION_EQ_DIABETES.getKey())
          .groupByCountOperator(BinaryFilterVariable.BinaryOperator.EQUALS)
          .groupByCountValue(11)
          .build();
  public static final CohortRevision.CriteriaGroup CRITERIA_GROUP_3 =
      CohortRevision.CriteriaGroup.builder()
          .displayName("group 3")
          .criteria(List.of(PROCEDURE_EQ_AMPUTATION.getValue()))
          .entity(PROCEDURE_EQ_AMPUTATION.getKey())
          .build();

  public static final CohortRevision.CriteriaGroupSection CRITERIA_GROUP_SECTION_1 =
      CohortRevision.CriteriaGroupSection.builder()
          .displayName("section 1")
          .criteriaGroups(List.of(CRITERIA_GROUP_1, CRITERIA_GROUP_2))
          .operator(BooleanAndOrFilterVariable.LogicalOperator.OR)
          .build();
  public static final CohortRevision.CriteriaGroupSection CRITERIA_GROUP_SECTION_2 =
      CohortRevision.CriteriaGroupSection.builder()
          .displayName("section 2")
          .criteriaGroups(List.of(CRITERIA_GROUP_3))
          .operator(BooleanAndOrFilterVariable.LogicalOperator.AND)
          .setIsExcluded(true)
          .build();
  public static final CohortRevision.CriteriaGroupSection CRITERIA_GROUP_SECTION_3 =
      CohortRevision.CriteriaGroupSection.builder()
          .displayName("section 3")
          .criteriaGroups(List.of(CRITERIA_GROUP_1))
          .build();
}
