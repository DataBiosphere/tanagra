package bio.terra.tanagra.api;

import bio.terra.tanagra.query.OrderByDirection;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.HierarchyField;
import bio.terra.tanagra.underlay.RelationshipField;
import bio.terra.tanagra.underlay.Underlay;
import java.util.Collections;
import java.util.List;

public class EntityQueryRequest {
  private final Entity entity;
  private final Underlay.MappingType mappingType;
  private final List<Attribute> selectAttributes;
  private final List<HierarchyField> selectHierarchyFields;
  private final List<RelationshipField> selectRelationshipFields;
  private final EntityFilter filter;
  private final List<Attribute> orderByAttributes;
  private final OrderByDirection orderByDirection;
  private final int limit;

  private EntityQueryRequest(Builder builder) {
    this.entity = builder.entity;
    this.mappingType = builder.mappingType;
    this.selectAttributes = builder.selectAttributes;
    this.selectHierarchyFields = builder.selectHierarchyFields;
    this.selectRelationshipFields = builder.selectRelationshipFields;
    this.filter = builder.filter;
    this.orderByAttributes = builder.orderByAttributes;
    this.orderByDirection = builder.orderByDirection;
    this.limit = builder.limit;
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

  public List<Attribute> getOrderByAttributes() {
    return orderByAttributes == null
        ? Collections.emptyList()
        : Collections.unmodifiableList(orderByAttributes);
  }

  public OrderByDirection getOrderByDirection() {
    return orderByDirection;
  }

  public int getLimit() {
    return limit;
  }

  public static class Builder {
    private Entity entity;
    private Underlay.MappingType mappingType;
    private List<Attribute> selectAttributes;
    private List<HierarchyField> selectHierarchyFields;
    private List<RelationshipField> selectRelationshipFields;
    private EntityFilter filter;
    private List<Attribute> orderByAttributes;
    private OrderByDirection orderByDirection;
    private int limit;

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

    public Builder orderByAttributes(List<Attribute> orderByAttributes) {
      this.orderByAttributes = orderByAttributes;
      return this;
    }

    public Builder orderByDirection(OrderByDirection orderByDirection) {
      this.orderByDirection = orderByDirection;
      return this;
    }

    public Builder limit(int limit) {
      this.limit = limit;
      return this;
    }

    public EntityQueryRequest build() {
      return new EntityQueryRequest(this);
    }
  }
}
