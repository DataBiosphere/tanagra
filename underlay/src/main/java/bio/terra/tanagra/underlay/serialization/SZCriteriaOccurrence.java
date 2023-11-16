package bio.terra.tanagra.underlay.serialization;

import java.util.Set;

/**
 * <p>Criteria-Occurrence entity group configuration.</p>
 * <p>Define a version of this file for each entity group of this type.</p>
 * <p>This entity group type defines a relationship between three entities.
 * For each criteria entity instance and primary entity instance, there are one or more occurrence entity instances.</p>
 */
public class SZCriteriaOccurrence {
  /**
   * <p>Name of the entity group.</p>
   * <p>This is the unique identifier for the entity group.
   * In a single underlay, the entity group names of any group type cannot overlap.</p>
   * <p>Name may not include spaces or special characters, only letters and numbers.
   * The first character must be a letter.</p>
   */
  public String name;

  /**
   * <p>Name of the criteria entity.</p>
   */
  public String criteriaEntity;

  /**
   * <p>Set of occurrence entity configurations.</p>
   * <p>Most entity groups of this type will have a single occurrence entity
   * (e.g. SNOMED condition code only maps to condition occurrences), but we also support the case of
   * multiple (e.g. ICD9-CM condition code maps to condition, measurement, observation and procedure occurrences).</p>
   */
  public Set<OccurrenceEntity> occurrenceEntities;

  /**
   * <p>Relationship or join between the primary and criteria entities.</p>
   */
  // TODO: Remove this. We should be able to infer this relationship from the criteria-occurrence and occurrence-primary relationships.
  public PrimaryCriteriaRelationship primaryCriteriaRelationship;

  /**
   * <p>Occurrence entity configuration.</p>
   */
  public static class OccurrenceEntity {
    /**
     * <p>Name of occurrence entity.</p>
     */
    @SuppressWarnings("PMD.AvoidFieldNameMatchingTypeName")
    public String occurrenceEntity;

    /**
     * <p>Relationship or join between this occurrence entity and the criteria entity (e.g. condition occurrence and ICD9-CM).</p>
     */
    public CriteriaRelationship criteriaRelationship;

    /**
     * <p>Relationship or join between this occurrence entity and the primary entity (e.g. condition occurrence and person).</p>
     */
    public PrimaryRelationship primaryRelationship;

    /**
     * <p>Names of attributes that we want to calculate instance-level hints for.</p>
     * <p>Instance-level hints are ranges of possible values for a particular criteria instance.
     * They are used to support criteria-specific modifiers (e.g. range of values for measurement code "glucose test").</p>
     */
    public Set<String> attributesWithInstanceLevelHints;

    /**
     * <p>Relationship or join between an occurrence entity and the criteria entity (e.g. condition occurrence and ICD9-CM).</p>
     */
    public static class CriteriaRelationship {
      /**
       * <p><strong>(optional)</strong> Attribute of the occurrence entity that is a foreign key to the id attribute of the criteria entity.</p>
       * <p>If this property is set, then the {@link #idPairsSqlFile} must be unset.</p>
       */
      public String foreignKeyAttributeOccurrenceEntity;

      /**
       * <p><strong>(optional)</strong> Name of the occurrence entity - criteria entity id pairs SQL file.</p>
       * <p>File must be in the same directory as the entity group file. Name includes file extension (e.g. occurrenceCriteria.sql).</p>
       * <p>There can be other columns selected in the SQL file (e.g. <code>SELECT * FROM relationships</code>),
       * but the occurrence and criteria entity ids are required.</p>
       * <p>If this property is set, then the {@link #foreignKeyAttributeOccurrenceEntity} must be unset.</p>
       */
      public String idPairsSqlFile;

      /**
       * <p><strong>(optional)</strong> Name of the field or column name that maps to the occurrence entity id.</p>
       * <p>If the {@link #idPairsSqlFile} property is defined, then this property is required.</p>
       */
      public String occurrenceEntityIdFieldName;

      /**
       * <p><strong>(optional)</strong> Name of the field or column name that maps to the criteria entity id.</p>
       * <p>If the {@link #idPairsSqlFile} property is defined, then this property is required.</p>
       */
      public String criteriaEntityIdFieldName;
    }

    /**
     * <p>Relationship or join between an occurrence entity and the primary entity (e.g. condition occurrence and person).</p>
     */
    public static class PrimaryRelationship {
      /**
       * <p><strong>(optional)</strong> Attribute of the occurrence entity that is a foreign key to the id attribute of the primary entity.</p>
       * <p>If this property is set, then the {@link #idPairsSqlFile} must be unset.</p>
       */
      public String foreignKeyAttributeOccurrenceEntity;

      /**
       * <p><strong>(optional)</strong> Name of the occurrence entity - primary entity id pairs SQL file.</p>
       * <p>File must be in the same directory as the entity group file. Name includes file extension (e.g. occurrencePrimary.sql).</p>
       * <p>There can be other columns selected in the SQL file (e.g. <code>SELECT * FROM relationships</code>),
       * but the occurrence and primary entity ids are required.</p>
       * <p>If this property is set, then the {@link #foreignKeyAttributeOccurrenceEntity} must be unset.</p>
       */
      public String idPairsSqlFile;

      /**
       * <p><strong>(optional)</strong> Name of the field or column name that maps to the occurrence entity id.</p>
       * <p>If the {@link #idPairsSqlFile} property is defined, then this property is required.</p>
       */
      public String occurrenceEntityIdFieldName;

      /**
       * <p><strong>(optional)</strong> Name of the field or column name that maps to the primary entity id.</p>
       * <p>If the {@link #idPairsSqlFile} property is defined, then this property is required.</p>
       */
      public String primaryEntityIdFieldName;
    }
  }

  /**
   * <p>Relationship or join between the primary and criteria entities.</p>
   */
  public static class PrimaryCriteriaRelationship {
    /**
     * <p>Name of the primary entity - criteria entity id pairs SQL file.</p>
     * <p>File must be in the same directory as the entity group file. Name includes file extension (e.g. primaryCriteria.sql).</p>
     * <p>There can be other columns selected in the SQL file (e.g. <code>SELECT * FROM relationships</code>),
     * but the primary and criteria entity ids are required.</p>
     */
    public String idPairsSqlFile;

    /**
     * <p>Name of the field or column name that maps to the primary entity id.</p>
     */
    public String primaryEntityIdFieldName;

    /**
     * <p>Name of the field or column name that maps to the criteria entity id.</p>
     */
    public String criteriaEntityIdFieldName;
  }
}
