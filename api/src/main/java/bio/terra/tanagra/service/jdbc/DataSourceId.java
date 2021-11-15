package bio.terra.tanagra.service.jdbc;

import com.google.auto.value.AutoValue;

/**
 * Strongly typed wrapper around a String identifying a JDBC DataSource.
 *
 * <p>A {@link DataSourceId} identifies a DataSource across an Underlay configuration and a service
 * deployment configuration. This allows the underlay to be decoupled from the JDBC configuration
 * needed to connect to an underlying database. This id should be used in both the Spring JDBC
 * configuration as well as in the underlay to link the two together. The id does not have any
 * particular format - it only serves to link the underlay and the connection information.
 *
 * <p>TODO actually add the corresponding field in tables.proto's Database message)
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
