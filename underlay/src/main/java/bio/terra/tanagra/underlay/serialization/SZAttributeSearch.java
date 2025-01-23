package bio.terra.tanagra.underlay.serialization;

import bio.terra.tanagra.annotation.AnnotatedClass;
import bio.terra.tanagra.annotation.AnnotatedField;
import java.util.List;

@AnnotatedClass(
    name = "SZAttributeSearch",
    markdown =
        "Configuration to optimize entity search by attributes.\n\n"
            + "Define the list of attributes to group together for optimization "
            + "and specific is search for null attribute values is supported. ")
public class SZAttributeSearch {
  @AnnotatedField(
      name = "SZAttributeSearch.attributes",
      markdown =
          "List of attributes grouped together for search optimization.\n\n"
              + " Order matter. Each entry is a list of attributes that are search for together. "
              + "For example search is typically performed for contig and position together. ")
  public List<String> attributes;

  @AnnotatedField(
      name = "SZAttributeSearch.includeNullValues",
      markdown = "True if search for null values in attributes is supported. ",
      optional = true,
      defaultValue = "false")
  public boolean includeNullValues;

  @AnnotatedField(
      name = "SZAttributeSearch.includeEntityMainColumns",
      markdown =
          "Whether all columns in the entity main table should also be included "
              + "in this search table. Improves performance if other attributes are also fetched "
              + "when performing this search by attributes.",
      optional = true,
      defaultValue = "false")
  public boolean includeEntityMainColumns;
}
