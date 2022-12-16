package bio.terra.tanagra.service.instances;

import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.service.artifact.AnnotationValue;
import bio.terra.tanagra.service.instances.filter.AnnotationFilter;
import bio.terra.tanagra.service.instances.filter.EntityFilter;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Underlay;
import java.util.Collections;
import java.util.List;

public class ReviewQueryRequest {
  private final Entity entity;
  private final Underlay.MappingType mappingType;
  private final List<Attribute> attributes;
  private final EntityFilter entityFilter;
  private final AnnotationFilter annotationFilter;
  private final List<Literal> entityInstanceIds;
  private final List<AnnotationValue> annotationValues;
  private final List<ReviewQueryOrderBy> orderBys;

  private ReviewQueryRequest(Builder builder) {
    this.entity = builder.entity;
    this.mappingType = builder.mappingType;
    this.attributes = builder.attributes;
    this.entityFilter = builder.entityFilter;
    this.annotationFilter = builder.annotationFilter;
    this.entityInstanceIds = builder.entityInstanceIds;
    this.annotationValues = builder.annotationValues;
    this.orderBys = builder.orderBys;
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

  public EntityFilter getEntityFilter() {
    return entityFilter;
  }

  public AnnotationFilter getAnnotationFilter() {
    return annotationFilter;
  }

  public boolean hasAnnotationFilter() {
    return annotationFilter != null;
  }

  public List<Literal> getEntityInstanceIds() {
    return Collections.unmodifiableList(entityInstanceIds);
  }

  public List<AnnotationValue> getAnnotationValues() {
    return Collections.unmodifiableList(annotationValues);
  }

  public List<ReviewQueryOrderBy> getOrderBys() {
    return orderBys == null ? Collections.emptyList() : Collections.unmodifiableList(orderBys);
  }

  public static class Builder {
    private Entity entity;
    private Underlay.MappingType mappingType;
    private List<Attribute> attributes;
    private EntityFilter entityFilter;
    private AnnotationFilter annotationFilter;
    private List<Literal> entityInstanceIds;
    private List<AnnotationValue> annotationValues;
    private List<ReviewQueryOrderBy> orderBys;

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

    public Builder entityFilter(EntityFilter entityFilter) {
      this.entityFilter = entityFilter;
      return this;
    }

    public Builder annotationFilter(AnnotationFilter annotationFilter) {
      this.annotationFilter = annotationFilter;
      return this;
    }

    public Builder entityInstanceIds(List<Literal> entityInstanceIds) {
      this.entityInstanceIds = entityInstanceIds;
      return this;
    }

    public Builder annotationValues(List<AnnotationValue> annotationValues) {
      this.annotationValues = annotationValues;
      return this;
    }

    public Builder orderBys(List<ReviewQueryOrderBy> orderBys) {
      this.orderBys = orderBys;
      return this;
    }

    public ReviewQueryRequest build() {
      return new ReviewQueryRequest(this);
    }
  }
}
