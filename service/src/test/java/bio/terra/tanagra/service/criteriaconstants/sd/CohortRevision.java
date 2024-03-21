package bio.terra.tanagra.service.criteriaconstants.sd;

import static bio.terra.tanagra.service.criteriaconstants.sd.CriteriaGroupSection.CGS_CONDITION_EXCLUDED;
import static bio.terra.tanagra.service.criteriaconstants.sd.CriteriaGroupSection.CGS_GENDER;
import static bio.terra.tanagra.service.criteriaconstants.sd.CriteriaGroupSection.CGS_PROCEDURE;

import bio.terra.tanagra.service.artifact.model.Cohort;
import java.util.List;

public final class CohortRevision {
  private CohortRevision() {}

  private static final String UNDERLAY_NAME = "sd";

  public static final bio.terra.tanagra.service.artifact.model.CohortRevision CR_EMPTY =
      bio.terra.tanagra.service.artifact.model.CohortRevision.builder()
          .id("cr1")
          .setIsMostRecent(true)
          .build();

  public static final bio.terra.tanagra.service.artifact.model.CohortRevision
      CR_CONDITION_EXCLUDED =
          bio.terra.tanagra.service.artifact.model.CohortRevision.builder()
              .id("cr2")
              .sections(List.of(CGS_CONDITION_EXCLUDED))
              .setIsMostRecent(true)
              .build();

  public static final bio.terra.tanagra.service.artifact.model.CohortRevision
      CR_CONDITION_EXCLUDED_AND_GENDER =
          bio.terra.tanagra.service.artifact.model.CohortRevision.builder()
              .id("cr3")
              .sections(List.of(CGS_CONDITION_EXCLUDED, CGS_GENDER))
              .setIsMostRecent(true)
              .build();
  public static final bio.terra.tanagra.service.artifact.model.CohortRevision CR_PROCEDURE =
      bio.terra.tanagra.service.artifact.model.CohortRevision.builder()
          .id("cr4")
          .sections(List.of(CGS_PROCEDURE))
          .setIsMostRecent(true)
          .build();
  public static final Cohort C_EMPTY =
      Cohort.builder().underlay(UNDERLAY_NAME).id("c1").revisions(List.of(CR_EMPTY)).build();
  public static final Cohort C_CONDITION_EXCLUDED =
      Cohort.builder()
          .underlay(UNDERLAY_NAME)
          .id("c2")
          .revisions(List.of(CR_CONDITION_EXCLUDED))
          .build();
  public static final Cohort C_CONDITION_EXCLUDED_AND_GENDER =
      Cohort.builder()
          .underlay(UNDERLAY_NAME)
          .id("c3")
          .revisions(List.of(CR_CONDITION_EXCLUDED_AND_GENDER))
          .build();
  public static final Cohort C_PROCEDURE =
      Cohort.builder().underlay(UNDERLAY_NAME).id("c5").revisions(List.of(CR_PROCEDURE)).build();
}
