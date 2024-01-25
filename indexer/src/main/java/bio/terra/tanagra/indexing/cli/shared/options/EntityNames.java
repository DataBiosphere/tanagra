package bio.terra.tanagra.indexing.cli.shared.options;

import bio.terra.tanagra.cli.exception.UserActionableException;
import java.util.List;
import picocli.CommandLine;

/**
 * Command helper class that defines the single entity name and all entities flag options.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class EntityNames {
  @CommandLine.Option(
      names = "--names",
      split = ",",
      description = "Entity name(s). Comma-separated list if more than one.")
  public List<String> names;

  @CommandLine.Option(
      names = "--all",
      description = "Include all entities.",
      showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
  public boolean allEntities;

  public void validate() {
    if (!allEntities && (names == null || names.isEmpty())) {
      throw new UserActionableException(
          "Either the entity name or the all entities flag must be defined.");
    }
  }
}
