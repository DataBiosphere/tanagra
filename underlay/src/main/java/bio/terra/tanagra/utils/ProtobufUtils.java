package bio.terra.tanagra.utils;

import bio.terra.tanagra.exception.InvalidConfigException;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import java.util.Base64;

public final class ProtobufUtils {

  private ProtobufUtils() {}

  @SuppressWarnings({"checkstyle:EmptyCatchBlock", "PMD.EmptyCatchBlock"})
  public static <T extends Message.Builder> T deserializeFromJsonOrProtoBytes(
      String serialized, T builder) {
    try {
      return deserializeFromJson(serialized, builder);
    } catch (InvalidConfigException icEx) {
      // Don't throw an exception, try to deserialize from Protobuf bytes instead.
    }
    return deserializeFromBase64Protobuf(serialized, builder);
  }

  public static <T extends Message.Builder> T deserializeFromJson(String serialized, T builder) {
    try {
      JsonFormat.parser().ignoringUnknownFields().merge(serialized, builder);
      return builder;
    } catch (InvalidProtocolBufferException ipbEx) {
      throw new InvalidConfigException("Error deserializing from JSON", ipbEx);
    }
  }

  public static <T extends Message.Builder> T deserializeFromBase64Protobuf(
      String serialized, T builder) {
    try {
      return (T)
          builder
              .getDefaultInstanceForType()
              .getParserForType()
              .parseFrom(Base64.getDecoder().decode(serialized))
              .toBuilder();
    } catch (InvalidProtocolBufferException ipbEx) {
      throw new InvalidConfigException("Error deserializing from protobuf bytes", ipbEx);
    }
  }

  public static <T extends Message> String serializeToJson(T message) {
    try {
      return JsonFormat.printer().omittingInsignificantWhitespace().print(message);
    } catch (InvalidProtocolBufferException ipbEx) {
      throw new InvalidConfigException("Error serializing to JSON", ipbEx);
    }
  }

  public static <T extends Message> String serializeToPrettyJson(T message) {
    try {
      return JsonFormat.printer().print(message);
    } catch (InvalidProtocolBufferException ipbEx) {
      throw new InvalidConfigException("Error serializing to pretty JSON", ipbEx);
    }
  }
}
