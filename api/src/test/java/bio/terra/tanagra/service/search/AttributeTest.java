package bio.terra.tanagra.service.search;

import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.tanagra.model.Attribute;
import bio.terra.tanagra.model.DataType;
import bio.terra.tanagra.model.Entity;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class AttributeTest {

  @Test
  void nameValidation() {
    Entity entity = Entity.builder().name("foo").underlay("bar").build();
    Attribute.builder().name("foo_Bar").dataType(DataType.STRING).entity(entity).build();
    assertThrows(
        IllegalArgumentException.class,
        () -> Attribute.builder().name("").dataType(DataType.STRING).entity(entity).build());
    assertThrows(
        IllegalArgumentException.class,
        () -> Attribute.builder().name("f?:").dataType(DataType.STRING).entity(entity).build());
  }
}
