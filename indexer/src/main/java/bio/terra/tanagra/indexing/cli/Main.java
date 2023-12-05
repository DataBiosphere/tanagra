package bio.terra.tanagra.indexing.cli;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.cli.BaseMain;
import bio.terra.tanagra.exception.InvalidConfigException;
import picocli.CommandLine;

/**
 * This class corresponds to the top-level "tanagra" command. It is also the entry-point for the
 * picocli library.
 */
@CommandLine.Command(
    name = "tanagra",
    header = "Tanagra command-line interface.",
    subcommands = {Index.class, Clean.class})
public class Main extends BaseMain {
  /**
   * Main entry point into the CLI application. This creates and executes the top-level command,
   * sets the exit code and terminates the process.
   *
   * @param args from stdin
   */
  public static void main(String... args) {
    new Main().runCommandAndExit(args);
  }

  @Override
  public Boolean isUserActionableException(Exception ex) {
    return ex instanceof InvalidConfigException || ex instanceof NotFoundException;
  }
}
