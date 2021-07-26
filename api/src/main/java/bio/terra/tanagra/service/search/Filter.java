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

    R visitThereExists(ThereExists thereExists);
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

  // DO NOT SUBMIT comment me.
  // There exists var X with relationship X -> Y and Filter F
  @AutoValue
  abstract class ThereExists implements Filter {

    /** A new {@link EntityVariable} introduced by this filter. */
    public abstract EntityVariable bound();

    /** An already existing {@link EntityVariable} that the new variable is related to. */
    public abstract EntityVariable relatedTo();

    /** The relationship between the new bound variable and the existing variable. */
    public abstract Relationship relationship();

    /** The filter to apply to the bound variable. */
    public abstract Filter filter();

    public Attribute boundAttribute() {
      return
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitThereExists(this);
    }

    public static Builder builder() {
      return new AutoValue_Filter_ThereExists.Builder();
    }

    /** A builder for {@link ThereExists}. */
    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder bound(EntityVariable bound);
      public abstract  EntityVariable bound();

      public abstract Builder relatedTo(EntityVariable relatedTo);
      public abstract EntityVariable relatedTo();

      public abstract Builder relationship(Relationship relationship);
      public abstract Relationship relationship();

      public abstract Builder filter(Filter filter);
      public abstract Filter filter();

      public ThereExists build() {
        boolean boundToRole1 = bound().entity().equals(relationship().role1().entity()) && relatedTo().entity().equals(relationship().role2().entity());
        boolean boundToRole2 = bound().entity().equals(relationship().role2().entity()) && relatedTo().entity().equals(relationship().role1().entity())
        Preconditions.checkArgument(
        boundToRole1 ^ boundToRole2, "The relationship entity roles must match the bound and relatedTo variables.\nrelationship %s\nbound %s\nrelatedTo %s", relationship(),
            bound(), relatedTo());
        return autoBuild();
      }
      abstract ThereExists autoBuild();
    }
  }
}
