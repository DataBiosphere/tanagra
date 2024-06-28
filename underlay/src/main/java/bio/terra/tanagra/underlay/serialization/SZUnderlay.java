package bio.terra.tanagra.underlay.serialization;

import bio.terra.tanagra.annotation.AnnotatedClass;
import bio.terra.tanagra.annotation.AnnotatedField;
import java.util.List;
import java.util.Map;
import java.util.Set;

@AnnotatedClass(
    name = "SZUnderlay",
    markdown =
        "Underlay configuration.\n\n"
            + "Define a version of this file for each dataset. If you index and/or serve a dataset in "
            + "multiple places or deployments, you only need one version of this file.")
public class SZUnderlay {
  @AnnotatedField(
      name = "SZUnderlay.name",
      markdown =
          "Name of the underlay.\n\n"
              + "This is the unique identifier for the underlay. If you serve multiple underlays in a single "
              + "service deployment, the underlay names cannot overlap. Name may not include spaces or special "
              + "characters, only letters and numbers.\n\n"
              + "This name is stored in the application database for cohorts and data feature sets, so once "
              + "there are artifacts associated with an underlay, you can't change the underlay name.")
  public String name;

  @AnnotatedField(
      name = "SZUnderlay.primaryEntity",
      markdown =
          "Name of the primary entity.\n\n"
              + "A cohort contains instances of the primary entity (e.g. persons).")
  public String primaryEntity;

  @AnnotatedField(
      name = "SZUnderlay.entities",
      markdown =
          "List of paths of all the entities.\n\n"
              + "An entity is any object that the UI might show a list of (e.g. list of persons, conditions, "
              + "condition occurrences). The list must include the primary entity.\n\n"
              + "Path consists of two parts: [Data-Mapping Group]/[Entity Name] (e.g. `omop/condition`).\n\n"
              + "[Data-Mapping Group] is the name of a sub-directory of the config/datamapping/ sub-directory "
              + "in the underlay sub-project resources (e.g. `omop`).\n\n"
              + "[Entity Name] is specified in the entity file, and also matches the name of the "
              + "sub-directory of the config/datamapping/[Data-Mapping Group]/entity sub-directory in the "
              + "underlay sub-project resources (e.g. `condition`).\n\n"
              + "Using the path here instead of just the entity name allows us to share entity definitions "
              + "across underlays. For example, the `omop` data-mapping group contains template "
              + "entity definitions for standing up a new underlay.")
  public Set<String> entities;

  @AnnotatedField(
      name = "SZUnderlay.groupItemsEntityGroups",
      markdown =
          "List of paths of `group-items` type entity groups.\n\n"
              + "A `group-items` type entity group defines a relationship between two entities.\n\n"
              + "Path consists of two parts: [Data-Mapping Group]/[Entity Group Name] (e.g. `omop/brandIngredient`).\n\n"
              + "[Data-Mapping Group] is the name of a sub-directory of the config/datamapping/ sub-directory "
              + "in the underlay sub-project resources (e.g. `omop`).\n\n"
              + "[Entity Group Name] is specified in the entity group file, and also matches the name of the "
              + "sub-directory of the config/datamapping/[Data-Mapping Group]/entitygroup sub-directory in the "
              + "underlay sub-project resources (e.g. `brandIngredient`).\n\n"
              + "Using the path here instead of just the entity group name allows us to share entity group "
              + "definitions across underlays. For example, the `omop` data-mapping group contains "
              + "template entity group definitions for standing up a new underlay.")
  public Set<String> groupItemsEntityGroups;

  @AnnotatedField(
      name = "SZUnderlay.criteriaOccurrenceEntityGroups",
      markdown =
          "List of paths of `criteria-occurrence` type entity groups.\n\n"
              + "A `criteria-occurrence` type entity group defines a relationship between three entities.\n\n"
              + "Path consists of two parts: [Data-Mapping Group]/[Entity Group Name] (e.g. `omop/conditionPerson`).\n\n"
              + "[Data-Mapping Group] is the name of a sub-directory of the config/datamapping/ sub-directory "
              + "in the underlay sub-project resources (e.g. `omop`).\n\n"
              + "[Entity Group Name] is specified in the entity group file, and also matches the name of the "
              + "sub-directory of the config/datamapping/[Data-Mapping Group]/entitygroup sub-directory in the "
              + "underlay sub-project resources (e.g. `conditionPerson`).\n\n"
              + "Using the path here instead of just the entity group name allows us to share entity group "
              + "definitions across underlays. For example, the `omop` data-mapping group contains "
              + "template entity group definitions for standing up a new underlay.")
  public Set<String> criteriaOccurrenceEntityGroups;

  @AnnotatedField(
      name = "SZUnderlay.criteriaSelectors",
      markdown =
          "List of paths of all the criteria selectors.\n\n"
              + "A criteria selector is an option for defining a filter on an entity (e.g. select a condition). It "
              + "corresponds to one or more UI display plugins. (e.g. condition selector uses the entity group plugin "
              + "for selecting the condition, the attribute plugin for selecting the visit type modifier, and the "
              + "unhinted-value plugin for selecting the occurrence count modifier).\n\n"
              + "Path consists of two parts: [Display Group]/[Criteria Selector Name] (e.g. `omop/gender`).\n\n"
              + "[Display Group] is the name of a sub-directory of the config/display/ sub-directory "
              + "in the underlay sub-project resources (e.g. `omop`).\n\n"
              + "[Criteria Selector Name] is specified in the selector file, and also matches the name of the "
              + "sub-directory of the config/display/[Display Group]/criteriaselector sub-directory in the "
              + "underlay sub-project resources (e.g. `gender`).\n\n"
              + "Using the path here instead of just the selector name allows us to share selector definitions "
              + "across underlays. For example, the `omop` display group contains template "
              + "selector definitions for standing up a new underlay.")
  public List<String> criteriaSelectors;

  @AnnotatedField(
      name = "SZUnderlay.prepackagedDataFeatures",
      markdown =
          "List of paths of all the prepackaged data features.\n\n"
              + "A prepackaged data feature is a predefined data feature for exporting data (e.g. demographics). It "
              + "contains data for zero or more UI display plugins. (e.g. type 2 diabetes data feature defines data "
              + "for the entity group plugin).\n\n"
              + "Path consists of two parts: [Display Group]/[Prepackaged Data Feature Name] (e.g. "
              + "`omop/demographics`).\n\n"
              + "[Display Group] is the name of a sub-directory of the config/display/ sub-directory "
              + "in the underlay sub-project resources (e.g. `omop`).\n\n"
              + "[Prepackaged Data Feature Name] is specified in the prepackaged file, and also matches the name of the "
              + "sub-directory of the config/display/[Display Group]/prepackagedcriteria sub-directory in the "
              + "underlay sub-project resources (e.g. `demographics`).\n\n"
              + "Using the path here instead of just the prepackaged criteria name allows us to share criteria "
              + "definitions across underlays. For example, the `omop` display group contains template "
              + "criteria definitions for standing up a new underlay.")
  public Set<String> prepackagedDataFeatures;

  @AnnotatedField(
      name = "SZUnderlay.visualizations",
      markdown =
          "List of paths of all the visualizations.\n\n"
              + "A visualization contains all of the configuration to display a underlay or cohort level visualization "
              + "in the UI.\n\n"
              + "Path consists of two parts: [Display Group]/[Visualization Name] (e.g. `omop/peopleByAge`).\n\n"
              + "[Display Group] is the name of a sub-directory of the config/ui/ sub-directory "
              + "in the underlay sub-project resources (e.g. `omop`).\n\n"
              + "[Visualization Name] is specified in the visualization file, and also matches the name of the "
              + "sub-directory of the config/ui/[Display Group]/viz sub-directory in the underlay sub-project "
              + "resources (e.g. `peopleByAge`).\n\n"
              + "Using the path here instead of just the visualization name allows us to share visualization "
              + "definitions across underlays. For example, the `omop` visualization group contains template "
              + "visualization definitions for standing up a new underlay.")
  public List<String> visualizations;

  @AnnotatedField(name = "SZUnderlay.metadata", markdown = "Metadata for the underlay.")
  public Metadata metadata;

  // TODO: Merge UI config into backend config.
  @AnnotatedField(
      name = "SZUnderlay.uiConfigFile",
      markdown =
          "Name of the UI config file.\n\n"
              + "File must be in the same directory as the underlay file. Name includes file extension.",
      exampleValue = "ui.json")
  public String uiConfigFile;

  @AnnotatedClass(
      name = "SZMetadata",
      markdown =
          "Metadata for the underlay.\n\n"
              + "Information in this object is not used in the operation of the indexer or service, it is for "
              + "display purposes only.")
  public static class Metadata {
    @AnnotatedField(
        name = "SZMetadata.displayName",
        markdown =
            "Display name for the underlay.\n\n"
                + "Unlike the underlay [name](${SZUnderlay.name}), it may include spaces and special characters.")
    public String displayName;

    @AnnotatedField(
        name = "SZMetadata.description",
        markdown = "Description of the underlay.",
        optional = true)
    public String description;

    // TODO: Pass these to the access control and export implementation classes.
    @AnnotatedField(
        name = "SZMetadata.properties",
        markdown =
            "Key-value map of underlay properties.\n\n"
                + "Keys may not include spaces or special characters, only letters and numbers.",
        optional = true)
    public Map<String, String> properties;
  }
}
