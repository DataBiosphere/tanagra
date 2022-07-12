package bio.terra.tanagra.service.query.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.tanagra.generated.model.ApiAttributeValue;
import bio.terra.tanagra.generated.model.ApiEntityInstanceStruct;
import bio.terra.tanagra.service.databaseaccess.ColumnHeaderSchema;
import bio.terra.tanagra.service.databaseaccess.ColumnSchema;
import bio.terra.tanagra.service.databaseaccess.EagerCellValue;
import bio.terra.tanagra.service.databaseaccess.EagerRowResult;
import bio.terra.tanagra.service.databaseaccess.QueryResult;
import bio.terra.tanagra.service.search.DataType;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class QueryResultConverterTest {
  @Test
  void convertRowResult() {
    ColumnHeaderSchema columnHeaderSchema =
        ColumnHeaderSchema.builder()
            .columnSchemas(
                ImmutableList.of(
                    ColumnSchema.builder().name("a").dataType(DataType.STRING).build(),
                    ColumnSchema.builder().name("b").dataType(DataType.INT64).build()))
            .build();
    List<ApiEntityInstanceStruct> structs =
        QueryResultConverter.convertToEntityInstances(
            QueryResult.builder()
                .rowResults(
                    ImmutableList.of(
                        EagerRowResult.builder()
                            .columnHeaderSchema(columnHeaderSchema)
                            .cellValues(
                                ImmutableList.of(EagerCellValue.of("foo"), EagerCellValue.of(42L)))
                            .build(),
                        EagerRowResult.builder()
                            .columnHeaderSchema(columnHeaderSchema)
                            .cellValues(
                                ImmutableList.of(EagerCellValue.of("bar"), EagerCellValue.of(43L)))
                            .build()))
                .columnHeaderSchema(columnHeaderSchema)
                .build());

    ApiEntityInstanceStruct struct0 = new ApiEntityInstanceStruct();
    struct0.put("a", new ApiAttributeValue().stringVal("foo"));
    struct0.put("b", new ApiAttributeValue().int64Val(42L));
    ApiEntityInstanceStruct struct1 = new ApiEntityInstanceStruct();
    struct1.put("a", new ApiAttributeValue().stringVal("bar"));
    struct1.put("b", new ApiAttributeValue().int64Val(43L));

    assertThat(structs, Matchers.contains(struct0, struct1));
  }

  @Test
  void convertCellValue() {
    assertEquals(
        new ApiAttributeValue().stringVal("foo"),
        QueryResultConverter.convert(EagerCellValue.of("foo")));
    assertEquals(
        new ApiAttributeValue().int64Val(42L),
        QueryResultConverter.convert(EagerCellValue.of(42L)));
    assertEquals(null, QueryResultConverter.convert(EagerCellValue.ofNull(DataType.STRING)));
  }
}
