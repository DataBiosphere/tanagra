package bio.terra.tanagra.cli.utils;

import bio.terra.tanagra.cli.exception.InternalErrorException;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class Context {
  public static final String LOGS_DIRNAME = "logs";
  private static final String LOG_FILENAME = "tanagra.log";
  public static final String IS_TEST = "IS_TEST";
  private static final String IS_TEST_TRUE_VALUE = "true";

  // Env var name to optionally override where the context is persisted on disk.
  private static final String CONTEXT_DIR_OVERRIDE_NAME = "TANAGRA_CONTEXT_PARENT_DIR";
  // File paths related to persisting the context on disk.
  private static final String CONTEXT_DIRNAME = ".tanagra";

  // Singleton object that defines the current context or state.
  private static Config currentConfig;

  private Context() {}

  public static void initialize() {
    currentConfig = new Config();
  }

  public static Config getConfig() {
    return currentConfig;
  }

  /**
   * Get the context directory.
   *
   * @return absolute path to context directory
   */
  public static Path getContextDir() {
    // Default to the user's home directory.
    Path contextPath = Paths.get(System.getProperty("user.home"));
    // If the override environment variable is set, use it instead.
    String overrideDirName = System.getenv(CONTEXT_DIR_OVERRIDE_NAME);
    if (overrideDirName != null && !overrideDirName.isBlank()) {
      contextPath = Paths.get(overrideDirName);
    }
    // If this is a test, append the current runner's ID. This lets us run multiple tests in
    // parallel without clobbering context across runners.
    String isTest = System.getProperty(IS_TEST);
    if (isTest != null && isTest.equals(IS_TEST_TRUE_VALUE)) {
      // cleanupTestUserWorkspaces uses CLI Test Harness to call commands outside of test context.
      // In this case IS_TEST is true, but "org.gradle.test.worker" is not set, causing testWorker
      // to be NULL.
      String testWorker = System.getProperty("org.gradle.test.worker");
      if (testWorker != null) {
        contextPath = contextPath.resolve(testWorker);
      }
    }
    // build.gradle test task makes contextDir. However, with test-runner specific directories,
    // this test is executed in a different place from where the test task mkdir was run. So need
    // to create directory for if tests are running in parallel.
    if (!contextPath.toAbsolutePath().toFile().exists()) {
      boolean dirCreated = contextPath.toAbsolutePath().toFile().mkdir();
      if (!dirCreated) {
        throw new InternalErrorException(
            "Error creating context directory: " + contextPath.toAbsolutePath());
      }
    }

    return contextPath.resolve(CONTEXT_DIRNAME).toAbsolutePath();
  }

  /**
   * Get the log file name.
   *
   * @return absolute path to the log file
   */
  public static Path getLogFile() {
    return getContextDir().resolve(LOGS_DIRNAME).resolve(LOG_FILENAME);
  }
}
