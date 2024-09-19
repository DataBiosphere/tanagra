package bio.terra.tanagra.db;

import bio.terra.common.exception.MissingRequiredFieldException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/** Utility functions for interacting with Tanagra's database */
public final class JdbcUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(JdbcUtils.class);

  private JdbcUtils() {}

  /**
   * This method builds an SQL clause string for setting fields specified in the given parameters.
   * The method generates the column_name = :column_name list. It is an error if the params map is
   * empty.
   *
   * @param columnParams map of sql parameters.
   * @param jsonColumns param columns that needs to be cast as jsonb.
   */
  public static String setColumnsClause(MapSqlParameterSource columnParams, String... jsonColumns) {
    StringBuilder sb = new StringBuilder();
    String[] parameterNames = columnParams.getParameterNames();
    if (parameterNames.length == 0) {
      throw new MissingRequiredFieldException("Must specify some data to be updated.");
    }
    Set<String> jsonColumnSet = new HashSet<>(Arrays.asList(jsonColumns));
    for (int i = 0; i < parameterNames.length; i++) {
      String columnName = parameterNames[i];
      if (i > 0) {
        sb.append(", ");
      }
      if (jsonColumnSet.contains(columnName)) {
        sb.append(columnName).append(" = cast(:").append(columnName).append(" AS jsonb)");
      } else {
        sb.append(columnName).append(" = :").append(columnName);
      }
    }

    return sb.toString();
  }

  public static OffsetDateTime timestampToOffsetDateTime(Timestamp timestamp) {
    return OffsetDateTime.ofInstant(timestamp.toInstant(), ZoneOffset.UTC);
  }

  public static Timestamp sqlTimestampUTC() {
    return Timestamp.valueOf(
        OffsetDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).toLocalDateTime());
  }

  /** returns the number of rows inserted */
  static int insertRows(
      NamedParameterJdbcTemplate jdbcTemplate,
      String table,
      String sql,
      List<MapSqlParameterSource> revisionParamSets) {
    LOGGER.debug("CREATE {}: {}", table, sql);
    int rowsAffected =
        Arrays.stream(
                jdbcTemplate.batchUpdate(
                    sql, revisionParamSets.toArray(new MapSqlParameterSource[0])))
            .sum();
    LOGGER.debug("CREATE {} rowsAffected = {}", table, rowsAffected);
    return rowsAffected;
  }
}
