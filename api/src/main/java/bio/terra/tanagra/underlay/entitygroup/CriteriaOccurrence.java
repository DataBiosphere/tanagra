package bio.terra.tanagra.underlay.entitygroup;

import bio.terra.tanagra.indexing.IndexingJob;
import bio.terra.tanagra.indexing.job.ComputeRollupCounts;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.serialization.UFEntityGroup;
import bio.terra.tanagra.underlay.AuxiliaryData;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityGroup;
import bio.terra.tanagra.underlay.EntityGroupMapping;
import bio.terra.tanagra.underlay.Relationship;
import bio.terra.tanagra.underlay.RelationshipMapping;
import bio.terra.tanagra.underlay.Underlay;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CriteriaOccurrence extends EntityGroup {
  private static final String CRITERIA_ENTITY_NAME = "criteria";
  private static final String OCCURRENCE_ENTITY_NAME = "occurrence";
  private static final String OCCURRENCE_TO_CRITERIA_RELATIONSHIP_NAME = "occurrenceToCriteria";
  private static final String OCCURRENCE_TO_PRIMARY_RELATIONSHIP_NAME = "occurrenceToPrimary";

  private static final String CRITERIA_PRIMARY_ROLLUP_COUNT_AUXILIARY_DATA_NAME =
      "criteriaPrimaryRollupCount";
  private static final AuxiliaryData CRITERIA_PRIMARY_ROLLUP_COUNT_AUXILIARY_DATA =
      new AuxiliaryData(
          CRITERIA_PRIMARY_ROLLUP_COUNT_AUXILIARY_DATA_NAME, List.of("id", "rollup_count"));

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
                OCCURRENCE_TO_CRITERIA_RELATIONSHIP_NAME, occurrenceEntity, criteriaEntity),
            OCCURRENCE_TO_PRIMARY_RELATIONSHIP_NAME,
            new Relationship(
                OCCURRENCE_TO_PRIMARY_RELATIONSHIP_NAME, occurrenceEntity, primaryEntity));

    // Auxiliary data.
    Map<String, AuxiliaryData> auxiliaryData =
        Map.of(
            CRITERIA_PRIMARY_ROLLUP_COUNT_AUXILIARY_DATA_NAME,
            CRITERIA_PRIMARY_ROLLUP_COUNT_AUXILIARY_DATA.cloneWithoutMappings());

    // Source+index entity group mappings.
    EntityGroupMapping sourceDataMapping =
        EntityGroupMapping.fromSerialized(serialized.getSourceDataMapping(), dataPointers);
    EntityGroupMapping indexDataMapping =
        EntityGroupMapping.fromSerialized(serialized.getIndexDataMapping(), dataPointers);

    Builder builder = new Builder();
    builder
        .name(serialized.getName())
        .relationships(relationships)
        .auxiliaryData(auxiliaryData)
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

    // Source+index relationship, auxiliary data mappings.
    EntityGroup.deserializeRelationshipMappings(serialized, criteriaOccurrence);
    EntityGroup.deserializeAuxiliaryDataMappings(serialized, criteriaOccurrence);

    return criteriaOccurrence;
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
  public List<IndexingJob> getIndexingJobs() {
    if (criteriaEntity.hasHierarchies()) {
      return criteriaEntity.getHierarchies().stream()
          .map(hierarchy -> new ComputeRollupCounts(this, hierarchy.getName()))
          .collect(Collectors.toList());
    } else {
      return List.of(new ComputeRollupCounts(this));
    }
  }

  public Entity getCriteriaEntity() {
    return criteriaEntity;
  }

  public Entity getPrimaryEntity() {
    return primaryEntity;
  }

  public AuxiliaryData getCriteriaPrimaryRollupAuxiliaryData() {
    return getAuxiliaryData().get(CRITERIA_PRIMARY_ROLLUP_COUNT_AUXILIARY_DATA_NAME);
  }

  public Query queryCriteriaPrimaryPairs(String criteriaIdAlias, String primaryIdAlias) {
    RelationshipMapping occToPriRelationshipMapping =
        relationships
            .get(OCCURRENCE_TO_PRIMARY_RELATIONSHIP_NAME)
            .getMapping(Underlay.MappingType.SOURCE);
    RelationshipMapping occToCriRelationshipMapping =
        relationships
            .get(OCCURRENCE_TO_CRITERIA_RELATIONSHIP_NAME)
            .getMapping(Underlay.MappingType.SOURCE);

    TableVariable occToPriTableVar =
        TableVariable.forPrimary(occToPriRelationshipMapping.getTablePointer());
    FieldVariable priIdFieldVar =
        new FieldVariable(
            occToPriRelationshipMapping.getToEntityId(), occToPriTableVar, primaryIdAlias);

    if (occToPriRelationshipMapping
        .getTablePointer()
        .equals(occToCriRelationshipMapping.getTablePointer())) {
      // if the two relationship mappings are in the same table, then just select from a single
      // table
      FieldVariable criIdFieldVar =
          new FieldVariable(
              occToCriRelationshipMapping.getToEntityId(), occToPriTableVar, criteriaIdAlias);
      return new Query.Builder()
          .select(List.of(criIdFieldVar, priIdFieldVar))
          .tables(List.of(occToPriTableVar))
          .build();
    } else {
      // otherwise, join the two tables
      // SELECT primaryId, criteriaId FROM occurrencePrimaryTable
      // JOIN occurrenceCriteriaTable ON occurrenceCriteriaTable.occurrenceId =
      // occurrencePrimaryTable.occurrenceId
      TableVariable occToCriTableVar =
          TableVariable.forJoined(
              occToCriRelationshipMapping.getTablePointer(),
              occToCriRelationshipMapping.getFromEntityId().getColumnName(),
              new FieldVariable(occToPriRelationshipMapping.getFromEntityId(), occToPriTableVar));
      FieldVariable criIdFieldVar =
          new FieldVariable(
              occToCriRelationshipMapping.getToEntityId(), occToCriTableVar, criteriaIdAlias);
      return new Query.Builder()
          .select(List.of(criIdFieldVar, priIdFieldVar))
          .tables(List.of(occToPriTableVar, occToCriTableVar))
          .build();
    }
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
