package bio.terra.tanagra.service.search;

import com.google.auto.value.AutoValue;

/**
 * A variable to bound to an entity to allow referring to multiple instances of the same entity.
 *
 * <p>A variable name must be lowercase a-z letters, numbers, and underscores, but must start with a
 * letter.
 */
@AutoValue
public abstract class Variable {
  public abstract String name();

  public static Variable create(String name) {
    NameUtils.checkName(name, "Variable name");
    return new AutoValue_Variable(name);
  }
}
