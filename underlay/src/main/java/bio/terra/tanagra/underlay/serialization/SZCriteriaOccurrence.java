package bio.terra.tanagra.underlay.serialization;

import java.util.Set;

/**
 * Criteria-Occurrence entity group configuration.
 *
 * <p>Define a version of this file for each entity group of this type.
 *
 * <p>This entity group type defines a relationship between three entities. For each criteria entity
 * instance and primary entity instance, there are one or more occurrence entity instances.
 */
public class SZCriteriaOccurrence {
  /**
   * Name of the entity group.
   *
   * <p>This is the unique identifier for the entity group. In a single underlay, the entity group
   * names of any group type cannot overlap.
   *
   * <p>Name may not include spaces or special characters, only letters and numbers. The first
   * character must be a letter.
   */
  public String name;

  /** Name of the criteria entity. */
  public String criteriaEntity;

  /**
   * Set of occurrence entity configurations.
   *
   * <p>Most entity groups of this type will have a single occurrence entity (e.g. SNOMED condition
   * code only maps to condition occurrences), but we also support the case of multiple (e.g.
   * ICD9-CM condition code maps to condition, measurement, observation and procedure occurrences).
   */
  public Set<OccurrenceEntity> occurrenceEntities;

  /** Relationship or join between the primary and criteria entities. */
  // TODO: Remove this. We should be able to infer this relationship from the criteria-occurrence
  // and occurrence-primary relationships.
  public PrimaryCriteriaRelationship primaryCriteriaRelationship;

  /** Occurrence entity configuration. */
  public static class OccurrenceEntity {
    /** Name of occurrence entity. */
    @SuppressWarnings("PMD.AvoidFieldNameMatchingTypeName")
    public String occurrenceEntity;

    /**
     * Relationship or join between this occurrence entity and the criteria entity (e.g. condition
     * occurrence and ICD9-CM).
     */
    public CriteriaRelationship criteriaRelationship;

    /**
     * Relationship or join between this occurrence entity and the primary entity (e.g. condition
     * occurrence and person).
     */
    public PrimaryRelationship primaryRelationship;

    /**
     * Names of attributes that we want to calculate instance-level hints for.
     *
     * <p>Instance-level hints are ranges of possible values for a particular criteria instance.
     * They are used to support criteria-specific modifiers (e.g. range of values for measurement
     * code "glucose test").
     */
    public Set<String> attributesWithInstanceLevelHints;

    /**
     * Relationship or join between an occurrence entity and the criteria entity (e.g. condition
     * occurrence and ICD9-CM).
     */
    public static class CriteriaRelationship {
      /**
       * <strong>(optional)</strong> Attribute of the occurrence entity that is a foreign key to the
       * id attribute of the criteria entity.
       *
       * <p>If this property is set, then the {@link #idPairsSqlFile} must be unset.
       */
      public String foreignKeyAttributeOccurrenceEntity;

      /**
       * <strong>(optional)</strong> Name of the occurrence entity - criteria entity id pairs SQL
       * file.
       *
       * <p>File must be in the same directory as the entity group file. Name includes file
       * extension (e.g. occurrenceCriteria.sql).
       *
       * <p>There can be other columns selected in the SQL file (e.g. <code>
       * SELECT * FROM relationships</code>), but the occurrence and criteria entity ids are
       * required.
       *
       * <p>If this property is set, then the {@link #foreignKeyAttributeOccurrenceEntity} must be
       * unset.
       */
      public String idPairsSqlFile;

      /**
       * <strong>(optional)</strong> Name of the field or column name that maps to the occurrence
       * entity id.
       *
       * <p>If the {@link #idPairsSqlFile} property is defined, then this property is required.
       */
      public String occurrenceEntityIdFieldName;

      /**
       * <strong>(optional)</strong> Name of the field or column name that maps to the criteria
       * entity id.
       *
       * <p>If the {@link #idPairsSqlFile} property is defined, then this property is required.
       */
      public String criteriaEntityIdFieldName;
    }

    /**
     * Relationship or join between an occurrence entity and the primary entity (e.g. condition
     * occurrence and person).
     */
    public static class PrimaryRelationship {
      /**
       * <strong>(optional)</strong> Attribute of the occurrence entity that is a foreign key to the
       * id attribute of the primary entity.
       *
       * <p>If this property is set, then the {@link #idPairsSqlFile} must be unset.
       */
      public String foreignKeyAttributeOccurrenceEntity;

      /**
       * <strong>(optional)</strong> Name of the occurrence entity - primary entity id pairs SQL
       * file.
       *
       * <p>File must be in the same directory as the entity group file. Name includes file
       * extension (e.g. occurrencePrimary.sql).
       *
       * <p>There can be other columns selected in the SQL file (e.g. <code>
       * SELECT * FROM relationships</code>), but the occurrence and primary entity ids are
       * required.
       *
       * <p>If this property is set, then the {@link #foreignKeyAttributeOccurrenceEntity} must be
       * unset.
       */
      public String idPairsSqlFile;

      /**
       * <strong>(optional)</strong> Name of the field or column name that maps to the occurrence
       * entity id.
       *
       * <p>If the {@link #idPairsSqlFile} property is defined, then this property is required.
       */
      public String occurrenceEntityIdFieldName;

      /**
       * <strong>(optional)</strong> Name of the field or column name that maps to the primary
       * entity id.
       *
       * <p>If the {@link #idPairsSqlFile} property is defined, then this property is required.
       */
      public String primaryEntityIdFieldName;
    }
  }

  /** Relationship or join between the primary and criteria entities. */
  public static class PrimaryCriteriaRelationship {
    /**
     * Name of the primary entity - criteria entity id pairs SQL file.
     *
     * <p>File must be in the same directory as the entity group file. Name includes file extension
     * (e.g. primaryCriteria.sql).
     *
     * <p>There can be other columns selected in the SQL file (e.g. <code>
     * SELECT * FROM relationships</code>), but the primary and criteria entity ids are required.
     */
    public String idPairsSqlFile;

    /** Name of the field or column name that maps to the primary entity id. */
    public String primaryEntityIdFieldName;

    /** Name of the field or column name that maps to the criteria entity id. */
    public String criteriaEntityIdFieldName;
  }
}
