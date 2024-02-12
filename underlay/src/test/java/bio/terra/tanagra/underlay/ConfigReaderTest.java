package bio.terra.tanagra.underlay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.filterbuilder.CriteriaSelector;
import bio.terra.tanagra.underlay.serialization.SZCriteriaSelector;
import bio.terra.tanagra.underlay.serialization.SZEntity;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import bio.terra.tanagra.underlay.serialization.SZService;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import org.junit.jupiter.api.Test;

public class ConfigReaderTest {
  @Test
  void deserializeEntity() {
    SZEntity szPerson = ConfigReader.fromJarResources().readEntity("sd/person");
    assertNotNull(szPerson);
    Entity person = Underlay.fromConfigEntity(szPerson, "person");
    assertNotNull(person);
    assertTrue(person.isPrimary());
  }

  @Test
  void deserializeCriteriaSelector() {
    SZCriteriaSelector szGender =
        ConfigReader.fromJarResources().readCriteriaSelector("omop/gender");
    assertNotNull(szGender);
    CriteriaSelector gender =
        Underlay.fromConfigCriteriaSelector(
            szGender, "omop/gender", ConfigReader.fromJarResources());
    assertNotNull(gender);
    assertTrue(gender.getPluginConfig().contains("\"attribute\": \"gender\""));
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

  @Test
  void deserializeCmssynpufServiceAndUnderlay() {
    SZService verilyCmssynpuf = ConfigReader.fromJarResources().readService("cmssynpuf_verily");
    assertNotNull(verilyCmssynpuf);
    SZUnderlay szCmssynpuf = ConfigReader.fromJarResources().readUnderlay("cmssynpuf");
    assertNotNull(szCmssynpuf);
    Underlay cmssynpuf =
        Underlay.fromConfig(verilyCmssynpuf.bigQuery, szCmssynpuf, ConfigReader.fromJarResources());
    assertNotNull(cmssynpuf);
    assertEquals("cmssynpuf", cmssynpuf.getName());
  }
}
