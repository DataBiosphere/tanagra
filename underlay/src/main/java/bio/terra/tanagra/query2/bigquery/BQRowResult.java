package bio.terra.tanagra.query2.bigquery;

import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query2.sql.SqlRowResult;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import java.sql.Timestamp;

public class BQRowResult implements SqlRowResult {
  private final FieldValueList fieldValues;

  public BQRowResult(FieldValueList fieldValues) {
    this.fieldValues = fieldValues;
  }

  @Override
  public Literal get(String columnName, Literal.DataType expectedDataType) {
    FieldValue fieldValue = fieldValues.get(columnName);
    if (fieldValue.isNull()) {
      return new Literal(null);
    }
    switch (expectedDataType) {
      case STRING:
        return new Literal(fieldValue.getStringValue());
      case INT64:
        return new Literal(fieldValue.getLongValue());
      case BOOLEAN:
        return new Literal(fieldValue.getBooleanValue());
      case DOUBLE:
        return new Literal(fieldValue.getDoubleValue());
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
