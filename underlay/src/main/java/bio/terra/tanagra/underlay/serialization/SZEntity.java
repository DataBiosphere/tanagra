package bio.terra.tanagra.underlay.serialization;

import bio.terra.tanagra.annotation.AnnotatedClass;
import bio.terra.tanagra.annotation.AnnotatedField;
import java.util.List;
import java.util.Set;

@AnnotatedClass(
    name = "SZEntity",
    markdown = "Entity configuration.\n\n" + "Define a version of this file for each entity.")
public class SZEntity {
  @AnnotatedField(
      name = "SZEntity.name",
      markdown =
          "Name of the entity.\n\n"
              + "This is the unique identifier for the entity. In a single underlay, the entity names cannot overlap.\n\n"
              + "Name may not include spaces or special characters, only letters and numbers. The first character must be a letter.")
  public String name;

  @AnnotatedField(
      name = "SZEntity.displayName",
      markdown =
          "Display name for the entity.\n\n"
              + "Unlike the entity [name](${SZEntity.name}), it may include spaces and special characters.",
      optional = true)
  public String displayName;

  @AnnotatedField(
      name = "SZEntity.description",
      markdown = "Description of the entity.",
      optional = true)
  public String description;

  @AnnotatedField(
      name = "SZEntity.allInstancesSqlFile",
      markdown =
          "Name of the all instances SQL file.\n\n"
              + "File must be in the same directory as the entity file. Name includes file extension.",
      exampleValue = "all.sql")
  public String allInstancesSqlFile;

  @AnnotatedField(
      name = "SZEntity.attributes",
      markdown =
          "List of all the entity attributes.\n\n"
              + "The generated index table will preserve the order of the attributes as defined here. "
              + "The list must include the id attribute.")
  public List<SZAttribute> attributes;

  @AnnotatedField(
      name = "SZEntity.idAttribute",
      markdown =
          "Name of the id attribute.\n\n"
              + "This must be a unique identifier for each entity instance. It must also have the `INT64` "
              + "[data type](${SZDataType}).")
  public String idAttribute;

  @AnnotatedField(
      name = "SZEntity.optimizeGroupByAttributes",
      markdown =
          "List of attributes to optimize for group by queries.\n\n"
              + "The typical use case for this is to optimize cohort breakdown queries on the primary entity. "
              + "For example, to optimize breakdowns by age, race, gender, specify those attributes here. Order matters.\n\n"
              + "You can currently specify a maximum of four attributes, because we implement this using "
              + "BigQuery clustering which has this [limitation](https://cloud.google.com/bigquery/docs/clustered-tables#limitations).",
      optional = true)
  public List<String> optimizeGroupByAttributes;

  @AnnotatedField(
      name = "SZEntity.hierarchies",
      markdown =
          "List of hierarchies.\n\n"
              + "While the code supports multiple hierarchies, we currently only have examples with zero or one hierarchy.",
      optional = true)
  public Set<SZHierarchy> hierarchies;

  @AnnotatedField(
      name = "SZEntity.textSearch",
      markdown =
          "Text search configuration.\n\n"
              + "This is used when filtering a list of instances of this entity (e.g. list of conditions) by "
              + "text. If unset, filtering by text is unsupported.",
      optional = true)
  public SZTextSearch textSearch;

  @AnnotatedField(
      name = "SZEntity.sourceQueryTableName",
      markdown =
          "Full name of the table to use when exporting a query against the source data.\n\n"
              + "SQL substitutions are supported in this table name.\n\n"
              + "If unspecified, exporting a query against the source data is unsupported.",
      exampleValue = "${omopDataset}.condition_occurrence",
      optional = true)
  public String sourceQueryTableName;
}
