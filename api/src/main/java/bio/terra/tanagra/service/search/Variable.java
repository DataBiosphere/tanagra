package bio.terra.tanagra.service.search;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import java.util.regex.Pattern;

/**
 * A variable to bound to an entity to allow referring to multiple instances of the same entity.
 *
 * <p>A variable name must be lowercase a-z letters, numbers, and underscores, but must start with a
 * letter.
 */
@AutoValue
public abstract class Variable {
  private static final String NAME_REGEX = "[a-z][a-z0-9_]*";
  private static final Pattern NAME_VALIDATOR = Pattern.compile(NAME_REGEX);

  public abstract String name();

  public static Variable create(String name) {
    Preconditions.checkArgument(
        NAME_VALIDATOR.matcher(name).matches(),
        "Variable name must match regex '%s' but name was '%s'",
        NAME_REGEX,
        name);
    return new AutoValue_Variable(name);
  }
}
