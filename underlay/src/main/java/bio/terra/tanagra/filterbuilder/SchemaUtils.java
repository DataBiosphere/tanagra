package bio.terra.tanagra.filterbuilder;

import bio.terra.tanagra.api.shared.*;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.proto.criteriaselector.ValueOuterClass.Value;
import java.sql.Timestamp;
import java.time.Instant;

public final class SchemaUtils {
  private SchemaUtils() {}

  public static Literal toLiteral(Value value, DataType dataType) {
    return switch (value.getValueCase()) {
      case BOOL_VALUE -> Literal.forBoolean(value.getBoolValue());
      case INT64_VALUE -> Literal.forInt64(value.getInt64Value());
      case STRING_VALUE -> Literal.forString(value.getStringValue());
      case TIMESTAMP_VALUE -> Literal.forTimestamp(
          Timestamp.from(
              Instant.ofEpochSecond(
                  value.getTimestampValue().getSeconds(), value.getTimestampValue().getNanos())));
      case VALUE_NOT_SET -> Literal.forGeneric(dataType, null, null, null, null, null);
      default ->    throw new SystemException("Error converting value to literal: " + value);
    };
  }
}
