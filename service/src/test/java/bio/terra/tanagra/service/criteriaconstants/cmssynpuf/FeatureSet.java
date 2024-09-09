package bio.terra.tanagra.service.criteriaconstants.cmssynpuf;

import static bio.terra.tanagra.service.criteriaconstants.cmssynpuf.Criteria.DEMOGRAPHICS_PREPACKAGED_DATA_FEATURE;

import java.util.List;
import java.util.Map;

public final class FeatureSet {
  private static final String UNDERLAY_NAME = "cmssynpuf";

  private FeatureSet() {}

  public static final bio.terra.tanagra.service.artifact.model.FeatureSet CS_DEMOGRAPHICS =
      bio.terra.tanagra.service.artifact.model.FeatureSet.builder()
          .underlay(UNDERLAY_NAME)
          .criteria(List.of(DEMOGRAPHICS_PREPACKAGED_DATA_FEATURE.getRight()))
          .build();

  public static final bio.terra.tanagra.service.artifact.model.FeatureSet
      CS_DEMOGRAPHICS_EXCLUDE_ID_GENDER =
          bio.terra.tanagra.service.artifact.model.FeatureSet.builder()
              .underlay(UNDERLAY_NAME)
              .criteria(List.of(DEMOGRAPHICS_PREPACKAGED_DATA_FEATURE.getRight()))
              .excludeOutputAttributesPerEntity(Map.of("person", List.of("id", "gender")))
              .build();
}
