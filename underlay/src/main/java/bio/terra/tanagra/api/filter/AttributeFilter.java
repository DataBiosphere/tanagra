package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.query.FunctionTemplate;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import com.google.common.collect.ImmutableList;
import java.util.List;

public class AttributeFilter extends EntityFilter {
  private final Underlay underlay;
  private final Entity entity;
  private final Attribute attribute;
  private final BinaryFilterVariable.BinaryOperator operator;
  private final FunctionTemplate functionTemplate;
  private final ImmutableList<Literal> values;

  public AttributeFilter(
      Underlay underlay,
      Entity entity,
      Attribute attribute,
      BinaryFilterVariable.BinaryOperator operator,
      Literal value) {
    this.underlay = underlay;
    this.entity = entity;
    this.attribute = attribute;
    this.operator = operator;
    this.functionTemplate = null;
    this.values = ImmutableList.of(value);
  }

  public AttributeFilter(
      Underlay underlay,
      Entity entity,
      Attribute attribute,
      FunctionTemplate functionTemplate,
      List<Literal> values) {
    this.underlay = underlay;
    this.entity = entity;
    this.attribute = attribute;
    this.operator = null;
    this.functionTemplate = functionTemplate;
    this.values = ImmutableList.copyOf(values);
  }

  public Underlay getUnderlay() {
    return underlay;
  }

  public Attribute getAttribute() {
    return attribute;
  }

  public Entity getEntity() {
    return entity;
  }

  public BinaryFilterVariable.BinaryOperator getOperator() {
    return operator;
  }

  public FunctionTemplate getFunctionTemplate() {
    return functionTemplate;
  }

  public ImmutableList<Literal> getValues() {
    return values;
  }

  public boolean hasFunctionTemplate() {
    return functionTemplate != null;
  }
}
