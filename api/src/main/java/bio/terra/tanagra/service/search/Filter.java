package bio.terra.tanagra.service.search;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.List;

/**
 * A construct in a query that represents a filter on a {@link Entity}.
 *
 * <p>A filter is a function that evaluates to true or false on every instance of the associated
 * {@link Entity}.
 */
public interface Filter {
  /**
   * A visitor pattern interface for a {@link Filter}.
   *
   * @param <R> the return value for the visitor.
   */
  interface Visitor<R> {
    R visitArrayFunction(ArrayFunction arrayFunction);

    R visitBinaryComparision(BinaryFunction binaryFunction);
  }

  /** Accept the {@link Visitor} pattern. */
  <R> R accept(Visitor<R> visitor);

  /** A {@link Filter} for the comparison of two {@link Expression} that evaluates to a boolean. */
  @AutoValue
  abstract class BinaryFunction implements Filter {
    public abstract Expression left();

    public abstract Operator operator();

    public abstract Expression right();

    public static BinaryFunction create(Expression left, Operator operator, Expression right) {
      return new AutoValue_Filter_BinaryFunction(left, operator, right);
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitBinaryComparision(this);
    }

    public enum Operator {
      EQUALS,
      LESS_THAN
      // TODO add more including DESCENDANT_OF
    }
  }

  /** A {@link Filter} that composes one or more other {@link Filter}s. */
  @AutoValue
  abstract class ArrayFunction implements Filter {
    public abstract ImmutableList<Filter> operands();

    public abstract Operator operator();

    public static ArrayFunction create(List<Filter> operands, Operator operator) {
      Preconditions.checkArgument(!operands.isEmpty(), "ArrayFunction Operands must not be empty.");
      return new AutoValue_Filter_ArrayFunction(ImmutableList.copyOf(operands), operator);
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitArrayFunction(this);
    }

    public enum Operator {
      AND, // All of the operands must be true.
      OR // Any of the operands must be true.
    }
  }

  // TODO implement a relationship filter tht allows chaining to another entity.
}
