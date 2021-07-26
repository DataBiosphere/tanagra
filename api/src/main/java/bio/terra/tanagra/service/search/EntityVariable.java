package bio.terra.tanagra.service.search;

import com.google.auto.value.AutoValue;

/** A variable binding for an entity. */
@AutoValue
public abstract class EntityVariable {
  /* The name of the variable. */
  public abstract String name();

  /* The entity of this variable's type.  */
  public abstract Entity entity();
}

// DO NOT SUBMIT also AttributeVariable?