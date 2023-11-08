package bio.terra.tanagra.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.underlay2.Underlay;
import bio.terra.tanagra.underlay2.entitymodel.Entity;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Main.class)
@SpringBootTest
@ActiveProfiles("test")
public class UnderlayServiceTest {
  @Autowired private UnderlayService underlayService;

  @Test
  void listAllOrSelected() {
    String underlayName = "cmssynpuf";

    // List underlays.
    List<Underlay> allUnderlays =
        underlayService.listUnderlays(
            ResourceCollection.allResourcesAllPermissions(ResourceType.UNDERLAY, null));
    assertEquals(3, allUnderlays.size());

    List<Underlay> oneUnderlay =
        underlayService.listUnderlays(
            ResourceCollection.resourcesSamePermissions(
                Permissions.allActions(ResourceType.UNDERLAY),
                Set.of(ResourceId.forUnderlay(underlayName))));
    assertEquals(1, oneUnderlay.size());
    assertEquals(underlayName, oneUnderlay.get(0).getName());

    // List entities.
    List<Entity> allEntities = underlayService.getUnderlay(underlayName).getEntities();
    assertEquals(17, allEntities.size());
  }

  @Test
  void invalid() {
    // Get an invalid underlay.
    assertThrows(NotFoundException.class, () -> underlayService.getUnderlay("invalid underlay"));

    // Get an invalid entity.
    assertThrows(
        NotFoundException.class,
        () -> underlayService.getUnderlay("cmssynpuf").getEntity("invalid entity"));
  }
}
