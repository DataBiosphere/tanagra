package bio.terra.tanagra.service.jdbc;

import com.google.auto.value.AutoValue;

/**
 * Strongly typed wrapper around a String identifying a JDBC DataSource.
 *
 * <p>A {@link DataSourceId} identifies a DataSource across an Underlay configuration and a service
 * deployment configuration. This allows the underlay to be decoupled from the JDBC configuration
 * needed to connect to an underlying database.
 *
 * <p>Strongly instead of Stringly typing this makes its usage clearer and more type safe.
 */
@AutoValue
public abstract class DataSourceId {
  public abstract String id();

  public static DataSourceId create(String id) {
    return new AutoValue_DataSourceId(id);
  }
}
