package bio.terra.tanagra.api.shared;

import static bio.terra.tanagra.api.shared.DataType.BOOLEAN;
import static bio.terra.tanagra.api.shared.DataType.DATE;
import static bio.terra.tanagra.api.shared.DataType.DOUBLE;
import static bio.terra.tanagra.api.shared.DataType.INT64;
import static bio.terra.tanagra.api.shared.DataType.STRING;
import static bio.terra.tanagra.api.shared.DataType.TIMESTAMP;

import bio.terra.tanagra.exception.SystemException;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Objects;

public final class Literal {
  public static final Literal NULL = new Literal();
  private final boolean isNull;
  private final DataType dataType;
  private String stringVal;
  private Long int64Val;
  private Double doubleVal;
  private Boolean booleanVal;
  private Date dateVal;
  private Timestamp timestampVal;

  private Literal() {
    this.isNull = true;
    this.dataType = null;
  }

  private Literal(
      DataType dataType,
      String stringVal,
      Long int64Val,
      Double doubleVal,
      Boolean booleanVal,
      Date dateVal,
      Timestamp timestampVal) {
    this.isNull = false;
    this.dataType = dataType;
    this.stringVal = stringVal;
    this.int64Val = int64Val;
    this.doubleVal = doubleVal;
    this.booleanVal = booleanVal;
    this.dateVal = dateVal;
    this.timestampVal = timestampVal;
  }

  public static Literal forString(String stringVal) {
    return stringVal == null ? NULL : new Literal(STRING, stringVal, null, null, null, null, null);
  }

  public static Literal forInt64(Long int64Val) {
    return int64Val == null ? NULL : new Literal(INT64, null, int64Val, null, null, null, null);
  }

  public static Literal forDouble(Double doubleVal) {
    return doubleVal == null ? NULL : new Literal(DOUBLE, null, null, doubleVal, null, null, null);
  }

  public static Literal forBoolean(Boolean booleanVal) {
    return booleanVal == null
        ? NULL
        : new Literal(BOOLEAN, null, null, null, booleanVal, null, null);
  }

  public static Literal forDate(Date dateVal) {
    return dateVal == null ? NULL : new Literal(DATE, null, null, null, null, dateVal, null);
  }

  public static Literal forDate(String dateVal) {
    return dateVal == null ? NULL : forDate(Date.valueOf(dateVal));
  }

  public static Literal forTimestamp(Timestamp timestampVal) {
    return timestampVal == null
        ? NULL
        : new Literal(TIMESTAMP, null, null, null, null, null, timestampVal);
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
    return dataType.equals(STRING) ? stringVal : null;
  }

  public Long getInt64Val() {
    return dataType.equals(INT64) ? int64Val : null;
  }

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "NP_BOOLEAN_RETURN_NULL",
      justification =
          "This value will be used in constructing a SQL string, not used directly in a Java conditional")
  public Boolean getBooleanVal() {
    return dataType.equals(BOOLEAN) ? booleanVal : null;
  }

  public Double getDoubleVal() {
    return dataType.equals(DOUBLE) ? doubleVal : null;
  }

  public Date getDateVal() {
    return dataType.equals(DATE) ? dateVal : null;
  }

  public Timestamp getTimestampVal() {
    return dataType.equals(TIMESTAMP) ? timestampVal : null;
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

  @Override
  public String toString() {
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
