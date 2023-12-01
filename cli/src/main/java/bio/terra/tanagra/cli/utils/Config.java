package bio.terra.tanagra.cli.utils;

import bio.terra.tanagra.cli.command.Format;

/**
 * Internal representation of a configuration. An instance of this class is part of the current
 * context or state.
 */
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public final class Config {
  // Launch a browser automatically or not.
  public static final BrowserLaunchOption BROWSER_LAUNCH_OPTION = BrowserLaunchOption.AUTO;
  // log levels for file and stdout
  public static final Logger.LogLevel CONSOLE_LOGGING_LEVEL = Logger.LogLevel.OFF;
  public static final Logger.LogLevel FILE_LOGGING_LEVEL = Logger.LogLevel.INFO;
  // Output format option
  public static final Format.FormatOptions FORMAT = Format.FormatOptions.TEXT;

  /** Options for handling the browser during the OAuth process. */
  public enum BrowserLaunchOption {
    MANUAL,
    AUTO
  }
}
