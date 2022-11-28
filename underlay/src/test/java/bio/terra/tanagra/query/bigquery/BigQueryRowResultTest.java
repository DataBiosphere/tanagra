package bio.terra.tanagra.query.bigquery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnHeaderSchema;
import bio.terra.tanagra.query.ColumnSchema;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

public class BigQueryRowResultTest {
  @Test
  void rowResult() {
    BigQueryRowResult row =
        new BigQueryRowResult(
            FieldValueList.of(
                ImmutableList.of(
                    FieldValue.of(FieldValue.Attribute.PRIMITIVE, "foo"),
                    FieldValue.of(FieldValue.Attribute.PRIMITIVE, "42"))),
            new ColumnHeaderSchema(
                ImmutableList.of(
                    new ColumnSchema("a", CellValue.SQLDataType.STRING),
                    new ColumnSchema("b", CellValue.SQLDataType.INT64))));

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
                FieldValueList.of(
                    ImmutableList.of(FieldValue.of(FieldValue.Attribute.PRIMITIVE, "foo"))),
                new ColumnHeaderSchema(
                    ImmutableList.of(
                        new ColumnSchema("a", CellValue.SQLDataType.STRING),
                        new ColumnSchema("b", CellValue.SQLDataType.INT64)))));
  }
}
