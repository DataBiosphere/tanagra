package bio.terra.tanagra.service.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class RelationshipTest {
  private static final Entity ENTITY1 = Entity.builder().name("entity1").underlay("foo").build();
  private static final Entity ENTITY2 = Entity.builder().name("entity2").underlay("foo").build();
  private static final Entity OTHER = Entity.builder().name("other").underlay("foo").build();

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

  @Test
  void unorderedEntitiesAre() {
    Relationship relationship =
        Relationship.builder().name("r").entity1(ENTITY1).entity2(ENTITY2).build();
    assertTrue(relationship.unorderedEntitiesAre(ENTITY1, ENTITY2));
    assertTrue(relationship.unorderedEntitiesAre(ENTITY2, ENTITY1));
    assertFalse(relationship.unorderedEntitiesAre(ENTITY1, OTHER));
    assertFalse(relationship.unorderedEntitiesAre(OTHER, ENTITY2));
    assertFalse(relationship.unorderedEntitiesAre(ENTITY1, ENTITY1));
    assertFalse(relationship.unorderedEntitiesAre(ENTITY2, ENTITY2));
  }

  @Test
  void hasEntity() {
    Relationship relationship =
        Relationship.builder().name("r").entity1(ENTITY1).entity2(ENTITY2).build();
    assertTrue(relationship.hasEntity(ENTITY1));
    assertTrue(relationship.hasEntity(ENTITY2));
    assertFalse(relationship.hasEntity(OTHER));
  }

  @Test
  void other() {
    Relationship relationship =
        Relationship.builder().name("r").entity1(ENTITY1).entity2(ENTITY2).build();
    assertEquals(ENTITY2, relationship.other(ENTITY1));
    assertEquals(ENTITY1, relationship.other(ENTITY2));
    assertThrows(IllegalArgumentException.class, () -> relationship.other(OTHER));
  }
}
