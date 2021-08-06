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

    R visitRelationship(RelationshipFilter relationshipFilter);
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

  /**
   * A {@link Filter} that introduces a constraint that there must be a relationship to another
   * entity.
   *
   * <p>This introduces a new {@link Variable} and implicitly binds it to an entity in {@link
   * #newVariable()}. The newly bound and outer entities should have a relationship. The outer
   * variable may be any variable introduced higher in the outer filter scope.
   *
   * <p>This is equivalent to the pseudo-SQL "outer.key IN (SELECT bound.outer_key FROM bound.entity
   * AS bound.variable WHERE filter)".
   */
  @AutoValue
  abstract class RelationshipFilter implements Filter {

    // DO NOT SUBMIT bound/inner/new?
    /** The newly introduced entity variable that is related to {@link #outerVariable}. */
    public abstract EntityVariable newVariable();

    /** The attribute of an outer variable that is related to {@link #newVariable}. */
    public abstract EntityVariable outerVariable();

    /** The filter to apply to the bound variable. This may refer to outer variables. */
    public abstract Filter filter();

    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitRelationship(this);
    }

    public static Builder builder() {
      return new AutoValue_Filter_RelationshipFilter.Builder();
    }

    /** Builder for {@link RelationshipFilter}. */
    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder newVariable(EntityVariable boundVariable);

      public abstract EntityVariable newVariable();

      public abstract Builder outerVariable(EntityVariable outerVariable);

      public abstract EntityVariable outerVariable();

      public abstract Builder filter(Filter filter);

      public RelationshipFilter build() {
        Preconditions.checkArgument(
            !newVariable().variable().equals(outerVariable().variable()),
            "Cannot bind a new variable with the same names as the outer attribute's variable "
                + "in a RelationshipFilter: %s. Pick a new name for the newly bound variable.",
            outerVariable().variable().name());
        return autoBuild();
      }

      abstract RelationshipFilter autoBuild();
    }
  }
}
