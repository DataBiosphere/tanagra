package bio.terra.tanagra.serialization;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import bio.terra.tanagra.serialization2.*;
import bio.terra.tanagra.underlay2.Underlay;
import org.junit.jupiter.api.Test;

public class DeserializationUtilsTest {
  @Test
  void deserializeEntity() {
    SZEntity person = DeserializationUtils.deserializeEntity("sdd", "person");
    assertNotNull(person);
  }

  @Test
  void deserializeUnderlay() {
    SZUnderlay sdd = DeserializationUtils.deserializeUnderlay("sdd");
    assertNotNull(sdd);
  }

  @Test
  void deserializeIndexer() {
    SZIndexer verilySdd = DeserializationUtils.deserializeIndexer("sdd_verily");
    assertNotNull(verilySdd);
  }

  @Test
  void buildUnderlayFromIndexerConfig() {
    SZIndexer verilySdd = DeserializationUtils.deserializeIndexer("sdd_verily");
    SZUnderlay sdd = DeserializationUtils.deserializeUnderlay(verilySdd.underlay);
    Underlay underlay = DeserializationUtils.convertToInternalObject(verilySdd.bigQuery, sdd);
    assertNotNull(underlay);
  }

  @Test
  void buildUnderlayFromServiceConfig() {
    SZService verilySdd = DeserializationUtils.deserializeService("sdd_verily");
    SZUnderlay sdd = DeserializationUtils.deserializeUnderlay(verilySdd.underlay);
    Underlay underlay = DeserializationUtils.convertToInternalObject(verilySdd.bigQuery, sdd);
    assertNotNull(underlay);
  }
}
