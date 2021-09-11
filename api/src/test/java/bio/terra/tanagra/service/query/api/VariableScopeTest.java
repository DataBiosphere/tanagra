package bio.terra.tanagra.service.query.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.common.exception.BadRequestException;
import bio.terra.tanagra.model.Entity;
import bio.terra.tanagra.service.search.EntityVariable;
import bio.terra.tanagra.service.search.Variable;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class VariableScopeTest {
  private static final Entity FOO = Entity.builder().underlay("u").name("foo").build();
  private static final Entity BAR = Entity.builder().underlay("u").name("bar").build();

  private static final EntityVariable X_FOO = EntityVariable.create(FOO, Variable.create("x"));
  private static final EntityVariable Y_FOO = EntityVariable.create(FOO, Variable.create("y"));
  private static final EntityVariable Z_FOO = EntityVariable.create(FOO, Variable.create("z"));
  private static final EntityVariable X_BAR = EntityVariable.create(BAR, Variable.create("x"));

  @Test
  void scope() {
    VariableScope outer = new VariableScope().add(Y_FOO).add(Z_FOO);
    VariableScope inner = new VariableScope(outer).add(X_FOO);

    assertEquals(inner.get("w"), Optional.empty());
    assertEquals(inner.get("x"), Optional.of(X_FOO));
    assertEquals(inner.get("y"), Optional.of(Y_FOO));
    assertEquals(inner.get("z"), Optional.of(Z_FOO));

    assertEquals(outer.get("w"), Optional.empty());
    assertEquals(outer.get("x"), Optional.empty());
    assertEquals(outer.get("y"), Optional.of(Y_FOO));
    assertEquals(outer.get("z"), Optional.of(Z_FOO));
  }

  /**
   * Test that the same variable cannot be associated with different EntityVariables at the same
   * scope.
   */
  @Test
  void duplicateVariableThrows() {
    VariableScope scope = new VariableScope();
    scope.add(X_FOO);
    assertThrows(BadRequestException.class, () -> scope.add(X_BAR));
  }

  /**
   * Test that the same variable can be associated with different EntityVariables at different
   * scopes.
   */
  @Test
  void shadowingAllowed() {
    VariableScope outer = new VariableScope().add(X_FOO);
    VariableScope inner = new VariableScope(outer).add(X_BAR);

    assertEquals(outer.get("x"), Optional.of(X_FOO));
    assertEquals(inner.get("x"), Optional.of(X_BAR));
  }
}
