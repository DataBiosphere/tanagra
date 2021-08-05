package bio.terra.tanagra.service.underlay;

import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.tanagra.service.search.Attribute;
import bio.terra.tanagra.service.search.AttributeVariable;
import bio.terra.tanagra.service.search.DataType;
import bio.terra.tanagra.service.search.Entity;
import bio.terra.tanagra.service.search.EntityVariable;
import bio.terra.tanagra.service.search.Variable;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class UnderlayTest {
  private static Underlay nauticalUnderlay;

  @BeforeAll
  private static void setUnderlay() throws IOException {
    nauticalUnderlay = NauticalUnderlayUtils.loadNauticalUnderlay();
  }

  @Test
  void getEntity() {
    assertEquals(Optional.of(SAILOR), nauticalUnderlay.getEntity("sailors"));
    assertEquals(Optional.empty(), nauticalUnderlay.getEntity("foobar"));
  }

  @Test
  void getAttribute() {
    assertEquals(Optional.of(SAILOR_NAME), nauticalUnderlay.getAttribute(SAILOR, "name"));
    assertEquals(Optional.empty(), nauticalUnderlay.getAttribute(SAILOR, "foobar"));
    assertEquals(
        Optional.empty(),
        nauticalUnderlay.getAttribute(
            Entity.builder().name("foo").underlay("bar").build(), "name"));
  }

  @Test
  void resolveTable() {
    assertEquals(
        "my-project-id.nautical.sailors AS s",
        nauticalUnderlay
            .getUnderlaySqlResolver()
            .resolveTable(EntityVariable.create(SAILOR, Variable.create("s"))));
  }

  @Test
  public void resolveTableUnknownEntityThrows() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            nauticalUnderlay
                .getUnderlaySqlResolver()
                .resolveTable(
                    EntityVariable.create(
                        Entity.builder().underlay("foo").name("bar").build(),
                        Variable.create("f"))));
  }

  @Test
  public void resolveAttribute() {
    assertEquals(
        "s.s_name",
        nauticalUnderlay
            .getUnderlaySqlResolver()
            .resolve(AttributeVariable.create(SAILOR_NAME, Variable.create("s"))));
  }

  @Test
  public void resolveAttributeUnknownAttributeThrows() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            nauticalUnderlay
                .getUnderlaySqlResolver()
                .resolve(
                    AttributeVariable.create(
                        Attribute.builder()
                            .entity(Entity.builder().underlay("foo").name("bar").build())
                            .name("baz")
                            .dataType(DataType.INT64)
                            .build(),
                        Variable.create("f"))));
  }
}
