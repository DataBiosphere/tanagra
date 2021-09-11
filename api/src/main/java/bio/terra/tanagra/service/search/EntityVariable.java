package bio.terra.tanagra.service.search;

import bio.terra.tanagra.model.Entity;
import com.google.auto.value.AutoValue;

/**
 * A {@link Entity} bound to a variable.
 *
 * <p>e.g. "entity as x" in "SELECT x.* FROM entity as x".
 */
@AutoValue
public abstract class EntityVariable {
  /* The entity of this variable's type.  */
  public abstract Entity entity();

  /* The variable bound to the entity. */
  public abstract Variable variable();

  public static EntityVariable create(Entity entity, Variable variable) {
    return new AutoValue_EntityVariable(entity, variable);
  }
}
