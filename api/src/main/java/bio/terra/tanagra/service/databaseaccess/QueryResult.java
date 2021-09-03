package bio.terra.tanagra.service.databaseaccess;

import com.google.auto.value.AutoValue;

/** The result of a data access query. */
// TODO add pagination.
@AutoValue
public abstract class QueryResult {

  /** The {@link RowResult}s that make of the data of the query result. */
  public abstract Iterable<RowResult> rowResults();

  /** The {@link ColumnHeaderSchema}s for the {@link #rowResults()}. */
  public abstract ColumnHeaderSchema columnHeaderSchema();

  public static Builder builder() {
    return new AutoValue_QueryResult.Builder();
  }

  /** Builder for {@link QueryResult}. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder rowResults(Iterable<RowResult> rowResults);

    public abstract Builder columnHeaderSchema(ColumnHeaderSchema value);

    public abstract QueryResult build();
  }
}
