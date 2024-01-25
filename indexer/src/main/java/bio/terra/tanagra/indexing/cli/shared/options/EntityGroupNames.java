package bio.terra.tanagra.indexing.cli.shared.options;

import bio.terra.tanagra.cli.exception.UserActionableException;
import java.util.List;
import picocli.CommandLine;

/**
 * Command helper class that defines the single entity group name and all entity groups flag
 * options.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class EntityGroupNames {
  @CommandLine.Option(
      names = "--names",
      split = ",",
      description = "Entity group name(s). Comma-separated list if more than one.")
  public List<String> names;

  @CommandLine.Option(
      names = "--all",
      description = "Include all entity groups.",
      showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
  public boolean allEntityGroups;

  public void validate() {
    if (!allEntityGroups && (names == null || names.isEmpty())) {
      throw new UserActionableException(
          "Either the entity group name or the all entity groups flag must be defined.");
    }
  }
}
