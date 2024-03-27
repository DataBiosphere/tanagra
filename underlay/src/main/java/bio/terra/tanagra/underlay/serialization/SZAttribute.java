package bio.terra.tanagra.underlay.serialization;

import bio.terra.tanagra.annotation.AnnotatedClass;
import bio.terra.tanagra.annotation.AnnotatedField;

@AnnotatedClass(
    name = "SZAttribute",
    markdown =
        "Attribute or property of an entity.\n\n"
            + "Define an attribute for each column you want to display (e.g. `condition.vocabulary_id`) "
            + "or filter on (e.g. `conditionOccurrence.person_id`).")
public class SZAttribute {
  @AnnotatedField(
      name = "SZAttribute.name",
      markdown =
          "Name of the attribute.\n\n"
              + "This is the unique identifier for the attribute. In a single entity, the attribute names "
              + "cannot overlap.\n\n"
              + "Name may not include spaces or special characters, only letters and numbers. The first "
              + "character must be a letter.")
  public String name;

  @AnnotatedField(name = "SZAttribute.dataType", markdown = "Data type of the attribute.")
  public SZDataType dataType;

  @AnnotatedField(
      name = "SZAttribute.valueFieldName",
      markdown =
          "Field or column name in the [all instances SQL file](${SZEntity.allInstancesSqlFile}) that "
              + "maps to the value of this attribute. If unset, we assume the field name is the same "
              + "as the attribute name.",
      optional = true)
  public String valueFieldName;

  @AnnotatedField(
      name = "SZAttribute.displayFieldName",
      markdown =
          "Field or column name in the [all instances SQL file](${SZEntity.allInstancesSqlFile}) that "
              + "maps to the display string of this attribute. If unset, we assume the attribute has only "
              + "a value, no separate display.\n\n"
              + "A separate display field is useful for enum-type attributes, which often use a foreign-key "
              + "to another table to get a readable string from a code (e.g. in OMOP, `person.gender_concept_id` "
              + "and `concept.concept_name`).",
      optional = true)
  public String displayFieldName;

  @AnnotatedField(
      name = "SZAttribute.runtimeSqlFunctionWrapper",
      markdown =
          "SQL function to apply at runtime (i.e. when running the query), instead of at indexing time. "
              + "Useful for attributes we expect to be updated dynamically (e.g. a person's age).\n\n"
              + "For a simple function call that just wraps the column (e.g. `UPPER(column)`), "
              + "you can specify just the function name (e.g. `UPPER`). For a more complicated "
              + "function call, put `${fieldSql}` where the column name should be substituted "
              + "(e.g. `CAST(FLOOR(TIMESTAMP_DIFF(CURRENT_TIMESTAMP(), ${fieldSql}, DAY) / 365.25) AS INT64)`).",
      optional = true)
  public String runtimeSqlFunctionWrapper;

  @AnnotatedField(
      name = "SZAttribute.runtimeDataType",
      markdown =
          "Data type of the attribute at runtime.\n\n"
              + "If the [runtime SQL wrapper](${SZAttribute.runtimeSqlFunctionWrapper}) is set, this field must also be set. "
              + "The data type at runtime may be different from the data type at rest when the column is "
              + "passed to a function at runtime. Otherwise, the data type at runtime will always match the "
              + "attribute [data type](${SZDataType}), so no need to specify it again here.",
      optional = true)
  public SZDataType runtimeDataType;

  @AnnotatedField(
      name = "SZAttribute.isComputeDisplayHint",
      markdown =
          "When set to true, an indexing job will try to compute a display hint for this attribute "
              + "(e.g. set of enum values and counts, range of numeric values). Not all data types are supported by "
              + "the indexing job, yet.",
      optional = true,
      defaultValue = "false")
  public boolean isComputeDisplayHint;

  @AnnotatedField(
      name = "SZAttribute.displayHintRangeMin",
      markdown =
          "The minimum value to display when filtering on this attribute. This is useful when the underlying "
              + "data has outliers that we want to exclude from the display, but not from the available data.\n\n"
              + "e.g. A person has an invalid date of birth that produces an age range that spans negative "
              + "numbers. This causes the slider when filtering by age to span negative numbers also. Setting this "
              + "property sets the left end of the slider. It does not remove the person with the invalid date of "
              + "birth from the table. So if they have asthma, they would still show up in a cohort filtering on "
              + "this condition.\n\n"
              + "The ${SZAttribute.displayHintRangeMax} may be set as well, but they are not required to be set "
              + "together. The ${SZAttribute.isComputeDisplayHint} is also independent of this property. You can "
              + "still calculate the actual minimum in the data, if you set this property.",
      optional = true)
  public Double displayHintRangeMin;

  @AnnotatedField(
      name = "SZAttribute.displayHintRangeMax",
      markdown =
          "The maximum value to display when filtering on this attribute. This is useful when the underlying "
              + "data has outliers that we want to exclude from the display, but not from the available data.\n\n"
              + "e.g. A person has an invalid date of birth that produces an age range that spans very large "
              + "numbers. This causes the slider when filtering by age to span very large numbers also. Setting this "
              + "property sets the right end of the slider. It does not remove the person with the invalid date of "
              + "birth from the table. So if they have asthma, they would still show up in a cohort filtering on "
              + "this condition.\n\n"
              + "The ${SZAttribute.displayHintRangeMin} may be set as well, but they are not required to be set "
              + "together. The ${SZAttribute.isComputeDisplayHint} is also independent of this property. You can "
              + "still calculate the actual maximum in the data, if you set this property.",
      optional = true)
  public Double displayHintRangeMax;

  @AnnotatedField(
      name = "SZAttribute.sourceQuery",
      markdown =
          "How to generate a query against the source data that includes this attribute.\n\n"
              + "If unspecified and exporting queries against the source data is supported for this entity is enabled "
              + "(i.e. ${SZEntity.sourceQueryTableName} is specified), we assume the field name in the source table "
              + "(${SZEntity.sourceQueryTableName}) corresponding to this attribute is the same as the "
              + "${SZAttribute.valueFieldName}.",
      optional = true)
  public SourceQuery sourceQuery;

  @AnnotatedClass(
      name = "SZSourceQuery",
      markdown =
          "Information to generate a SQL query against the source dataset for a given attribute.\n\n"
              + "This query isn't actually run by the service, only generated as an export option "
              + "(e.g. as part of a notebook file).")
  public static class SourceQuery {
    @AnnotatedField(
        name = "SZSourceQuery.valueFieldName",
        markdown =
            "Name of the field to use for the attribute value in the source dataset table "
                + "(${SZEntity.sourceQueryTableName}).\n\n"
                + "If unspecified, we assume the field name in the source table (${SZEntity.sourceQueryTableName}) "
                + "corresponding to this attribute is the same as the ${SZAttribute.valueFieldName}.",
        exampleValue = "condition_concept_id",
        optional = true)
    public String valueFieldName;

    @AnnotatedField(
        name = "SZSourceQuery.displayFieldName",
        markdown =
            "Name of the field to use for the attribute display in the source dataset.\n\n"
                + "If unspecified, exporting a query with this attribute against the source data will not include "
                + "a separate display field.\n\n"
                + "The table can optionally be specified in ${SZSourceQuery.displayFieldTable}.",
        exampleValue = "concept_name",
        optional = true)
    public String displayFieldName;

    @AnnotatedField(
        name = "SZSourceQuery.displayFieldTable",
        markdown =
            "Full name of the table to JOIN with the main table (${SZEntity.sourceQueryTableName}) to get the attribute "
                + "display field in the source dataset.\n\n"
                + "SQL substitutions are supported in this table name.\n\n"
                + "If unspecified, and ${SZSourceQuery.displayFieldName} is specified, then we assume that the source "
                + "display field is also in the main table, same as the source value field.\n\n"
                + "The ${SZSourceQuery.displayFieldTableJoinFieldName} is required if this property is specified.",
        exampleValue = "${omopDataset}.concept",
        optional = true)
    public String displayFieldTable;

    @AnnotatedField(
        name = "SZSourceQuery.displayFieldTableJoinFieldName",
        markdown =
            "Name of the field in the display table (${SZSourceQuery.displayFieldTable}) that is used to "
                + "JOIN to the main table (${SZEntity.sourceQueryTableName}) using the source value field "
                + "(${SZSourceQuery.valueFieldName}).\n\n"
                + "This is required if the ${SZSourceQuery.displayFieldTable} is specified.",
        exampleValue = "concept_id",
        optional = true)
    public String displayFieldTableJoinFieldName;
  }
}
