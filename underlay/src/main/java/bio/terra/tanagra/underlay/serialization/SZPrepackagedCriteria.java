package bio.terra.tanagra.underlay.serialization;

import bio.terra.tanagra.annotation.AnnotatedClass;
import bio.terra.tanagra.annotation.AnnotatedField;
import java.util.List;

@AnnotatedClass(name = "SZPrepackagedCriteria", markdown = "Prepackaged criteria configuration.")
public class SZPrepackagedCriteria {
  @AnnotatedField(
      name = "SZPrepackagedCriteria.name",
      markdown =
          "Name of the prepackaged criteria.\n\n"
              + "This is the unique identifier for the criteria. The criteria names cannot overlap within "
              + "an underlay.\n\n"
              + "Name may not include spaces or special characters, only letters and numbers.\n\n"
              + "This name is stored in the application database for data feature sets, so once there are "
              + "artifacts associated with a prepackaged criteria, you can't change the criteria name.")
  public String name;

  @AnnotatedField(name = "SZPrepackagedCriteria.displayName", markdown = "Display name.")
  public String displayName;

  @AnnotatedField(
      name = "SZPrepackagedCriteria.criteriaSelector",
      markdown =
          "Name of the criteria selector this criteria is associated with.\n\n"
              + "The criteria selector must be defined for the underlay. (e.g. The condition selector must be "
              + "defined in order to define a prepackaged data feature for condition = Type 2 Diabetes.)")
  public String criteriaSelector;

  @AnnotatedField(
      name = "SZPrepackagedCriteria.selectionData",
      markdown = "List of selection data.")
  public List<SelectionData> selectionData;

  @AnnotatedClass(
      name = "SZSelectionData",
      markdown = "Prepackaged criteria selection data, one per UI display plugin.")
  public static class SelectionData {
    @AnnotatedField(
        name = "SZSelectionData.modifierName",
        markdown =
            "Name of the modifier (e.g. age_at_occurrence, visit_type).\n\n"
                + "This name is stored in the application database, so once there are cohorts or data features "
                + "that use this prepackaged criteria, you can't change the modifier names.\n\n"
                + "This property is ignored for the first selection data, which is the primary selection.",
        optional = true)
    public String modifierName;

    @AnnotatedField(
        name = "SZSelectionData.pluginData",
        markdown = "Serialized data for the UI display plugin e.g. \"{\"conceptId\":\"201826\"}\".")
    public String pluginData;

    @AnnotatedField(
        name = "SZSelectionData.pluginDataFile",
        markdown =
            "Name of the file that contains the serialized data for the UI display plugin.\n\n"
                + "This file should be in the same directory as the prepackaged criteria (e.g. `condition.json`).\n\n"
                + "If this property is specified, the value of the `pluginData` property is ignored.")
    public String pluginDataFile;
  }
}
