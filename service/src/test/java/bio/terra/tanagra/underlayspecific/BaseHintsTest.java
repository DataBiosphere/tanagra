package bio.terra.tanagra.underlayspecific;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.api2.query.EntityQueryRunner;
import bio.terra.tanagra.api2.query.hint.HintInstance;
import bio.terra.tanagra.api2.query.hint.HintQueryRequest;
import bio.terra.tanagra.api2.query.hint.HintQueryResult;
import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.service.UnderlayService;
import bio.terra.tanagra.underlay2.Underlay;
import bio.terra.tanagra.underlay2.entitymodel.Entity;
import bio.terra.tanagra.underlay2.entitymodel.entitygroup.EntityGroup;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Utility methods for testing the values of display hints computed across all entity instances
 * (e.g. enum values for person.gender).
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Main.class)
@SpringBootTest
@ActiveProfiles("test")
@Tag("requires-cloud-access")
public abstract class BaseHintsTest {
  @Autowired protected UnderlayService underlayService;

  protected abstract String getUnderlayName();

  protected void assertEntityLevelHintsMatch(String entityName, List<HintInstance> expectedHints) {
    Underlay underlay = underlayService.getUnderlay(getUnderlayName());
    Entity entity = underlay.getEntity(entityName);
    HintQueryRequest hintQueryRequest = new HintQueryRequest(underlay, entity, null, null, null);
    assertHintsMatch(hintQueryRequest, expectedHints);
  }

  protected void assertInstanceLevelHintsMatch(
      String entityName,
      String relatedEntityName,
      String entityGroupName,
      Literal relatedEntityId,
      List<HintInstance> expectedHints) {
    Underlay underlay = underlayService.getUnderlay(getUnderlayName());
    Entity entity = underlay.getEntity(entityName);
    Entity relatedEntity = underlay.getEntity(relatedEntityName);
    EntityGroup entityGroup = underlay.getEntityGroup(entityGroupName);
    HintQueryRequest hintQueryRequest =
        new HintQueryRequest(underlay, entity, relatedEntity, relatedEntityId, entityGroup);
    assertHintsMatch(hintQueryRequest, expectedHints);
  }

  private void assertHintsMatch(
      HintQueryRequest hintQueryRequest, List<HintInstance> expectedHints) {
    Underlay underlay = underlayService.getUnderlay(getUnderlayName());
    HintQueryResult hintQueryResult =
        EntityQueryRunner.run(hintQueryRequest, underlay.getQueryExecutor());

    for (HintInstance expected : expectedHints) {
      Optional<HintInstance> actual = hintQueryResult.getHintInstance(expected.getAttribute());
      assertTrue(actual.isPresent());
      assertEquals(expected, actual.get());
    }
  }
}
