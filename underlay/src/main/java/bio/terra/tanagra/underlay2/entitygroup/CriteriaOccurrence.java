package bio.terra.tanagra.underlay2.entitygroup;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.underlay2.Entity;
import bio.terra.tanagra.underlay2.Relationship;
import bio.terra.tanagra.underlay2.indexschema.EntityMain;
import bio.terra.tanagra.underlay2.indexschema.InstanceLevelDisplayHints;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public class CriteriaOccurrence extends EntityGroup {
  private final Entity criteriaEntity;
  private final ImmutableList<Entity> occurrenceEntities;
  private final Entity primaryEntity;
  private final ImmutableMap<String, Relationship> criteriaOccurrenceRelationships;
  private final ImmutableMap<String, Relationship> occurrencePrimaryRelationships;
  private final Relationship criteriaPrimaryRelationship;
  private final ImmutableMap<String, FieldPointer> indexNumPrimaryForCriteriaCountFields;
  private final ImmutableMap<String, ImmutableList<String>>
      occurrenceAttributesWithInstanceLevelDisplayHints;
  private final ImmutableMap<String, TablePointer> indexInstanceLevelDisplayHintTables;

  @SuppressWarnings("checkstyle:ParameterNumber")
  public CriteriaOccurrence(
      String name,
      String description,
      Entity criteriaEntity,
      List<Entity> occurrenceEntities,
      Entity primaryEntity,
      Map<String, Relationship> criteriaOccurrenceRelationships,
      Map<String, Relationship> occurrencePrimaryRelationships,
      Relationship criteriaPrimaryRelationship,
      Map<String, List<String>> occurrenceAttributesWithInstanceLevelDisplayHints) {
    super(name, description);
    this.criteriaEntity = criteriaEntity;
    this.occurrenceEntities = ImmutableList.copyOf(occurrenceEntities);
    this.primaryEntity = primaryEntity;
    this.criteriaOccurrenceRelationships = ImmutableMap.copyOf(criteriaOccurrenceRelationships);
    this.occurrencePrimaryRelationships = ImmutableMap.copyOf(occurrencePrimaryRelationships);
    this.criteriaPrimaryRelationship = criteriaPrimaryRelationship;

    Map<String, ImmutableList<String>> occurrenceAttributesWithInstanceLevelDisplayHintsBuilder =
        new HashMap<>();
    occurrenceAttributesWithInstanceLevelDisplayHints.entrySet().stream()
        .forEach(
            entry -> {
              String occurrenceEntity = entry.getKey();
              List<String> attributes = entry.getValue();
              occurrenceAttributesWithInstanceLevelDisplayHintsBuilder.put(
                  occurrenceEntity, ImmutableList.copyOf(attributes));
            });
    this.occurrenceAttributesWithInstanceLevelDisplayHints =
        ImmutableMap.copyOf(occurrenceAttributesWithInstanceLevelDisplayHintsBuilder);

    // Resolve index tables and fields.
    Map<String, FieldPointer> indexNumPrimaryForCriteriaCountFieldsBuilder = new HashMap<>();
    indexNumPrimaryForCriteriaCountFieldsBuilder.put(
        null, EntityMain.getEntityGroupCountField(criteriaEntity.getName(), null, name));
    criteriaEntity.getHierarchies().stream()
        .forEach(
            h ->
                indexNumPrimaryForCriteriaCountFieldsBuilder.put(
                    h.getName(),
                    EntityMain.getEntityGroupCountField(
                        criteriaEntity.getName(), h.getName(), name)));
    this.indexNumPrimaryForCriteriaCountFields =
        ImmutableMap.copyOf(indexNumPrimaryForCriteriaCountFieldsBuilder);

    Map<String, TablePointer> indexInstanceLevelDisplayHintTablesBuilder = new HashMap<>();
    occurrenceAttributesWithInstanceLevelDisplayHints.keySet().stream()
        .forEach(
            occurrenceEntity ->
                indexInstanceLevelDisplayHintTablesBuilder.put(
                    occurrenceEntity,
                    InstanceLevelDisplayHints.getTable(
                        criteriaEntity.getName(), occurrenceEntity)));
    this.indexInstanceLevelDisplayHintTables =
        ImmutableMap.copyOf(indexInstanceLevelDisplayHintTablesBuilder);
  }

  @Override
  public Type getType() {
    return Type.CRITERIA_OCCURRENCE;
  }

  @Override
  public boolean includesEntity(String name) {
    return criteriaEntity.getName().equals(name)
        || primaryEntity.getName().equals(name)
        || occurrenceEntities.stream()
            .filter(oe -> oe.getName().equals(name))
            .findFirst()
            .isPresent();
  }

  @Override
  public FieldPointer getCountField(
      String countForEntity, String countedEntity, @Nullable String countForHierarchy) {
    if (!criteriaEntity.getName().equals(countForEntity)
        || !primaryEntity.getName().equals(countedEntity)) {
      throw new SystemException(
          "No count field for "
              + countForEntity
              + "/"
              + countedEntity
              + ". Expected "
              + criteriaEntity.getName()
              + "/"
              + primaryEntity.getName());
    }
    FieldPointer countField = indexNumPrimaryForCriteriaCountFields.get(countForHierarchy);
    if (countField == null) {
      throw new SystemException(
          "No count field for "
              + countForEntity
              + "/"
              + countedEntity
              + ", hierarchy "
              + countForHierarchy);
    }
    return countField;
  }

  public Entity getCriteriaEntity() {
    return criteriaEntity;
  }

  public ImmutableList<Entity> getOccurrenceEntities() {
    return occurrenceEntities;
  }

  public Entity getPrimaryEntity() {
    return primaryEntity;
  }

  public Relationship getOccurrencePrimaryRelationship(String occurrenceEntity) {
    return occurrencePrimaryRelationships.get(occurrenceEntity);
  }

  public Relationship getOccurrenceCriteriaRelationship(String occurrenceEntity) {
    return criteriaOccurrenceRelationships.get(occurrenceEntity);
  }

  public Relationship getPrimaryCriteriaRelationship() {
    return criteriaPrimaryRelationship;
  }

  public ImmutableList<String> getAttributesWithInstanceLevelDisplayHints(String occurrenceEntity) {
    return occurrenceAttributesWithInstanceLevelDisplayHints.get(occurrenceEntity);
  }

  public FieldPointer getIndexNumPrimaryForCriteriaCountField(@Nullable String hierarchyName) {
    FieldPointer field = indexNumPrimaryForCriteriaCountFields.get(hierarchyName);
    if (field == null) {
      throw new SystemException("No index count field found for hierarchy: " + hierarchyName);
    }
    return field;
  }

  public TablePointer getIndexInstanceLevelDisplayHintTable(String occurrenceEntity) {
    return indexInstanceLevelDisplayHintTables.get(occurrenceEntity);
  }
}
