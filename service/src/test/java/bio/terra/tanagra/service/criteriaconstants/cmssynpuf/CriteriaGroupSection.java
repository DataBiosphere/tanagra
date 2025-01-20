package bio.terra.tanagra.service.criteriaconstants.cmssynpuf;

import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.shared.JoinOperator;
import bio.terra.tanagra.api.shared.ReducingOperator;
import bio.terra.tanagra.service.artifact.model.CohortRevision;
import java.util.List;

public final class CriteriaGroupSection {
  private CriteriaGroupSection() {}

  public static final CohortRevision.CriteriaGroupSection CRITERIA_GROUP_SECTION_GENDER =
      CohortRevision.CriteriaGroupSection.builder()
          .displayName("section gender")
          .criteriaGroups(List.of(CriteriaGroup.CRITERIA_GROUP_GENDER))
          .build();
  public static final CohortRevision.CriteriaGroupSection CRITERIA_GROUP_SECTION_AGE =
      CohortRevision.CriteriaGroupSection.builder()
          .displayName("section age")
          .criteriaGroups(List.of(CriteriaGroup.CRITERIA_GROUP_AGE))
          .build();
  public static final CohortRevision.CriteriaGroupSection CRITERIA_GROUP_SECTION_DEMOGRAPHICS =
      CohortRevision.CriteriaGroupSection.builder()
          .displayName("section demographics")
          .criteriaGroups(List.of(CriteriaGroup.CRITERIA_GROUP_DEMOGRAPHICS))
          .build();
  public static final CohortRevision.CriteriaGroupSection
      CRITERIA_GROUP_SECTION_DEMOGRAPHICS_AND_CONDITION =
          CohortRevision.CriteriaGroupSection.builder()
              .displayName("section demographics and condition")
              .criteriaGroups(
                  List.of(
                      CriteriaGroup.CRITERIA_GROUP_DEMOGRAPHICS,
                      CriteriaGroup.CRITERIA_GROUP_CONDITION))
              .operator(BooleanAndOrFilter.LogicalOperator.OR)
              .build();

  public static final CohortRevision.CriteriaGroupSection
      DISABLED_CRITERIA_GROUP_SECTION_DEMOGRAPHICS_AND_CONDITION =
          CohortRevision.CriteriaGroupSection.builder()
              .displayName("disabled section demographics and condition")
              .criteriaGroups(
                  List.of(
                      CriteriaGroup.CRITERIA_GROUP_DEMOGRAPHICS,
                      CriteriaGroup.CRITERIA_GROUP_CONDITION))
              .operator(BooleanAndOrFilter.LogicalOperator.OR)
              .setIsDisabled(true)
              .build();

  public static final CohortRevision.CriteriaGroupSection
      CRITERIA_GROUP_SECTION_CONDITION_AND_DISABLED_DEMOGRAPHICS =
          CohortRevision.CriteriaGroupSection.builder()
              .displayName("section condition and disabled demographics")
              .criteriaGroups(
                  List.of(
                      CriteriaGroup.CRITERIA_GROUP_CONDITION,
                      CriteriaGroup.DISABLED_CRITERIA_GROUP_DEMOGRAPHICS))
              .operator(BooleanAndOrFilter.LogicalOperator.OR)
              .build();

  public static final CohortRevision.CriteriaGroupSection CRITERIA_GROUP_SECTION_PROCEDURE =
      CohortRevision.CriteriaGroupSection.builder()
          .displayName("section procedure")
          .criteriaGroups(List.of(CriteriaGroup.CRITERIA_GROUP_PROCEDURE))
          .operator(BooleanAndOrFilter.LogicalOperator.AND)
          .setIsExcluded(true)
          .build();

  public static final CohortRevision.CriteriaGroupSection
      CRITERIA_GROUP_SECTION_TEMPORAL_WITHIN_NUM_DAYS =
          CohortRevision.CriteriaGroupSection.builder()
              .displayName("temporal section")
              .criteriaGroups(List.of(CriteriaGroup.CRITERIA_GROUP_CONDITION))
              .secondConditionCriteriaGroups(List.of(CriteriaGroup.CRITERIA_GROUP_PROCEDURE))
              .operator(BooleanAndOrFilter.LogicalOperator.OR)
              .firstConditionReducingOperator(ReducingOperator.FIRST_MENTION_OF)
              .secondConditionReducingOperator(ReducingOperator.LAST_MENTION_OF)
              .joinOperator(JoinOperator.WITHIN_NUM_DAYS)
              .joinOperatorValue(5)
              .setIsExcluded(false)
              .build();

  public static final CohortRevision.CriteriaGroupSection
      CRITERIA_GROUP_SECTION_TEMPORAL_DURING_SAME_ENCOUNTER =
          CohortRevision.CriteriaGroupSection.builder()
              .displayName("temporal section")
              .criteriaGroups(
                  List.of(
                      CriteriaGroup.CRITERIA_GROUP_CONDITION, CriteriaGroup.CRITERIA_GROUP_GENDER))
              .secondConditionCriteriaGroups(
                  List.of(CriteriaGroup.CRITERIA_GROUP_AGE, CriteriaGroup.CRITERIA_GROUP_PROCEDURE))
              .operator(BooleanAndOrFilter.LogicalOperator.OR)
              .firstConditionReducingOperator(ReducingOperator.FIRST_MENTION_OF)
              .secondConditionReducingOperator(null)
              .joinOperator(JoinOperator.DURING_SAME_ENCOUNTER)
              .joinOperatorValue(null)
              .setIsExcluded(false)
              .build();
}
