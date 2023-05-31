package bio.terra.tanagra.service;

import bio.terra.tanagra.service.artifact.Criteria;
import java.util.Collections;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;

public final class CriteriaValues {
  private CriteriaValues() {}

  // TODO: Replace the pluginName, selectionData, and uiConfig values with actual values from the
  // UI.
  public static final Pair<String, Criteria> GENDER_EQ_WOMAN =
      Pair.of(
          "person",
          Criteria.builder()
              .displayName("women")
              .pluginName("demographic")
              .selectionData("{gender:'F'}")
              .uiConfig("{entity:'person', attribute:'gender'}")
              .tags(Map.of("0", "tag1", "1", "tag2", "2", "tag3"))
              .build());

  public static final Pair<String, Criteria> ETHNICITY_EQ_JAPANESE =
      Pair.of(
          "person",
          Criteria.builder()
              .displayName("japanese")
              .pluginName("demographic")
              .selectionData("{ethnicity:'jpn'}")
              .uiConfig("{entity:'person', attribute:'ethnicity'}")
              .tags(Map.of("1", "tag1"))
              .build());

  public static final Pair<String, Criteria> CONDITION_EQ_DIABETES =
      Pair.of(
          "condition",
          Criteria.builder()
              .displayName("diabetes")
              .pluginName("condition")
              .selectionData("{condition:445645}")
              .uiConfig("{entity:'condition', attribute:'id'}")
              .tags(Collections.emptyMap())
              .build());

  public static final Pair<String, Criteria> PROCEDURE_EQ_AMPUTATION =
      Pair.of(
          "procedure",
          Criteria.builder()
              .displayName("amputation")
              .pluginName("procedure")
              .selectionData("{procedure:234523}")
              .uiConfig("{entity:'procedure', attribute:'id'}")
              .tags(Map.of("0", "tag4", "2", "tag5"))
              .build());
}
