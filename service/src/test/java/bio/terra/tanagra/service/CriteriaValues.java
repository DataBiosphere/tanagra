package bio.terra.tanagra.service;

import static bio.terra.tanagra.utils.ProtobufUtils.serializeToJson;

import bio.terra.tanagra.proto.criteriaselector.KeyOuterClass;
import bio.terra.tanagra.proto.criteriaselector.ValueOuterClass;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTAttribute;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTEntityGroup;
import bio.terra.tanagra.service.artifact.model.Criteria;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;

public final class CriteriaValues {
  private CriteriaValues() {}

  public static final Pair<String, Criteria> DEMOGRAPHICS_PREPACKAGED_DATA_FEATURE =
      Pair.of(
          "person",
          Criteria.builder()
              .predefinedId("_demographics")
              .pluginName("ouptutUnfiltered")
              .pluginVersion(0)
              .selectionData("")
              .uiConfig("")
              .build());

  public static final Pair<String, Criteria> GENDER_EQ_WOMAN =
      Pair.of(
          "person",
          Criteria.builder()
              .selectorOrModifierName("tanagra-gender")
              .pluginName("attribute")
              .pluginVersion(0)
              .selectionData(
                  serializeToJson(
                      DTAttribute.Attribute.newBuilder()
                          .addSelected(
                              DTAttribute.Attribute.Selection.newBuilder()
                                  .setValue(
                                      ValueOuterClass.Value.newBuilder()
                                          .setInt64Value(8_532L)
                                          .build())
                                  .setName("Female")
                                  .build())
                          .build()))
              .uiConfig("")
              .tags(Map.of("0", "tag1", "1", "tag2", "2", "tag3"))
              .build());

  public static final Pair<String, Criteria> ETHNICITY_EQ_HISPANIC_OR_LATINO =
      Pair.of(
          "person",
          Criteria.builder()
              .selectorOrModifierName("tanagra-ethnicity")
              .pluginName("attribute")
              .pluginVersion(4)
              .selectionData(
                  serializeToJson(
                      DTAttribute.Attribute.newBuilder()
                          .addSelected(
                              DTAttribute.Attribute.Selection.newBuilder()
                                  .setValue(
                                      ValueOuterClass.Value.newBuilder()
                                          .setInt64Value(38_003_563L)
                                          .build())
                                  .setName("Hispanic or Latino")
                                  .build())
                          .build()))
              .uiConfig("")
              .tags(Map.of("1", "tag1"))
              .build());

  public static final Pair<String, Criteria> CONDITION_EQ_TYPE_2_DIABETES =
      Pair.of(
          "condition",
          Criteria.builder()
              .selectorOrModifierName("tanagra-conditions")
              .pluginName("entityGroup")
              .pluginVersion(0)
              .selectionData(
                  serializeToJson(
                      DTEntityGroup.EntityGroup.newBuilder()
                          .addSelected(
                              DTEntityGroup.EntityGroup.Selection.newBuilder()
                                  .setKey(
                                      KeyOuterClass.Key.newBuilder().setInt64Key(201_826L).build())
                                  .setName("Type 2 diabetes mellitus")
                                  .setEntityGroup("conditionPerson")
                                  .build())
                          .build()))
              .uiConfig("")
              .tags(Map.of())
              .build());

  public static final Pair<String, Criteria> PROCEDURE_EQ_AMPUTATION =
      Pair.of(
          "procedure",
          Criteria.builder()
              .selectorOrModifierName("tanagra-procedures")
              .pluginName("entityGroup")
              .pluginVersion(11)
              .selectionData(
                  serializeToJson(
                      DTEntityGroup.EntityGroup.newBuilder()
                          .addSelected(
                              DTEntityGroup.EntityGroup.Selection.newBuilder()
                                  .setKey(
                                      KeyOuterClass.Key.newBuilder().setInt64Key(234_523L).build())
                                  .setName("Amputation")
                                  .setEntityGroup("procedurePerson")
                                  .build())
                          .build()))
              .uiConfig("")
              .tags(Map.of("0", "tag4", "2", "tag5"))
              .build());
}
