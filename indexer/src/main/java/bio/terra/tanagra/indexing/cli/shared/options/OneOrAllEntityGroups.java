package bio.terra.tanagra.indexing.cli.shared.options;

import bio.terra.tanagra.cli.exception.UserActionableException;
import picocli.CommandLine;

/**
 * Command helper class that defines the single entity group name and all entity groups flag
 * options.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class OneOrAllEntityGroups {
  @CommandLine.Option(names = "--group", description = "Entity group name.")
  public String entityGroup;

  @CommandLine.Option(names = "--all", description = "Include all entity groups.")
  public boolean allEntityGroups;

  public void validate() {
    if (!allEntityGroups && (entityGroup == null || entityGroup.isEmpty())) {
      throw new UserActionableException(
          "Either the entity group name or the all entity groups flag must be defined.");
    }
  }
}
