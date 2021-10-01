package bio.terra.tanagra.service.jdbc;

import static bio.terra.tanagra.service.jdbc.JdbcTestUtils.TEST_ID;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.tanagra.testing.BaseSpringUnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("jdbc")
public class DataSourceFactoryTest extends BaseSpringUnitTest {
  @Autowired private DataSourceFactory dataSourceFactory;

  @Test
  void dataSources() {
    assertNotNull(dataSourceFactory.getDataSource(TEST_ID));
    assertThrows(
        IllegalArgumentException.class,
        () -> dataSourceFactory.getDataSource(DataSourceId.create("unknown id")));
  }
}
