package bio.terra.tanagra.service.instances;

import static bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence.*;

import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.query.*;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.service.UnderlaysService;
import bio.terra.tanagra.underlay.*;
import bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence;
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

  private EntityHintRequest(Builder builder) {
    this.entity = builder.entity;
    this.relatedEntity = builder.relatedEntity;
    this.relatedEntityId = builder.relatedEntityId;
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
    List<ColumnSchema> columnSchemas = new ArrayList<>();
    Entity.DISPLAY_HINTS_TABLE_SCHEMA.entrySet().stream()
        .forEach(
            entry -> {
              String columnName = entry.getKey();
              CellValue.SQLDataType columnDataType = entry.getValue().getKey();
              FieldPointer fieldPointer =
                  new FieldPointer.Builder()
                      .tablePointer(displayHintsTablePointer)
                      .columnName(columnName)
                      .build();
              selectFieldVars.add(fieldPointer.buildVariable(primaryTableVar, tableVars));
              columnSchemas.add(new ColumnSchema(columnName, columnDataType));
            });
    for (ColumnSchema cs : columnSchemas) {
      LOGGER.info("column schema: {}, {}", cs.getColumnName(), cs.getSqlDataType());
    }

    Query query = new Query.Builder().select(selectFieldVars).tables(tableVars).build();
    LOGGER.info("Generated entity-level display hint query: {}", query.renderSQL());
    return new QueryRequest(query.renderSQL(), new ColumnHeaderSchema(columnSchemas));
  }

  private QueryRequest buildInstanceLevelHintsQuery() {
    // TODO: Support display hints for any relationship, not just for the CRITERIA_OCCURRENCE entity
    // group.
    EntityGroup entityGroup =
        UnderlaysService.getRelationship(
                entity.getUnderlay().getEntityGroups().values(), entity, relatedEntity)
            .getEntityGroup();
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
    // TODO: Centralize this schema definition used both here and in the indexing job. Also handle
    // other enum_value data types.
    List<ColumnSchema> columnSchemas =
        List.of(
            new ColumnSchema(MODIFIER_AUX_DATA_ID_COL, CellValue.SQLDataType.INT64),
            new ColumnSchema(MODIFIER_AUX_DATA_ATTR_COL, CellValue.SQLDataType.STRING),
            new ColumnSchema(MODIFIER_AUX_DATA_MIN_COL, CellValue.SQLDataType.FLOAT),
            new ColumnSchema(MODIFIER_AUX_DATA_MAX_COL, CellValue.SQLDataType.FLOAT),
            new ColumnSchema(MODIFIER_AUX_DATA_ENUM_VAL_COL, CellValue.SQLDataType.INT64),
            new ColumnSchema(MODIFIER_AUX_DATA_ENUM_DISPLAY_COL, CellValue.SQLDataType.STRING),
            new ColumnSchema(MODIFIER_AUX_DATA_ENUM_COUNT_COL, CellValue.SQLDataType.INT64));

    // Build the WHERE filter variables from the entity filter.
    FieldVariable entityIdFieldVar =
        selectFieldVars.stream()
            .filter(fv -> fv.getAlias().equals(MODIFIER_AUX_DATA_ID_COL))
            .findFirst()
            .get();
    BinaryFilterVariable filterVar =
        new BinaryFilterVariable(
            entityIdFieldVar, BinaryFilterVariable.BinaryOperator.EQUALS, relatedEntityId);

    Query query =
        new Query.Builder().select(selectFieldVars).tables(tableVars).where(filterVar).build();
    LOGGER.info("Generated instance-level display hint query: {}", query.renderSQL());
    return new QueryRequest(query.renderSQL(), new ColumnHeaderSchema(columnSchemas));
  }

  public static class Builder {
    private Entity entity;
    private Entity relatedEntity;
    private Literal relatedEntityId;

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

    public EntityHintRequest build() {
      return new EntityHintRequest(this);
    }
  }
}
