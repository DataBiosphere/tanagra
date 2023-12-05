package bio.terra.tanagra.cli;

import bio.terra.tanagra.cli.exception.InternalErrorException;
import bio.terra.tanagra.cli.exception.UserActionableException;
import bio.terra.tanagra.cli.utils.Context;
import bio.terra.tanagra.cli.utils.Logger;
import bio.terra.tanagra.cli.utils.UserIO;
import com.google.common.annotations.VisibleForTesting;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/**
 * This class corresponds to the top-level command. It is also the entry-point for the picocli
 * library.
 */
@CommandLine.Command(
    exitCodeListHeading = "Exit codes: \n",
    exitCodeList = {
      "0 : Successful program execution",
      "1 : User-actionable error (e.g. missing parameter)",
      "2 : System or internal error (e.g. validation error from within the Tanagra code)",
      "3 : Unexpected error (e.g. Java exception)"
    })
public abstract class BaseMain implements Runnable {
  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(BaseMain.class);

  // Color scheme used by all commands.
  private static final CommandLine.Help.ColorScheme COLOR_SCHEME =
      new CommandLine.Help.ColorScheme.Builder()
          .commands(CommandLine.Help.Ansi.Style.bold)
          .options(CommandLine.Help.Ansi.Style.fg_yellow)
          .parameters(CommandLine.Help.Ansi.Style.fg_yellow)
          .optionParams(CommandLine.Help.Ansi.Style.italic)
          .errors(CommandLine.Help.Ansi.Style.fg_blue)
          .stackTraces(CommandLine.Help.Ansi.Style.italic)
          .build();

  /**
   * Create and execute the top-level command. Tests call this method instead of {@link
   * #runCommandAndExit(String...)} so that the process isn't terminated.
   *
   * @param args command and arguments
   * @return process exit code
   */
  @VisibleForTesting
  public int runCommand(String... args) {
    CommandLine cmd = new CommandLine(this);
    cmd.setExecutionStrategy(new CommandLine.RunLast());
    cmd.setExecutionExceptionHandler(
        new UserActionableAndSystemExceptionHandler(this::isUserActionableException));
    cmd.setColorScheme(COLOR_SCHEME);
    cmd.setCaseInsensitiveEnumValuesAllowed(true);

    // Set the output and error streams to the defaults: stdout, stderr
    // save pointers to these streams in a singleton class, so we can access them throughout the
    // codebase without passing them around.
    UserIO.setupPrinting(cmd);

    // Initialize the context and setup logging.
    Context.initialize();
    Logger.setupLogging(
        Context.getConfig().CONSOLE_LOGGING_LEVEL, Context.getConfig().FILE_LOGGING_LEVEL);

    // Log the command
    LOGGER.debug("[COMMAND RUN] main " + String.join(" ", Arrays.asList(args)));

    // Delegate to the appropriate command class, or print the usage if no command was specified.
    int exitCode = cmd.execute(args);
    if (args.length == 0) {
      cmd.usage(cmd.getOut());
    }

    return exitCode;
  }

  public Boolean isUserActionableException(Exception ex) {
    return false;
  }

  /**
   * Creates and execute the top-level command, set the exit code and terminate the process.
   *
   * <p>This method should be called from child classes static main method.
   *
   * @param args from stdin
   */
  public void runCommandAndExit(String... args) {
    // Run the command.
    int exitCode = runCommand(args);

    // Set the exit code and terminate the process.
    System.exit(exitCode);
  }

  /** Required method to implement Runnable, but not actually called by picocli. */
  @Override
  @SuppressWarnings({
    "PMD.EmptyMethodInAbstractClassShouldBeAbstract",
    "PMD.UncommentedEmptyMethodBody"
  })
  public void run() {}

  /**
   * Custom handler class that intercepts all exceptions.
   *
   * <p>There are three categories of exceptions. All print a message to stderr and log the
   * exception.
   *
   * <p>- UserActionable = user can fix
   *
   * <p>- System = user cannot fix, exception specifically thrown by CLI code
   *
   * <p>- Unexpected = user cannot fix, exception not thrown by CLI code
   *
   * <p>The System and Unexpected cases are very similar, except that the message on the system
   * exception might be more readable/relevant.
   */
  private static class UserActionableAndSystemExceptionHandler
      implements CommandLine.IExecutionExceptionHandler {

    // Color scheme used for printing out system and unexpected errors.
    // There is only a single error style that you can define for all commands, and we are already
    // using that for user-actionable errors.
    private static final CommandLine.Help.ColorScheme SYSTEM_AND_UNEXPECTED_ERROR_STYLE =
        new CommandLine.Help.ColorScheme.Builder()
            .errors(CommandLine.Help.Ansi.Style.fg_red, CommandLine.Help.Ansi.Style.bold)
            .build();

    // Exit codes to use for each type of exception thrown.
    private static final int USER_ACTIONABLE_EXIT_CODE = 1;
    private static final int SYSTEM_EXIT_CODE = 2;
    private static final int UNEXPECTED_EXIT_CODE = 3;

    private final Function<Exception, Boolean> isUserActionableExceptionFn;

    UserActionableAndSystemExceptionHandler(
        Function<Exception, Boolean> isUserActionableExceptionFn) {
      this.isUserActionableExceptionFn = isUserActionableExceptionFn;
    }

    @Override
    public int handleExecutionException(
        Exception ex, CommandLine cmd, CommandLine.ParseResult parseResult) {
      String errorMessage;
      CommandLine.Help.Ansi.Text formattedErrorMessage;
      int exitCode;
      boolean printPointerToLogFile;
      if (ex instanceof UserActionableException || isUserActionableExceptionFn.apply(ex)) {
        errorMessage = ex.getMessage();
        formattedErrorMessage =
            cmd.getColorScheme()
                .errorText(
                    Objects.requireNonNullElse(
                        errorMessage, ex.getClass().getName() + ": Error message not found."));
        exitCode = USER_ACTIONABLE_EXIT_CODE;
        printPointerToLogFile = false;
      } else if (ex instanceof InternalErrorException) {
        errorMessage = ex.getMessage();
        formattedErrorMessage =
            SYSTEM_AND_UNEXPECTED_ERROR_STYLE.errorText("[ERROR] ").concat(errorMessage);
        exitCode = SYSTEM_EXIT_CODE;
        printPointerToLogFile = true;
      } else {
        errorMessage =
            "An unexpected error occurred in "
                + ex.getClass().getCanonicalName()
                + ": "
                + ex.getMessage();
        formattedErrorMessage =
            SYSTEM_AND_UNEXPECTED_ERROR_STYLE.errorText("[ERROR] ").concat(errorMessage);
        exitCode = UNEXPECTED_EXIT_CODE;
        printPointerToLogFile = true;
      }

      // Print the error for the user.
      cmd.getErr().println(formattedErrorMessage);
      if (printPointerToLogFile) {
        cmd.getErr()
            .println(
                cmd.getColorScheme()
                    .stackTraceText("See " + Context.getLogFile() + " for more information"));
      }

      // Log the exact message that was printed to the console, for easier debugging.
      LOGGER.error(errorMessage, ex);

      // Set the process return code.
      return exitCode;
    }
  }
}
