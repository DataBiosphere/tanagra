package bio.terra.tanagra.service.databaseaccess.postgres;

import bio.terra.tanagra.service.databaseaccess.QueryExecutor;
import bio.terra.tanagra.service.databaseaccess.QueryRequest;
import bio.terra.tanagra.service.databaseaccess.QueryResult;
import bio.terra.tanagra.service.databaseaccess.RowResult;
import java.util.List;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/** A {@link QueryExecutor} for executing queries against a Postgres JDBC data source. */
public class PostgresExecutor implements QueryExecutor {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public PostgresExecutor(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public QueryResult execute(QueryRequest queryRequest) {
    // TODO implement pagination by generating appropriate offsets & limits.
    List<RowResult> results =
        jdbcTemplate.query(
            queryRequest.sql(), new RowResultMapper(queryRequest.columnHeaderSchema()));
    return QueryResult.builder()
        .columnHeaderSchema(queryRequest.columnHeaderSchema())
        .rowResults(results)
        .build();
  }
}
