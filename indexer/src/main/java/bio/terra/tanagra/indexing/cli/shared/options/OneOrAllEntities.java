package bio.terra.tanagra.indexing.cli.shared.options;

import bio.terra.tanagra.cli.exception.UserActionableException;
import picocli.CommandLine;

/**
 * Command helper class that defines the single entity name and all entities flag options.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class OneOrAllEntities {
  @CommandLine.Option(names = "--entity", description = "Entity name.")
  public String entity;

  @CommandLine.Option(names = "--all", description = "Include all entities.")
  public boolean allEntities;

  public void validate() {
    if (!allEntities && (entity == null || entity.isEmpty())) {
      throw new UserActionableException(
          "Either the entity name or the all entities flag must be defined.");
    }
  }
}
