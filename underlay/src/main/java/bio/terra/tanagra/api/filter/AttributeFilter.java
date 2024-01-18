package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.NaryOperator;
import bio.terra.tanagra.api.shared.UnaryOperator;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import com.google.common.collect.ImmutableList;
import java.util.List;

public class AttributeFilter extends EntityFilter {
  private final Underlay underlay;
  private final Entity entity;
  private final Attribute attribute;
  private final UnaryOperator unaryOperator;
  private final BinaryOperator binaryOperator;
  private final NaryOperator naryOperator;
  private final ImmutableList<Literal> values;

  public AttributeFilter(
      Underlay underlay, Entity entity, Attribute attribute, UnaryOperator unaryOperator) {
    this.underlay = underlay;
    this.entity = entity;
    this.attribute = attribute;
    this.unaryOperator = unaryOperator;
    this.binaryOperator = null;
    this.naryOperator = null;
    this.values = ImmutableList.of();
  }

  public AttributeFilter(
      Underlay underlay,
      Entity entity,
      Attribute attribute,
      BinaryOperator binaryOperator,
      Literal value) {
    this.underlay = underlay;
    this.entity = entity;
    this.attribute = attribute;
    this.unaryOperator = null;
    this.binaryOperator = binaryOperator;
    this.naryOperator = null;
    this.values = ImmutableList.of(value);
  }

  public AttributeFilter(
      Underlay underlay,
      Entity entity,
      Attribute attribute,
      NaryOperator naryOperator,
      List<Literal> values) {
    this.underlay = underlay;
    this.entity = entity;
    this.attribute = attribute;
    this.unaryOperator = null;
    this.binaryOperator = null;
    this.naryOperator = naryOperator;
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

  public UnaryOperator getUnaryOperator() {
    return unaryOperator;
  }

  public BinaryOperator getBinaryOperator() {
    return binaryOperator;
  }

  public NaryOperator getNaryOperator() {
    return naryOperator;
  }

  public ImmutableList<Literal> getValues() {
    return values;
  }

  public boolean hasUnaryOperator() {
    return unaryOperator != null;
  }

  public boolean hasBinaryOperator() {
    return binaryOperator != null;
  }
}
