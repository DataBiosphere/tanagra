package bio.terra.tanagra.api.query;

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

public class EntityQueryRequest {
  private static final Logger LOGGER = LoggerFactory.getLogger(EntityQueryRequest.class);
  private static final int DEFAULT_PAGE_SIZE = 250;

  private final Entity entity;
  private final Underlay.MappingType mappingType;
  private final List<Attribute> selectAttributes;
  private final List<HierarchyField> selectHierarchyFields;
  private final List<RelationshipField> selectRelationshipFields;
  private final EntityFilter filter;
  private final List<EntityQueryOrderBy> orderBys;
  private final Integer limit;
  private final PageMarker pageMarker;
  private final Integer pageSize;

  private EntityQueryRequest(Builder builder) {
    this.entity = builder.entity;
    this.mappingType = builder.mappingType;
    this.selectAttributes = builder.selectAttributes;
    this.selectHierarchyFields = builder.selectHierarchyFields;
    this.selectRelationshipFields = builder.selectRelationshipFields;
    this.filter = builder.filter;
    this.orderBys = builder.orderBys;
    this.limit = builder.limit;
    this.pageMarker = builder.pageMarker;
    this.pageSize = builder.pageSize;
  }

  public Entity getEntity() {
    return entity;
  }

  public Underlay.MappingType getMappingType() {
    return mappingType;
  }

  public List<Attribute> getSelectAttributes() {
    return selectAttributes == null
        ? Collections.emptyList()
        : Collections.unmodifiableList(selectAttributes);
  }

  public List<HierarchyField> getSelectHierarchyFields() {
    return selectHierarchyFields == null
        ? Collections.emptyList()
        : Collections.unmodifiableList(selectHierarchyFields);
  }

  public List<RelationshipField> getSelectRelationshipFields() {
    return selectRelationshipFields == null
        ? Collections.emptyList()
        : Collections.unmodifiableList(selectRelationshipFields);
  }

  public EntityFilter getFilter() {
    return filter;
  }

  public List<EntityQueryOrderBy> getOrderBys() {
    return orderBys == null ? Collections.emptyList() : Collections.unmodifiableList(orderBys);
  }

  public Integer getLimit() {
    return limit;
  }

  public PageMarker getPageMarker() {
    return pageMarker;
  }

  public Integer getPageSize() {
    return pageSize;
  }

  public QueryRequest buildInstancesQuery() {
    EntityMapping entityMapping = getEntity().getMapping(getMappingType());
    TableVariable entityTableVar = TableVariable.forPrimary(entityMapping.getTablePointer());
    List<TableVariable> tableVars = Lists.newArrayList(entityTableVar);

    // build the SELECT field variables and column schemas from attributes
    List<FieldVariable> selectFieldVars = new ArrayList<>();
    List<ColumnSchema> columnSchemas = new ArrayList<>();
    getSelectAttributes().stream()
        .forEach(
            attribute -> {
              AttributeMapping attributeMapping = attribute.getMapping(getMappingType());
              selectFieldVars.addAll(
                  attributeMapping.buildFieldVariables(entityTableVar, tableVars));
              columnSchemas.addAll(attributeMapping.buildColumnSchemas());
            });

    // build the additional SELECT field variables and column schemas from hierarchy fields
    getSelectHierarchyFields().stream()
        .forEach(
            hierarchyField -> {
              HierarchyMapping hierarchyMapping =
                  getEntity()
                      .getHierarchy(hierarchyField.getHierarchy().getName())
                      .getMapping(getMappingType());
              selectFieldVars.add(
                  hierarchyField.buildFieldVariableFromEntityId(
                      hierarchyMapping, entityTableVar, tableVars));
              columnSchemas.add(hierarchyField.buildColumnSchema());
            });

    // build the additional SELECT field variables and column schemas from relationship fields
    getSelectRelationshipFields().stream()
        .forEach(
            relationshipField -> {
              RelationshipMapping relationshipMapping =
                  relationshipField.getRelationship().getMapping(getMappingType());
              selectFieldVars.add(
                  relationshipField.buildFieldVariableFromEntityId(
                      relationshipMapping, entityTableVar, tableVars));
              columnSchemas.add(relationshipField.buildColumnSchema());
            });

    // build the ORDER BY field variables from attributes and relationship fields
    List<OrderByVariable> orderByVars =
        getOrderBys().stream()
            .map(
                entityOrderBy -> {
                  FieldVariable fieldVar;
                  if (entityOrderBy.isByAttribute()) {
                    fieldVar =
                        entityOrderBy
                            .getAttribute()
                            .getMapping(getMappingType())
                            .buildValueFieldVariable(entityTableVar, tableVars);
                  } else {
                    RelationshipMapping relationshipMapping =
                        entityOrderBy
                            .getRelationshipField()
                            .getRelationship()
                            .getMapping(getMappingType());
                    fieldVar =
                        entityOrderBy
                            .getRelationshipField()
                            .buildFieldVariableFromEntityId(
                                relationshipMapping, entityTableVar, tableVars);
                  }
                  return new OrderByVariable(fieldVar, entityOrderBy.getDirection());
                })
            .collect(Collectors.toList());

    // build the WHERE filter variables from the entity filter
    FilterVariable filterVar =
        getFilter() == null ? null : getFilter().getFilterVariable(entityTableVar, tableVars);

    Query query =
        new Query.Builder()
            .select(selectFieldVars)
            .tables(tableVars)
            .where(filterVar)
            .orderBy(orderByVars)
            .limit(getLimit())
            .build();
    LOGGER.info("Generated query: {}", query.renderSQL());
    return new QueryRequest(
        query.renderSQL(), new ColumnHeaderSchema(columnSchemas), getPageMarker(), getPageSize());
  }

  public static class Builder {
    private Entity entity;
    private Underlay.MappingType mappingType;
    private List<Attribute> selectAttributes;
    private List<HierarchyField> selectHierarchyFields;
    private List<RelationshipField> selectRelationshipFields;
    private EntityFilter filter;
    private List<EntityQueryOrderBy> orderBys;
    private Integer limit;
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

    public Builder selectAttributes(List<Attribute> selectAttributes) {
      this.selectAttributes = selectAttributes;
      return this;
    }

    public Builder selectHierarchyFields(List<HierarchyField> selectHierarchyFields) {
      this.selectHierarchyFields = selectHierarchyFields;
      return this;
    }

    public Builder selectRelationshipFields(List<RelationshipField> selectRelationshipFields) {
      this.selectRelationshipFields = selectRelationshipFields;
      return this;
    }

    public Builder filter(EntityFilter filter) {
      this.filter = filter;
      return this;
    }

    public Builder orderBys(List<EntityQueryOrderBy> orderBys) {
      this.orderBys = orderBys;
      return this;
    }

    public Builder limit(Integer limit) {
      this.limit = limit;
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

    public EntityQueryRequest build() {
      if (pageMarker == null && pageSize == null) {
        pageSize = DEFAULT_PAGE_SIZE;
      }
      return new EntityQueryRequest(this);
    }
  }
}
