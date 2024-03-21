package bio.terra.tanagra.service.criteriaconstants.sd;

import static bio.terra.tanagra.utils.ProtobufUtils.serializeToJson;

import bio.terra.tanagra.proto.criteriaselector.KeyOuterClass;
import bio.terra.tanagra.proto.criteriaselector.ValueOuterClass;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTAttribute;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTEntityGroup;
import java.util.Map;

public final class Criteria {
  private Criteria() {}

  public static final bio.terra.tanagra.service.artifact.model.Criteria GENDER_EQ_WOMAN =
      bio.terra.tanagra.service.artifact.model.Criteria.builder()
          .selectorOrModifierName("tanagra-gender")
          .pluginName("attribute")
          .pluginVersion(0)
          .selectionData(
              serializeToJson(
                  DTAttribute.Attribute.newBuilder()
                      .addSelected(
                          DTAttribute.Attribute.Selection.newBuilder()
                              .setValue(
                                  ValueOuterClass.Value.newBuilder().setInt64Value(8_532L).build())
                              .setName("Female")
                              .build())
                      .build()))
          .build();

  public static final bio.terra.tanagra.service.artifact.model.Criteria
      CONDITION_EQ_TYPE_2_DIABETES =
          bio.terra.tanagra.service.artifact.model.Criteria.builder()
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
              .build();

  public static final bio.terra.tanagra.service.artifact.model.Criteria
      CONDITION_AGE_AT_OCCURRENCE_EQ_65 =
          bio.terra.tanagra.service.artifact.model.Criteria.builder()
              .selectorOrModifierName("ageAtOccurrence")
              .pluginName("attribute")
              .pluginVersion(0)
              .selectionData(
                  serializeToJson(
                      DTAttribute.Attribute.newBuilder()
                          .addSelected(
                              DTAttribute.Attribute.Selection.newBuilder()
                                  .setValue(
                                      ValueOuterClass.Value.newBuilder().setInt64Value(65L).build())
                                  .setName("age_at_occurrence")
                                  .build())
                          .build()))
              .build();

  public static final bio.terra.tanagra.service.artifact.model.Criteria PROCEDURE_EQ_AMPUTATION =
      bio.terra.tanagra.service.artifact.model.Criteria.builder()
          .selectorOrModifierName("tanagra-procedures")
          .pluginName("entityGroup")
          .pluginVersion(11)
          .selectionData(
              serializeToJson(
                  DTEntityGroup.EntityGroup.newBuilder()
                      .addSelected(
                          DTEntityGroup.EntityGroup.Selection.newBuilder()
                              .setKey(KeyOuterClass.Key.newBuilder().setInt64Key(234_523L).build())
                              .setName("Amputation")
                              .setEntityGroup("procedurePerson")
                              .build())
                      .build()))
          .build();
  public static final bio.terra.tanagra.service.artifact.model.Criteria
      DEMOGRAPHICS_PREPACKAGED_DATA_FEATURE =
          bio.terra.tanagra.service.artifact.model.Criteria.builder()
              .predefinedId("_demographics")
              .pluginName("ouptutUnfiltered")
              .pluginVersion(0)
              .selectionData("")
              .uiConfig("")
              .build();
}
