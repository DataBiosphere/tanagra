package bio.terra.tanagra.service.query.api;

import bio.terra.common.exception.BadRequestException;
import bio.terra.tanagra.service.search.EntityVariable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * The scope of {@link EntityVariable} bindings at a given level of API filter parsing.
 *
 * <p>Variables need to be declared with what entity they're binding to before they can be used.
 * VariableScope reflects that the variable naming does not have a global namespace, but that
 * there's lexical scoping.
 */
class VariableScope {

  /** The VariableScope that encloses this, or null if there is none. */
  @Nullable private final VariableScope enclosing;

  /** The variable names to {@link EntityVariable}s bound at this scope. */
  private final Map<String, EntityVariable> variables = new HashMap<>();

  VariableScope(@Nullable VariableScope enclosing) {
    this.enclosing = enclosing;
  }

  VariableScope() {
    this(null);
  }

  /** Adds the variable to this scope. Returns this. */
  public VariableScope add(EntityVariable entityVariable) {
    if (variables.put(entityVariable.variable().name(), entityVariable) != null) {
      throw new BadRequestException(
          String.format(
              "Duplicate variable name %s in single scope. Second EntityVariable: %s",
              entityVariable.variable().name(), entityVariable));
    }
    return this;
  }

  /**
   * Returns the {@link EntityVariable} matching the variable name, if this or recursively its
   * enclosing scope has a variable of that name.
   */
  public Optional<EntityVariable> get(String variableName) {
    EntityVariable result = variables.get(variableName);
    if (result != null) {
      return Optional.of(result);
    }
    if (enclosing == null) {
      return Optional.empty();
    }
    // Recurse to the enclosing scope if not present in this scope and there is an enclosing scope.
    return enclosing.get(variableName);
  }
}
