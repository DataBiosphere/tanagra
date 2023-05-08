package bio.terra.tanagra.service;

import static org.junit.jupiter.api.Assertions.*;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceIdCollection;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Underlay;
import java.util.List;
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
public class UnderlaysServiceTest {
  @Autowired private UnderlaysService underlaysService;

  @Test
  void listAllOrSelected() {
    String underlayName = "cms_synpuf";

    // List underlays.
    List<Underlay> allUnderlays =
        underlaysService.listUnderlays(ResourceIdCollection.allResourceIds());
    assertEquals(3, allUnderlays.size());

    List<Underlay> oneUnderlay =
        underlaysService.listUnderlays(
            ResourceIdCollection.forCollection(List.of(new ResourceId(underlayName))));
    assertEquals(1, oneUnderlay.size());
    assertEquals(underlayName, oneUnderlay.get(0).getName());

    // List entities.
    List<Entity> allEntities = underlaysService.listEntities(underlayName);
    assertEquals(12, allEntities.size());
  }

  @Test
  void invalid() {
    // Get an invalid underlay.
    assertThrows(NotFoundException.class, () -> underlaysService.getUnderlay("invalid underlay"));

    // Get an invalid entity.
    assertThrows(
        NotFoundException.class, () -> underlaysService.getEntity("cms_synpuf", "invalid entity"));
  }
}
