package bio.terra.tanagra.underlay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.proto.criteriaselector.KeyOuterClass.Key;
import bio.terra.tanagra.proto.criteriaselector.ValueDataOuterClass.ValueData;
import bio.terra.tanagra.proto.criteriaselector.ValueOuterClass.Value;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.junit.jupiter.api.Test;

public class ProtoSchemaTest {
  @Test
  void deserializeSimpleSchema() throws InvalidProtocolBufferException {
    String serialized = "{ \"int64Key\":14 }";
    Key.Builder builder = Key.newBuilder();
    JsonFormat.parser().ignoringUnknownFields().merge(serialized, builder);
    Key key = builder.build();
    assertNotNull(key);
    assertFalse(key.hasStringKey());
    assertEquals(14, key.getInt64Key());
  }

  @Test
  void serializeSimpleSchema() throws InvalidProtocolBufferException {
    Key key = Key.newBuilder().setInt64Key(45).build();
    String serialized = JsonFormat.printer().omittingInsignificantWhitespace().print(key);
    assertEquals("{\"int64Key\":\"45\"}", serialized);
  }

  @Test
  void deserializeNestedSchema() throws InvalidProtocolBufferException {
    String serialized =
        "{ \"attribute\": \"id\", \"numeric\": true,  \"range\": { \"id\": \"15\", \"min\": 0, \"max\": 89 } }";
    ValueData.Builder builder = ValueData.newBuilder();
    JsonFormat.parser().ignoringUnknownFields().merge(serialized, builder);
    ValueData valueData = builder.build();
    assertNotNull(valueData);
    assertEquals("id", valueData.getAttribute());
    assertTrue(valueData.getNumeric());
    assertEquals(0, valueData.getSelectedCount());
    assertNotNull(valueData.getRange());
    assertEquals("15", valueData.getRange().getId());
    assertEquals(0, valueData.getRange().getMin());
    assertEquals(89, valueData.getRange().getMax());
  }

  @Test
  void serializeNestedSchema() throws InvalidProtocolBufferException {
    ValueData valueData =
        ValueData.newBuilder()
            .setAttribute("vocabulary")
            .setNumeric(false)
            .addSelected(
                ValueData.Selection.newBuilder()
                    .setValue(Value.newBuilder().setInt64Value(24).build())
                    .setName("SNOMED")
                    .build())
            .build();
    String serialized = JsonFormat.printer().omittingInsignificantWhitespace().print(valueData);
    assertEquals(
        "{\"attribute\":\"vocabulary\",\"selected\":[{\"value\":{\"int64Value\":\"24\"},\"name\":\"SNOMED\"}]}",
        serialized);
  }
}
