package bio.terra.tanagra.query.bigquery;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.query.sql.SqlRowResult;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import java.sql.Timestamp;
import java.util.*;

public class BQRowResult implements SqlRowResult {
  private final FieldValueList fieldValues;

  public BQRowResult(FieldValueList fieldValues) {
    this.fieldValues = fieldValues;
  }

  @Override
  public Literal get(String columnName, DataType expectedDataType) {
    FieldValue fieldValue = fieldValues.get(columnName);
    return toLiteral(fieldValue, expectedDataType);
  }

  @Override
  public List<Literal> getRepeated(String columnName, DataType expectedDataType) {
    List<FieldValue> repeatedFieldValue = fieldValues.get(columnName).getRepeatedValue();
    return repeatedFieldValue.stream()
        .map(fieldValue -> toLiteral(fieldValue, expectedDataType))
        .toList();
  }

  private static Literal toLiteral(FieldValue fieldValue, DataType expectedDataType) {
    return switch (expectedDataType) {
      case STRING -> Literal.forString(fieldValue.isNull() ? null : fieldValue.getStringValue());
      case INT64 -> Literal.forInt64(fieldValue.isNull() ? null : fieldValue.getLongValue());
      case BOOLEAN -> Literal.forBoolean(fieldValue.isNull() ? null : fieldValue.getBooleanValue());
      case DOUBLE -> Literal.forDouble(fieldValue.isNull() ? null : fieldValue.getDoubleValue());
      case DATE -> Literal.forDate(fieldValue.isNull() ? null : fieldValue.getStringValue());
      case TIMESTAMP ->
          Literal.forTimestamp(
              fieldValue.isNull() ? null : Timestamp.from(fieldValue.getTimestampInstant()));
    };
  }

  @Override
  public int size() {
    return fieldValues.size();
  }
}
