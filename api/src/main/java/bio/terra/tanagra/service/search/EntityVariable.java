package bio.terra.tanagra.service.search;

import com.google.auto.value.AutoValue;

/** A variable binding for an entity. */
// DO NOT SUBMIT better comment.
@AutoValue
public abstract class EntityVariable {
  /* The entity of this variable's type.  */
  public abstract Entity entity();

  public abstract Variable variable();

  public static EntityVariable create(Entity entity, Variable variable) {
    return new AutoValue_EntityVariable(entity, variable);
  }
}
