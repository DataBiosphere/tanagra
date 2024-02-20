package bio.terra.tanagra.underlay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.filterbuilder.FilterBuilder;
import bio.terra.tanagra.filterbuilder.impl.core.EntityGroupFilterBuilder;
import bio.terra.tanagra.filterbuilder.impl.core.PrimaryEntityFilterBuilder;
import bio.terra.tanagra.underlay.serialization.SZCorePlugin;
import bio.terra.tanagra.underlay.serialization.SZService;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import bio.terra.tanagra.underlay.uiplugin.CriteriaSelector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CriteriaSelectorTest {
  private Underlay underlay;

  @BeforeEach
  void setup() {
    ConfigReader configReader = ConfigReader.fromJarResources();
    SZService szService = configReader.readService("cmssynpuf_broad");
    SZUnderlay szUnderlay = configReader.readUnderlay(szService.underlay);
    underlay = Underlay.fromConfig(szService.bigQuery, szUnderlay, configReader);
  }

  @Test
  void gender() {
    CriteriaSelector gender = underlay.getCriteriaSelector("tanagra-gender");
    assertNotNull(gender);
    assertTrue(gender.isEnabledForCohorts());
    assertFalse(gender.isEnabledForDataFeatureSets());
    assertEquals(SZCorePlugin.ATTRIBUTE.getIdInConfig(), gender.getPlugin());
    assertTrue(gender.getModifiers().isEmpty());

    FilterBuilder filterBuilder = gender.getFilterBuilder();
    assertNotNull(filterBuilder);
    assertEquals(PrimaryEntityFilterBuilder.class, filterBuilder.getClass());
  }

  @Test
  void condition() {
    CriteriaSelector condition = underlay.getCriteriaSelector("tanagra-conditions");
    assertNotNull(condition);
    assertTrue(condition.isEnabledForCohorts());
    assertTrue(condition.isEnabledForDataFeatureSets());
    assertEquals(SZCorePlugin.ENTITY_GROUP.getIdInConfig(), condition.getPlugin());
    assertEquals(2, condition.getModifiers().size());

    CriteriaSelector.Modifier ageAtOccurrenceModifier = condition.getModifier("ageAtOccurrence");
    assertNotNull(ageAtOccurrenceModifier);
    assertEquals(SZCorePlugin.ATTRIBUTE.getIdInConfig(), ageAtOccurrenceModifier.getPlugin());

    FilterBuilder filterBuilder = condition.getFilterBuilder();
    assertNotNull(filterBuilder);
    assertEquals(EntityGroupFilterBuilder.class, filterBuilder.getClass());
  }
}
