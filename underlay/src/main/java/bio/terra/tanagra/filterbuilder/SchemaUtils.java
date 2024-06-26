package bio.terra.tanagra.filterbuilder;

import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.proto.criteriaselector.ValueOuterClass.Value;
import java.sql.Timestamp;
import java.time.Instant;

public final class SchemaUtils {
  private SchemaUtils() {}

  public static Literal toLiteral(Value value) {
    switch (value.getValueCase()) {
      case BOOL_VALUE:
        return Literal.forBoolean(value.getBoolValue());
      case INT64_VALUE:
        return Literal.forInt64(value.getInt64Value());
      case STRING_VALUE:
        return Literal.forString(value.getStringValue());
      case TIMESTAMP_VALUE:
        return Literal.forTimestamp(
            Timestamp.from(
                Instant.ofEpochSecond(
                    value.getTimestampValue().getSeconds(), value.getTimestampValue().getNanos())));
      case VALUE_NOT_SET:
        throw new InvalidConfigException(
            "Cannot convert a value with no type to a literal: " + value.getValueCase());
    }
    throw new SystemException("Error converting value to literal: " + value);
  }
}
