package bio.terra.tanagra.cli.command;

import bio.terra.tanagra.cli.BaseMain;
import bio.terra.tanagra.cli.utils.UserIO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.PrintStream;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/**
 * Base class for all commands. This class handles:
 *
 * <p>- reading in the current global and workspace context
 *
 * <p>- setting up logging
 *
 * <p>- executing the command
 *
 * <p>Subclasses define how to execute the command (i.e. the implementation of {@link #execute}).
 */
@CommandLine.Command
@SuppressFBWarnings(
    value = {"ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", "MS_CANNOT_BE_FINAL"},
    justification =
        "Output streams are static, so the OUT & ERR properties should also be so. Each command "
            + "execution should only instantiate a single BaseCommand sub-class. So in practice, "
            + "the call() instance method will only be called once, and these static pointers to "
            + "the output streams will only be initialized once. And since picocli handles instantiating "
            + "the command classes, we can't set the output streams in the constructor and make them "
            + "static.")
public abstract class BaseCommand implements Callable<Integer> {
  private static final Logger LOGGER = LoggerFactory.getLogger(BaseCommand.class);

  // Output streams for commands to write to.
  @SuppressWarnings({
    "checkstyle:StaticVariableName",
    "PMD.FieldNamingConventions",
    "PMD.MutableStaticState"
  })
  protected static PrintStream OUT;

  @SuppressWarnings({
    "checkstyle:StaticVariableName",
    "PMD.FieldNamingConventions",
    "PMD.MutableStaticState"
  })
  protected static PrintStream ERR;

  @Override
  public Integer call() {
    // Pull the output streams from the singleton object setup by the top-level Main class
    // in the future, these streams could also be controlled by a global context property.
    OUT = UserIO.getOut();
    ERR = UserIO.getErr();

    // Execute the command
    LOGGER.debug("[COMMAND ARGS] " + String.join(" ", BaseMain.getArgList()));
    execute();

    // Set the command exit code
    return 0;
  }

  /**
   * Required override for executing this command and printing any output.
   *
   * <p>Subclasses should throw exceptions for errors. The Main class handles turning these
   * exceptions into the appropriate exit code.
   */
  protected abstract void execute();
}
