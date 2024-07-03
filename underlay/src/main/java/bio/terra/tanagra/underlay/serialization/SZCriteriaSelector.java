package bio.terra.tanagra.underlay.serialization;

import bio.terra.tanagra.annotation.AnnotatedClass;
import bio.terra.tanagra.annotation.AnnotatedField;
import java.util.List;

@AnnotatedClass(
    name = "SZCriteriaSelector",
    markdown =
        "Criteria selector configuration.\n\n"
            + "Define a version of this file for each set of UI plugins + configuration.")
public class SZCriteriaSelector {
  @AnnotatedField(
      name = "SZCriteriaSelector.name",
      markdown =
          "Name of the criteria selector.\n\n"
              + "This is the unique identifier for the selector. The selector names cannot overlap within an underlay.\n\n"
              + "Name may not include spaces or special characters, only letters and numbers.\n\n"
              + "This name is stored in the application database for cohorts and data feature sets, so once "
              + "there are artifacts associated with a criteria selector, you can't change the selector name.")
  public String name;

  @AnnotatedField(
      name = "SZCriteriaSelector.isEnabledForCohorts",
      markdown = "True if this criteria selector should be displayed in the cohort builder.")
  public boolean isEnabledForCohorts;

  @AnnotatedField(
      name = "SZCriteriaSelector.isEnabledForDataFeatureSets",
      markdown =
          "True if this criteria selector should be displayed in the data feature set builder.")
  public boolean isEnabledForDataFeatureSets;

  @AnnotatedField(name = "SZCriteriaSelector.displayName", markdown = "Display name.")
  public String displayName;

  @AnnotatedField(name = "SZCriteriaSelector.display", markdown = "Display information.")
  public Display display;

  @AnnotatedField(
      name = "SZCriteriaSelector.filterBuilder",
      markdown =
          "Name of a Java class that implements the `FilterBuilder` interface. "
              + "This class will take in the selector configuration and user selections and produce an "
              + "`EntityFilter` on either the primary entity (for a cohort) or another entity (for a data feature).")
  public String filterBuilder;

  @AnnotatedField(
      name = "SZCriteriaSelector.plugin",
      markdown =
          "Name of the primary UI display plugin. (e.g. selector for condition, not any of the modifiers).\n\n"
              + "This plugin name is stored in the application database, so once there are cohorts or "
              + "data features that use this selector, you can't change the plugin names.\n\n"
              + "The plugin can either be one of the core plugins (e.g. core/attribute, all possibilities are "
              + "listed [here](${SZCorePlugin}), or a dataset-specific plugin (e.g. sd/biovu).")
  public String plugin;

  @AnnotatedField(
      name = "SZCriteriaSelector.pluginConfig",
      markdown =
          "Serialized configuration of the primary UI display plugin e.g. \"{\"attribute\":\"gender\"}\".")
  public String pluginConfig;

  @AnnotatedField(
      name = "SZCriteriaSelector.pluginConfig",
      markdown =
          "Name of the file that contains the serialized configuration of the primary UI display plugin.\n\n"
              + "This file should be in the same directory as the criteria selector (e.g. `gender.json`).\n\n"
              + "If this property is specified, the value of the `pluginConfig` property is ignored.")
  public String pluginConfigFile;

  @AnnotatedField(
      name = "SZCriteriaSelector.supportsTemporalQueries",
      markdown = "True if this criteria selector supports temporal queries.")
  public boolean supportsTemporalQueries;

  @AnnotatedField(name = "SZCriteriaSelector.modifiers", markdown = "Configuration for modifiers.")
  public List<Modifier> modifiers;

  @AnnotatedClass(
      name = "SZCriteriaSelectorDisplay",
      markdown = "Criteria selector display configuration.")
  public static class Display {
    @AnnotatedField(
        name = "SZCriteriaSelectorDisplay.category",
        markdown =
            "Category that the criteria selector is listed under when a user goes to \n\n"
                + "add a new criteria. (e.g. \"Vitals\")")
    public String category;

    @AnnotatedField(
        name = "SZCriteriaSelectorDisplay.tags",
        markdown =
            "Tags that the criteria selector should match when a user uses the dropdown in the add new\n\n"
                + "criteria page. (e.g. \"Source Codes\")")
    public List<String> tags;
  }

  @AnnotatedClass(
      name = "SZCriteriaSelectorModifier",
      markdown = "Criteria selector display configuration.")
  public static class Modifier {
    @AnnotatedField(
        name = "SZCriteriaSelectorModifier.name",
        markdown =
            "Name of the criteria selector modifier.\n\n"
                + "This is the unique identifier for the modifier. The modifier names cannot overlap within a selector.\n\n"
                + "Name may not include spaces or special characters, only letters and numbers.\n\n"
                + "This name is stored in the application database for cohorts and data feature sets, so once "
                + "there are artifacts associated with a modifier, you can't change the modifier name.")
    public String name;

    @AnnotatedField(name = "SZCriteriaSelectorModifier.displayName", markdown = "Display name.")
    public String displayName;

    @AnnotatedField(
        name = "SZCriteriaSelectorModifier.plugin",
        markdown =
            "Name of the modifier UI display plugin. (e.g. selector for condition visit type).\n\n"
                + "This plugin name is stored in the application database, so once there are cohorts or "
                + "data features that use this modifier, you can't change the plugin names.\n\n"
                + "The plugin can either be one of the core plugins (e.g. core/attribute, all possibilities are "
                + "listed [here](${SZCorePlugin}), or a dataset-specific plugin (e.g. sd/biovu).")
    public String plugin;

    @AnnotatedField(
        name = "SZCriteriaSelectorModifier.pluginConfig",
        markdown =
            "Serialized configuration of the modifier UI display plugin e.g. \"{\"attribute\":\"visitType\"}\".")
    public String pluginConfig;

    @AnnotatedField(
        name = "SZCriteriaSelectorModifier.pluginConfig",
        markdown =
            "Name of the file that contains the serialized configuration of the modifier UI display plugin.\n\n"
                + "This file should be in the same directory as the criteria selector (e.g. `visitType.json`).\n\n"
                + "If this property is specified, the value of the `pluginConfig` property is ignored.")
    public String pluginConfigFile;

    @AnnotatedField(
        name = "SZCriteriaSelector.supportsTemporalQueries",
        markdown = "True if this modifier supports temporal queries.")
    public boolean supportsTemporalQueries;
  }
}
