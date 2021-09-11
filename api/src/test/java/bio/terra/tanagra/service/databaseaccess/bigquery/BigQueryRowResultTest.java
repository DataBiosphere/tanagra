package bio.terra.tanagra.service.databaseaccess.bigquery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.tanagra.service.databaseaccess.ColumnHeaderSchema;
import bio.terra.tanagra.service.databaseaccess.ColumnSchema;
import bio.terra.tanagra.service.search.DataType;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValue.Attribute;
import com.google.cloud.bigquery.FieldValueList;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class BigQueryRowResultTest {
  @Test
  void rowResult() {
    BigQueryRowResult row =
        new BigQueryRowResult(
            FieldValueList.of(
                ImmutableList.of(
                    FieldValue.of(Attribute.PRIMITIVE, "foo"),
                    FieldValue.of(Attribute.PRIMITIVE, "42"))),
            ColumnHeaderSchema.builder()
                .columnSchemas(
                    ImmutableList.of(
                        ColumnSchema.builder().name("a").dataType(DataType.STRING).build(),
                        ColumnSchema.builder().name("b").dataType(DataType.INT64).build()))
                .build());

    assertEquals(2, row.size());
    assertEquals(Optional.of("foo"), row.get(0).getString());
    assertEquals(Optional.of("foo"), row.get("a").getString());
    assertEquals(OptionalLong.of(42L), row.get(1).getLong());
    assertEquals(OptionalLong.of(42L), row.get("b").getLong());
  }

  @Test
  void fieldListAndColumnsThrowIfNotMatching() {
    // One less FieldValue than the number of columns.
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new BigQueryRowResult(
                FieldValueList.of(ImmutableList.of(FieldValue.of(Attribute.PRIMITIVE, "foo"))),
                ColumnHeaderSchema.builder()
                    .columnSchemas(
                        ImmutableList.of(
                            ColumnSchema.builder().name("a").dataType(DataType.STRING).build(),
                            ColumnSchema.builder().name("b").dataType(DataType.INT64).build()))
                    .build()));
  }
}
