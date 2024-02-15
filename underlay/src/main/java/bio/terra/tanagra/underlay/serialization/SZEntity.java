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
  public List<Attribute> attributes;

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
  public Set<Hierarchy> hierarchies;

  @AnnotatedField(
      name = "SZEntity.textSearch",
      markdown =
          "Text search configuration.\n\n"
              + "This is used when filtering a list of instances of this entity (e.g. list of conditions) by "
              + "text. If unset, filtering by text is unsupported.",
      optional = true)
  public TextSearch textSearch;

  @AnnotatedClass(
      name = "SZAttribute",
      markdown =
          "Attribute or property of an entity.\n\n"
              + "Define an attribute for each column you want to display (e.g. `condition.vocabulary_id`) "
              + "or filter on (e.g. `conditionOccurrence.person_id`).")
  public static class Attribute {
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
    public DataType dataType;

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
    public DataType runtimeDataType;

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
  }

  @AnnotatedClass(name = "SZHierarchy", markdown = "Hierarchy for an entity.")
  public static class Hierarchy {
    @AnnotatedField(
        name = "SZHierarchy.name",
        markdown =
            "Name of the hierarchy.\n\n"
                + "This is the unique identifier for the hierarchy. In a single entity, the hierarchy names cannot overlap. "
                + "Name may not include spaces or special characters, only letters and numbers. "
                + "The first character must be a letter.\n\n"
                + "If there is only one hierarchy, the name is optional and, if unspecified, will be set to `default`. "
                + "If there are multiple hierarchies, the name is required for each one.",
        optional = true,
        defaultValue = "default")
    public String name;

    @AnnotatedField(
        name = "SZHierarchy.childParentIdPairsSqlFile",
        markdown =
            "Name of the child parent id pairs SQL file.\n\n"
                + "File must be in the same directory as the entity file. Name includes file extension.\n\n"
                + "There can be other columns selected in the SQL file (e.g. `SELECT * FROM relationships`), "
                + "but the child and parent ids are required.",
        exampleValue = "childParent.sql")
    public String childParentIdPairsSqlFile;

    @AnnotatedField(
        name = "SZHierarchy.childIdFieldName",
        markdown =
            "Name of the field or column name in the [child parent id pairs SQL](${SZHierarchy.childParentIdPairsSqlFile}) "
                + "that maps to the child id.",
        exampleValue = "child")
    public String childIdFieldName;

    @AnnotatedField(
        name = "SZHierarchy.parentIdFieldName",
        markdown =
            "Name of the field or column name in the [child parent id pairs SQL](${SZHierarchy.childParentIdPairsSqlFile}) "
                + "that maps to the parent id.",
        exampleValue = "parent")
    public String parentIdFieldName;

    @AnnotatedField(
        name = "SZHierarchy.rootNodeIds",
        markdown =
            "Set of root ids. Indexing jobs will filter out any hierarchy root nodes that are not in this set. "
                + "If the [root node ids SQL](${SZHierarchy.rootNodeIdsSqlFile}) is defined, then this property "
                + "must be unset.",
        optional = true)
    public Set<Long> rootNodeIds;

    @AnnotatedField(
        name = "SZHierarchy.rootNodeIdsSqlFile",
        markdown =
            "Name of the root id SQL file. File must be in the same directory as the entity file. Name includes file extension.\n\n"
                + "There can be other columns selected in the SQL file (e.g. `SELECT * FROM roots`), but the root id is required. "
                + "Indexing jobs will filter out any hierarchy root nodes that are not returned by this query. "
                + "If the [root node ids set](${SZHierarchy.rootNodeIds}) is defined, then this property must be unset.",
        optional = true,
        exampleValue = "rootNode.sql")
    public String rootNodeIdsSqlFile;

    @AnnotatedField(
        name = "SZHierarchy.rootIdFieldName",
        markdown =
            "Name of the field or column name that maps to the root id.\n\n"
                + "If the [root node ids SQL](${SZHierarchy.rootNodeIdsSqlFile}) is defined, then this property is required. "
                + "If the [root node ids set](${SZHierarchy.rootNodeIds}) is defined, then this property must be unset.",
        optional = true,
        exampleValue = "root_id")
    public String rootIdFieldName;

    @AnnotatedField(
        name = "SZHierarchy.maxDepth",
        markdown =
            "Maximum depth of the hierarchy. If there are branches of the hierarchy that are deeper "
                + "than the number specified here, they will be truncated.")
    public int maxDepth;

    @AnnotatedField(
        name = "SZHierarchy.keepOrphanNodes",
        markdown =
            "An orphan node has no parents or children. "
                + "When false, indexing jobs will filter out orphan nodes. "
                + "When true, indexing jobs skip this filtering step and we keep the orphan nodes in the hierarchy.",
        optional = true,
        defaultValue = "false")
    public boolean keepOrphanNodes;

    @AnnotatedField(
        name = "SZHierarchy.cleanHierarchyNodesWithZeroCounts",
        markdown =
            "When false, indexing jobs will not clean hierarchy nodes with both a zero item and rollup counts. "
                + "When true, indexing jobs will clean hierarchy nodes with both a zero item and rollup counts.",
        optional = true,
        defaultValue = "false")
    public boolean cleanHierarchyNodesWithZeroCounts;
  }

  @AnnotatedClass(name = "SZTextSearch", markdown = "Text search configuration for an entity.")
  public static class TextSearch {

    @AnnotatedField(
        name = "SZTextSearch.attributes",
        markdown =
            "Set of attributes to allow text search on. Text search on attributes not included here is unsupported.",
        optional = true)
    public Set<String> attributes;

    @AnnotatedField(
        name = "SZTextSearch.idTextPairsSqlFile",
        markdown =
            "Name of the id text pairs SQL file. "
                + "File must be in the same directory as the entity file. Name includes file extension.\n\n"
                + "There can be other columns selected in the SQL file (e.g. `SELECT * FROM synonyms`), "
                + "but the entity id and text string is required. The SQL query may return multiple rows per entity id.",
        optional = true,
        exampleValue = "textSearch.sql")
    public String idTextPairsSqlFile;

    @AnnotatedField(
        name = "SZTextSearch.idFieldName",
        markdown =
            "Name of the field or column name that maps to the entity id. "
                + "If the [id text pairs SQL](${SZTextSearch.idTextPairsSqlFile}) is defined, then this property is required.",
        optional = true,
        exampleValue = "id")
    public String idFieldName;

    @AnnotatedField(
        name = "SZTextSearch.textFieldName",
        markdown =
            "Name of the field or column name that maps to the text search string. "
                + "If the [id text pairs SQL](${SZTextSearch.idTextPairsSqlFile}) is defined, then this property is required.",
        optional = true,
        exampleValue = "text")
    public String textFieldName;
  }

  @AnnotatedClass(
      name = "SZDataType",
      markdown =
          "Supported data types. Each type corresponds to one or more data types in the underlying database.")
  public enum DataType {
    @AnnotatedField(name = "SZDataType.INT64", markdown = "Maps to BigQuery `INTEGER` data type.")
    INT64,

    @AnnotatedField(name = "SZDataType.STRING", markdown = "Maps to BigQuery `STRING` data type.")
    STRING,

    @AnnotatedField(name = "SZDataType.BOOLEAN", markdown = "Maps to BigQuery `BOOLEAN` data type.")
    BOOLEAN,

    @AnnotatedField(name = "SZDataType.DATE", markdown = "Maps to BigQuery `DATE` data type.")
    DATE,

    @AnnotatedField(
        name = "SZDataType.DOUBLE",
        markdown = "Maps to BigQuery `NUMERIC` and `FLOAT` data types.")
    DOUBLE,

    @AnnotatedField(
        name = "SZDataType.TIMESTAMP",
        markdown = "Maps to BigQuery `TIMESTAMP` data type.")
    TIMESTAMP
  }
}
