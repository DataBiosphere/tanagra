package bio.terra.tanagra.service.criteriaconstants.sd;

import static bio.terra.tanagra.service.criteriaconstants.sd.CriteriaGroup.CG_CONDITION;
import static bio.terra.tanagra.service.criteriaconstants.sd.CriteriaGroup.CG_CONDITION_WITH_GROUP_BY_MODIFIER;
import static bio.terra.tanagra.service.criteriaconstants.sd.CriteriaGroup.CG_CONDITION_WITH_MODIFIER;
import static bio.terra.tanagra.service.criteriaconstants.sd.CriteriaGroup.CG_GENDER;
import static bio.terra.tanagra.service.criteriaconstants.sd.CriteriaGroup.CG_PROCEDURE;
import static bio.terra.tanagra.service.criteriaconstants.sd.CriteriaGroup.CG_PROCEDURE_WITH_MODIFIER;

import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.shared.*;
import bio.terra.tanagra.service.artifact.model.CohortRevision;
import java.util.List;

public final class CriteriaGroupSection {
  private CriteriaGroupSection() {}

  public static final CohortRevision.CriteriaGroupSection CGS_EMPTY =
      CohortRevision.CriteriaGroupSection.builder().id("cgs1").build();
  public static final CohortRevision.CriteriaGroupSection CGS_CONDITION_EXCLUDED =
      CohortRevision.CriteriaGroupSection.builder()
          .id("cgs2")
          .criteriaGroups(List.of(CG_CONDITION_WITH_MODIFIER))
          .setIsExcluded(true)
          .build();
  public static final CohortRevision.CriteriaGroupSection CGS_GENDER_AND_CONDITION =
      CohortRevision.CriteriaGroupSection.builder()
          .id("cgs3")
          .criteriaGroups(List.of(CG_GENDER, CG_CONDITION_WITH_MODIFIER))
          .operator(BooleanAndOrFilter.LogicalOperator.AND)
          .build();
  public static final CohortRevision.CriteriaGroupSection CGS_GENDER =
      CohortRevision.CriteriaGroupSection.builder()
          .id("cgs4")
          .criteriaGroups(List.of(CG_GENDER))
          .build();
  public static final CohortRevision.CriteriaGroupSection CGS_PROCEDURE =
      CohortRevision.CriteriaGroupSection.builder()
          .id("cgs5")
          .criteriaGroups(List.of(CG_PROCEDURE))
          .build();

  public static final CohortRevision.CriteriaGroupSection CGS_TEMPORAL_SINGLE_GROUP_PER_CONDITION =
      CohortRevision.CriteriaGroupSection.builder()
          .id("cgs6")
          .criteriaGroups(List.of(CG_CONDITION))
          .secondConditionCriteriaGroups(List.of(CG_PROCEDURE))
          .firstConditionReducingOperator(ReducingOperator.FIRST_MENTION_OF)
          .secondConditionReducingOperator(null)
          .operator(BooleanAndOrFilter.LogicalOperator.OR)
          .joinOperator(JoinOperator.NUM_DAYS_BEFORE)
          .joinOperatorValue(10)
          .build();

  public static final CohortRevision.CriteriaGroupSection
      CGS_TEMPORAL_MULTIPLE_GROUPS_PER_CONDITION =
          CohortRevision.CriteriaGroupSection.builder()
              .id("cgs7")
              .criteriaGroups(List.of(CG_CONDITION, CG_PROCEDURE_WITH_MODIFIER))
              .secondConditionCriteriaGroups(List.of(CG_PROCEDURE, CG_CONDITION_WITH_MODIFIER))
              .firstConditionReducingOperator(ReducingOperator.LAST_MENTION_OF)
              .secondConditionReducingOperator(ReducingOperator.FIRST_MENTION_OF)
              .operator(BooleanAndOrFilter.LogicalOperator.OR)
              .joinOperator(JoinOperator.DURING_SAME_ENCOUNTER)
              .joinOperatorValue(null)
              .build();

  public static final CohortRevision.CriteriaGroupSection
      CGS_TEMPORAL_UNSUPPORTED_CRITERIA_SELECTOR =
          CohortRevision.CriteriaGroupSection.builder()
              .id("cgs8")
              .criteriaGroups(List.of(CG_GENDER, CG_CONDITION))
              .secondConditionCriteriaGroups(List.of(CG_PROCEDURE))
              .firstConditionReducingOperator(ReducingOperator.FIRST_MENTION_OF)
              .secondConditionReducingOperator(null)
              .operator(BooleanAndOrFilter.LogicalOperator.OR)
              .joinOperator(JoinOperator.NUM_DAYS_BEFORE)
              .joinOperatorValue(10)
              .build();

  public static final CohortRevision.CriteriaGroupSection CGS_TEMPORAL_UNSUPPORTED_MODIFIER =
      CohortRevision.CriteriaGroupSection.builder()
          .id("cgs9")
          .criteriaGroups(List.of(CG_CONDITION_WITH_GROUP_BY_MODIFIER))
          .secondConditionCriteriaGroups(List.of(CG_PROCEDURE))
          .firstConditionReducingOperator(ReducingOperator.FIRST_MENTION_OF)
          .secondConditionReducingOperator(null)
          .operator(BooleanAndOrFilter.LogicalOperator.OR)
          .joinOperator(JoinOperator.NUM_DAYS_BEFORE)
          .joinOperatorValue(10)
          .build();

  public static final CohortRevision.CriteriaGroupSection
      CGS_TEMPORAL_NO_SUPPORTED_GROUPS_IN_SECOND_CONDITION =
          CohortRevision.CriteriaGroupSection.builder()
              .id("cgs10")
              .criteriaGroups(List.of(CG_CONDITION_WITH_MODIFIER))
              .secondConditionCriteriaGroups(List.of(CG_GENDER))
              .firstConditionReducingOperator(ReducingOperator.FIRST_MENTION_OF)
              .secondConditionReducingOperator(null)
              .operator(BooleanAndOrFilter.LogicalOperator.OR)
              .joinOperator(JoinOperator.NUM_DAYS_BEFORE)
              .joinOperatorValue(10)
              .build();

  public static final CohortRevision.CriteriaGroupSection
      CGS_NON_TEMPORAL_GROUPS_IN_SECOND_CONDITION =
          CohortRevision.CriteriaGroupSection.builder()
              .id("cgs11")
              .criteriaGroups(List.of(CG_GENDER))
              .secondConditionCriteriaGroups(List.of(CG_CONDITION_WITH_MODIFIER))
              .operator(BooleanAndOrFilter.LogicalOperator.AND)
              .build();
}
