package bio.terra.tanagra.api.jdbc;

/**
 * Strongly typed wrapper around a String identifying a JDBC DataSource.
 *
 * <p>A {@link DataSourceId} identifies a DataSource across an Underlay configuration and a service
 * deployment configuration. This allows the underlay to be decoupled from the JDBC configuration
 * needed to connect to an underlying database. This id should be used in both the Spring JDBC
 * configuration as well as in the underlay to link the two together. The id does not have any
 * particular format - it only serves to link the underlay and the connection information.
 *
 * <p>Strongly instead of Stringly typing this makes its usage clearer and more type safe.
 */
public class DataSourceId {
  private final String id;

  public DataSourceId(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }
}
