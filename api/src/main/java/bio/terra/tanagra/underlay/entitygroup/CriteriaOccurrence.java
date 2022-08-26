package bio.terra.tanagra.underlay.entitygroup;

import bio.terra.tanagra.indexing.WorkflowCommand;
import bio.terra.tanagra.indexing.command.PrecomputeCounts;
import bio.terra.tanagra.serialization.UFEntityGroup;
import bio.terra.tanagra.underlay.AuxiliaryData;
import bio.terra.tanagra.underlay.AuxiliaryDataMapping;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityGroup;
import bio.terra.tanagra.underlay.EntityGroupMapping;
import bio.terra.tanagra.underlay.Relationship;
import bio.terra.tanagra.underlay.RelationshipMapping;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;

public class CriteriaOccurrence extends EntityGroup {
  private static final String CRITERIA_ENTITY_NAME = "criteria";
  private static final String OCCURRENCE_ENTITY_NAME = "occurrence";
  private static final String OCCURRENCE_TO_CRITERIA_RELATIONSHIP_NAME = "occurrenceToCriteria";
  private static final String OCCURRENCE_TO_PRIMARY_RELATIONSHIP_NAME = "occurrenceToPrimary";

  private static final String CRITERIA_PRIMARY_ROLLUP_COUNT_AUXILIARY_DATA_NAME =
      "criteriaPrimaryRollupCount";
  private static final AuxiliaryData CRITERIA_PRIMARY_ROLLUP_COUNT_AUXILIARY_DATA =
      new AuxiliaryData(
          CRITERIA_PRIMARY_ROLLUP_COUNT_AUXILIARY_DATA_NAME, List.of("criteriaId", "primaryCount"));

  private Entity criteriaEntity;
  private Entity occurrenceEntity;
  private Entity primaryEntity;

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
    Entity criteriaEntity = deserializeEntity(serialized, CRITERIA_ENTITY_NAME, entities);
    Entity occurrenceEntity = deserializeEntity(serialized, OCCURRENCE_ENTITY_NAME, entities);
    Entity primaryEntity = entities.get(primaryEntityName);

    Map<String, Relationship> relationships =
        Map.of(
            OCCURRENCE_TO_CRITERIA_RELATIONSHIP_NAME,
                new Relationship(
                    OCCURRENCE_TO_CRITERIA_RELATIONSHIP_NAME, occurrenceEntity, criteriaEntity),
            OCCURRENCE_TO_PRIMARY_RELATIONSHIP_NAME,
                new Relationship(
                    OCCURRENCE_TO_PRIMARY_RELATIONSHIP_NAME, occurrenceEntity, primaryEntity));
    Map<String, AuxiliaryData> auxiliaryData =
        Map.of(
            CRITERIA_PRIMARY_ROLLUP_COUNT_AUXILIARY_DATA_NAME,
            CRITERIA_PRIMARY_ROLLUP_COUNT_AUXILIARY_DATA);

    EntityGroupMapping sourceDataMapping =
        EntityGroupMapping.fromSerializedForSourceData(
            serialized.getSourceDataMapping(), dataPointers, relationships, auxiliaryData);
    EntityGroupMapping indexDataMapping =
        EntityGroupMapping.fromSerializedForIndexData(
            serialized.getIndexDataMapping(), dataPointers, relationships, auxiliaryData);

    Builder builder = new Builder();
    builder
        .name(serialized.getName())
        .relationships(relationships)
        .auxiliaryData(auxiliaryData)
        .sourceDataMapping(sourceDataMapping)
        .indexDataMapping(indexDataMapping);
    return builder
        .criteriaEntity(criteriaEntity)
        .occurrenceEntity(occurrenceEntity)
        .primaryEntity(primaryEntity)
        .build();
  }

  @Override
  public EntityGroup.Type getType() {
    return Type.CRITERIA_OCCURRENCE;
  }

  @Override
  public Map<String, Entity> getEntities() {
    return ImmutableMap.of(
        CRITERIA_ENTITY_NAME, criteriaEntity, OCCURRENCE_ENTITY_NAME, occurrenceEntity);
  }

  @Override
  public List<WorkflowCommand> getIndexingCommands() {
    return List.of(PrecomputeCounts.forEntityGroup(this));
  }

  public Entity getCriteriaEntity() {
    return criteriaEntity;
  }

  public Entity getOccurrenceEntity() {
    return occurrenceEntity;
  }

  public Entity getPrimaryEntity() {
    return primaryEntity;
  }

  public RelationshipMapping getOccurrenceToCriteriaRelationshipMapping() {
    return sourceDataMapping
        .getRelationshipMappings()
        .get(OCCURRENCE_TO_CRITERIA_RELATIONSHIP_NAME);
  }

  public RelationshipMapping getOccurrenceToPrimaryRelationshipMapping() {
    return sourceDataMapping.getRelationshipMappings().get(OCCURRENCE_TO_PRIMARY_RELATIONSHIP_NAME);
  }

  public AuxiliaryDataMapping getCriteriaPrimaryRollupCountAuxiliaryDataMapping() {
    return indexDataMapping
        .getAuxiliaryDataMappings()
        .get(CRITERIA_PRIMARY_ROLLUP_COUNT_AUXILIARY_DATA_NAME);
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

    public CriteriaOccurrence build() {
      return new CriteriaOccurrence(this);
    }
  }
}
