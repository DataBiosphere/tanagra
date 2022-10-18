package bio.terra.tanagra.api.entityfilter;

import bio.terra.tanagra.api.EntityFilter;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.query.filtervariable.SubQueryFilterVariable;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityMapping;
import bio.terra.tanagra.underlay.RelationshipMapping;
import bio.terra.tanagra.underlay.Underlay;
import com.google.common.collect.Lists;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RelationshipFilter extends EntityFilter {
  private static final Logger LOGGER = LoggerFactory.getLogger(RelationshipFilter.class);

  private final Entity relatedEntity;
  private final EntityMapping relatedEntityMapping;
  private final RelationshipMapping relationshipMapping;
  private final EntityFilter subFilter;

  public RelationshipFilter(
      Entity entity,
      Entity relatedEntity,
      EntityMapping entityMapping,
      EntityMapping relatedEntityMapping,
      RelationshipMapping relationshipMapping,
      EntityFilter subFilter) {
    super(entity, entityMapping);
    this.relatedEntity = relatedEntity;
    this.relatedEntityMapping = relatedEntityMapping;
    this.relationshipMapping = relationshipMapping;
    this.subFilter = subFilter;
  }

  @Override
  public FilterVariable getFilterVariable(
      TableVariable entityTableVar, List<TableVariable> tableVars) {
    FieldPointer entityIdFieldPointer =
        getEntity().getIdAttribute().getMapping(Underlay.MappingType.INDEX).getValue();

    // build a query to get all related entity instance ids:
    //   SELECT relatedEntityId FROM relatedEntityTable WHERE subFilter
    TableVariable relatedEntityTableVar =
        TableVariable.forPrimary(relatedEntityMapping.getTablePointer());
    List<TableVariable> relatedEntityTableVars = Lists.newArrayList(relatedEntityTableVar);

    FieldVariable relatedEntityIdFieldVar =
        relatedEntity
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
    LOGGER.info("Generated query: {}", relatedEntityQuery.renderSQL());

    if (relationshipMapping
        .getTablePointer()
        .getTableName()
        .equals(getEntityMapping().getTablePointer().getTableName())) {
      // TODO: Copy the relationship mapping into the index dataset so we're not querying across
      // datasets.
      LOGGER.info("Relationship table is the same as the entity table");
      // build a filter variable for the entity table on the sub query
      //  WHERE relatedEntityId IN (SELECT relatedEntityId FROM relatedEntityTable WHERE subFilter)
      FieldVariable toEntityIdFieldVar =
          relationshipMapping.getToEntityId().buildVariable(entityTableVar, tableVars);
      return new SubQueryFilterVariable(
          toEntityIdFieldVar, SubQueryFilterVariable.Operator.IN, relatedEntityQuery);
    } else {
      LOGGER.info("Relationship table is different from the entity table");
      // build another query to get all entity instance ids from the relationship table:
      //  SELECT fromEntityId FROM relationshipTable WHERE
      //  toEntityId IN (SELECT relatedEntityId FROM relatedEntityTable WHERE subFilter)
      TableVariable relationshipTableVar =
          TableVariable.forPrimary(relationshipMapping.getTablePointer());
      List<TableVariable> relationshipTableVars = Lists.newArrayList(relationshipTableVar);

      FieldVariable fromEntityIdFieldVar =
          relationshipMapping
              .getFromEntityId()
              .buildVariable(relationshipTableVar, relationshipTableVars);
      FieldVariable toEntityIdFieldVar =
          relationshipMapping
              .getToEntityId()
              .buildVariable(relationshipTableVar, relationshipTableVars);
      SubQueryFilterVariable relationshipFilterVar =
          new SubQueryFilterVariable(
              toEntityIdFieldVar, SubQueryFilterVariable.Operator.IN, relatedEntityQuery);

      Query relationshipQuery =
          new Query.Builder()
              .select(List.of(fromEntityIdFieldVar))
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
    }
  }
}
