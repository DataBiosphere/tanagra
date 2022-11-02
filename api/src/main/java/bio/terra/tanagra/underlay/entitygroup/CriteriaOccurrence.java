package bio.terra.tanagra.underlay.entitygroup;

import bio.terra.tanagra.indexing.IndexingJob;
import bio.terra.tanagra.indexing.job.ComputeDisplayHints;
import bio.terra.tanagra.indexing.job.ComputeRollupCounts;
import bio.terra.tanagra.serialization.entitygroup.UFCriteriaOccurrence;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.AuxiliaryData;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityGroup;
import bio.terra.tanagra.underlay.EntityGroupMapping;
import bio.terra.tanagra.underlay.Relationship;
import bio.terra.tanagra.underlay.RelationshipField;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.relationshipfield.Count;
import bio.terra.tanagra.underlay.relationshipfield.DisplayHints;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CriteriaOccurrence extends EntityGroup {
  private static final String CRITERIA_ENTITY_NAME = "criteria";
  private static final String OCCURRENCE_ENTITY_NAME = "occurrence";
  private static final String OCCURRENCE_TO_CRITERIA_RELATIONSHIP_NAME = "occurrenceToCriteria";
  private static final String OCCURRENCE_TO_PRIMARY_RELATIONSHIP_NAME = "occurrenceToPrimary";
  private static final String CRITERIA_TO_PRIMARY_RELATIONSHIP_NAME = "criteriaToPrimary";

  private static final AuxiliaryData MODIFIER_AUXILIARY_DATA =
      new AuxiliaryData(
          "modifiers",
          List.of(
              "entity_id",
              "attribute_name",
              "min",
              "max",
              "enum_value",
              "enum_display",
              "enum_count"));

  private final Entity criteriaEntity;
  private final Entity occurrenceEntity;
  private final Entity primaryEntity;
  private final List<Attribute> modifierAttributes;
  private final AuxiliaryData modifierAuxiliaryData;

  private CriteriaOccurrence(Builder builder) {
    super(builder);
    this.criteriaEntity = builder.criteriaEntity;
    this.occurrenceEntity = builder.occurrenceEntity;
    this.primaryEntity = builder.primaryEntity;
    this.modifierAttributes = builder.modifierAttributes;
    boolean hasModifierAttributes =
        builder.modifierAttributes != null && !builder.modifierAttributes.isEmpty();
    this.modifierAuxiliaryData =
        hasModifierAttributes ? MODIFIER_AUXILIARY_DATA.cloneWithoutMappings() : null;
  }

  public static CriteriaOccurrence fromSerialized(
      UFCriteriaOccurrence serialized,
      Map<String, DataPointer> dataPointers,
      Map<String, Entity> entities,
      String primaryEntityName) {
    // Entities.
    Entity criteriaEntity = entities.get(serialized.getCriteriaEntity());
    Entity occurrenceEntity = entities.get(serialized.getOccurrenceEntity());
    Entity primaryEntity = entities.get(primaryEntityName);

    // Modifier attributes.
    List<Attribute> modifierAttributes =
        serialized.getModifierAttributes() == null
            ? Collections.emptyList()
            : serialized.getModifierAttributes().stream()
                .map(attrName -> occurrenceEntity.getAttribute(attrName))
                .collect(Collectors.toList());

    // Relationships.
    Map<String, Relationship> relationships =
        Map.of(
            OCCURRENCE_TO_CRITERIA_RELATIONSHIP_NAME,
            new Relationship(
                OCCURRENCE_TO_CRITERIA_RELATIONSHIP_NAME,
                occurrenceEntity,
                criteriaEntity,
                buildRelationshipFieldList(criteriaEntity)),
            OCCURRENCE_TO_PRIMARY_RELATIONSHIP_NAME,
            new Relationship(
                OCCURRENCE_TO_PRIMARY_RELATIONSHIP_NAME,
                occurrenceEntity,
                primaryEntity,
                Collections.emptyList()),
            CRITERIA_TO_PRIMARY_RELATIONSHIP_NAME,
            new Relationship(
                CRITERIA_TO_PRIMARY_RELATIONSHIP_NAME,
                criteriaEntity,
                primaryEntity,
                buildRelationshipFieldList(criteriaEntity)));

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
            .occurrenceEntity(occurrenceEntity)
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

  private static List<RelationshipField> buildRelationshipFieldList(Entity entity) {
    List<RelationshipField> fields = new ArrayList<>();
    fields.add(new Count(entity));
    fields.add(new DisplayHints(entity));

    if (entity.hasHierarchies()) {
      entity.getHierarchies().stream()
          .forEach(
              hierarchy -> {
                fields.add(new Count(entity, hierarchy));
                fields.add(new DisplayHints(entity, hierarchy));
              });
    }
    return fields;
  }

  @Override
  public EntityGroup.Type getType() {
    return Type.CRITERIA_OCCURRENCE;
  }

  @Override
  public Map<String, Entity> getEntityMap() {
    return ImmutableMap.of(
        CRITERIA_ENTITY_NAME, criteriaEntity, OCCURRENCE_ENTITY_NAME, occurrenceEntity);
  }

  @Override
  public List<IndexingJob> getIndexingJobs() {
    List<IndexingJob> jobs = super.getIndexingJobs();

    // Compute the criteria rollup counts for both the criteria-primary and criteria-occurrence
    // relationships.
    jobs.add(new ComputeRollupCounts(criteriaEntity, getCriteriaPrimaryRelationship(), null));
    jobs.add(new ComputeRollupCounts(criteriaEntity, getOccurrenceCriteriaRelationship(), null));

    // If the criteria entity has a hierarchy, then also compute the counts for each
    // hierarchy.
    if (criteriaEntity.hasHierarchies()) {
      criteriaEntity.getHierarchies().stream()
          .forEach(
              hierarchy -> {
                jobs.add(
                    new ComputeRollupCounts(
                        criteriaEntity, getCriteriaPrimaryRelationship(), hierarchy));
                jobs.add(
                    new ComputeRollupCounts(
                        criteriaEntity, getOccurrenceCriteriaRelationship(), hierarchy));
              });
    }

    // Compute display hints for the occurrence entity.
    if (!modifierAttributes.isEmpty()) {
      jobs.add(new ComputeDisplayHints(this, modifierAttributes));
    }

    return jobs;
  }

  public Entity getCriteriaEntity() {
    return criteriaEntity;
  }

  public Entity getPrimaryEntity() {
    return primaryEntity;
  }

  public Entity getOccurrenceEntity() {
    return occurrenceEntity;
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
    return relationships.get(CRITERIA_TO_PRIMARY_RELATIONSHIP_NAME);
  }

  public Relationship getOccurrenceCriteriaRelationship() {
    return relationships.get(OCCURRENCE_TO_CRITERIA_RELATIONSHIP_NAME);
  }

  public Relationship getOccurrencePrimaryRelationship() {
    return relationships.get(OCCURRENCE_TO_PRIMARY_RELATIONSHIP_NAME);
  }

  private static class Builder extends EntityGroup.Builder {
    private Entity criteriaEntity;
    private Entity occurrenceEntity;
    private Entity primaryEntity;
    private List<Attribute> modifierAttributes;

    public Builder criteriaEntity(Entity criteriaEntity) {
      this.criteriaEntity = criteriaEntity;
      return this;
    }

    public Builder occurrenceEntity(Entity occurrenceEntity) {
      this.occurrenceEntity = occurrenceEntity;
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
