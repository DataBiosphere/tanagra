package bio.terra.tanagra.service.instances.filter;

import bio.terra.tanagra.query.*;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.HavingFilterVariable;
import bio.terra.tanagra.query.filtervariable.SubQueryFilterVariable;
import bio.terra.tanagra.underlay.*;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
    FieldPointer entityIdFieldPointer =
        selectEntity.getIdAttribute().getMapping(Underlay.MappingType.INDEX).getValue();

    // build a query to get all related entity instance ids:
    //   SELECT relatedEntityId FROM relatedEntityTable WHERE subFilter
    TableVariable relatedEntityTableVar =
        TableVariable.forPrimary(
            filterEntity.getMapping(Underlay.MappingType.INDEX).getTablePointer());
    List<TableVariable> relatedEntityTableVars = Lists.newArrayList(relatedEntityTableVar);

    FieldVariable relatedEntityIdFieldVar =
        filterEntity
            .getIdAttribute()
            .getMapping(Underlay.MappingType.INDEX)
            .getValue()
            .buildVariable(relatedEntityTableVar, relatedEntityTableVars);
    FilterVariable relatedEntityFilterVar =
        subFilter.getFilterVariable(relatedEntityTableVar, relatedEntityTableVars);

    Query relatedEntityQuery =
        new Query.Builder()
            .select(List.of(relatedEntityIdFieldVar))
            .tables(relatedEntityTableVars)
            .where(relatedEntityFilterVar)
            .build();
    LOGGER.info("Generated related entity query: {}", relatedEntityQuery.renderSQL());

    RelationshipMapping indexMapping = relationship.getMapping(Underlay.MappingType.INDEX);
    if (indexMapping
        .getIdPairsTable()
        .equals(selectEntity.getMapping(Underlay.MappingType.INDEX).getTablePointer())) {
      LOGGER.info("Relationship table is the same as the entity table");
      // build a filter variable for the entity table on the sub query
      //  WHERE relatedEntityId IN (SELECT relatedEntityId FROM relatedEntityTable WHERE subFilter)
      FieldVariable filterEntityIdFieldVar =
          indexMapping.getIdPairsId(filterEntity).buildVariable(entityTableVar, tableVars);
      return new SubQueryFilterVariable(
          filterEntityIdFieldVar, SubQueryFilterVariable.Operator.IN, relatedEntityQuery);
    } else if (groupByCountAttribute == null) {
      LOGGER.info("Relationship table is different from the entity table");
      // build another query to get all entity instance ids from the relationship table:
      //  SELECT fromEntityId FROM relationshipTable WHERE
      //  toEntityId IN (SELECT relatedEntityId FROM relatedEntityTable WHERE subFilter)
      TableVariable relationshipTableVar = TableVariable.forPrimary(indexMapping.getIdPairsTable());
      List<TableVariable> relationshipTableVars = Lists.newArrayList(relationshipTableVar);

      FieldVariable selectEntityIdFieldVar =
          indexMapping
              .getIdPairsId(selectEntity)
              .buildVariable(relationshipTableVar, relationshipTableVars);
      FieldVariable filterEntityIdFieldVar =
          indexMapping
              .getIdPairsId(filterEntity)
              .buildVariable(relationshipTableVar, relationshipTableVars);
      SubQueryFilterVariable relationshipFilterVar =
          new SubQueryFilterVariable(
              filterEntityIdFieldVar, SubQueryFilterVariable.Operator.IN, relatedEntityQuery);

      Query relationshipQuery =
          new Query.Builder()
              .select(List.of(selectEntityIdFieldVar))
              .tables(relationshipTableVars)
              .where(relationshipFilterVar)
              .build();
      LOGGER.info("Generated query: {}", relatedEntityQuery.renderSQL());

      // build a filter variable for the entity table on the sub query
      //  WHERE entityId IN (SELECT fromEntityId FROM relationshipTable WHERE
      //  toEntityId IN (SELECT relatedEntityId FROM relatedEntityTable WHERE subFilter))
      FieldVariable entityIdFieldVar =
          entityIdFieldPointer.buildVariable(entityTableVar, tableVars);
      return new SubQueryFilterVariable(
          entityIdFieldVar, SubQueryFilterVariable.Operator.IN, relationshipQuery);
    } else {
      LOGGER.info("Group by count on filter entity");
      // See https://github.com/DataBiosphere/tanagra/pull/350#issue-1614006284
      // Example query for "People who have > 1 occurrence date for condition 4002818":
      //   SELECT x.person_id,
      //   FROM (
      //     SELECT c.person_id, c.start_date
      //     FROM `verily-tanagra-dev.sdstatic_index_022723`.condition_occurrence AS c
      //     WHERE c.condition = 4002818
      //     GROUP BY c.person_id, c.start_date) as x
      //   GROUP BY x.person_id
      //   HAVING COUNT(*) > 1
      // The inner GROUP BY only keeps one row per [person_id, start_date].
      // The outer GROUP BY/HAVING implements "> 1 occurrence date".

      // SELECT c.person_id AS person_id, c.start_date AS start_date
      String selectEntityIdAttrName =
          relationship.getMapping(Underlay.MappingType.SOURCE).getIdPairsIdB().getColumnName();
      List<FieldVariable> selectEntityIdFieldVars =
          filterEntity
              .getAttribute(selectEntityIdAttrName)
              .getMapping(Underlay.MappingType.INDEX)
              .buildFieldVariables(relatedEntityTableVar, relatedEntityTableVars);
      List<FieldVariable> groupByCountAttrFieldVars =
          groupByCountAttribute
              .getMapping(Underlay.MappingType.INDEX)
              .buildFieldVariables(relatedEntityTableVar, relatedEntityTableVars);
      List<FieldVariable> innerSelectFieldVars =
          Stream.concat(selectEntityIdFieldVars.stream(), groupByCountAttrFieldVars.stream())
              .collect(Collectors.toList());

      // Inner SELECT query
      Query innerQuery =
          new Query.Builder()
              .select(innerSelectFieldVars)
              .tables(relatedEntityTableVars)
              .where(relatedEntityFilterVar)
              .groupBy(innerSelectFieldVars)
              .build();

      // OUTER SELECT query
      TableVariable innerQueryTableVar =
          TableVariable.forPrimary(new TablePointer.Builder().sql(innerQuery.renderSQL()).build());
      FieldPointer selectEntityIdFieldPointer =
          filterEntity
              .getAttribute(selectEntityIdAttrName)
              .getMapping(Underlay.MappingType.INDEX)
              .getFieldPointers()
              .get(0);
      // SELECT x.person_id (as opposed to SELECT c.person_id)
      FieldVariable outerSelectFieldVar =
          new FieldVariable(selectEntityIdFieldPointer, innerQueryTableVar);
      HavingFilterVariable havingFilterVar =
          new HavingFilterVariable(groupByCountOperator, groupByCountValue);
      Query outerQuery =
          new Query.Builder()
              .select(List.of(outerSelectFieldVar))
              .tables(List.of(innerQueryTableVar))
              .groupBy(List.of(outerSelectFieldVar))
              .having(havingFilterVar)
              .build();
      LOGGER.info("Generated group by count sub-query: {}", outerQuery.renderSQL());

      // build a filter variable for the entity table on the sub query
      //  WHERE entityId IN (SELECT fromEntityId FROM relationshipTable WHERE
      //  toEntityId IN (SELECT relatedEntityId FROM relatedEntityTable WHERE subFilter))
      FieldVariable entityIdFieldVar =
          entityIdFieldPointer.buildVariable(entityTableVar, tableVars);
      return new SubQueryFilterVariable(
          entityIdFieldVar, SubQueryFilterVariable.Operator.IN, outerQuery);
    }
  }
}
