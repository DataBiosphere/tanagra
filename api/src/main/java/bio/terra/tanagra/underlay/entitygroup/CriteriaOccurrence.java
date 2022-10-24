package bio.terra.tanagra.underlay.entitygroup;

import bio.terra.tanagra.indexing.IndexingJob;
import bio.terra.tanagra.indexing.job.ComputeRollupCounts;
import bio.terra.tanagra.serialization.UFEntityGroup;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityGroup;
import bio.terra.tanagra.underlay.EntityGroupMapping;
import bio.terra.tanagra.underlay.Relationship;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.relationshipfield.Count;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CriteriaOccurrence extends EntityGroup {
  private static final String CRITERIA_ENTITY_NAME = "criteria";
  private static final String OCCURRENCE_ENTITY_NAME = "occurrence";
  private static final String OCCURRENCE_TO_CRITERIA_RELATIONSHIP_NAME = "occurrenceToCriteria";
  private static final String OCCURRENCE_TO_PRIMARY_RELATIONSHIP_NAME = "occurrenceToPrimary";
  private static final String CRITERIA_TO_PRIMARY_RELATIONSHIP_NAME = "criteriaToPrimary";

  private final Entity criteriaEntity;
  private final Entity occurrenceEntity;
  private final Entity primaryEntity;

  private CriteriaOccurrence(Builder builder) {
    super(builder);
    this.criteriaEntity = builder.criteriaEntity;
    this.occurrenceEntity = builder.occurrenceEntity;
    this.primaryEntity = builder.primaryEntity;
  }

  public static CriteriaOccurrence fromSerialized(
      UFEntityGroup serialized,
      Map<String, DataPointer> dataPointers,
      Map<String, Entity> entities,
      String primaryEntityName) {
    // Entities.
    Entity criteriaEntity = getDeserializedEntity(serialized, CRITERIA_ENTITY_NAME, entities);
    Entity occurrenceEntity = getDeserializedEntity(serialized, OCCURRENCE_ENTITY_NAME, entities);
    Entity primaryEntity = entities.get(primaryEntityName);

    // Relationships.
    Map<String, Relationship> relationships =
        Map.of(
            OCCURRENCE_TO_CRITERIA_RELATIONSHIP_NAME,
            new Relationship(
                OCCURRENCE_TO_CRITERIA_RELATIONSHIP_NAME,
                occurrenceEntity,
                criteriaEntity,
                List.of(new Count(criteriaEntity))),
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
                List.of(new Count(criteriaEntity))));

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
            .build();

    sourceDataMapping.initialize(criteriaOccurrence);
    indexDataMapping.initialize(criteriaOccurrence);

    // Source+index relationship mappings.
    EntityGroup.deserializeRelationshipMappings(serialized, criteriaOccurrence);

    return criteriaOccurrence;
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

    // If the criteria entity has a hierarchy, then also compute the counts for each hierarchy.
    //    if (criteriaEntity.hasHierarchies()) {
    //      criteriaEntity.getHierarchies().stream()
    //          .forEach(
    //              hierarchy -> {
    //                jobs.add(
    //                    new ComputeRollupCounts(
    //                        criteriaEntity, getCriteriaPrimaryRelationship(), hierarchy));
    //                jobs.add(
    //                    new ComputeRollupCounts(
    //                        criteriaEntity, getOccurrenceCriteriaRelationship(), hierarchy));
    //              });
    //    }

    return jobs;
  }

  public Entity getCriteriaEntity() {
    return criteriaEntity;
  }

  public Entity getPrimaryEntity() {
    return primaryEntity;
  }

  public Relationship getCriteriaPrimaryRelationship() {
    return relationships.get(CRITERIA_TO_PRIMARY_RELATIONSHIP_NAME);
  }

  public Relationship getOccurrenceCriteriaRelationship() {
    return relationships.get(OCCURRENCE_TO_CRITERIA_RELATIONSHIP_NAME);
  }

  private static class Builder extends EntityGroup.Builder {
    private Entity criteriaEntity;
    private Entity occurrenceEntity;
    private Entity primaryEntity;

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

    @Override
    public CriteriaOccurrence build() {
      return new CriteriaOccurrence(this);
    }
  }
}
