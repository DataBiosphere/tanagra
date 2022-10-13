package bio.terra.tanagra.api;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.query.ColumnHeaderSchema;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.OrderByDirection;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.QueryRequest;
import bio.terra.tanagra.query.QueryResult;
import bio.terra.tanagra.query.RowResult;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.AttributeMapping;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityMapping;
import bio.terra.tanagra.underlay.FieldPointer;
import bio.terra.tanagra.underlay.HierarchyField;
import bio.terra.tanagra.underlay.HierarchyMapping;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class QuerysService {
  private static final Logger LOGGER = LoggerFactory.getLogger(QuerysService.class);

  public QueryRequest buildInstancesQuery(
      EntityMapping entityMapping,
      List<Attribute> selectAttributes,
      List<HierarchyField> selectHierarchyFields,
      @Nullable EntityFilter filter,
      List<Attribute> orderByAttributes,
      OrderByDirection orderByDirection,
      int limit) {
    TableVariable entityTableVar = TableVariable.forPrimary(entityMapping.getTablePointer());
    List<TableVariable> tableVars = Lists.newArrayList(entityTableVar);

    // build the SELECT field variables and column schemas from attributes
    List<FieldVariable> selectFieldVars = new ArrayList<>();
    List<ColumnSchema> columnSchemas = new ArrayList<>();
    selectAttributes.stream()
        .forEach(
            attribute -> {
              AttributeMapping attributeMapping =
                  entityMapping.getAttributeMapping(attribute.getName());
              selectFieldVars.addAll(
                  attributeMapping.buildFieldVariables(
                      entityTableVar, tableVars, attribute.getName()));
              columnSchemas.addAll(
                  attributeMapping.buildColumnSchemas(
                      attribute.getName(), attribute.getDataType()));
            });

    // build the additional SELECT field variables and column schemas from hierarchy fields
    FieldPointer entityIdFieldPointer = entityMapping.getIdAttributeMapping().getValue();
    selectHierarchyFields.stream()
        .forEach(
            hierarchyField -> {
              HierarchyMapping hierarchyMapping =
                  entityMapping.getHierarchyMapping(hierarchyField.getHierarchyName());
              selectFieldVars.add(
                  hierarchyMapping.buildFieldVariableFromEntityId(
                      hierarchyField, entityIdFieldPointer, entityTableVar, tableVars));
              columnSchemas.add(hierarchyMapping.buildColumnSchema(hierarchyField));
            });

    // build the ORDER BY field variables from attributes
    List<FieldVariable> orderByFieldVars =
        orderByAttributes.stream()
            .map(
                attribute -> {
                  AttributeMapping attributeMapping =
                      entityMapping.getAttributeMapping(attribute.getName());
                  return attributeMapping.getValue().buildVariable(entityTableVar, tableVars);
                })
            .collect(Collectors.toList());

    // build the WHERE filter variables from the entity filter
    FilterVariable filterVar =
        filter == null ? null : filter.getFilterVariable(entityTableVar, tableVars);

    Query query =
        new Query.Builder()
            .select(selectFieldVars)
            .tables(tableVars)
            .where(filterVar)
            .orderBy(orderByFieldVars)
            .orderByDirection(orderByDirection)
            .limit(limit)
            .build();
    LOGGER.info("Generated query: {}", query.renderSQL());

    return new QueryRequest(query.renderSQL(), new ColumnHeaderSchema(columnSchemas));
  }

  public List<EntityInstance> runInstancesQuery(
      EntityMapping entityMapping,
      List<Attribute> selectAttributes,
      List<HierarchyField> selectHierarchyFields,
      QueryRequest queryRequest) {
    DataPointer dataPointer = entityMapping.getTablePointer().getDataPointer();
    QueryResult queryResult = dataPointer.getQueryExecutor().execute(queryRequest);

    List<EntityInstance> instances = new ArrayList<>();
    Iterator<RowResult> rowResultsItr = queryResult.getRowResults().iterator();
    while (rowResultsItr.hasNext()) {
      instances.add(
          EntityInstance.fromRowResult(
              rowResultsItr.next(), selectAttributes, selectHierarchyFields));
    }
    return instances;
  }

  public Attribute getAttribute(Entity entity, String attributeName) {
    Attribute attribute = entity.getAttribute(attributeName);
    if (attribute == null) {
      throw new NotFoundException(
          "Attribute not found: " + entity.getName() + ", " + attributeName);
    }
    return attribute;
  }

  public HierarchyMapping getHierarchy(EntityMapping entityMapping, String hierarchyName) {
    HierarchyMapping hierarchyMapping = entityMapping.getHierarchyMappings().get(hierarchyName);
    if (hierarchyMapping == null) {
      throw new NotFoundException("Hierarchy not found: " + hierarchyName);
    }
    return hierarchyMapping;
  }
}
