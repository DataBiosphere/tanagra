package bio.terra.tanagra.service.databaseaccess.bigquery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.service.databaseaccess.ColumnSchema;
import bio.terra.tanagra.service.search.DataType;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValue.Attribute;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class BigQueryCellValueTest {
  @Test
  void string() {
    BigQueryCellValue value =
        new BigQueryCellValue(
            FieldValue.of(Attribute.PRIMITIVE, "foo"),
            ColumnSchema.builder().dataType(DataType.STRING).name("f").build());

    assertFalse(value.isNull());
    assertEquals("foo", value.getString());
    assertThrows(ClassCastException.class, value::getLong);
    assertEquals(DataType.STRING, value.dataType());
  }

  @Test
  void stringNull() {
    BigQueryCellValue value =
        new BigQueryCellValue(
            FieldValue.of(Attribute.PRIMITIVE, null),
            ColumnSchema.builder().dataType(DataType.STRING).name("f").build());

    assertTrue(value.isNull());
    assertThrows(NullPointerException.class, value::getString);
    assertEquals(DataType.STRING, value.dataType());
  }

  @Test
  void int64() {
    BigQueryCellValue value =
        new BigQueryCellValue(
            FieldValue.of(Attribute.PRIMITIVE, "42"),
            ColumnSchema.builder().dataType(DataType.INT64).name("f").build());

    assertFalse(value.isNull());
    assertEquals(42L, value.getLong());
    assertThrows(ClassCastException.class, value::getString);
    assertEquals(DataType.INT64, value.dataType());
  }

  @Test
  void int64Null() {
    BigQueryCellValue value =
        new BigQueryCellValue(
            FieldValue.of(Attribute.PRIMITIVE, null),
            ColumnSchema.builder().dataType(DataType.INT64).name("f").build());

    assertTrue(value.isNull());
    assertThrows(NullPointerException.class, value::getLong);
    assertEquals(DataType.INT64, value.dataType());
  }
}
