package bio.terra.tanagra.api.query.filter;

import bio.terra.tanagra.query.*;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.SubQueryFilterVariable;
import bio.terra.tanagra.underlay.*;
import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RelationshipFilter extends EntityFilter {
  private static final Logger LOGGER = LoggerFactory.getLogger(RelationshipFilter.class);

  private final Entity selectEntity;
  private final Entity filterEntity;
  private final Relationship relationship;
  private final EntityFilter subFilter;
  private final @Nullable Attribute groupByCountAttribute;
  private final @Nullable BinaryFilterVariable.BinaryOperator groupByCountOperator;
  private final @Nullable Integer groupByCountValue;

  public RelationshipFilter(
      Entity selectEntity,
      Relationship relationship,
      EntityFilter subFilter,
      @Nullable Attribute groupByCountAttribute,
      @Nullable BinaryFilterVariable.BinaryOperator groupByCountOperator,
      @Nullable Integer groupByCountValue) {
    this.selectEntity = selectEntity;
    this.filterEntity =
        relationship.getEntityA().equals(selectEntity)
            ? relationship.getEntityB()
            : relationship.getEntityA();
    this.relationship = relationship;
    this.subFilter = subFilter;
    this.groupByCountAttribute = groupByCountAttribute;
    this.groupByCountOperator = groupByCountOperator;
    this.groupByCountValue = groupByCountValue;
  }

  @Override
  public FilterVariable getFilterVariable(
      TableVariable entityTableVar, List<TableVariable> tableVars) {
    RelationshipMapping indexMapping = relationship.getMapping(Underlay.MappingType.INDEX);

    // Build sub-filter variable.
    TableVariable filterEntityTableVar =
        TableVariable.forPrimary(
            filterEntity.getMapping(Underlay.MappingType.INDEX).getTablePointer());
    List<TableVariable> filterEntityTableVars = Lists.newArrayList(filterEntityTableVar);
    FilterVariable filterEntitySubFilterVar =
        subFilter.getFilterVariable(filterEntityTableVar, filterEntityTableVars);

    TablePointer idPairsTable = indexMapping.getIdPairsTable();
    boolean fkOnSelectTable =
        idPairsTable.equals(selectEntity.getMapping(Underlay.MappingType.INDEX).getTablePointer());
    boolean fkOnFilterTable =
        idPairsTable.equals(filterEntity.getMapping(Underlay.MappingType.INDEX).getTablePointer());

    if (fkOnSelectTable) {
      LOGGER.info("Foreign key on select entity table: {}", selectEntity.getName());
      return getFilterVariableForFKOnSelectEntity(
          indexMapping,
          filterEntityTableVar,
          filterEntityTableVars,
          filterEntitySubFilterVar,
          entityTableVar,
          tableVars);
    } else if (fkOnFilterTable) {
      LOGGER.info("Foreign key on filter entity table: {}", filterEntity.getName());
      return getFilterVariableForFKOnFilterEntity(
          indexMapping,
          filterEntityTableVar,
          filterEntityTableVars,
          filterEntitySubFilterVar,
          entityTableVar,
          tableVars);
    } else {
      LOGGER.info(
          "Intermediate table connects select entity ({}) and filter entity ({}) tables",
          selectEntity.getName(),
          filterEntity.getName());
      return getFilterVariableForIntermediateTable(
          indexMapping,
          filterEntityTableVar,
          filterEntityTableVars,
          filterEntitySubFilterVar,
          entityTableVar,
          tableVars);
    }

    // Handle group by count.
  }

  private SubQueryFilterVariable getFilterVariableForFKOnSelectEntity(
      RelationshipMapping indexMapping,
      TableVariable filterEntityTableVar,
      List<TableVariable> filterEntityTableVars,
      FilterVariable filterEntitySubFilterVar,
      TableVariable entityTableVar,
      List<TableVariable> tableVars) {
    // SELECT id from filterEntity WHERE [subfilter]
    FieldVariable filterEntityIdFieldVar =
        filterEntity
            .getIdAttribute()
            .getMapping(Underlay.MappingType.INDEX)
            .getValue()
            .buildVariable(filterEntityTableVar, filterEntityTableVars);
    Query filterEntitySubQuery =
        new Query.Builder()
            .select(List.of(filterEntityIdFieldVar))
            .tables(filterEntityTableVars)
            .where(filterEntitySubFilterVar)
            .build();

    // WHERE selectEntity.foreignKey IN
    //    (SELECT id FROM filterEntity WHERE [subfilter]) --> from above
    FieldVariable selectEntityFKFieldVar =
        selectEntity
            .getAttribute(indexMapping.getForeignKeyAttribute())
            .getMapping(Underlay.MappingType.INDEX)
            .getValue()
            .buildVariable(entityTableVar, tableVars);
    return new SubQueryFilterVariable(
        selectEntityFKFieldVar, SubQueryFilterVariable.Operator.IN, filterEntitySubQuery);
  }

  private FilterVariable getFilterVariableForFKOnFilterEntity(
      RelationshipMapping indexMapping,
      TableVariable filterEntityTableVar,
      List<TableVariable> filterEntityTableVars,
      FilterVariable filterEntitySubFilterVar,
      TableVariable entityTableVar,
      List<TableVariable> tableVars) {
    // SELECT foreignKey from filterEntity WHERE [subfilter]
    FieldVariable filterEntityFKFieldVar =
        filterEntity
            .getAttribute(indexMapping.getForeignKeyAttribute())
            .getMapping(Underlay.MappingType.INDEX)
            .getValue()
            .buildVariable(filterEntityTableVar, tableVars);
    Query filterEntitySubQuery =
        new Query.Builder()
            .select(List.of(filterEntityFKFieldVar))
            .tables(filterEntityTableVars)
            .where(filterEntitySubFilterVar)
            .build();

    // WHERE selectEntity.id IN
    //    (SELECT foreignKey FROM filterEntity WHERE [subfilter]) --> from above
    FieldVariable selectEntityIdFieldVar =
        selectEntity
            .getIdAttribute()
            .getMapping(Underlay.MappingType.INDEX)
            .getValue()
            .buildVariable(entityTableVar, tableVars);
    return new SubQueryFilterVariable(
        selectEntityIdFieldVar, SubQueryFilterVariable.Operator.IN, filterEntitySubQuery);
  }

  private FilterVariable getFilterVariableForIntermediateTable(
      RelationshipMapping indexMapping,
      TableVariable filterEntityTableVar,
      List<TableVariable> filterEntityTableVars,
      FilterVariable filterEntitySubFilterVar,
      TableVariable entityTableVar,
      List<TableVariable> tableVars) {
    // SELECT id from filterEntity WHERE [subfilter]
    FieldVariable filterEntityIdFieldVar =
        filterEntity
            .getIdAttribute()
            .getMapping(Underlay.MappingType.INDEX)
            .getValue()
            .buildVariable(filterEntityTableVar, filterEntityTableVars);
    Query filterEntitySubQuery =
        new Query.Builder()
            .select(List.of(filterEntityIdFieldVar))
            .tables(filterEntityTableVars)
            .where(filterEntitySubFilterVar)
            .build();

    //    SELECT selectEntityId FROM intermediateTable WHERE filterEntityId IN
    //        (SELECT id FROM filterEntity WHERE [subfilter]) --> from above
    TableVariable intermediateTableVar = TableVariable.forPrimary(indexMapping.getIdPairsTable());
    List<TableVariable> intermediateTableVars = Lists.newArrayList(intermediateTableVar);
    FieldVariable intermediateSelectEntityIdFieldVar =
        indexMapping
            .getIdPairsId(selectEntity)
            .buildVariable(intermediateTableVar, intermediateTableVars);
    FieldVariable intermediateFilterEntityIdFieldVar =
        indexMapping
            .getIdPairsId(filterEntity)
            .buildVariable(intermediateTableVar, intermediateTableVars);
    Query intermediateSubQuery =
        new Query.Builder()
            .select(List.of(intermediateSelectEntityIdFieldVar))
            .tables(intermediateTableVars)
            .where(
                new SubQueryFilterVariable(
                    intermediateFilterEntityIdFieldVar,
                    SubQueryFilterVariable.Operator.IN,
                    filterEntitySubQuery))
            .build();

    // WHERE selectEntity.id IN (
    //    SELECT selectEntityId FROM intermediateTable WHERE filterEntityId IN --> from above
    //        (SELECT id FROM filterEntity WHERE [subfilter]) --> from above
    //    )
    FieldVariable selectEntityIdFieldVar =
        selectEntity
            .getIdAttribute()
            .getMapping(Underlay.MappingType.INDEX)
            .getValue()
            .buildVariable(entityTableVar, tableVars);
    return new SubQueryFilterVariable(
        selectEntityIdFieldVar, SubQueryFilterVariable.Operator.IN, intermediateSubQuery);
  }
}
