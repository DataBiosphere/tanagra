package bio.terra.tanagra.query.bigquery;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.query.sql.SqlRowResult;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import java.sql.Timestamp;

public class BQRowResult implements SqlRowResult {
  private final FieldValueList fieldValues;

  public BQRowResult(FieldValueList fieldValues) {
    this.fieldValues = fieldValues;
  }

  @Override
  public Literal get(String columnName, DataType expectedDataType) {
    FieldValue fieldValue = fieldValues.get(columnName);
    if (fieldValue.isNull()) {
      return Literal.NULL;
    }
    switch (expectedDataType) {
      case STRING:
        return Literal.forString(fieldValue.getStringValue());
      case INT64:
        return Literal.forInt64(fieldValue.getLongValue());
      case BOOLEAN:
        return Literal.forBoolean(fieldValue.getBooleanValue());
      case DOUBLE:
        return Literal.forDouble(fieldValue.getDoubleValue());
      case DATE:
        return Literal.forDate(fieldValue.getStringValue());
      case TIMESTAMP:
        return Literal.forTimestamp(Timestamp.from(fieldValue.getTimestampInstant()));
      default:
        throw new InvalidQueryException(
            "Unsupported data type for BigQuery row result: " + expectedDataType);
    }
  }

  @Override
  public int size() {
    return fieldValues.size();
  }
}
