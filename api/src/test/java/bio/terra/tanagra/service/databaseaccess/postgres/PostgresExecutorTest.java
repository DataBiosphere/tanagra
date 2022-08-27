package bio.terra.tanagra.service.databaseaccess.postgres;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.tanagra.service.databaseaccess.CellValue;
import bio.terra.tanagra.service.databaseaccess.ColumnHeaderSchema;
import bio.terra.tanagra.service.databaseaccess.ColumnSchema;
import bio.terra.tanagra.service.databaseaccess.QueryRequest;
import bio.terra.tanagra.service.databaseaccess.QueryResult;
import bio.terra.tanagra.service.databaseaccess.RowResult;
import bio.terra.tanagra.service.jdbc.DataSourceFactory;
import bio.terra.tanagra.service.jdbc.JdbcTestUtils;
import bio.terra.tanagra.service.search.DataType;
import bio.terra.tanagra.testing.BaseSpringUnitTest;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.hamcrest.Matchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("jdbc")
public class PostgresExecutorTest extends BaseSpringUnitTest {
  @Autowired private DataSourceFactory dataSourceFactory;

  private PostgresExecutor createExecutor() {
    return new PostgresExecutor(
        new NamedParameterJdbcTemplate(dataSourceFactory.getDataSource(JdbcTestUtils.TEST_ID)));
  }

  // @Test
  void executeAllDataTypes() {
    PostgresExecutor postgresExecutor = createExecutor();
    String sql =
        "SELECT * FROM "
            + "  (VALUES \n"
            + "    (1, 'a'),\n"
            + "    (NULL, NULL)\n"
            + "   ) AS v(c_int, c_str)\n"
            + ";";
    QueryRequest request =
        QueryRequest.builder()
            .sql(sql)
            .columnHeaderSchema(
                ColumnHeaderSchema.builder()
                    .columnSchemas(
                        ImmutableList.of(
                            ColumnSchema.builder().dataType(DataType.INT64).name("c_int").build(),
                            ColumnSchema.builder().dataType(DataType.STRING).name("c_str").build()))
                    .build())
            .build();

    QueryResult result = postgresExecutor.execute(request);
    assertEquals(request.columnHeaderSchema(), result.columnHeaderSchema());
    List<RowResult> rowResults =
        StreamSupport.stream(result.rowResults().spliterator(), false).collect(Collectors.toList());
    assertThat(rowResults, Matchers.hasSize(2));
    // Get cell with column name.
    CellValue cInt0 = rowResults.get(0).get("c_int");
    assertEquals(OptionalLong.of(1L), cInt0.getLong());
    assertEquals(DataType.INT64, cInt0.dataType());
    assertThrows(ClassCastException.class, cInt0::getString);
    // Get cell with index.
    CellValue cStr0 = rowResults.get(0).get(1);
    assertEquals(Optional.of("a"), cStr0.getString());
    assertEquals(DataType.STRING, cStr0.dataType());
    assertThrows(ClassCastException.class, cStr0::getLong);

    // Null int64
    CellValue cIntNull = rowResults.get(1).get("c_int");
    assertEquals(OptionalLong.empty(), cIntNull.getLong());
    assertEquals(DataType.INT64, cIntNull.dataType());
    assertThrows(ClassCastException.class, cIntNull::getString);
    // Null string
    CellValue cStrNull = rowResults.get(1).get("c_str");
    assertEquals(Optional.empty(), cStrNull.getString());
    assertEquals(DataType.STRING, cStrNull.dataType());
    assertThrows(ClassCastException.class, cStrNull::getLong);
  }
}
