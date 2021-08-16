package bio.terra.tanagra.service.search;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class RelationshipTest {

  @Test
  void nameValidation() {
    Entity entity = Entity.builder().name("foo").underlay("bar").build();
    Relationship.builder().name("foo_Bar").entity1(entity).entity2(entity).build();
    assertThrows(
        IllegalArgumentException.class,
        () -> Relationship.builder().name("").entity1(entity).entity2(entity).build());
    assertThrows(
        IllegalArgumentException.class,
        () -> Relationship.builder().name("f?:").entity1(entity).entity2(entity).build());
  }
}
