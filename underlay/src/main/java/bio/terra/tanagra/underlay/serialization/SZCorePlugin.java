package bio.terra.tanagra.underlay.serialization;

import bio.terra.tanagra.annotation.AnnotatedClass;
import bio.terra.tanagra.annotation.AnnotatedField;

@AnnotatedClass(
    name = "SZCorePlugin",
    markdown =
        "Names of core plugins in the criteria selector and prepackaged criteria definitions.")
public enum SZCorePlugin {
  @AnnotatedField(name = "SZCorePlugin.ATTRIBUTE", markdown = "Use `plugin: \"core/attribute\"`.")
  ATTRIBUTE("core/attribute"),
  @AnnotatedField(
      name = "SZCorePlugin.ENTITY_GROUP",
      markdown = "Use `plugin: \"core/entityGroup\"`.")
  ENTITY_GROUP("core/entityGroup"),
  @AnnotatedField(
      name = "SZCorePlugin.MULTI_ATTRIBUTE",
      markdown = "Use `plugin: \"core/multiAttribute\"`.")
  MULTI_ATTRIBUTE("core/multiAttribute"),
  @AnnotatedField(name = "SZCorePlugin.TEXT_SEARCH", markdown = "Use `plugin: \"core/search\"`.")
  TEXT_SEARCH("core/search"),
  @AnnotatedField(
      name = "SZCorePlugin.GROUP_BY_COUNT",
      markdown = "Use `plugin: \"core/unhinted-value\"`.")
  GROUP_BY_COUNT("core/unhinted-value");
  private final String idInConfig;

  SZCorePlugin(String idInConfig) {
    this.idInConfig = idInConfig;
  }

  public String getIdInConfig() {
    return idInConfig;
  }
}
