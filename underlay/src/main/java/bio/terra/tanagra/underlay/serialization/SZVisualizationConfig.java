package bio.terra.tanagra.underlay.serialization;

import bio.terra.tanagra.annotation.AnnotatedClass;
import bio.terra.tanagra.annotation.AnnotatedField;

@AnnotatedClass(
    name = "SZVisualizationConfig",
    markdown = "Configuration for a single visualization.")
public class SZVisualizationConfig {
  @AnnotatedField(
      name = "SZVisualizationConfig.name",
      markdown =
          "Name of the visualization.\n\n"
              + "This is the unique identifier for the vizualization. The vizualization names cannot overlap within an "
              + "underlay.\n\n"
              + "Name may not include spaces or special characters, only letters and numbers.")
  public String name;

  @AnnotatedField(
      name = "SZVisualizationConfig.title",
      markdown = "Visible title of the visualization.")
  public String title;

  @AnnotatedField(
      name = "SZVisualizationConfig.dataConfig",
      markdown =
          "Serialized configuration of the visualization. VizConfig protocol buffer as JSON.")
  public String dataConfig;

  @AnnotatedField(
      name = "SZVisualizationConfig.dataConfigFile",
      markdown =
          "Name of the file that contains the serialized configuration of the visualization.\n\n"
              + "This file should be in the same directory as the visualization (e.g. `gender.json`).\n\n"
              + "If this property is specified, the value of the `config` property is ignored.")
  public String dataConfigFile;

  @AnnotatedField(
      name = "SZVisualizationConfig.plugin",
      markdown = "Name of the visualization UI plugin.")
  public String plugin;

  @AnnotatedField(
      name = "SZVisualizationConfig.pluginConfig",
      markdown = "Serialized configuration of the visualization UI plugin as JSON.")
  public String pluginConfig;

  @AnnotatedField(
      name = "SZVisualizationConfig.pluginConfigFile",
      markdown =
          "Name of the file that contains the serialized configuration of the visualization UI plugin.\n\n"
              + "This file should be in the same directory as the visualization (e.g. `gender.json`).\n\n"
              + "If this property is specified, the value of the `pluginConfig` property is ignored.")
  public String pluginConfigFile;
}
