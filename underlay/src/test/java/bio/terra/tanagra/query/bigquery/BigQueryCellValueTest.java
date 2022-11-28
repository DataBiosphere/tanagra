package bio.terra.tanagra.query.bigquery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnSchema;
import com.google.cloud.bigquery.FieldValue;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

public class BigQueryCellValueTest {
  @Test
  void string() {
    BigQueryCellValue value =
        new BigQueryCellValue(
            FieldValue.of(FieldValue.Attribute.PRIMITIVE, "foo"),
            new ColumnSchema("f", CellValue.SQLDataType.STRING));

    assertEquals(CellValue.SQLDataType.STRING, value.dataType());
    assertEquals(Optional.of("foo"), value.getString());
    assertThrows(SystemException.class, value::getLong);
  }

  @Test
  void stringNull() {
    BigQueryCellValue value =
        new BigQueryCellValue(
            FieldValue.of(FieldValue.Attribute.PRIMITIVE, null),
            new ColumnSchema("f", CellValue.SQLDataType.STRING));

    assertEquals(CellValue.SQLDataType.STRING, value.dataType());
    assertEquals(Optional.empty(), value.getString());
  }

  @Test
  void int64() {
    BigQueryCellValue value =
        new BigQueryCellValue(
            FieldValue.of(FieldValue.Attribute.PRIMITIVE, "42"),
            new ColumnSchema("f", CellValue.SQLDataType.INT64));

    assertEquals(CellValue.SQLDataType.INT64, value.dataType());
    assertEquals(OptionalLong.of(42L), value.getLong());
    assertThrows(SystemException.class, value::getString);
  }

  @Test
  void int64Null() {
    BigQueryCellValue value =
        new BigQueryCellValue(
            FieldValue.of(FieldValue.Attribute.PRIMITIVE, null),
            new ColumnSchema("f", CellValue.SQLDataType.INT64));

    assertEquals(CellValue.SQLDataType.INT64, value.dataType());
    assertEquals(OptionalLong.empty(), value.getLong());
  }
}
