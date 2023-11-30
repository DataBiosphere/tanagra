package bio.terra.tanagra.underlay.serialization;

import bio.terra.tanagra.annotation.AnnotatedClass;
import bio.terra.tanagra.annotation.AnnotatedField;
import java.util.Set;

@AnnotatedClass(
    name = "SZCriteriaOccurrence",
    markdown =
        "Criteria-Occurrence entity group configuration.\n\n"
            + "Define a version of this file for each entity group of this type. "
            + "This entity group type defines a relationship between three entities. For each criteria entity "
            + "instance and primary entity instance, there are one or more occurrence entity instances.")
public class SZCriteriaOccurrence {
  @AnnotatedField(
      name = "SZCriteriaOccurrence.name",
      markdown =
          "Name of the entity group.\n\n"
              + "This is the unique identifier for the entity group. In a single underlay, the entity group "
              + "names of any group type cannot overlap. Name may not include spaces or special characters, "
              + "only letters and numbers. The first character must be a letter.")
  public String name;

  @AnnotatedField(
      name = "SZCriteriaOccurrence.criteriaEntity",
      markdown = "Name of the criteria entity.")
  public String criteriaEntity;

  @AnnotatedField(
      name = "SZCriteriaOccurrence.occurrenceEntities",
      markdown =
          "Set of occurrence entity configurations.\n\n"
              + "Most entity groups of this type will have a single occurrence entity (e.g. SNOMED condition "
              + "code only maps to condition occurrences), but we also support the case of multiple (e.g. "
              + "ICD9-CM condition code maps to condition, measurement, observation and procedure occurrences).")
  public Set<OccurrenceEntity> occurrenceEntities;

  // TODO: Remove this. We can infer this relationship from the criteria-occurrence and
  // occurrence-primary relationships.
  @AnnotatedField(
      name = "SZCriteriaOccurrence.primaryCriteriaRelationship",
      markdown = "Relationship or join between the primary and criteria entities.")
  public PrimaryCriteriaRelationship primaryCriteriaRelationship;

  @AnnotatedClass(name = "SZOccurrenceEntity", markdown = "Occurrence entity configuration.")
  public static class OccurrenceEntity {
    @SuppressWarnings("PMD.AvoidFieldNameMatchingTypeName")
    @AnnotatedField(
        name = "SZOccurrenceEntity.occurrenceEntity",
        markdown = "Name of occurrence entity.")
    public String occurrenceEntity;

    @AnnotatedField(
        name = "SZOccurrenceEntity.criteriaRelationship",
        markdown =
            "Relationship or join between this occurrence entity and the criteria entity "
                + "(e.g. condition occurrence and ICD9-CM).")
    public CriteriaRelationship criteriaRelationship;

    @AnnotatedField(
        name = "SZOccurrenceEntity.primaryRelationship",
        markdown =
            "Relationship or join between this occurrence entity and the primary entity (e.g. condition "
                + "occurrence and person).")
    public PrimaryRelationship primaryRelationship;

    @AnnotatedField(
        name = "SZOccurrenceEntity.attributesWithInstanceLevelHints",
        markdown =
            "Names of attributes that we want to calculate instance-level hints for.\n\n"
                + "Instance-level hints are ranges of possible values for a particular criteria instance. "
                + "They are used to support criteria-specific modifiers (e.g. range of values for measurement "
                + "code \"glucose test\").")
    public Set<String> attributesWithInstanceLevelHints;

    @AnnotatedClass(
        name = "SZCriteriaRelationship",
        markdown =
            "Relationship or join between an occurrence entity and the criteria entity (e.g. condition "
                + "occurrence and ICD9-CM).")
    public static class CriteriaRelationship {
      @AnnotatedField(
          name = "SZCriteriaRelationship.foreignKeyAttributeOccurrenceEntity",
          markdown =
              "Attribute of the occurrence entity that is a foreign key to the "
                  + "id attribute of the criteria entity. If this property is set, then the "
                  + "[id pairs SQL](${SZCriteriaRelationship.idPairsSqlFile}) must be unset.",
          optional = true)
      public String foreignKeyAttributeOccurrenceEntity;

      @AnnotatedField(
          name = "SZCriteriaRelationship.idPairsSqlFile",
          markdown =
              "Name of the occurrence entity - criteria entity id pairs SQL file. "
                  + "File must be in the same directory as the entity group file. Name includes file extension. "
                  + "If this property is set, then the "
                  + "[foreign key attribute](${SZCriteriaRelationship.foreignKeyAttributeOccurrenceEntity}) must be unset.\n\n"
                  + "There can be other columns selected in the SQL file (e.g. `SELECT * FROM relationships`), but the "
                  + "occurrence and criteria entity ids are required.",
          optional = true,
          exampleValue = "occurrenceCriteria.sql")
      public String idPairsSqlFile;

      @AnnotatedField(
          name = "SZCriteriaRelationship.occurrenceEntityIdFieldName",
          markdown =
              "Name of the field or column name that maps to the occurrence entity id. "
                  + "Required if the [id pairs SQL](${SZCriteriaRelationship.idPairsSqlFile}) is defined.",
          optional = true,
          exampleValue = "occurrence_id")
      public String occurrenceEntityIdFieldName;

      @AnnotatedField(
          name = "SZCriteriaRelationship.criteriaEntityIdFieldName",
          markdown =
              "Name of the field or column name that maps to the criteria entity id. "
                  + "Required if the [id pairs SQL](${SZCriteriaRelationship.idPairsSqlFile}) is defined.",
          optional = true,
          exampleValue = "criteria_id")
      public String criteriaEntityIdFieldName;
    }

    @AnnotatedClass(
        name = "SZPrimaryRelationship",
        markdown =
            "Relationship or join between an occurrence entity and the primary entity (e.g. condition "
                + "occurrence and person).")
    public static class PrimaryRelationship {
      @AnnotatedField(
          name = "SZPrimaryRelationship.foreignKeyAttributeOccurrenceEntity",
          markdown =
              "Attribute of the occurrence entity that is a foreign key to the "
                  + "id attribute of the primary entity. If this property is set, then the "
                  + "[id pairs SQL](${SZPrimaryRelationship.idPairsSqlFile}) must be unset.",
          optional = true)
      public String foreignKeyAttributeOccurrenceEntity;

      @AnnotatedField(
          name = "SZPrimaryRelationship.idPairsSqlFile",
          markdown =
              "Name of the occurrence entity - primary entity id pairs SQL file. "
                  + "File must be in the same directory as the entity group file. Name includes file extension. "
                  + "If this property is set, then the "
                  + "[foreign key attribute](${SZPrimaryRelationship.foreignKeyAttributeOccurrenceEntity}) must be unset.\n\n"
                  + "There can be other columns selected in the SQL file (e.g. `SELECT * FROM relationships`), but the "
                  + "occurrence and primary entity ids are required.",
          optional = true,
          exampleValue = "occurrencePrimary.sql")
      public String idPairsSqlFile;

      @AnnotatedField(
          name = "SZPrimaryRelationship.occurrenceEntityIdFieldName",
          markdown =
              "Name of the field or column name that maps to the occurrence entity id. "
                  + "Required if the [id pairs SQL](${SZPrimaryRelationship.idPairsSqlFile}) is defined.",
          optional = true,
          exampleValue = "occurrence_id")
      public String occurrenceEntityIdFieldName;

      @AnnotatedField(
          name = "SZPrimaryRelationship.primaryEntityIdFieldName",
          markdown =
              "Name of the field or column name that maps to the primary entity id. "
                  + "Required if the [id pairs SQL](${SZPrimaryRelationship.idPairsSqlFile}) is defined.",
          optional = true,
          exampleValue = "primary_id")
      public String primaryEntityIdFieldName;
    }
  }

  @AnnotatedClass(
      name = "SZPrimaryCriteriaRelationship",
      markdown =
          "Relationship or join between the primary and criteria entities (e.g. condition and person).")
  public static class PrimaryCriteriaRelationship {
    @AnnotatedField(
        name = "SZPrimaryCriteriaRelationship.idPairsSqlFile",
        markdown =
            "Name of the primary entity - criteria entity id pairs SQL file. "
                + "File must be in the same directory as the entity group file. Name includes file extension. "
                + "There can be other columns selected in the SQL file (e.g. `SELECT * FROM relationships`), but the "
                + "primary and criteria entity ids are required.",
        exampleValue = "primaryCriteria.sql")
    public String idPairsSqlFile;

    @AnnotatedField(
        name = "SZPrimaryCriteriaRelationship.primaryEntityIdFieldName",
        markdown = "Name of the field or column name that maps to the primary entity id.",
        exampleValue = "primary_id")
    public String primaryEntityIdFieldName;

    @AnnotatedField(
        name = "SZPrimaryCriteriaRelationship.criteriaEntityIdFieldName",
        markdown = "Name of the field or column name that maps to the criteria entity id.",
        exampleValue = "criteria_id")
    public String criteriaEntityIdFieldName;
  }
}
