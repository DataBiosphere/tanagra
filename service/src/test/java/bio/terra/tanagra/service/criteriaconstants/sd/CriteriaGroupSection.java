package bio.terra.tanagra.service.criteriaconstants.sd;

import static bio.terra.tanagra.service.criteriaconstants.sd.CriteriaGroup.CG_CONDITION_WITH_MODIFIER;
import static bio.terra.tanagra.service.criteriaconstants.sd.CriteriaGroup.CG_GENDER;
import static bio.terra.tanagra.service.criteriaconstants.sd.CriteriaGroup.CG_PROCEDURE;

import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
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
}
