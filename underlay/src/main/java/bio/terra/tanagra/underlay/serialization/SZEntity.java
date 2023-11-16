package bio.terra.tanagra.underlay.serialization;

import java.util.List;
import java.util.Set;

/**
 * Entity configuration.
 *
 * <p>Define a version of this file for each entity.
 */
public class SZEntity {
  /**
   * Name of the entity.
   *
   * <p>This is the unique identifier for the entity. In a single underlay, the entity names cannot
   * overlap.
   *
   * <p>Name may not include spaces or special characters, only letters and numbers. The first
   * character must be a letter.
   */
  public String name;

  /**
   * Display name for the entity.
   *
   * <p>Unlike the entity {@link bio.terra.tanagra.underlay.serialization.SZEntity#name}, it may
   * include spaces and special characters.
   */
  public String displayName;

  /** <strong>(optional)</strong> Description of the entity. */
  public String description;

  /**
   * Name of the all instances SQL file.
   *
   * <p>File must be in the same directory as the entity file. Name includes file extension (e.g.
   * all.sql).
   */
  public String allInstancesSqlFile;

  /**
   * List of all the entity attributes.
   *
   * <p>The generated index table will preserve the order of the attributes as defined here.
   *
   * <p>The list must include the id attribute.
   */
  public List<Attribute> attributes;

  /**
   * Name of the id attribute.
   *
   * <p>This must be a unique identifier for each entity instance. It must also have the <code>INT64
   * </code> ${@link bio.terra.tanagra.underlay.serialization.SZEntity.DataType}.
   */
  public String idAttribute;

  /**
   * List of attributes to optimize for group by queries.
   *
   * <p>The typical use case for this is to optimize cohort breakdown queries on the primary entity.
   * For example, to optimize breakdowns by age, race, gender, specify those attributes here. Order
   * matters.
   *
   * <p>You can currently specify a maximum of four attributes, because we implement this using
   * BigQuery clustering which has this <a
   * href="https://cloud.google.com/bigquery/docs/clustered-tables#limitations">limitation</a>.
   */
  public List<String> optimizeGroupByAttributes;

  /**
   * <strong>(optional)</strong> List of hierarchies.
   *
   * <p>While the code supports multiple hierarchies, we currently only have examples with zero or
   * one hierarchy.
   */
  public Set<Hierarchy> hierarchies;

  /**
   * <strong>(optional)</strong> Text search configuration.
   *
   * <p>This is used when filtering a list of instances of this entity (e.g. list of conditions) by
   * text. If unset, filtering by text is unsupported.
   */
  public TextSearch textSearch;

  /**
   * Attribute or property of an entity.
   *
   * <p>Define an attribute for each column you want to display (e.g. <code>condition.vocabulary_id
   * </code>) or filter on (e.g. <code>conditionOccurrence.person_id</code>).
   */
  public static class Attribute {
    /**
     * Name of the attribute.
     *
     * <p>This is the unique identifier for the attribute. In a single entity, the attribute names
     * cannot overlap.
     *
     * <p>Name may not include spaces or special characters, only letters and numbers. The first
     * character must be a letter.
     */
    public String name;

    /** Data type. */
    public DataType dataType;

    /**
     * <strong>(optional)</strong> Field or column name in the {@link
     * bio.terra.tanagra.underlay.serialization.SZEntity#allInstancesSqlFile} that maps to the value
     * of this attribute.
     *
     * <p>If unset, we assume the field name is the same as the attribute name.
     */
    public String valueFieldName;

    /**
     * <strong>(optional)</strong> Field or column name in the {@link
     * bio.terra.tanagra.underlay.serialization.SZEntity#allInstancesSqlFile} that maps to the
     * display string of this attribute.
     *
     * <p>A separate display field is useful for enum-type attributes, which often use a foreign-key
     * to another table to get a readable string from a code (e.g. in OMOP, <code>
     * person.gender_concept_id</code> and <code>concept.concept_name</code>).
     *
     * <p>If unset, we assume the attribute has only a value, no separate display.
     */
    public String displayFieldName;

    /**
     * <strong>(optional)</strong> SQL function to apply at runtime (i.e. when running the query),
     * instead of at indexing time.
     *
     * <p>Useful for attributes we expect to be updated dynamically (e.g. a person's age).
     *
     * <p>For a simple function call that just wraps the column (e.g. <code>UPPER(column)</code>),
     * you can specify just the function name (e.g. <code>UPPER</code>). For a more complicated
     * function call, put <code>${fieldSql}</code> where the column name should be substituted (e.g.
     * <code>CAST(FLOOR(TIMESTAMP_DIFF(CURRENT_TIMESTAMP(), ${fieldSql}, DAY) / 365.25) AS INT64)
     * </code>).
     */
    public String runtimeSqlFunctionWrapper;

    /**
     * <strong>(optional)</strong> Data type of the attribute at runtime.
     *
     * <p>If {@link #runtimeSqlFunctionWrapper} is set, this field must also be set.
     *
     * <p>The data type at runtime may be different from the data type at rest when the column is
     * passed to a function at runtime. Otherwise, the data type at runtime will always match the
     * {@link #dataType}, so no need to specify it again here.
     */
    public DataType runtimeDataType;

    /**
     * <strong>(optional)</strong> Whether to compute a display hint for the attribute.
     *
     * <p>If set to true, an indexing job will try to create a display hint for this attribute (e.g.
     * set of enum values and counts, range of numeric values). Not all data types are supported by
     * the indexing job, yet. Default is false, no hint.
     */
    public boolean isComputeDisplayHint;
  }

  /** Hierarchy for an entity.> */
  public static class Hierarchy {
    /**
     * <strong>(optional)</strong> Name of the hierarchy.
     *
     * <p>This is the unique identifier for the hierarchy. In a single entity, the hierarchy names
     * cannot overlap.
     *
     * <p>Name may not include spaces or special characters, only letters and numbers. The first
     * character must be a letter.
     *
     * <p>If there is only one hierarchy, the name is optional and, if unspecified, will be set to
     * "default".
     */
    public String name;

    /**
     * Name of the child parent id pairs SQL file.
     *
     * <p>File must be in the same directory as the entity file. Name includes file extension (e.g.
     * childParent.sql).
     *
     * <p>There can be other columns selected in the SQL file (e.g. <code>
     * SELECT * FROM relationships</code>), but the child and parent ids are required.
     */
    public String childParentIdPairsSqlFile;

    /** Name of the field or column name that maps to the child id. */
    public String childIdFieldName;

    /** Name of the field or column name that maps to the parent id. */
    public String parentIdFieldName;

    /**
     * <strong>(optional)</strong> Set of root ids.
     *
     * <p>Indexing jobs will filter out any hierarchy root nodes that are not in this set.
     *
     * <p>If the {@link #rootNodeIdsSqlFile} property is defined, then this property must be unset.
     */
    public Set<Long> rootNodeIds;

    /**
     * <strong>(optional)</strong> Name of the root id SQL file.
     *
     * <p>File must be in the same directory as the entity file. Name includes file extension (e.g.
     * rootNode.sql). There can be other columns selected in the SQL file (e.g. <code>
     * SELECT * FROM roots</code>), but the root id is required.
     *
     * <p>Indexing jobs will filter out any hierarchy root nodes that are not returned by this
     * query.
     *
     * <p>If the {@link #rootNodeIds} property is defined, then this property must be unset.
     */
    public String rootNodeIdsSqlFile;

    /**
     * <strong>(optional)</strong> Name of the field or column name that maps to the root id.
     *
     * <p>If the {@link #rootNodeIdsSqlFile} property is defined, then this property is required.
     *
     * <p>If the {@link #rootNodeIds} property is defined, then this property must be unset.
     */
    public String rootIdFieldName;

    /**
     * Maximum depth of the hierarchy.
     *
     * <p>If there are branches of the hierarchy that are deeper than the number specified here,
     * they will be truncated.
     */
    public int maxDepth;

    /**
     * <strong>(optional)</strong> Whether to keep orphan nodes in the hierarchy.
     *
     * <p>An orphan node has no parents or children. Default is false, indexing jobs will filter out
     * orphan nodes. If set to true, we will skip this filtering step.
     */
    public boolean keepOrphanNodes;
  }

  /** Text search instructions for an entity. */
  public static class TextSearch {

    /**
     * <strong>(optional)</strong> Set of attributes to allow text search on.
     *
     * <p>Text search on attributes not included here is unsupported.
     */
    public Set<String> attributes;

    /**
     * <strong>(optional)</strong> Name of the id text pairs SQL file.
     *
     * <p>File must be in the same directory as the entity file. Name includes file extension (e.g.
     * textSearch.sql). There can be other columns selected in the SQL file (e.g. <code>
     * SELECT * FROM synonyms</code>), but the entity id and text string is required. The SQL query
     * may return multiple rows per entity id.
     */
    public String idTextPairsSqlFile;

    /**
     * <strong>(optional)</strong> Name of the field or column name that maps to the entity id.
     *
     * <p>If the {@link #idTextPairsSqlFile} property is defined, then this property is required.
     */
    public String idFieldName;

    /**
     * <strong>(optional)</strong> Name of the field or column name that maps to the text search
     * string.
     *
     * <p>If the {@link #idTextPairsSqlFile} property is defined, then this property is required.
     */
    public String textFieldName;
  }

  /**
   * Set of supported data types.
   *
   * <p>Each type corresponds to one or more data types in the underlying database.
   */
  public enum DataType {
    /** Maps to BigQuery <code>INTEGER</code> data type. */
    INT64,

    /** Maps to BigQuery <code>STRING</code> data type. */
    STRING,

    /** Maps to BigQuery <code>BOOLEAN</code> data type. */
    BOOLEAN,

    /** Maps to BigQuery <code>DATE</code> data type. */
    DATE,

    /** Maps to BigQuery <code>NUMERIC</code> and <code>FLOAT</code> data types. */
    DOUBLE,

    /** Maps to BigQuery <code>TIMESTAMP</code> data type. */
    TIMESTAMP
  }
}
