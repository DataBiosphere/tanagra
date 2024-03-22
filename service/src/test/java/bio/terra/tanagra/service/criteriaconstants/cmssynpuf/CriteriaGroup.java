package bio.terra.tanagra.service.criteriaconstants.cmssynpuf;

import static bio.terra.tanagra.service.criteriaconstants.cmssynpuf.Criteria.CONDITION_EQ_TYPE_2_DIABETES;
import static bio.terra.tanagra.service.criteriaconstants.cmssynpuf.Criteria.ETHNICITY_EQ_HISPANIC_OR_LATINO;
import static bio.terra.tanagra.service.criteriaconstants.cmssynpuf.Criteria.GENDER_EQ_WOMAN;
import static bio.terra.tanagra.service.criteriaconstants.cmssynpuf.Criteria.PROCEDURE_EQ_AMPUTATION;

import bio.terra.tanagra.service.artifact.model.CohortRevision;
import java.util.List;

public final class CriteriaGroup {
  private CriteriaGroup() {}

  public static final CohortRevision.CriteriaGroup CRITERIA_GROUP_GENDER =
      CohortRevision.CriteriaGroup.builder()
          .displayName("group gender")
          .criteria(List.of(GENDER_EQ_WOMAN.getValue()))
          .entity(GENDER_EQ_WOMAN.getKey())
          .build();
  public static final CohortRevision.CriteriaGroup CRITERIA_GROUP_AGE =
      CohortRevision.CriteriaGroup.builder()
          .displayName("group gender")
          .criteria(List.of(GENDER_EQ_WOMAN.getValue()))
          .entity(GENDER_EQ_WOMAN.getKey())
          .build();
  public static final CohortRevision.CriteriaGroup CRITERIA_GROUP_DEMOGRAPHICS =
      CohortRevision.CriteriaGroup.builder()
          .displayName("group 1")
          .criteria(List.of(GENDER_EQ_WOMAN.getValue(), ETHNICITY_EQ_HISPANIC_OR_LATINO.getValue()))
          .entity(GENDER_EQ_WOMAN.getKey())
          .build();
  public static final CohortRevision.CriteriaGroup CRITERIA_GROUP_CONDITION =
      CohortRevision.CriteriaGroup.builder()
          .displayName("group condition")
          .criteria(List.of(CONDITION_EQ_TYPE_2_DIABETES.getValue()))
          .entity(CONDITION_EQ_TYPE_2_DIABETES.getKey())
          .build();
  public static final CohortRevision.CriteriaGroup CRITERIA_GROUP_PROCEDURE =
      CohortRevision.CriteriaGroup.builder()
          .displayName("group procedure")
          .criteria(List.of(PROCEDURE_EQ_AMPUTATION.getValue()))
          .entity(PROCEDURE_EQ_AMPUTATION.getKey())
          .build();
}
