package bio.terra.tanagra.service.search;

import com.google.auto.value.AutoValue;
import java.util.Optional;

/**
 * A construct in a query that represents a value to be selected.
 *
 * <p>Represents a single column in a SELECT SQL clause.
 */
public interface Selection {
  /**
   * A visitor pattern interface for a {@link Selection}.
   *
   * @param <R> the return value for the visitor.
   */
  interface Visitor<R> {
    R selectExpression(SelectExpression selectExpression);

    R count(Count count);

    R primaryKey(PrimaryKey primaryKey);
  }

  /** Accept the {@link Visitor} pattern. */
  <R> R accept(Selection.Visitor<R> visitor);

  /** A {@link Selection} that is an {@link Expression}. */
  @AutoValue
  abstract class SelectExpression implements Selection {
    abstract Expression expression();

    /** An alias to name this selection. */
    abstract Optional<String> alias();

    public static SelectExpression create(Expression expression, Optional<String> alias) {
      return new AutoValue_Selection_SelectExpression(expression, alias);
    }

    public static SelectExpression create(Expression expression) {
      return create(expression, Optional.empty());
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.selectExpression(this);
    }
  }

  /** A {@link Selection} for counting entities. */
  // TODO consider Count as an expression or aggregated entity.
  @AutoValue
  abstract class Count implements Selection {
    /** The entity to count. */
    public abstract EntityVariable entityVariable();

    /** An alias to name this selection. */
    abstract Optional<String> alias();

    public static Count create(EntityVariable entityVariable, Optional<String> alias) {
      return new AutoValue_Selection_Count(entityVariable, alias);
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.count(this);
    }
  }

  /** A {@link Selection} for selecting the primary keys of entities. */
  @AutoValue
  abstract class PrimaryKey implements Selection {
    /** The entity to select the primary key of. */
    public abstract EntityVariable entityVariable();

    /** An alias to name this selection. */
    abstract Optional<String> alias();

    public static PrimaryKey create(EntityVariable entityVariable, Optional<String> alias) {
      return new AutoValue_Selection_PrimaryKey(entityVariable, alias);
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.primaryKey(this);
    }
  }
}
