package bio.terra.tanagra.api2.filter;

import bio.terra.tanagra.query.*;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.HavingFilterVariable;
import bio.terra.tanagra.query.filtervariable.SubQueryFilterVariable;
import bio.terra.tanagra.underlay2.Attribute;
import bio.terra.tanagra.underlay2.Entity;
import bio.terra.tanagra.underlay2.Relationship;
import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.lang3.NotImplementedException;
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
    // Build where field variable and filter entity sub-query.
    TableVariable filterEntityTableVar =
        TableVariable.forPrimary(filterEntity.getIndexEntityTable());
    List<TableVariable> filterEntityTableVars = Lists.newArrayList(filterEntityTableVar);
    FilterVariable filterEntitySubFilterVar =
        subFilter.getFilterVariable(filterEntityTableVar, filterEntityTableVars);

    TablePointer idPairsTable = relationship.getIndexIdPairsTable();
    boolean fkOnSelectTable = idPairsTable.equals(selectEntity.getIndexEntityTable());
    boolean fkOnFilterTable = idPairsTable.equals(filterEntity.getIndexEntityTable());

    FieldVariable whereField;
    FieldVariable selectEntityIdInSubQuery;
    Query.Builder filterEntitySubQuery;
    if (fkOnSelectTable) {
      LOGGER.trace("Foreign key on select entity table: {}", selectEntity.getName());
      // SELECT id from filterEntity WHERE [subfilter]
      FieldVariable filterEntityIdFieldVar =
          filterEntity
              .getIdAttribute()
              .getIndexValueField()
              .buildVariable(filterEntityTableVar, filterEntityTableVars);
      filterEntitySubQuery =
          new Query.Builder()
              .select(List.of(filterEntityIdFieldVar))
              .tables(filterEntityTableVars)
              .where(filterEntitySubFilterVar);

      // WHERE selectEntity.foreignKey IN
      //    (SELECT id FROM filterEntity WHERE [subfilter]) --> from above
      FieldVariable selectEntityFKFieldVar =
          relationship
              .getIndexEntityIdField(filterEntity.getName())
              .buildVariable(entityTableVar, tableVars);
      whereField = selectEntityFKFieldVar;
      selectEntityIdInSubQuery = filterEntityIdFieldVar;
    } else if (fkOnFilterTable) {
      LOGGER.trace("Foreign key on filter entity table: {}", filterEntity.getName());
      // SELECT foreignKey from filterEntity WHERE [subfilter]
      FieldVariable filterEntityFKFieldVar =
          relationship
              .getIndexEntityIdField(selectEntity.getName())
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
              .getIndexValueField()
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
              .getIndexValueField()
              .buildVariable(filterEntityTableVar, filterEntityTableVars);
      Query innerFilterEntitySubQuery =
          new Query.Builder()
              .select(List.of(filterEntityIdFieldVar))
              .tables(filterEntityTableVars)
              .where(filterEntitySubFilterVar)
              .build();

      //    SELECT selectEntityId FROM intermediateTable WHERE filterEntityId IN
      //        (SELECT id FROM filterEntity WHERE [subfilter]) --> from above
      TableVariable intermediateTableVar =
          TableVariable.forPrimary(relationship.getIndexIdPairsTable());
      List<TableVariable> intermediateTableVars = Lists.newArrayList(intermediateTableVar);
      FieldVariable intermediateSelectEntityIdFieldVar =
          relationship
              .getIndexEntityIdField(selectEntity.getName())
              .buildVariable(intermediateTableVar, intermediateTableVars);
      FieldVariable intermediateFilterEntityIdFieldVar =
          relationship
              .getIndexEntityIdField(filterEntity.getName())
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
              .getIndexValueField()
              .buildVariable(entityTableVar, tableVars);
      whereField = selectEntityIdFieldVar;
      selectEntityIdInSubQuery = intermediateSelectEntityIdFieldVar;
      filterEntitySubQuery = intermediateSubQuery;
    }

    boolean hasGroupByFilter = groupByCountOperator != null && groupByCountValue != null;
    if (!hasGroupByFilter) {
      return new SubQueryFilterVariable(
          whereField, SubQueryFilterVariable.Operator.IN, filterEntitySubQuery.build());
    }

    Query.Builder innerQueryBuilder;
    if (groupByCountAttribute == null) {
      // We're only grouping by the select entity id, which is already included in the filter entity
      // sub-query, so no need to add anything.

      // SELECT selectEntityId
      // FROM filterEntity WHERE [subfilter]
      // GROUP BY selectEntityId
      innerQueryBuilder = filterEntitySubQuery;
    } else {
      // We're grouping by the select entity id (e.g. person_id) and another attribute on the filter
      // entity (e.g. start_date), so we need to add the other attribute to the inner query.
      if (fkOnSelectTable || fkOnFilterTable) {
        // Since we're already selecting the select entity id from filterEntity, just select an
        // additional field from the same table.

        // SELECT selectEntityId, groupByAttribute
        // FROM filterEntity WHERE [subfilter]
        // GROUP BY selectEntityId, groupByAttribute
        innerQueryBuilder = filterEntitySubQuery;
        FieldVariable groupByCountAttrFieldVar =
            groupByCountAttribute
                .getIndexValueField()
                .buildVariable(filterEntityTableVar, filterEntityTableVars);
        innerQueryBuilder.addSelect(groupByCountAttrFieldVar);
      } else {
        // Since we're selecting from an intermediate table, we need to add a sub-select on the
        // filter entity to get the group by attribute.
        if (relationship.getIndexEntityIdField(filterEntity.getName()).isForeignKey()) {
          throw new NotImplementedException(
              "Group by attribute is only supported for intermediate id pairs tables where both ids are inline (i.e. no foreign key to another table)");
        }
        if (groupByCountAttribute.getIndexValueField().isForeignKey()) {
          throw new NotImplementedException(
              "Group by attribute is only supported for inline attributes (i.e. no foreign key to another table).");
        }

        //  SELECT selectEntityId, (SELECT groupByAttribute FROM filterEntity WHERE
        // id=filterEntityId) AS groupByAttribute
        //  FROM intermediateTable WHERE filterEntityId IN
        //      (SELECT id FROM filterEntity WHERE [subfilter])
        // GROUP BY selectEntityId, groupByAttribute
        innerQueryBuilder = filterEntitySubQuery;
        FieldPointer groupByCountAttrValueField =
            new FieldPointer.Builder()
                .tablePointer(relationship.getIndexIdPairsTable())
                .columnName(
                    relationship.getIndexEntityIdField(filterEntity.getName()).getColumnName())
                .foreignTablePointer(filterEntity.getIndexEntityTable())
                .foreignKeyColumnName(
                    filterEntity.getIdAttribute().getIndexValueField().getColumnName())
                .foreignColumnName(groupByCountAttribute.getIndexValueField().getColumnName())
                .sqlFunctionWrapper(
                    groupByCountAttribute.getIndexValueField().getSqlFunctionWrapper())
                .build();
        TableVariable intermediateTableVar = selectEntityIdInSubQuery.getTableVariable();
        FieldVariable groupByCountAttrValueFieldVar =
            groupByCountAttrValueField.buildVariable(
                intermediateTableVar, filterEntitySubQuery.getTables());
        innerQueryBuilder.addSelect(groupByCountAttrValueFieldVar);
      }
    }
    // Group by all the select fields.
    Query innerQuery = innerQueryBuilder.groupBy(innerQueryBuilder.getSelect()).build();

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
