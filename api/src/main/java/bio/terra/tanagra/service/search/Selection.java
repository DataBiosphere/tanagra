package bio.terra.tanagra.service.search;

import com.google.auto.value.AutoValue;

/**
 * A construct in a query that represents a value to be selected.
 *
 * <p>Represents a single column in a SELECT SQL clause.
 */
public interface Selection {
  /** The name for the value being selected. Selection names should be unique within a query. */
  // TODO check names for SQL stop words.
  String name();

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

    @Override
    public abstract String name();

    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.selectExpression(this);
    }

    public static Builder builder() {
      return new AutoValue_Selection_SelectExpression.Builder();
    }

    /** Builder for {@link SelectExpression}. */
    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder expression(Expression expression);

      public abstract Builder name(String name);

      public abstract SelectExpression build();
    }
  }

  /** A {@link Selection} for counting entities. */
  // TODO consider Count as an expression or aggregated entity.
  @AutoValue
  abstract class Count implements Selection {
    /** The entity to count. */
    public abstract EntityVariable entityVariable();

    @Override
    public abstract String name();

    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.count(this);
    }

    public static Builder builder() {
      return new AutoValue_Selection_Count.Builder();
    }

    /** Builder for {@link Count}. */
    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder entityVariable(EntityVariable entityVariable);

      public abstract Builder name(String name);

      public abstract Count build();
    }
  }

  /** A {@link Selection} for selecting the primary keys of entities. */
  @AutoValue
  abstract class PrimaryKey implements Selection {
    /** The entity to select the primary key of. */
    public abstract EntityVariable entityVariable();

    @Override
    public abstract String name();

    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.primaryKey(this);
    }

    public static Builder builder() {
      return new AutoValue_Selection_PrimaryKey.Builder();
    }

    /** Builder for {@link PrimaryKey}. */
    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder entityVariable(EntityVariable entityVariable);

      public abstract Builder name(String name);

      public abstract PrimaryKey build();
    }
  }
}
