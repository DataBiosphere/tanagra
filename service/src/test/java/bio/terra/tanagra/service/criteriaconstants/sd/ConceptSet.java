package bio.terra.tanagra.service.criteriaconstants.sd;

import static bio.terra.tanagra.service.criteriaconstants.sd.Criteria.CONDITION_EQ_TYPE_2_DIABETES;
import static bio.terra.tanagra.service.criteriaconstants.sd.Criteria.DEMOGRAPHICS_PREPACKAGED_DATA_FEATURE;
import static bio.terra.tanagra.service.criteriaconstants.sd.Criteria.PROCEDURE_EQ_AMPUTATION;

import java.util.List;
import java.util.Map;

public final class ConceptSet {
  private static final String UNDERLAY_NAME = "sd";

  private ConceptSet() {}

  public static final bio.terra.tanagra.service.artifact.model.ConceptSet CS_EMPTY =
      bio.terra.tanagra.service.artifact.model.ConceptSet.builder()
          .underlay(UNDERLAY_NAME)
          .criteria(List.of())
          .build();

  public static final bio.terra.tanagra.service.artifact.model.ConceptSet CS_DEMOGRAPHICS =
      bio.terra.tanagra.service.artifact.model.ConceptSet.builder()
          .underlay(UNDERLAY_NAME)
          .criteria(List.of(DEMOGRAPHICS_PREPACKAGED_DATA_FEATURE))
          .build();

  public static final bio.terra.tanagra.service.artifact.model.ConceptSet
      CS_DEMOGRAPHICS_EXCLUDE_ID_GENDER =
          bio.terra.tanagra.service.artifact.model.ConceptSet.builder()
              .underlay(UNDERLAY_NAME)
              .criteria(List.of(DEMOGRAPHICS_PREPACKAGED_DATA_FEATURE))
              .excludeOutputAttributesPerEntity(Map.of("person", List.of("id", "gender")))
              .build();

  public static final bio.terra.tanagra.service.artifact.model.ConceptSet
      CS_DEMOGRAPHICS_EXCLUDE_ID_AGE =
          bio.terra.tanagra.service.artifact.model.ConceptSet.builder()
              .underlay(UNDERLAY_NAME)
              .criteria(List.of(DEMOGRAPHICS_PREPACKAGED_DATA_FEATURE))
              .excludeOutputAttributesPerEntity(Map.of("person", List.of("id", "age")))
              .build();

  public static final bio.terra.tanagra.service.artifact.model.ConceptSet
      CS_CONDITION_AND_PROCEDURE =
          bio.terra.tanagra.service.artifact.model.ConceptSet.builder()
              .underlay(UNDERLAY_NAME)
              .criteria(List.of(CONDITION_EQ_TYPE_2_DIABETES, PROCEDURE_EQ_AMPUTATION))
              .build();
}
