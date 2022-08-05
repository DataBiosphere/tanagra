package bio.terra.tanagra.underlay.entitygroup;

import bio.terra.tanagra.indexing.WorkflowCommand;
import bio.terra.tanagra.serialization.entitygroup.UFCriteriaOccurrence;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityGroup;
import bio.terra.tanagra.underlay.RelationshipMapping;
import bio.terra.tanagra.underlay.TablePointer;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CriteriaOccurrence extends EntityGroup {
  private static final String DEFAULT_ROLLUP_COUNT_TABLE_PREFIX = "t_rollup_";

  private Entity criteriaEntity;
  private Entity occurrenceEntity;
  private Entity primaryEntity;
  private RelationshipMapping occurrenceToCriteriaRelationship;
  private RelationshipMapping occurrenceToPrimaryRelationship;
  private TablePointer rollupCountTablePointer;

  private CriteriaOccurrence(Builder builder) {
    super(builder.name, builder.indexDataPointer);
    this.criteriaEntity = builder.criteriaEntity;
    this.occurrenceEntity = builder.occurrenceEntity;
    this.primaryEntity = builder.primaryEntity;
    this.occurrenceToCriteriaRelationship = builder.occurrenceToCriteriaRelationship;
    this.occurrenceToPrimaryRelationship = builder.occurrenceToPrimaryRelationship;
    this.rollupCountTablePointer = builder.rollupCountTablePointer;
  }

  public static CriteriaOccurrence fromSerialized(
      UFCriteriaOccurrence serialized,
      Map<String, DataPointer> dataPointers,
      Map<String, Entity> entities,
      String primaryEntityName) {
    if (serialized.getIndexDataPointer() == null || serialized.getIndexDataPointer().isEmpty()) {
      throw new IllegalArgumentException("Index data pointer is undefined");
    }
    DataPointer indexDataPointer = dataPointers.get(serialized.getIndexDataPointer());
    if (indexDataPointer == null) {
      throw new IllegalArgumentException("Index data pointer not found in set of data pointers");
    }

    if (serialized.getCriteriaEntity() == null || serialized.getCriteriaEntity().isEmpty()) {
      throw new IllegalArgumentException("No criteria entity defined");
    }
    Entity criteriaEntity = entities.get(serialized.getCriteriaEntity());
    if (criteriaEntity == null) {
      throw new IllegalArgumentException("Criteria entity not found in set of entities");
    }

    if (serialized.getOccurrenceEntity() == null || serialized.getOccurrenceEntity().isEmpty()) {
      throw new IllegalArgumentException("No occurrence entity defined");
    }
    Entity occurrenceEntity = entities.get(serialized.getOccurrenceEntity());
    if (occurrenceEntity == null) {
      throw new IllegalArgumentException("Occurrence entity not found in set of entities");
    }
    Entity primaryEntity = entities.get(primaryEntityName);

    if (serialized.getOccurrenceToCriteriaRelationship() == null) {
      throw new IllegalArgumentException("No occurrence-criteria relationship defined");
    }
    RelationshipMapping occurrenceToCriteriaRelationship =
        serialized
            .getOccurrenceToCriteriaRelationship()
            .deserializeToInternal(occurrenceEntity, criteriaEntity, dataPointers);

    if (serialized.getOccurrenceToPrimaryRelationship() == null) {
      throw new IllegalArgumentException("No occurrence-primary relationship defined");
    }
    RelationshipMapping occurrenceToPrimaryRelationship =
        serialized
            .getOccurrenceToPrimaryRelationship()
            .deserializeToInternal(occurrenceEntity, primaryEntity, dataPointers);

    TablePointer rollupCountTablePointer =
        serialized.getRollupCountTablePointer() != null
            ? new TablePointer(serialized.getRollupCountTablePointer(), indexDataPointer)
            : new TablePointer(
                DEFAULT_ROLLUP_COUNT_TABLE_PREFIX + serialized.getName(), indexDataPointer);

    return new Builder()
        .setName(serialized.getName())
        .setIndexDataPointer(indexDataPointer)
        .setCriteriaEntity(criteriaEntity)
        .setOccurrenceEntity(occurrenceEntity)
        .setPrimaryEntity(primaryEntity)
        .setOccurrenceToCriteriaRelationship(occurrenceToCriteriaRelationship)
        .setOccurrenceToPrimaryRelationship(occurrenceToPrimaryRelationship)
        .setRollupCountTablePointer(rollupCountTablePointer)
        .build();
  }

  @Override
  public EntityGroup.Type getType() {
    return Type.CRITERIA_OCCURRENCE;
  }

  @Override
  public List<WorkflowCommand> getIndexingCommands() {
    return Collections
        .emptyList(); // no indexing workflows for criteria-occurrence relationships, yet
  }

  public Entity getCriteriaEntity() {
    return criteriaEntity;
  }

  public Entity getOccurrenceEntity() {
    return occurrenceEntity;
  }

  public RelationshipMapping getOccurrenceToCriteriaRelationship() {
    return occurrenceToCriteriaRelationship;
  }

  public RelationshipMapping getOccurrenceToPrimaryRelationship() {
    return occurrenceToPrimaryRelationship;
  }

  public TablePointer getRollupCountTablePointer() {
    return rollupCountTablePointer;
  }

  public static class Builder {
    private String name;
    private DataPointer indexDataPointer;
    private Entity criteriaEntity;
    private Entity occurrenceEntity;
    private Entity primaryEntity;
    private RelationshipMapping occurrenceToCriteriaRelationship;
    private RelationshipMapping occurrenceToPrimaryRelationship;
    private TablePointer rollupCountTablePointer;

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public Builder setIndexDataPointer(DataPointer indexDataPointer) {
      this.indexDataPointer = indexDataPointer;
      return this;
    }

    public Builder setCriteriaEntity(Entity criteriaEntity) {
      this.criteriaEntity = criteriaEntity;
      return this;
    }

    public Builder setOccurrenceEntity(Entity occurrenceEntity) {
      this.occurrenceEntity = occurrenceEntity;
      return this;
    }

    public Builder setPrimaryEntity(Entity primaryEntity) {
      this.primaryEntity = primaryEntity;
      return this;
    }

    public Builder setOccurrenceToCriteriaRelationship(
        RelationshipMapping occurrenceToCriteriaRelationship) {
      this.occurrenceToCriteriaRelationship = occurrenceToCriteriaRelationship;
      return this;
    }

    public Builder setOccurrenceToPrimaryRelationship(
        RelationshipMapping occurrenceToPrimaryRelationship) {
      this.occurrenceToPrimaryRelationship = occurrenceToPrimaryRelationship;
      return this;
    }

    public Builder setRollupCountTablePointer(TablePointer rollupCountTablePointer) {
      this.rollupCountTablePointer = rollupCountTablePointer;
      return this;
    }

    public CriteriaOccurrence build() {
      return new CriteriaOccurrence(this);
    }
  }
}
