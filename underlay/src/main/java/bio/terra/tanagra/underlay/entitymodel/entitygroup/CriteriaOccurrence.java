package bio.terra.tanagra.underlay.entitymodel.entitygroup;

import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Relationship;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

public class CriteriaOccurrence extends EntityGroup {
  private final Entity criteriaEntity;
  private final ImmutableList<Entity> occurrenceEntities;
  private final Entity primaryEntity;
  private final ImmutableMap<String, Relationship> occurrenceCriteriaRelationships;
  private final ImmutableMap<String, Relationship> occurrencePrimaryRelationships;
  private final Relationship primaryCriteriaRelationship;
  private final ImmutableMap<String, ImmutableSet<String>>
      occurrenceAttributesWithInstanceLevelDisplayHints;
  private final ImmutableMap<String, ImmutableSet<String>>
      occurrenceAttributesWithRollupInstanceLevelDisplayHints;

  @SuppressWarnings("checkstyle:ParameterNumber")
  public CriteriaOccurrence(
      String name,
      Entity criteriaEntity,
      List<Entity> occurrenceEntities,
      Entity primaryEntity,
      Map<String, Relationship> occurrenceCriteriaRelationships,
      Map<String, Relationship> occurrencePrimaryRelationships,
      Relationship primaryCriteriaRelationship,
      Map<String, Set<String>> occurrenceAttributesWithInstanceLevelDisplayHints,
      Map<String, Set<String>> occurrenceAttributesWithRollupInstanceLevelDisplayHints) {
    super(name);
    this.criteriaEntity = criteriaEntity;
    this.occurrenceEntities = ImmutableList.copyOf(occurrenceEntities);
    this.primaryEntity = primaryEntity;
    this.occurrenceCriteriaRelationships = ImmutableMap.copyOf(occurrenceCriteriaRelationships);
    this.occurrencePrimaryRelationships = ImmutableMap.copyOf(occurrencePrimaryRelationships);
    this.primaryCriteriaRelationship = primaryCriteriaRelationship;
    this.occurrenceAttributesWithInstanceLevelDisplayHints =
        ImmutableMap.copyOf(
            occurrenceAttributesWithInstanceLevelDisplayHints.entrySet().stream()
                .collect(
                    Collectors.toMap(
                        Entry::getKey, entry -> ImmutableSet.copyOf(entry.getValue()))));
    this.occurrenceAttributesWithRollupInstanceLevelDisplayHints =
        ImmutableMap.copyOf(
            occurrenceAttributesWithRollupInstanceLevelDisplayHints.entrySet().stream()
                .collect(
                    Collectors.toMap(
                        Entry::getKey, entry -> ImmutableSet.copyOf(entry.getValue()))));
  }

  @Override
  public Type getType() {
    return Type.CRITERIA_OCCURRENCE;
  }

  @Override
  public boolean includesEntity(String name) {
    return criteriaEntity.getName().equals(name)
        || primaryEntity.getName().equals(name)
        || occurrenceEntities.stream().anyMatch(oe -> oe.getName().equals(name));
  }

  @Override
  public ImmutableSet<Relationship> getRelationships() {
    HashSet<Relationship> relationships = new HashSet<>();
    relationships.addAll(occurrenceCriteriaRelationships.values());
    relationships.addAll(occurrencePrimaryRelationships.values());
    relationships.add(primaryCriteriaRelationship);
    return ImmutableSet.copyOf(relationships);
  }

  @Override
  public boolean hasRollupCountField(String entity, String countedEntity) {
    // There are rollup counts on the criteria entity, counting the number of related primary
    // entities.
    return criteriaEntity.getName().equals(entity) && primaryEntity.getName().equals(countedEntity);
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
    return occurrenceCriteriaRelationships.get(occurrenceEntity);
  }

  public Relationship getPrimaryCriteriaRelationship() {
    return primaryCriteriaRelationship;
  }

  public boolean hasInstanceLevelDisplayHints(Entity occurrenceEntity) {
    return !occurrenceAttributesWithInstanceLevelDisplayHints
            .get(occurrenceEntity.getName())
            .isEmpty()
        || !occurrenceAttributesWithRollupInstanceLevelDisplayHints
            .get(occurrenceEntity.getName())
            .isEmpty();
  }

  public boolean hasRollupInstanceLevelDisplayHints(Entity occurrenceEntity) {
    return !occurrenceAttributesWithRollupInstanceLevelDisplayHints
        .get(occurrenceEntity.getName())
        .isEmpty();
  }

  public ImmutableMap<Attribute, Boolean> getAttributesWithInstanceLevelDisplayHints(
      Entity occurrenceEntity) {
    Map<Attribute, Boolean> merged =
        new HashMap<>(
            occurrenceAttributesWithInstanceLevelDisplayHints
                .get(occurrenceEntity.getName())
                .stream()
                .collect(Collectors.toMap(occurrenceEntity::getAttribute, entry -> false)));
    merged.putAll(
        occurrenceAttributesWithRollupInstanceLevelDisplayHints
            .get(occurrenceEntity.getName())
            .stream()
            .collect(Collectors.toMap(occurrenceEntity::getAttribute, entry -> true)));
    return ImmutableMap.copyOf(merged);
  }
}
