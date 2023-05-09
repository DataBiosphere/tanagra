package bio.terra.tanagra.service.instances;

import bio.terra.tanagra.service.instances.filter.AnnotationFilter;
import bio.terra.tanagra.service.instances.filter.EntityFilter;
import bio.terra.tanagra.underlay.Attribute;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReviewQueryRequest {
  private final List<Attribute> attributes;
  private final EntityFilter entityFilter;
  private final AnnotationFilter annotationFilter;
  private final List<ReviewQueryOrderBy> orderBys;

  private ReviewQueryRequest(Builder builder) {
    this.attributes = builder.attributes;
    this.entityFilter = builder.entityFilter;
    this.annotationFilter = builder.annotationFilter;
    this.orderBys = builder.orderBys;
  }

  public static Builder builder() {
    return new Builder();
  }

  public List<Attribute> getAttributes() {
    return attributes;
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

  public List<ReviewQueryOrderBy> getOrderBys() {
    return orderBys == null ? Collections.emptyList() : Collections.unmodifiableList(orderBys);
  }

  public void addAttribute(Attribute attribute) {
    attributes.add(attribute);
  }

  public static class Builder {
    private List<Attribute> attributes;
    private EntityFilter entityFilter;
    private AnnotationFilter annotationFilter;
    private List<ReviewQueryOrderBy> orderBys;

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

    public Builder orderBys(List<ReviewQueryOrderBy> orderBys) {
      this.orderBys = orderBys;
      return this;
    }

    public ReviewQueryRequest build() {
      attributes = attributes == null ? new ArrayList<>() : new ArrayList<>(attributes);
      return new ReviewQueryRequest(this);
    }
  }
}
