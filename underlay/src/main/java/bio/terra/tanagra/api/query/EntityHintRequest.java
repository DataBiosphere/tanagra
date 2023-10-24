package bio.terra.tanagra.api.query;

import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.query.*;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.underlay.*;
import bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay2.indexschema.EntityLevelDisplayHints;
import bio.terra.tanagra.underlay2.indexschema.InstanceLevelDisplayHints;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityHintRequest {
  private static final Logger LOGGER = LoggerFactory.getLogger(EntityHintRequest.class);
  private final Entity entity;
  private final Entity relatedEntity;
  private final Literal relatedEntityId;
  private final EntityGroup entityGroup;

  private EntityHintRequest(Builder builder) {
    this.entity = builder.entity;
    this.relatedEntity = builder.relatedEntity;
    this.relatedEntityId = builder.relatedEntityId;
    this.entityGroup = builder.entityGroup;
  }

  public Entity getEntity() {
    return entity;
  }

  public Underlay.MappingType getMappingType() {
    // Hints always hit the index dataset.
    return Underlay.MappingType.INDEX;
  }

  public Entity getRelatedEntity() {
    return relatedEntity;
  }

  public Literal getRelatedEntityId() {
    return relatedEntityId;
  }

  public boolean isEntityLevelHints() {
    return relatedEntity == null;
  }

  public QueryRequest buildHintsQuery() {
    return isEntityLevelHints() ? buildEntityLevelHintsQuery() : buildInstanceLevelHintsQuery();
  }

  private QueryRequest buildEntityLevelHintsQuery() {
    TablePointer displayHintsTablePointer =
        entity.getMapping(getMappingType()).getDisplayHintTablePointer();
    TableVariable primaryTableVar = TableVariable.forPrimary(displayHintsTablePointer);
    List<TableVariable> tableVars = Lists.newArrayList(primaryTableVar);

    List<FieldVariable> selectFieldVars = new ArrayList<>();
    ColumnHeaderSchema columnHeaderSchema =
        ColumnHeaderSchema.fromColumnSchemas(EntityLevelDisplayHints.getColumns());
    columnHeaderSchema.getColumnSchemas().stream()
        .forEach(
            cs -> {
              FieldPointer fieldPointer =
                  new FieldPointer.Builder()
                      .tablePointer(displayHintsTablePointer)
                      .columnName(cs.getColumnName())
                      .build();
              selectFieldVars.add(fieldPointer.buildVariable(primaryTableVar, tableVars));
            });

    Query query = new Query.Builder().select(selectFieldVars).tables(tableVars).build();
    LOGGER.info("Generated entity-level display hint query: {}", query.renderSQL());
    return new QueryRequest(query.renderSQL(), columnHeaderSchema);
  }

  private QueryRequest buildInstanceLevelHintsQuery() {
    if (!(entityGroup instanceof CriteriaOccurrence)) {
      throw new InvalidQueryException(
          "Only CRITERIA_OCCURENCE entity groups support display hints queries");
    }
    CriteriaOccurrence criteriaOccurrence = (CriteriaOccurrence) entityGroup;
    AuxiliaryDataMapping auxDataMapping =
        criteriaOccurrence.getModifierAuxiliaryData().getMapping(getMappingType());

    TableVariable primaryTableVar = TableVariable.forPrimary(auxDataMapping.getTablePointer());
    List<TableVariable> tableVars = Lists.newArrayList(primaryTableVar);

    List<FieldVariable> selectFieldVars =
        auxDataMapping.getFieldPointers().entrySet().stream()
            .map(
                entry -> entry.getValue().buildVariable(primaryTableVar, tableVars, entry.getKey()))
            .collect(Collectors.toList());

    // Build the WHERE filter variables from the entity filter.
    FieldVariable entityIdFieldVar =
        selectFieldVars.stream()
            .filter(
                fv ->
                    fv.getAlias()
                        .equals(
                            InstanceLevelDisplayHints.Column.ENTITY_ID.getSchema().getColumnName()))
            .findFirst()
            .get();
    BinaryFilterVariable filterVar =
        new BinaryFilterVariable(
            entityIdFieldVar, BinaryFilterVariable.BinaryOperator.EQUALS, relatedEntityId);

    Query query =
        new Query.Builder().select(selectFieldVars).tables(tableVars).where(filterVar).build();
    LOGGER.info("Generated instance-level display hint query: {}", query.renderSQL());
    return new QueryRequest(
        query.renderSQL(),
        ColumnHeaderSchema.fromColumnSchemas(InstanceLevelDisplayHints.getColumns()));
  }

  public static class Builder {
    private Entity entity;
    private Entity relatedEntity;
    private Literal relatedEntityId;
    private EntityGroup entityGroup;

    public Builder entity(Entity entity) {
      this.entity = entity;
      return this;
    }

    public Builder relatedEntity(Entity relatedEntity) {
      this.relatedEntity = relatedEntity;
      return this;
    }

    public Builder relatedEntityId(Literal relatedEntityId) {
      this.relatedEntityId = relatedEntityId;
      return this;
    }

    public Builder entityGroup(EntityGroup entityGroup) {
      this.entityGroup = entityGroup;
      return this;
    }

    public EntityHintRequest build() {
      return new EntityHintRequest(this);
    }
  }
}
