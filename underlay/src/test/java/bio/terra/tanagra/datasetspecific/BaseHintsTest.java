package bio.terra.tanagra.datasetspecific;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.api.query.hint.HintInstance;
import bio.terra.tanagra.api.query.hint.HintQueryRequest;
import bio.terra.tanagra.api.query.hint.HintQueryResult;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.EntityGroup;
import bio.terra.tanagra.underlay.serialization.SZService;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

/**
 * Utility methods for testing the values of display hints computed across all entity instances
 * (e.g. enum values for person.gender).
 */
@Tag("requires-cloud-access")
public abstract class BaseHintsTest {
  protected Underlay underlay;

  @BeforeEach
  void setup() {
    ConfigReader configReader = ConfigReader.fromJarResources();
    SZService szService = configReader.readService(getServiceConfigName());
    SZUnderlay szUnderlay = configReader.readUnderlay(szService.underlay);
    underlay = Underlay.fromConfig(szService.bigQuery, szUnderlay, configReader);
  }

  protected abstract String getServiceConfigName();

  protected void assertEntityLevelHintsMatch(String entityName, List<HintInstance> expectedHints) {
    Entity entity = underlay.getEntity(entityName);
    HintQueryRequest hintQueryRequest =
        new HintQueryRequest(underlay, entity, null, null, null, false);
    assertHintsMatch(hintQueryRequest, expectedHints);
  }

  protected void assertInstanceLevelHintsMatch(
      String entityName,
      String relatedEntityName,
      String entityGroupName,
      Literal relatedEntityId,
      List<HintInstance> expectedHints) {
    Entity entity = underlay.getEntity(entityName);
    Entity relatedEntity = underlay.getEntity(relatedEntityName);
    EntityGroup entityGroup = underlay.getEntityGroup(entityGroupName);
    HintQueryRequest hintQueryRequest =
        new HintQueryRequest(underlay, entity, relatedEntity, relatedEntityId, entityGroup, false);
    assertHintsMatch(hintQueryRequest, expectedHints);
  }

  private void assertHintsMatch(
      HintQueryRequest hintQueryRequest, List<HintInstance> expectedHints) {
    HintQueryResult hintQueryResult = underlay.getQueryRunner().run(hintQueryRequest);

    for (HintInstance expected : expectedHints) {
      Optional<HintInstance> actual = hintQueryResult.getHintInstance(expected.getAttribute());
      assertTrue(actual.isPresent());
      assertEquals(expected, actual.get());
    }
  }
}
