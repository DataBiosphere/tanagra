package bio.terra.tanagra.underlay;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import bio.terra.tanagra.underlay.serialization.SZEntity;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import org.junit.jupiter.api.Test;

public class ConfigReaderTest {
  @Test
  void deserializeEntity() {
    SZEntity person = ConfigReader.fromJarResources().readEntity("sd/person");
    assertNotNull(person);
  }

  @Test
  void deserializeUnderlay() {
    SZUnderlay sdd = ConfigReader.fromJarResources().readUnderlay("sd");
    assertNotNull(sdd);
  }

  @Test
  void deserializeIndexer() {
    SZIndexer verilySdd = ConfigReader.fromJarResources().readIndexer("sd20230331_verily");
    assertNotNull(verilySdd);
  }
}
