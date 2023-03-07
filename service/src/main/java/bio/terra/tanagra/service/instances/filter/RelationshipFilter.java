package bio.terra.tanagra.service.instances.filter;

import bio.terra.tanagra.query.*;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.HavingFilterVariable;
import bio.terra.tanagra.query.filtervariable.SubQueryFilterVariable;
import bio.terra.tanagra.underlay.*;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RelationshipFilter extends EntityFilter {
  private static final Logger LOGGER = LoggerFactory.getLogger(RelationshipFilter.class);

  private final Entity selectEntity;
  private final Entity filterEntity;
  private final Relationship relationship;
  private final EntityFilter subFilter;
  private final Attribute groupByCountAttribute;
  private final BinaryFilterVariable.BinaryOperator groupByCountOperator;
  private final Literal groupByCountValue;

  public RelationshipFilter(
      Entity selectEntity,
      Relationship relationship,
      EntityFilter subFilter,
      Attribute groupByCountAttribute,
      BinaryFilterVariable.BinaryOperator groupByCountOperator,
      Literal groupByCountValue) {
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
      // Build something like:
      //      SELECT DISTINCT c.person_id
      //      FROM `verily-tanagra-dev.sdstatic_index_022723`.condition_occurrence AS c
      //      WHERE c.condition = 4002818
      //      GROUP BY c.person_id, c.start_date
      //      HAVING count(*) > 1

      // SELECT DISTINCT c.person_id
      String selectEntityIdAttrName =
          relationship.getMapping(Underlay.MappingType.SOURCE).getIdPairsIdB().getColumnName();
      List<FieldVariable> selectEntityIdFieldVars =
          filterEntity
              .getAttribute(selectEntityIdAttrName)
              .getMapping(Underlay.MappingType.INDEX)
              .buildFieldVariables(relatedEntityTableVar, relatedEntityTableVars);
      List<FieldVariable> selectEntityIdFieldVarsDistinct =
          filterEntity
              .getAttribute(selectEntityIdAttrName)
              .getMapping(Underlay.MappingType.INDEX)
              .buildFieldVariables(relatedEntityTableVar, relatedEntityTableVars);
      selectEntityIdFieldVarsDistinct.get(0).setIsDistinct(true);

      // GROUP BY c.person_id, c.start_date
      List<FieldVariable> groupByFieldVars = new ArrayList<>(selectEntityIdFieldVars);
      groupByFieldVars.addAll(
          groupByCountAttribute
              .getMapping(Underlay.MappingType.INDEX)
              .buildFieldVariables(relatedEntityTableVar, relatedEntityTableVars));

      // HAVING count(*) > 1
      HavingFilterVariable havingFilterVar =
          new HavingFilterVariable(groupByCountOperator, groupByCountValue);

      Query groupByCountQuery =
          new Query.Builder()
              .select(selectEntityIdFieldVarsDistinct)
              .tables(relatedEntityTableVars)
              .where(relatedEntityFilterVar)
              .groupBy(groupByFieldVars)
              .having(havingFilterVar)
              .build();
      LOGGER.info("Generated group by count sub-query: {}", groupByCountQuery.renderSQL());

      // build a filter variable for the entity table on the sub query
      //  WHERE entityId IN (SELECT fromEntityId FROM relationshipTable WHERE
      //  toEntityId IN (SELECT relatedEntityId FROM relatedEntityTable WHERE subFilter))
      FieldVariable entityIdFieldVar =
          entityIdFieldPointer.buildVariable(entityTableVar, tableVars);
      return new SubQueryFilterVariable(
          entityIdFieldVar, SubQueryFilterVariable.Operator.IN, groupByCountQuery);
    }
  }
}
