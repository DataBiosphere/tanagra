package bio.terra.tanagra.utils;

import bio.terra.tanagra.exception.InvalidConfigException;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

public final class ProtobufUtils {
  private ProtobufUtils() {}

  public static <T extends Message.Builder> T deserializeFromJson(String serialized, T builder) {
    try {
      JsonFormat.parser().ignoringUnknownFields().merge(serialized, builder);
      return builder;
    } catch (InvalidProtocolBufferException ipbEx) {
      throw new InvalidConfigException("Error deserializing from JSON", ipbEx);
    }
  }

  public static <T extends Message> String serializeToJson(T message) {
    try {
      return JsonFormat.printer().omittingInsignificantWhitespace().print(message);
    } catch (InvalidProtocolBufferException ipbEx) {
      throw new InvalidConfigException("Error serializing to JSON", ipbEx);
    }
  }
}
