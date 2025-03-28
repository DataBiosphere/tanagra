package bio.terra.tanagra.underlay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.serialization.SZCriteriaSelector;
import bio.terra.tanagra.underlay.serialization.SZEntity;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import bio.terra.tanagra.underlay.serialization.SZPrepackagedCriteria;
import bio.terra.tanagra.underlay.serialization.SZService;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import bio.terra.tanagra.underlay.uiplugin.CriteriaSelector;
import bio.terra.tanagra.underlay.uiplugin.PrepackagedCriteria;
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
  void deserializePrepackagedCriteria() {
    // With no selection data.
    SZPrepackagedCriteria szDemographics =
        ConfigReader.fromJarResources().readPrepackagedCriteria("omop/demographics");
    assertNotNull(szDemographics);
    PrepackagedCriteria demographics =
        Underlay.fromConfigPrepackagedCriteria(
            szDemographics, "omop/demographics", ConfigReader.fromJarResources());
    assertNotNull(demographics);
    assertEquals("demographics", demographics.getName());
    assertFalse(demographics.hasSelectionData());

    // With selection data.
    SZPrepackagedCriteria szType2Diabetes =
        ConfigReader.fromJarResources().readPrepackagedCriteria("omop/type2Diabetes");
    assertNotNull(szType2Diabetes);
    PrepackagedCriteria type2diabetes =
        Underlay.fromConfigPrepackagedCriteria(
            szType2Diabetes, "omop/type2Diabetes", ConfigReader.fromJarResources());
    assertNotNull(type2diabetes);
    assertEquals("type2Diabetes", type2diabetes.getName());
    assertEquals("condition", type2diabetes.getCriteriaSelector());
    assertTrue(type2diabetes.hasSelectionData());
    assertNull(type2diabetes.getSelectionData().modifierName());
    assertTrue(type2diabetes.getSelectionData().pluginData().contains("\"keys\": [ 201826 ]"));
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
    SZService verilyCmssynpuf = ConfigReader.fromJarResources().readService("cmssynpuf_broad");
    assertNotNull(verilyCmssynpuf);
    SZUnderlay szCmssynpuf = ConfigReader.fromJarResources().readUnderlay("cmssynpuf");
    assertNotNull(szCmssynpuf);
    Underlay cmssynpuf =
        Underlay.fromConfig(verilyCmssynpuf.bigQuery, szCmssynpuf, ConfigReader.fromJarResources());
    assertNotNull(cmssynpuf);
    assertEquals("cmssynpuf", cmssynpuf.getName());

    Entity person = cmssynpuf.getPrimaryEntity();
    assertEquals(
        "bigquery-public-data.cms_synthetic_patient_data_omop.person",
        person.getSourceQueryTableName());
    Attribute gender = person.getAttribute("gender");
    assertNotNull(gender.getSourceQuery());
    assertEquals(
        "bigquery-public-data.cms_synthetic_patient_data_omop.concept",
        gender.getSourceQuery().getDisplayFieldTable());
    assertEquals("concept_id", gender.getSourceQuery().getDisplayFieldTableJoinFieldName());
  }
}
