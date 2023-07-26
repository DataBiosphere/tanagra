package bio.terra.tanagra.underlayspecific;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.service.QuerysService;
import bio.terra.tanagra.service.UnderlaysService;
import bio.terra.tanagra.service.instances.EntityHintRequest;
import bio.terra.tanagra.service.instances.EntityHintResult;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.DisplayHint;
import bio.terra.tanagra.underlay.displayhint.EnumVal;
import bio.terra.tanagra.underlay.displayhint.EnumVals;
import java.util.*;
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
  @Autowired protected UnderlaysService underlaysService;
  @Autowired protected QuerysService querysService;

  protected abstract String getUnderlayName();

  protected void assertEntityLevelHintsMatch(
      String entityName, Map<String, DisplayHint> expectedHints) {
    EntityHintRequest entityHintRequest =
        new EntityHintRequest.Builder()
            .entity(underlaysService.getEntity(getUnderlayName(), entityName))
            .build();
    assertHintsMatch(entityHintRequest, expectedHints);
  }

  protected void assertInstanceLevelHintsMatch(
      String entityName,
      String relatedEntityName,
      Literal relatedEntityId,
      Map<String, DisplayHint> expectedHints) {
    EntityHintRequest entityHintRequest =
        new EntityHintRequest.Builder()
            .entity(underlaysService.getEntity(getUnderlayName(), entityName))
            .relatedEntity(underlaysService.getEntity(getUnderlayName(), relatedEntityName))
            .relatedEntityId(relatedEntityId)
            .build();
    assertHintsMatch(entityHintRequest, expectedHints);
  }

  private void assertHintsMatch(
      EntityHintRequest entityHintRequest, Map<String, DisplayHint> expectedHints) {
    EntityHintResult entityHintResult = querysService.listEntityHints(entityHintRequest);

    for (Map.Entry<String, DisplayHint> expected : expectedHints.entrySet()) {
      Attribute attr = entityHintRequest.getEntity().getAttribute(expected.getKey());
      DisplayHint actual = entityHintResult.getHintMap().get(attr);
      assertEquals(expected.getValue(), actual);
    }
  }

  protected static EnumVals buildEnumVals(List<EnumVal> enumVals) {
    List<EnumVal> modifiableList = new ArrayList<>(enumVals);
    modifiableList.sort(
        Comparator.comparing(ev -> String.valueOf(ev.getValueDisplay().getDisplay())));
    return new EnumVals(modifiableList);
  }
}
