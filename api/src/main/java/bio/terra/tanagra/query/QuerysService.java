package bio.terra.tanagra.query;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.AttributeMapping;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityMapping;
import bio.terra.tanagra.underlay.Literal;
import bio.terra.tanagra.underlay.ValueDisplay;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class QuerysService {
  private static final Logger LOGGER = LoggerFactory.getLogger(QuerysService.class);

  public QueryRequest buildInstancesQuery(
      Entity entity,
      List<Attribute> selectAttributes,
      List<Attribute> orderByAttributes,
      OrderByDirection orderByDirection,
      int limit) {
    // TODO: Allow building queries against the source data mapping also.
    EntityMapping entityMapping = entity.getIndexDataMapping();

    TableVariable entityTableVar = TableVariable.forPrimary(entityMapping.getTablePointer());
    List<TableVariable> tableVars = List.of(entityTableVar);

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

    Query query =
        new Query.Builder()
            .select(selectFieldVars)
            .tables(tableVars)
            .orderBy(orderByFieldVars)
            .orderByDirection(orderByDirection)
            .limit(limit)
            .build();
    LOGGER.info("Generated query: {}", query.renderSQL());
    
    return new QueryRequest(query.renderSQL(), new ColumnHeaderSchema(columnSchemas));
  }

  public List<Map<String, ValueDisplay>> runInstancesQuery(
      Entity entity, List<Attribute> selectAttributes, QueryRequest queryRequest) {
    // TODO: Allow running queries against the source data mapping also.
    DataPointer dataPointer = entity.getIndexDataMapping().getTablePointer().getDataPointer();
    QueryResult queryResult = dataPointer.getQueryExecutor().execute(queryRequest);

    List<Map<String, ValueDisplay>> instances = new ArrayList<>();
    Iterator<RowResult> rowResultsItr = queryResult.getRowResults().iterator();
    while (rowResultsItr.hasNext()) {
      RowResult rowResult = rowResultsItr.next();

      Map<String, ValueDisplay> instance = new HashMap<>();
      for (Attribute selectAttribute : selectAttributes) {
        Literal value = rowResult.get(selectAttribute.getName()).getLiteral();
        switch (selectAttribute.getType()) {
          case SIMPLE:
            instance.put(selectAttribute.getName(), new ValueDisplay(value));
            break;
          case KEY_AND_DISPLAY:
            String display =
                rowResult
                    .get(AttributeMapping.getDisplayMappingAlias(selectAttribute.getName()))
                    .getString()
                    .get();
            instance.put(selectAttribute.getName(), new ValueDisplay(value, display));
            break;
          default:
            throw new SystemException("Unknown attribute type: " + selectAttribute.getType());
        }
      }
      instances.add(instance);
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
}
