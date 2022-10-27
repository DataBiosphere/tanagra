package bio.terra.tanagra.api;

import static bio.terra.tanagra.api.EntityInstanceCount.DEFAULT_COUNT_COLUMN_NAME;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnHeaderSchema;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.QueryRequest;
import bio.terra.tanagra.query.QueryResult;
import bio.terra.tanagra.query.RowResult;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.AttributeMapping;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityGroup;
import bio.terra.tanagra.underlay.EntityMapping;
import bio.terra.tanagra.underlay.Hierarchy;
import bio.terra.tanagra.underlay.HierarchyField;
import bio.terra.tanagra.underlay.HierarchyMapping;
import bio.terra.tanagra.underlay.Relationship;
import bio.terra.tanagra.underlay.RelationshipField;
import bio.terra.tanagra.underlay.RelationshipMapping;
import bio.terra.tanagra.underlay.Underlay;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class QuerysService {
  private static final Logger LOGGER = LoggerFactory.getLogger(QuerysService.class);

  public QueryRequest buildInstancesQuery(EntityQueryRequest entityQueryRequest) {
    EntityMapping entityMapping =
        entityQueryRequest.getEntity().getMapping(entityQueryRequest.getMappingType());
    TableVariable entityTableVar = TableVariable.forPrimary(entityMapping.getTablePointer());
    List<TableVariable> tableVars = Lists.newArrayList(entityTableVar);

    // build the SELECT field variables and column schemas from attributes
    List<FieldVariable> selectFieldVars = new ArrayList<>();
    List<ColumnSchema> columnSchemas = new ArrayList<>();
    entityQueryRequest.getSelectAttributes().stream()
        .forEach(
            attribute -> {
              AttributeMapping attributeMapping =
                  attribute.getMapping(entityQueryRequest.getMappingType());
              selectFieldVars.addAll(
                  attributeMapping.buildFieldVariables(entityTableVar, tableVars));
              columnSchemas.addAll(attributeMapping.buildColumnSchemas());
            });

    // build the additional SELECT field variables and column schemas from hierarchy fields
    entityQueryRequest.getSelectHierarchyFields().stream()
        .forEach(
            hierarchyField -> {
              HierarchyMapping hierarchyMapping =
                  entityQueryRequest
                      .getEntity()
                      .getHierarchy(hierarchyField.getHierarchy().getName())
                      .getMapping(entityQueryRequest.getMappingType());
              selectFieldVars.add(
                  hierarchyField.buildFieldVariableFromEntityId(
                      hierarchyMapping, entityTableVar, tableVars));
              columnSchemas.add(hierarchyField.buildColumnSchema());
            });

    // build the additional SELECT field variables and column schemas from relationship fields
    entityQueryRequest.getSelectRelationshipFields().stream()
        .forEach(
            relationshipField -> {
              RelationshipMapping relationshipMapping =
                  relationshipField
                      .getRelationship()
                      .getMapping(entityQueryRequest.getMappingType());
              selectFieldVars.add(
                  relationshipField.buildFieldVariableFromEntityId(
                      relationshipMapping, entityTableVar, tableVars));
              columnSchemas.add(relationshipField.buildColumnSchema());
            });

    // build the ORDER BY field variables from attributes
    List<FieldVariable> orderByFieldVars =
        entityQueryRequest.getOrderByAttributes().stream()
            .map(
                attribute ->
                    attribute
                        .getMapping(entityQueryRequest.getMappingType())
                        .getValue()
                        .buildVariable(entityTableVar, tableVars))
            .collect(Collectors.toList());

    // build the WHERE filter variables from the entity filter
    FilterVariable filterVar =
        entityQueryRequest.getFilter() == null
            ? null
            : entityQueryRequest.getFilter().getFilterVariable(entityTableVar, tableVars);

    Query query =
        new Query.Builder()
            .select(selectFieldVars)
            .tables(tableVars)
            .where(filterVar)
            .orderBy(orderByFieldVars)
            .orderByDirection(entityQueryRequest.getOrderByDirection())
            .limit(entityQueryRequest.getLimit())
            .build();
    LOGGER.info("Generated query: {}", query.renderSQL());

    return new QueryRequest(query.renderSQL(), new ColumnHeaderSchema(columnSchemas));
  }

  public List<EntityInstance> runInstancesQuery(
      EntityMapping entityMapping,
      List<Attribute> selectAttributes,
      List<HierarchyField> selectHierarchyFields,
      List<RelationshipField> selectRelationshipFields,
      QueryRequest queryRequest) {
    DataPointer dataPointer = entityMapping.getTablePointer().getDataPointer();
    QueryResult queryResult = dataPointer.getQueryExecutor().execute(queryRequest);

    List<EntityInstance> instances = new ArrayList<>();
    Iterator<RowResult> rowResultsItr = queryResult.getRowResults().iterator();
    while (rowResultsItr.hasNext()) {
      instances.add(
          EntityInstance.fromRowResult(
              rowResultsItr.next(),
              selectAttributes,
              selectHierarchyFields,
              selectRelationshipFields));
    }
    return instances;
  }

  public QueryRequest buildInstanceCountsQuery(
      Entity entity,
      Underlay.MappingType mappingType,
      List<Attribute> attributes,
      @Nullable EntityFilter filter) {
    TableVariable entityTableVar =
        TableVariable.forPrimary(entity.getMapping(mappingType).getTablePointer());
    List<TableVariable> tableVars = Lists.newArrayList(entityTableVar);

    // Use the same attributes for SELECT, GROUP BY, and ORDER BY.
    // Build the field variables and column schemas from attributes.
    List<FieldVariable> attributeFieldVars = new ArrayList<>();
    List<ColumnSchema> columnSchemas = new ArrayList<>();
    attributes.stream()
        .forEach(
            attribute -> {
              AttributeMapping attributeMapping = attribute.getMapping(Underlay.MappingType.INDEX);
              attributeFieldVars.addAll(
                  attributeMapping.buildFieldVariables(entityTableVar, tableVars));
              columnSchemas.addAll(attributeMapping.buildColumnSchemas());
            });

    // Additionally, build a count field variable and column schema to SELECT.
    List<FieldVariable> selectFieldVars = new ArrayList<>(attributeFieldVars);
    FieldPointer entityIdFieldPointer =
        entity.getIdAttribute().getMapping(Underlay.MappingType.INDEX).getValue();
    FieldPointer countFieldPointer =
        new FieldPointer.Builder()
            .tablePointer(entityIdFieldPointer.getTablePointer())
            .columnName(entityIdFieldPointer.getColumnName())
            .sqlFunctionWrapper("COUNT")
            .build();
    selectFieldVars.add(
        countFieldPointer.buildVariable(entityTableVar, tableVars, DEFAULT_COUNT_COLUMN_NAME));
    columnSchemas.add(new ColumnSchema(DEFAULT_COUNT_COLUMN_NAME, CellValue.SQLDataType.INT64));

    // Build the WHERE filter variables from the entity filter.
    FilterVariable filterVar =
        filter == null ? null : filter.getFilterVariable(entityTableVar, tableVars);

    Query query =
        new Query.Builder()
            .select(selectFieldVars)
            .tables(tableVars)
            .where(filterVar)
            .groupBy(attributeFieldVars)
            .orderBy(attributeFieldVars)
            .build();
    LOGGER.info("Generated query: {}", query.renderSQL());

    return new QueryRequest(query.renderSQL(), new ColumnHeaderSchema(columnSchemas));
  }

  public List<EntityInstanceCount> runInstanceCountsQuery(
      DataPointer dataPointer, List<Attribute> attributes, QueryRequest queryRequest) {
    QueryResult queryResult = dataPointer.getQueryExecutor().execute(queryRequest);

    List<EntityInstanceCount> instanceCounts = new ArrayList<>();
    Iterator<RowResult> rowResultsItr = queryResult.getRowResults().iterator();
    while (rowResultsItr.hasNext()) {
      instanceCounts.add(EntityInstanceCount.fromRowResult(rowResultsItr.next(), attributes));
    }
    return instanceCounts;
  }

  public Attribute getAttribute(Entity entity, String attributeName) {
    Attribute attribute = entity.getAttribute(attributeName);
    if (attribute == null) {
      throw new NotFoundException(
          "Attribute not found: " + entity.getName() + ", " + attributeName);
    }
    return attribute;
  }

  public Hierarchy getHierarchy(Entity entity, String hierarchyName) {
    Hierarchy hierarchy = entity.getHierarchy(hierarchyName);
    if (hierarchy == null) {
      throw new NotFoundException("Hierarchy not found: " + hierarchyName);
    }
    return hierarchy;
  }

  public Relationship getRelationship(
      Collection<EntityGroup> entityGroups, Entity entity, Entity relatedEntity) {
    for (EntityGroup entityGroup : entityGroups) {
      Optional<Relationship> relationship = entityGroup.getRelationship(entity, relatedEntity);
      if (relationship.isPresent()) {
        return relationship.get();
      }
    }
    throw new NotFoundException(
        "Relationship not found for entities: "
            + entity.getName()
            + " -> "
            + relatedEntity.getName());
  }
}
