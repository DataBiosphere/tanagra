package bio.terra.tanagra.underlay;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import bio.terra.tanagra.underlay.serialization.SZEntity;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import org.junit.jupiter.api.Test;

public class ConfigReaderTest {
  @Test
  void deserializeEntity() {
    SZEntity person = ConfigReader.deserializeEntity("sdd/person");
    assertNotNull(person);
  }

  @Test
  void deserializeUnderlay() {
    SZUnderlay sdd = ConfigReader.deserializeUnderlay("sdd");
    assertNotNull(sdd);
  }

  @Test
  void deserializeIndexer() {
    SZIndexer verilySdd = ConfigReader.deserializeIndexer("sdd_verily");
    assertNotNull(verilySdd);
  }
}
