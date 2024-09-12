package bio.terra.tanagra.cli.utils;

import com.google.common.base.Preconditions;
import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/**
 * Singleton class for holding a reference to input and output streams (e.g. stdin, stdout, stderr).
 * The purpose of holding these references in a single place is so that we can read/write in/output
 * throughout the codebase without passing around the streams from the top-level command classes.
 */
public final class UserIO {
  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(UserIO.class);
  private static final PrintStream DEFAULT_OUT_STREAM = System.out;
  private static final PrintStream DEFAULT_ERR_STREAM = System.err;
  private static final InputStream DEFAULT_IN_STREAM = System.in;
  private static UserIO userIOSingleton;
  private final PrintStream out;
  private final PrintStream err;
  private final InputStream in;

  /** Constructor that initializes the printer with the specified output streams. */
  private UserIO(PrintStream out, PrintStream err, InputStream in) {
    this.out = out;
    this.err = err;
    this.in = in;
  }

  /**
   * This method should be called exactly once to setup printing in the top-level command (i.e.
   * Main). It sets up the following:
   *
   * <p>- Sets the output stream pointers on the top-level command. This will recursively set the
   * pointers on all sub-commands also.
   *
   * <p>- Initializes the singleton Printer object that holds pointers to the output streams for use
   * elsewhere in the codebase (i.e. outside of command classes). This method uses default values
   * for these output streams.
   *
   * @param cmd picocli top-level command line object that holds pointers to the output streams
   */
  public static void setupPrinting(CommandLine cmd) {
    if (userIOSingleton == null) {
      initialize(DEFAULT_OUT_STREAM, DEFAULT_ERR_STREAM, DEFAULT_IN_STREAM);
    } else {
      LOGGER.warn(
          "Printing setup called multiple times. This is expected when testing, not during normal operation.");
    }
    cmd.setOut(getPrintWriter(userIOSingleton.out));
    cmd.setErr(getPrintWriter(userIOSingleton.err));
  }

  /**
   * This method initializes the singleton Printer object with the given output streams.
   *
   * <p>- Tests call this method directly to redirect the output.
   *
   * <p>- In normal operation, this method is called once from the {@link
   * #setupPrinting(CommandLine)} method.
   *
   * @param standardOut stream to write standard out to
   * @param standardErr stream to write standard err to
   */
  public static void initialize(
      PrintStream standardOut, PrintStream standardErr, InputStream standardIn) {
    userIOSingleton = new UserIO(standardOut, standardErr, standardIn);
  }

  /**
   * Utility method to get the output stream from the singleton.
   *
   * @return stream to write output (e.g. stdout)
   */
  public static PrintStream getOut() {
    if (userIOSingleton == null) {
      LOGGER.warn("Attempt to access printer output stream before setup.");
      return DEFAULT_OUT_STREAM;
    }
    return userIOSingleton.out;
  }

  /**
   * Utility method to get the error stream from the singleton.
   *
   * @return stream to write errors and running status (e.g. stderr)
   */
  public static PrintStream getErr() {
    if (userIOSingleton == null) {
      LOGGER.warn("Attempt to access printer error stream before setup.");
      return DEFAULT_ERR_STREAM;
    }
    return userIOSingleton.err;
  }

  /**
   * Utility method to get the input stream from the singleton.
   *
   * @return stream to read input from (e.g. stdin)
   */
  public static InputStream getIn() {
    if (userIOSingleton == null) {
      LOGGER.warn("Attempt to access printer input stream before setup.");
      return DEFAULT_IN_STREAM;
    }
    return userIOSingleton.in;
  }

  /** Utility method to get a UTF-8 encoded character output stream from a raw byte stream. */
  private static PrintWriter getPrintWriter(PrintStream printStream) {
    return new PrintWriter(printStream, true, StandardCharsets.UTF_8);
  }

  /** Utility method to sort and map a list's contents. */
  public static <F, T> List<T> sortAndMap(
      List<F> fromList, Comparator<F> sorter, Function<F, T> mapper) {
    return fromList.stream().sorted(sorter).map(mapper).collect(Collectors.toList());
  }

  /**
   * Open a browser at the given URL using {@link Desktop} if available, or alternatively output the
   * URL to {@link System#out} for command-line applications. (copied from google-oauth-client-java)
   *
   * @param url URL to browse
   */
  public static void browse(String url) {
    Preconditions.checkNotNull(url);
    getOut().println("Please open the following address in your browser:");
    getOut().println("  " + url);
    try {
      getOut().println("Attempting to open that address in the default browser now...");
      Desktop.getDesktop().browse(URI.create(url));
    } catch (IOException | InternalError e) {
      LOGGER.warn("Unable to open browser", e);
    }
  }
}
