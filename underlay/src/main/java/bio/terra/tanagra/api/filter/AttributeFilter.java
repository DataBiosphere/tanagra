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
import java.util.Objects;
import org.slf4j.LoggerFactory;

public class AttributeFilter extends EntityFilter {
  private final Attribute attribute;
  private final UnaryOperator unaryOperator;
  private final BinaryOperator binaryOperator;
  private final NaryOperator naryOperator;
  private final ImmutableList<Literal> values;

  public AttributeFilter(
      Underlay underlay, Entity entity, Attribute attribute, UnaryOperator unaryOperator) {
    super(LoggerFactory.getLogger(AttributeFilter.class), underlay, entity);
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
    super(LoggerFactory.getLogger(AttributeFilter.class), underlay, entity);
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
    super(LoggerFactory.getLogger(AttributeFilter.class), underlay, entity);
    this.attribute = attribute;
    this.unaryOperator = null;
    this.binaryOperator = null;
    this.naryOperator = naryOperator;
    this.values = ImmutableList.copyOf(values);
  }

  @Override
  public List<Attribute> getFilterAttributes() {
    return List.of(attribute);
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

  public boolean hasNaryOperator() {
    return naryOperator != null;
  }

  public String getOperatorName() {
    if (hasUnaryOperator()) {
      return unaryOperator.name();
    } else if (hasBinaryOperator()) {
      return binaryOperator.name();
    } else if (hasNaryOperator()) {
      return naryOperator.name();
    } else {
      return null;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (!super.equals(o)) {
      return false;
    }
    AttributeFilter that = (AttributeFilter) o;
    return attribute.equals(that.attribute)
        && unaryOperator == that.unaryOperator
        && binaryOperator == that.binaryOperator
        && naryOperator == that.naryOperator
        && values.equals(that.values);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        super.hashCode(), attribute, unaryOperator, binaryOperator, naryOperator, values);
  }
}
