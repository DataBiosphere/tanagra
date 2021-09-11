package bio.terra.tanagra.service.search;

import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.tanagra.model.Entity;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class EntityTest {

  @Test
  void nameValidation() {
    Entity.builder().name("foo_Bar").underlay("baz2").build();
    assertThrows(
        IllegalArgumentException.class, () -> Entity.builder().name("").underlay("foo").build());
    assertThrows(
        IllegalArgumentException.class, () -> Entity.builder().name("foo").underlay("f?:").build());
  }
}
