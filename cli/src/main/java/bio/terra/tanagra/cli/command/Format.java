package bio.terra.tanagra.cli.command;

import bio.terra.tanagra.cli.exception.InternalErrorException;
import bio.terra.tanagra.cli.utils.Config;
import bio.terra.tanagra.cli.utils.UserIO;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.Optional;
import java.util.function.Consumer;
import picocli.CommandLine;

/**
 * Command helper class that defines the --format flag and provides utility methods to printing a
 * return value out in different formats.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class Format {
  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper()
          .findAndRegisterModules()
          .enable(JsonParser.Feature.ALLOW_COMMENTS)
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .enable(SerializationFeature.WRITE_DATES_WITH_ZONE_ID);

  @CommandLine.Option(
      names = "--format",
      showDefaultValue = CommandLine.Help.Visibility.ALWAYS,
      description = "Set the format for printing command output: ${COMPLETION-CANDIDATES}.")
  @SuppressWarnings({"PMD.AvoidFieldNameMatchingTypeName", "PMD.ImmutableField"})
  private FormatOptions format = FormatOptions.TEXT;

  /**
   * Default implementation of printing this command's return value in JSON format. This method uses
   * Jackson for serialization.
   *
   * @param returnValue command return value
   */
  public static <T> void printJson(T returnValue) {
    // Use Jackson to map the object to a JSON-formatted text block.
    ObjectWriter objectWriter = OBJECT_MAPPER.writerWithDefaultPrettyPrinter();
    try {
      UserIO.getOut().println(objectWriter.writeValueAsString(returnValue));
    } catch (JsonProcessingException jsonEx) {
      throw new InternalErrorException("Error JSON-formatting the command return value.", jsonEx);
    }
  }

  /**
   * Default implementation of printing the return value. This method uses the {@link
   * Object#toString} method of the return value object, and prints nothing if this object is null.
   *
   * @param returnValue command return value
   */
  public static <T> void printText(T returnValue) {
    if (returnValue != null) {
      UserIO.getOut().println(returnValue);
    }
  }

  // Return the option in force, either from the --format passed in or the Config system.
  public FormatOptions getEffectiveFormatOption() {
    return Optional.ofNullable(format).orElse(Config.FORMAT);
  }

  /**
   * This method calls the {@link #printJson} method if the --format flag is set to JSON. Otherwise,
   * it calls the {@link #printText} method, passing the return value object as an argument.
   *
   * @param returnValue command return value
   */
  public <T> void printReturnValue(T returnValue) {
    printReturnValue(returnValue, Format::printText, Format::printJson);
  }

  /**
   * This method calls the {@link #printJson} method if the --format flag is set to JSON. Otherwise,
   * it calls the given printTextFunction.
   *
   * @param returnValue command return value
   * @param printTextFunction reference to function that accepts the command return value and prints
   *     it out in text format
   */
  public <T> void printReturnValue(T returnValue, Consumer<T> printTextFunction) {
    printReturnValue(returnValue, printTextFunction, Format::printJson);
  }

  /**
   * This method calls the given printJsonFunction if the --format flag is set to JSON. Otherwise,
   * it calls the given printTextFunction.
   *
   * @param returnValue command return value
   * @param printTextFunction reference to function that accepts the command return value and prints
   *     it out in text format
   * @param printJsonFunction reference to function that accepts the command return value and prints
   *     it out in JSON format
   */
  public <T> void printReturnValue(
      T returnValue, Consumer<T> printTextFunction, Consumer<T> printJsonFunction) {
    if (getEffectiveFormatOption() == FormatOptions.JSON) {
      printJsonFunction.accept(returnValue);
    } else {
      printTextFunction.accept(returnValue);
    }
  }

  /** This enum specifies the format options for printing the command output. */
  public enum FormatOptions {
    JSON,
    TEXT
  }
}
