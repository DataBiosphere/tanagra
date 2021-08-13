package bio.terra.tanagra.service.search;

import com.google.auto.value.AutoValue;

/**
 * A variable to bound to an entity to allow referring to multiple instances of the same entity.
 *
 * <p>A variable name must a be letters, numbers, and underscores, but must start with a
 * lower case letter.
 */
@AutoValue
public abstract class Variable {
  public abstract String name();

  public static Variable create(String name) {
    // Use NameUtils for consistency, even though Variables are only ever client generated.
    // We could consisder having a different naming validation as needed.
    NameUtils.checkName(name, "Variable name");
    return new AutoValue_Variable(name);
  }
}
