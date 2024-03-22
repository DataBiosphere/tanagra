package bio.terra.tanagra.service.criteriaconstants.cmssynpuf;

import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
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

  public static final CohortRevision.CriteriaGroupSection CRITERIA_GROUP_SECTION_PROCEDURE =
      CohortRevision.CriteriaGroupSection.builder()
          .displayName("section procedure")
          .criteriaGroups(List.of(CriteriaGroup.CRITERIA_GROUP_PROCEDURE))
          .operator(BooleanAndOrFilter.LogicalOperator.AND)
          .setIsExcluded(true)
          .build();
}
