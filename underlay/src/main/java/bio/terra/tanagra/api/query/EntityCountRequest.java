package bio.terra.tanagra.api.query;

import static bio.terra.tanagra.api.query.EntityInstanceCount.DEFAULT_COUNT_COLUMN_NAME;

import bio.terra.tanagra.api.query.filter.EntityFilter;
import bio.terra.tanagra.query.*;
import bio.terra.tanagra.underlay.*;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityCountRequest {
  private static final Logger LOGGER = LoggerFactory.getLogger(EntityCountRequest.class);
  private static final int DEFAULT_PAGE_SIZE = 250;

  private final Entity entity;
  private final Underlay.MappingType mappingType;
  private final List<Attribute> attributes;
  private final EntityFilter filter;
  private final PageMarker pageMarker;
  private final Integer pageSize;

  private EntityCountRequest(Builder builder) {
    this.entity = builder.entity;
    this.mappingType = builder.mappingType;
    this.attributes = builder.attributes;
    this.filter = builder.filter;
    this.pageMarker = builder.pageMarker;
    this.pageSize = builder.pageSize;
  }

  public Entity getEntity() {
    return entity;
  }

  public Underlay.MappingType getMappingType() {
    return mappingType;
  }

  public List<Attribute> getAttributes() {
    return attributes == null ? Collections.emptyList() : Collections.unmodifiableList(attributes);
  }

  public EntityFilter getFilter() {
    return filter;
  }

  public PageMarker getPageMarker() {
    return pageMarker;
  }

  public Integer getPageSize() {
    return pageSize;
  }

  public QueryRequest buildCountsQuery() {
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

    // Build the ORDER BY variables using the default direction.
    List<OrderByVariable> orderByVars =
        attributeFieldVars.stream()
            .map(attrFv -> new OrderByVariable(attrFv))
            .collect(Collectors.toList());

    Query query =
        new Query.Builder()
            .select(selectFieldVars)
            .tables(tableVars)
            .where(filterVar)
            .groupBy(attributeFieldVars)
            .orderBy(orderByVars)
            .build();
    LOGGER.info("Generated query: {}", query.renderSQL());
    return new QueryRequest(query.renderSQL(), new ColumnHeaderSchema(columnSchemas));
  }

  public static class Builder {
    private Entity entity;
    private Underlay.MappingType mappingType;
    private List<Attribute> attributes;
    private EntityFilter filter;
    private PageMarker pageMarker;
    private Integer pageSize;

    public Builder entity(Entity entity) {
      this.entity = entity;
      return this;
    }

    public Builder mappingType(Underlay.MappingType mappingType) {
      this.mappingType = mappingType;
      return this;
    }

    public Builder attributes(List<Attribute> attributes) {
      this.attributes = attributes;
      return this;
    }

    public Builder filter(EntityFilter filter) {
      this.filter = filter;
      return this;
    }

    public Builder pageMarker(PageMarker pageMarker) {
      this.pageMarker = pageMarker;
      return this;
    }

    public Builder pageSize(Integer pageSize) {
      this.pageSize = pageSize;
      return this;
    }

    public EntityCountRequest build() {
      if (pageMarker == null && pageSize == null) {
        pageSize = DEFAULT_PAGE_SIZE;
      }
      return new EntityCountRequest(this);
    }
  }
}
