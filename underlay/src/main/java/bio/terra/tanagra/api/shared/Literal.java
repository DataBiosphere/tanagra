package bio.terra.tanagra.api.shared;

import static bio.terra.tanagra.api.shared.DataType.BOOLEAN;
import static bio.terra.tanagra.api.shared.DataType.DATE;
import static bio.terra.tanagra.api.shared.DataType.DOUBLE;
import static bio.terra.tanagra.api.shared.DataType.INT64;
import static bio.terra.tanagra.api.shared.DataType.STRING;
import static bio.terra.tanagra.api.shared.DataType.TIMESTAMP;

import bio.terra.tanagra.exception.SystemException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Objects;

@SuppressWarnings("PMD.ImmutableField")
public final class Literal {
  private final boolean isNull;
  private final DataType dataType;
  private String stringVal;
  private Long int64Val;
  private Double doubleVal;
  private Boolean booleanVal;
  private Date dateVal;
  private Timestamp timestampVal;

  @SuppressWarnings("checkstyle:ParameterNumber")
  private Literal(
      DataType dataType,
      boolean isNull,
      String stringVal,
      Long int64Val,
      Double doubleVal,
      Boolean booleanVal,
      Date dateVal,
      Timestamp timestampVal) {
    this.dataType = dataType;
    this.isNull = isNull;
    if (!isNull) {
      this.stringVal = stringVal;
      this.int64Val = int64Val;
      this.doubleVal = doubleVal;
      this.booleanVal = booleanVal;
      this.dateVal = dateVal;
      this.timestampVal = timestampVal;
    }
  }

  public static Literal forString(String stringVal) {
    return new Literal(STRING, stringVal == null, stringVal, null, null, null, null, null);
  }

  public static Literal forInt64(Long int64Val) {
    return new Literal(INT64, int64Val == null, null, int64Val, null, null, null, null);
  }

  public static Literal forDouble(Double doubleVal) {
    return new Literal(DOUBLE, doubleVal == null, null, null, doubleVal, null, null, null);
  }

  public static Literal forBoolean(Boolean booleanVal) {
    return new Literal(BOOLEAN, booleanVal == null, null, null, null, booleanVal, null, null);
  }

  public static Literal forDate(Date dateVal) {
    return new Literal(DATE, dateVal == null, null, null, null, null, dateVal, null);
  }

  public static Literal forDate(String dateVal) {
    return forDate(dateVal == null ? null : Date.valueOf(dateVal));
  }

  public static Literal forTimestamp(Timestamp timestampVal) {
    return new Literal(TIMESTAMP, timestampVal == null, null, null, null, null, null, timestampVal);
  }

  public static Literal forGeneric(
      DataType dataType,
      String stringVal,
      Long int64Val,
      Boolean booleanVal,
      Date dateVal,
      Timestamp timestampVal) {
    switch (dataType) {
      case STRING:
        return forString(stringVal);
      case INT64:
        return forInt64(int64Val);
      case BOOLEAN:
        return forBoolean(booleanVal);
      case DATE:
        return forDate(dateVal);
      case TIMESTAMP:
        return forTimestamp(timestampVal);
      default:
        throw new SystemException("Unsupported data type: " + dataType);
    }
  }

  public String getStringVal() {
    return !isNull && dataType.equals(STRING) ? stringVal : null;
  }

  public Long getInt64Val() {
    return !isNull && dataType.equals(INT64) ? int64Val : null;
  }

  @SuppressFBWarnings(
      value = "NP_BOOLEAN_RETURN_NULL",
      justification =
          "This value will be used in constructing a SQL string, not used directly in a Java conditional")
  public Boolean getBooleanVal() {
    return !isNull && dataType.equals(BOOLEAN) ? booleanVal : null;
  }

  public Double getDoubleVal() {
    return !isNull && dataType.equals(DOUBLE) ? doubleVal : null;
  }

  public Date getDateVal() {
    return !isNull && dataType.equals(DATE) ? dateVal : null;
  }

  public Timestamp getTimestampVal() {
    return !isNull && dataType.equals(TIMESTAMP) ? timestampVal : null;
  }

  public DataType getDataType() {
    return dataType;
  }

  public boolean isNull() {
    return isNull;
  }

  public int compareTo(Literal value) {
    if (isNull && value.isNull()) {
      return 0;
    } else if (isNull && !value.isNull()) {
      return -1;
    } else if (!isNull && value.isNull()) {
      return 1;
    } else if (!dataType.equals(value.getDataType())) {
      return -1;
    }
    switch (dataType) {
      case STRING:
        if (stringVal == null) {
          return value.getStringVal() == null ? 0 : -1;
        } else if (value.getStringVal() == null) {
          return 1;
        }
        return stringVal.compareTo(value.getStringVal());
      case INT64:
        return Long.compare(int64Val, value.getInt64Val());
      case BOOLEAN:
        return Boolean.compare(booleanVal, value.getBooleanVal());
      case DATE:
        return dateVal.compareTo(value.getDateVal());
      case DOUBLE:
        return Double.compare(doubleVal, value.getDoubleVal());
      case TIMESTAMP:
        return timestampVal.compareTo(value.getTimestampVal());
      default:
        throw new SystemException("Unknown Literal data type");
    }
  }

  @SuppressFBWarnings("NP_TOSTRING_COULD_RETURN_NULL")
  @Override
  public String toString() {
    if (isNull) {
      return "null";
    }
    switch (dataType) {
      case STRING:
        return stringVal;
      case INT64:
        return int64Val.toString();
      case BOOLEAN:
        return booleanVal.toString();
      case DATE:
        return dateVal.toString();
      case DOUBLE:
        return doubleVal.toString();
      case TIMESTAMP:
        return timestampVal.toString();
      default:
        throw new SystemException("Unknown data type");
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Literal)) {
      return false;
    }
    return compareTo((Literal) obj) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        isNull, dataType, stringVal, int64Val, doubleVal, booleanVal, dateVal, timestampVal);
  }
}
