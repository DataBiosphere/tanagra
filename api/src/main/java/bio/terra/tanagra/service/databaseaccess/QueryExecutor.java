package bio.terra.tanagra.service.databaseaccess;

import bio.terra.tanagra.underlay.Table;

/**
 * An interface for unifying read query access to different databases.
 *
 * <p>This allows us to interact with different underlying databases in a uniform manner. Each
 * database type we support should add an implementaiton.
 */
public interface QueryExecutor {
  /** Execute a query request, returning the results of the query. */
  QueryResult execute(QueryRequest queryRequest);

  /**
   * An interface for getting the appropriate QueryExecutor based on the primary table going to be
   * used.
   *
   * <p>This is useful to have as an interface for testing.
   */
  @FunctionalInterface
  interface Factory {
    QueryExecutor get(Table primaryTable);
  }
}
