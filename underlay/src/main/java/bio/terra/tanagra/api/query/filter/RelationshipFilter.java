package bio.terra.tanagra.api.query.filter;

import bio.terra.tanagra.query.*;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.HavingFilterVariable;
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

    // Build where field variable and filter entity sub-query.
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

    FieldVariable whereField;
    FieldVariable selectEntityIdInSubQuery;
    Query.Builder filterEntitySubQuery;
    if (fkOnSelectTable) {
      LOGGER.trace("Foreign key on select entity table: {}", selectEntity.getName());
      // SELECT id from filterEntity WHERE [subfilter]
      FieldVariable filterEntityIdFieldVar =
          filterEntity
              .getIdAttribute()
              .getMapping(Underlay.MappingType.INDEX)
              .getValue()
              .buildVariable(filterEntityTableVar, filterEntityTableVars);
      filterEntitySubQuery =
          new Query.Builder()
              .select(List.of(filterEntityIdFieldVar))
              .tables(filterEntityTableVars)
              .where(filterEntitySubFilterVar);

      // WHERE selectEntity.foreignKey IN
      //    (SELECT id FROM filterEntity WHERE [subfilter]) --> from above
      FieldVariable selectEntityFKFieldVar =
          selectEntity
              .getAttribute(indexMapping.getForeignKeyAttribute())
              .getMapping(Underlay.MappingType.INDEX)
              .getValue()
              .buildVariable(entityTableVar, tableVars);
      whereField = selectEntityFKFieldVar;
      selectEntityIdInSubQuery = filterEntityIdFieldVar;
    } else if (fkOnFilterTable) {
      LOGGER.trace("Foreign key on filter entity table: {}", filterEntity.getName());
      // SELECT foreignKey from filterEntity WHERE [subfilter]
      FieldVariable filterEntityFKFieldVar =
          filterEntity
              .getAttribute(indexMapping.getForeignKeyAttribute())
              .getMapping(Underlay.MappingType.INDEX)
              .getValue()
              .buildVariable(filterEntityTableVar, tableVars);
      filterEntitySubQuery =
          new Query.Builder()
              .select(List.of(filterEntityFKFieldVar))
              .tables(filterEntityTableVars)
              .where(filterEntitySubFilterVar);

      // WHERE selectEntity.id IN
      //    (SELECT foreignKey FROM filterEntity WHERE [subfilter]) --> from above
      FieldVariable selectEntityIdFieldVar =
          selectEntity
              .getIdAttribute()
              .getMapping(Underlay.MappingType.INDEX)
              .getValue()
              .buildVariable(entityTableVar, tableVars);
      whereField = selectEntityIdFieldVar;
      selectEntityIdInSubQuery = filterEntityFKFieldVar;
    } else {
      LOGGER.trace(
          "Intermediate table connects select entity ({}) and filter entity ({}) tables",
          selectEntity.getName(),
          filterEntity.getName());
      // SELECT id from filterEntity WHERE [subfilter]
      FieldVariable filterEntityIdFieldVar =
          filterEntity
              .getIdAttribute()
              .getMapping(Underlay.MappingType.INDEX)
              .getValue()
              .buildVariable(filterEntityTableVar, filterEntityTableVars);
      Query innerFilterEntitySubQuery =
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
      Query.Builder intermediateSubQuery =
          new Query.Builder()
              .select(List.of(intermediateSelectEntityIdFieldVar))
              .tables(intermediateTableVars)
              .where(
                  new SubQueryFilterVariable(
                      intermediateFilterEntityIdFieldVar,
                      SubQueryFilterVariable.Operator.IN,
                      innerFilterEntitySubQuery));

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
      whereField = selectEntityIdFieldVar;
      selectEntityIdInSubQuery = intermediateSelectEntityIdFieldVar;
      filterEntitySubQuery = intermediateSubQuery;
    }

    boolean hasGroupByFilter = groupByCountOperator != null && groupByCountValue != null;
    if (!hasGroupByFilter) {
      return new SubQueryFilterVariable(
          whereField, SubQueryFilterVariable.Operator.IN, filterEntitySubQuery.build());
    } else {
      Query.Builder innerQueryBuilder = filterEntitySubQuery;

      // SELECT selectEntityId, groupByAttributes
      // FROM filterEntity WHERE [subfilter]
      // GROUP BY selectEntityId, groupByAttributes)
      if (groupByCountAttribute != null) {
        FieldVariable groupByCountAttrFieldVar =
            groupByCountAttribute
                .getMapping(Underlay.MappingType.INDEX)
                .getValue()
                .buildVariable(filterEntityTableVar, filterEntityTableVars);
        innerQueryBuilder.addSelect(groupByCountAttrFieldVar);
      }
      Query innerQuery = innerQueryBuilder.groupBy(filterEntitySubQuery.getSelect()).build();

      // SELECT selectEntityId FROM
      //    (SELECT selectEntityId, groupByAttribute    --> from above
      //     FROM filterEntity WHERE [subfilter]         --> from above
      //     GROUP BY selectEntityId, groupByAttribute)  --> from above
      // GROUP BY selectEntityId HAVING COUNT(*) > countValue)
      TablePointer innerQueryTempTable =
          new TablePointer.Builder().sql(innerQuery.renderSQL()).build();
      TableVariable innerQueryTableVar = TableVariable.forPrimary(innerQueryTempTable);
      FieldPointer outerSelectEntityIdField =
          new FieldPointer.Builder()
              .tablePointer(innerQueryTempTable)
              .columnName(selectEntityIdInSubQuery.getAliasOrColumnName())
              .build();
      FieldVariable outerSelectEntityIdFieldVar =
          new FieldVariable(outerSelectEntityIdField, innerQueryTableVar);
      Query outerQuery =
          new Query.Builder()
              .select(List.of(outerSelectEntityIdFieldVar))
              .tables(List.of(innerQueryTableVar))
              .groupBy(List.of(outerSelectEntityIdFieldVar))
              .having(new HavingFilterVariable(groupByCountOperator, groupByCountValue))
              .build();

      // WHERE whereField IN
      //    (SELECT selectEntityId FROM                           --> from above
      //          (SELECT selectEntityId, groupByAttribute        --> from above
      //          FROM filterEntity WHERE [subfilter]             --> from above
      //          GROUP BY selectEntityId, groupByAttribute)      --> from above
      //    GROUP BY selectEntityId HAVING COUNT(*) > countValue) --> from above
      return new SubQueryFilterVariable(whereField, SubQueryFilterVariable.Operator.IN, outerQuery);
    }
  }
}
