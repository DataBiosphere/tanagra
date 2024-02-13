package bio.terra.tanagra.underlay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import bio.terra.tanagra.proto.criteriaselector.Simple;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.junit.jupiter.api.Test;

public class ProtoSchemaTest {
  @Test
  void deserializeConfig() throws InvalidProtocolBufferException {
    String serialized = "{ \"displayName\":\"test name\", \"description\":\"test description\" }";
    Simple.Config.Builder builder = Simple.Config.newBuilder();
    JsonFormat.parser().ignoringUnknownFields().merge(serialized, builder);
    Simple.Config config = builder.build();
    assertNotNull(config);
    assertEquals("test name", config.getDisplayName());
    assertEquals("test description", config.getDescription());
  }

  @Test
  void deserializeData() throws InvalidProtocolBufferException {
    String serialized = "{ \"selectedId\":14, \"selectedDisplay\":\"test display\" }";
    Simple.Data.Builder builder = Simple.Data.newBuilder();
    JsonFormat.parser().ignoringUnknownFields().merge(serialized, builder);
    Simple.Data data = builder.build();
    assertNotNull(data);
    assertEquals(14, data.getSelectedId());
    assertEquals("test display", data.getSelectedDisplay());
  }
}
