package bio.terra.tanagra.service.jdbc;

/** Test utilities for working with the JDBC package. */
public final class JdbcTestUtils {
  private JdbcTestUtils() {}

  /** The {@link DataSourceId} to use in testing. Should match what's in the "jdbc" test profile. */
  public static final DataSourceId TEST_ID = DataSourceId.create("test-data-source");
}
