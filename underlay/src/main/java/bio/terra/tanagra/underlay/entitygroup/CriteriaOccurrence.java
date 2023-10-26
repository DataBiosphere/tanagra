package bio.terra.tanagra.underlay.entitygroup;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.query.DataPointer;
import bio.terra.tanagra.serialization.entitygroup.UFCriteriaOccurrence;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.AuxiliaryData;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityGroup;
import bio.terra.tanagra.underlay.EntityGroupMapping;
import bio.terra.tanagra.underlay.Relationship;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay2.indextable.ITInstanceLevelDisplayHints;
import java.util.*;
import java.util.stream.Collectors;

public class CriteriaOccurrence extends EntityGroup {

  private static final String OCCURRENCE_TO_CRITERIA_RELATIONSHIP_NAME = "occurrenceToCriteria";
  private static final String OCCURRENCE_TO_PRIMARY_RELATIONSHIP_NAME = "occurrenceToPrimary";
  private static final String CRITERIA_TO_PRIMARY_RELATIONSHIP_NAME = "criteriaToPrimary";

  private static final AuxiliaryData MODIFIER_AUXILIARY_DATA =
      new AuxiliaryData(
          "modifiers",
          Arrays.stream(ITInstanceLevelDisplayHints.Column.values())
              .map(c -> c.getSchema().getColumnName())
              .collect(Collectors.toList()));

  private final Entity criteriaEntity;
  private final List<Entity> occurrenceEntities;
  private final Entity primaryEntity;
  private final List<Attribute> modifierAttributes;
  private final AuxiliaryData modifierAuxiliaryData;
  private final Relationship criteriaToPrimary;
  private final Map<Entity, Relationship> occurrenceToCriteria;
  private final Map<Entity, Relationship> occurrenceToPrimary;

  private CriteriaOccurrence(Builder builder) {
    super(builder);
    this.criteriaEntity = builder.criteriaEntity;
    this.occurrenceEntities = builder.occurrenceEntities;
    this.primaryEntity = builder.primaryEntity;
    this.modifierAttributes = builder.modifierAttributes;
    boolean hasModifierAttributes =
        builder.modifierAttributes != null && !builder.modifierAttributes.isEmpty();
    this.modifierAuxiliaryData =
        hasModifierAttributes ? MODIFIER_AUXILIARY_DATA.cloneWithoutMappings() : null;
    this.criteriaToPrimary = builder.getRelationships().get(CRITERIA_TO_PRIMARY_RELATIONSHIP_NAME);

    Map<Entity, Relationship> occurrenceToCriteria = new HashMap<>();
    builder.occurrenceEntities.stream()
        .forEach(
            oe ->
                occurrenceToCriteria.put(
                    oe,
                    builder
                        .getRelationships()
                        .get(
                            getOccurrenceRelationshipName(
                                OCCURRENCE_TO_CRITERIA_RELATIONSHIP_NAME,
                                oe,
                                occurrenceEntities))));
    this.occurrenceToCriteria = occurrenceToCriteria;

    Map<Entity, Relationship> occurrenceToPrimary = new HashMap<>();
    builder.occurrenceEntities.stream()
        .forEach(
            oe ->
                occurrenceToPrimary.put(
                    oe,
                    builder
                        .getRelationships()
                        .get(
                            getOccurrenceRelationshipName(
                                OCCURRENCE_TO_PRIMARY_RELATIONSHIP_NAME, oe, occurrenceEntities))));
    this.occurrenceToPrimary = occurrenceToPrimary;
  }

  public static CriteriaOccurrence fromSerialized(
      UFCriteriaOccurrence serialized,
      Map<String, DataPointer> dataPointers,
      Map<String, Entity> entities,
      String primaryEntityName) {
    // Entities.
    Entity criteriaEntity = entities.get(serialized.getCriteriaEntity());
    Entity primaryEntity = entities.get(primaryEntityName);
    List<Entity> occurrenceEntities =
        serialized.getOccurrenceEntities().stream()
            .map(name -> entities.get(name))
            .collect(Collectors.toList());

    // Modifier attributes.
    if (occurrenceEntities.size() > 1 && serialized.getModifierAttributes() != null) {
      throw new InvalidConfigException(
          "Modifiers are not supported for entity groups with >1 occurrence entity.");
    }
    Entity occurrenceEntity = occurrenceEntities.get(0);
    List<Attribute> modifierAttributes =
        serialized.getModifierAttributes() == null
            ? Collections.emptyList()
            : serialized.getModifierAttributes().stream()
                .map(attrName -> occurrenceEntity.getAttribute(attrName))
                .collect(Collectors.toList());

    // Relationships.
    Map<String, Relationship> relationships = new HashMap<>();
    relationships.put(
        CRITERIA_TO_PRIMARY_RELATIONSHIP_NAME,
        new Relationship(
            CRITERIA_TO_PRIMARY_RELATIONSHIP_NAME,
            criteriaEntity,
            primaryEntity,
            buildRelationshipFieldList(criteriaEntity)));
    occurrenceEntities.stream()
        .forEach(
            oe -> {
              String occurrenceToCriteriaRelationshipName =
                  getOccurrenceRelationshipName(
                      OCCURRENCE_TO_CRITERIA_RELATIONSHIP_NAME, oe, occurrenceEntities);
              relationships.put(
                  occurrenceToCriteriaRelationshipName,
                  new Relationship(
                      occurrenceToCriteriaRelationshipName,
                      oe,
                      criteriaEntity,
                      buildRelationshipFieldList(criteriaEntity)));

              String occurrenceToPrimaryRelationshipName =
                  getOccurrenceRelationshipName(
                      OCCURRENCE_TO_PRIMARY_RELATIONSHIP_NAME, oe, occurrenceEntities);
              relationships.put(
                  occurrenceToPrimaryRelationshipName,
                  new Relationship(
                      occurrenceToPrimaryRelationshipName,
                      oe,
                      primaryEntity,
                      Collections.emptyList()));
            });

    // Source+index entity group mappings.
    EntityGroupMapping sourceDataMapping =
        EntityGroupMapping.fromSerialized(
            serialized.getSourceDataMapping(), dataPointers, Underlay.MappingType.SOURCE);
    EntityGroupMapping indexDataMapping =
        EntityGroupMapping.fromSerialized(
            serialized.getIndexDataMapping(), dataPointers, Underlay.MappingType.INDEX);

    Builder builder = new Builder();
    builder
        .name(serialized.getName())
        .relationships(relationships)
        .sourceDataMapping(sourceDataMapping)
        .indexDataMapping(indexDataMapping);
    CriteriaOccurrence criteriaOccurrence =
        builder
            .criteriaEntity(criteriaEntity)
            .occurrenceEntities(occurrenceEntities)
            .primaryEntity(primaryEntity)
            .modifierAttributes(modifierAttributes)
            .build();

    sourceDataMapping.initialize(criteriaOccurrence);
    indexDataMapping.initialize(criteriaOccurrence);

    // Source+index relationship mappings.
    EntityGroup.deserializeRelationshipMappings(serialized, criteriaOccurrence);

    // Source+index auxiliary data mappings.
    EntityGroup.deserializeAuxiliaryDataMappings(serialized, criteriaOccurrence);

    return criteriaOccurrence;
  }

  private static String getOccurrenceRelationshipName(
      String prefix, Entity occurrenceEntity, List<Entity> occurrenceEntities) {
    return occurrenceEntities.size() == 1 ? prefix : prefix + ":" + occurrenceEntity.getName();
  }

  @Override
  public EntityGroup.Type getType() {
    return Type.CRITERIA_OCCURRENCE;
  }

  @Override
  protected Set<Entity> getEntities() {
    Set<Entity> entities = new HashSet<>();
    entities.add(criteriaEntity);
    entities.addAll(occurrenceEntities);
    return entities;
  }

  public Entity getCriteriaEntity() {
    return criteriaEntity;
  }

  public Entity getPrimaryEntity() {
    return primaryEntity;
  }

  public List<Entity> getOccurrenceEntities() {
    return occurrenceEntities;
  }

  public List<Attribute> getModifierAttributes() {
    return Collections.unmodifiableList(modifierAttributes);
  }

  public AuxiliaryData getModifierAuxiliaryData() {
    return modifierAuxiliaryData;
  }

  @Override
  public List<AuxiliaryData> getAuxiliaryData() {
    return modifierAuxiliaryData == null ? Collections.emptyList() : List.of(modifierAuxiliaryData);
  }

  @Override
  public UFCriteriaOccurrence serialize() {
    return new UFCriteriaOccurrence(this);
  }

  public Relationship getCriteriaPrimaryRelationship() {
    return criteriaToPrimary;
  }

  public Relationship getOccurrenceCriteriaRelationship(Entity occurrenceEntity) {
    return occurrenceToCriteria.get(occurrenceEntity);
  }

  public Relationship getOccurrencePrimaryRelationship(Entity occurrenceEntity) {
    return occurrenceToPrimary.get(occurrenceEntity);
  }

  private static class Builder extends EntityGroup.Builder {
    private Entity criteriaEntity;
    private List<Entity> occurrenceEntities;
    private Entity primaryEntity;
    private List<Attribute> modifierAttributes;

    public Builder criteriaEntity(Entity criteriaEntity) {
      this.criteriaEntity = criteriaEntity;
      return this;
    }

    public Builder occurrenceEntities(List<Entity> occurrenceEntities) {
      this.occurrenceEntities = occurrenceEntities;
      return this;
    }

    public Builder primaryEntity(Entity primaryEntity) {
      this.primaryEntity = primaryEntity;
      return this;
    }

    public Builder modifierAttributes(List<Attribute> modifierAttributes) {
      this.modifierAttributes = modifierAttributes;
      return this;
    }

    @Override
    public CriteriaOccurrence build() {
      return new CriteriaOccurrence(this);
    }
  }
}
