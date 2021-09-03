package bio.terra.tanagra.service.databaseaccess;

import com.google.auto.value.AutoValue;

/** The request for a query to execute against a database backend. */
// TODO add pagination.
@AutoValue
public abstract class QueryRequest {

  /** The SQL query to execute for the request. */
  // TODO add parameterized arguments.
  public abstract String sql();

  /** The expected schema of columns of the query result. */
  public abstract ColumnHeaderSchema columnHeaderSchema();

  public static Builder builder() {
    return new AutoValue_QueryRequest.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder sql(String sql);

    public abstract Builder columnHeaderSchema(ColumnHeaderSchema columnHeaderSchema);

    public abstract QueryRequest build();
  }
}
