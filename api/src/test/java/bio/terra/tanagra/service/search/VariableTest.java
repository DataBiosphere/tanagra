package bio.terra.tanagra.service.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class VariableTest {

  @Test
  void createValidation() {
    assertEquals(Variable.create("x_2y"), Variable.create("x_2y"));
    // Empty not allowed.
    assertThrows(IllegalArgumentException.class, () -> Variable.create(""));
    // Non-letter leading character not allowed.
    assertThrows(IllegalArgumentException.class, () -> Variable.create("_abc"));
    // Uppercase not allowed.
    assertThrows(IllegalArgumentException.class, () -> Variable.create("X"));
    // Special characters not allowed.
    assertThrows(IllegalArgumentException.class, () -> Variable.create("x;"));
  }
}
