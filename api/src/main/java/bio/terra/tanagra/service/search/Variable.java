package bio.terra.tanagra.service.search;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;

/** A variable to bound to an entity to allow referring to multiple instances of the same entity. */
@AutoValue
public abstract class Variable {
  public abstract String name();

  public static Variable create(String name) {
    Preconditions.checkArgument(!name.isEmpty(), "Variable names cannot be empty.");
    return new AutoValue_Variable(name);
  }
}
