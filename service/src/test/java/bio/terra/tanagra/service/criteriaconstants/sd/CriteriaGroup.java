package bio.terra.tanagra.service.criteriaconstants.sd;

import static bio.terra.tanagra.service.criteriaconstants.sd.Criteria.CONDITION_AGE_AT_OCCURRENCE_EQ_65;
import static bio.terra.tanagra.service.criteriaconstants.sd.Criteria.CONDITION_COUNT_GTE_2;
import static bio.terra.tanagra.service.criteriaconstants.sd.Criteria.CONDITION_EQ_TYPE_2_DIABETES;
import static bio.terra.tanagra.service.criteriaconstants.sd.Criteria.GENDER_EQ_WOMAN;
import static bio.terra.tanagra.service.criteriaconstants.sd.Criteria.PROCEDURE_AGE_AT_OCCURRENCE_EQ_45;
import static bio.terra.tanagra.service.criteriaconstants.sd.Criteria.PROCEDURE_EQ_AMPUTATION;

import bio.terra.tanagra.service.artifact.model.CohortRevision;
import java.util.List;

public final class CriteriaGroup {
  private CriteriaGroup() {}

  public static final CohortRevision.CriteriaGroup CG_EMPTY =
      CohortRevision.CriteriaGroup.builder().id("cg1").build();
  public static final CohortRevision.CriteriaGroup CG_GENDER =
      CohortRevision.CriteriaGroup.builder().id("cg2").criteria(List.of(GENDER_EQ_WOMAN)).build();
  public static final CohortRevision.CriteriaGroup DISABLED_CG_GENDER =
      CohortRevision.CriteriaGroup.builder()
          .id("cg2d")
          .criteria(List.of(GENDER_EQ_WOMAN))
          .isDisabled(true)
          .build();
  public static final CohortRevision.CriteriaGroup CG_CONDITION =
      CohortRevision.CriteriaGroup.builder()
          .id("cg3")
          .criteria(List.of(CONDITION_EQ_TYPE_2_DIABETES))
          .build();
  public static final CohortRevision.CriteriaGroup CG_CONDITION_WITH_MODIFIER =
      CohortRevision.CriteriaGroup.builder()
          .id("cg4")
          .criteria(List.of(CONDITION_EQ_TYPE_2_DIABETES, CONDITION_AGE_AT_OCCURRENCE_EQ_65))
          .build();
  public static final CohortRevision.CriteriaGroup CG_CONDITION_WITH_GROUP_BY_MODIFIER =
      CohortRevision.CriteriaGroup.builder()
          .id("cg5")
          .criteria(List.of(CONDITION_EQ_TYPE_2_DIABETES, CONDITION_COUNT_GTE_2))
          .build();
  public static final CohortRevision.CriteriaGroup CG_PROCEDURE =
      CohortRevision.CriteriaGroup.builder()
          .id("cg6")
          .criteria(List.of(PROCEDURE_EQ_AMPUTATION))
          .build();

  public static final CohortRevision.CriteriaGroup CG_PROCEDURE_WITH_MODIFIER =
      CohortRevision.CriteriaGroup.builder()
          .id("cg7")
          .criteria(List.of(PROCEDURE_EQ_AMPUTATION, PROCEDURE_AGE_AT_OCCURRENCE_EQ_45))
          .build();
}
