package bio.terra.tanagra.service.search;

import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

/** A construct in a query syntax tree that evaluates to a value. */
public interface Expression {
  /**
   * A visitor pattern interface for an {@link Expression}.
   *
   * @param <R> the return value for the visitor.
   */
  interface Visitor<R> {
    R visitLiteral(Literal literal);

    R visitAttribute(AttributeExpression attributeExpression);
  }

  /** Accept the {@link Visitor} pattern. */
  <R> R accept(Visitor<R> visitor);

  /** An {@link Expression} that's a literal value. */
  // TODO support null value.
  @AutoValue
  abstract class Literal implements Expression {
    public abstract DataType dataType();

    @Nullable
    public abstract String value();

    public static Literal create(DataType dataType, String value) {
      return new AutoValue_Expression_Literal(dataType, value);
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitLiteral(this);
    }
  }

  /** An {@link Expression} that's an {@link AttributeVariable}. */
  @AutoValue
  abstract class AttributeExpression implements Expression {
    public abstract AttributeVariable attributeVariable();

    public static AttributeExpression create(AttributeVariable attributeVariable) {
      return new AutoValue_Expression_AttributeExpression(attributeVariable);
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitAttribute(this);
    }
  }

  // TODO implement math expressions as needed, e.g. minus, plus.
}
