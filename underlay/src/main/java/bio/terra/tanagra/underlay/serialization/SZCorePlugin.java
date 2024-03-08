package bio.terra.tanagra.underlay.serialization;

import bio.terra.tanagra.annotation.AnnotatedClass;
import bio.terra.tanagra.annotation.AnnotatedField;

@AnnotatedClass(
    name = "SZCorePlugin",
    markdown =
        "Names of core plugins in the criteria selector and prepackaged criteria definitions.")
public enum SZCorePlugin {
  @AnnotatedField(name = "SZCorePlugin.ATTRIBUTE", markdown = "Use `plugin: \"attribute\"`.")
  ATTRIBUTE("attribute"),
  @AnnotatedField(name = "SZCorePlugin.ENTITY_GROUP", markdown = "Use `plugin: \"entityGroup\"`.")
  ENTITY_GROUP("entityGroup"),
  @AnnotatedField(
      name = "SZCorePlugin.MULTI_ATTRIBUTE",
      markdown = "Use `plugin: \"multiAttribute\"`.")
  MULTI_ATTRIBUTE("multiAttribute"),
  @AnnotatedField(name = "SZCorePlugin.TEXT_SEARCH", markdown = "Use `plugin: \"search\"`.")
  TEXT_SEARCH("search"),
  @AnnotatedField(
      name = "SZCorePlugin.UNHINTED_VALUE",
      markdown = "Use `plugin: \"unhinted-value\"`.")
  UNHINTED_VALUE("unhinted-value"),
  @AnnotatedField(
      name = "SZCorePlugin.OUTPUT_UNFILTERED",
      markdown = "Use `plugin: \"outputUnfiltered\"`.")
  OUTPUT_UNFILTERED("outputUnfiltered");
  private final String idInConfig;

  SZCorePlugin(String idInConfig) {
    this.idInConfig = idInConfig;
  }

  public String getIdInConfig() {
    return idInConfig;
  }
}
