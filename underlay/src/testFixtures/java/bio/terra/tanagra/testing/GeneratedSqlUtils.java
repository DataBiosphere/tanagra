package bio.terra.tanagra.testing;

import bio.terra.tanagra.utils.SqlFormatter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for:
 *
 * <p>- comparing the SQL generated by the API code against an expected string saved in a golden
 * file.
 *
 * <p>- overwriting the existing golden file with a new expected string.
 */
public final class GeneratedSqlUtils {
  private static final Logger LOG = LoggerFactory.getLogger(GeneratedSqlUtils.class);
  private static final Path GENERATED_SQL_FILES_PARENT_DIR =
      Path.of(System.getProperty("GRADLE_PROJECT_DIR")).resolve("src/test/resources/");

  private GeneratedSqlUtils() {}

  /**
   * - If the `generateSqlFiles` Gradle property is not set, then check the generated SQL against
   * the contents of the existing file. This is the default, most common behavior.
   *
   * <p>- If the `-PgenerateSqlFiles=true` Gradle property is set, then overwrite the existing file
   * and return. This is intended as a convenience to regenerate SQL strings when there is a code
   * change in the SQL generation logic.
   *
   * @param generatedSql SQL string generated by the current API code
   * @param fileName the name of the file in the `src/test/resources/` directory where the generated
   *     SQL to compare against during testing lives.
   */
  public static void checkMatchesOrOverwriteGoldenFile(String generatedSql, String fileName)
      throws IOException {
    LOG.info(generatedSql);
    if (System.getProperty("GENERATE_SQL_FILES") == null) {
      LOG.info("reading generated sql from file because generateSqlFiles flag is not set");
      String expectedSql = readSqlFromFile(fileName);
      Assertions.assertEquals(
          expectedSql,
          SqlFormatter.format(generatedSql),
          "Generated SQL does not match the expected. To regenerate the golden files that contain the expected SQL, you can run `./gradlew cleanTest test --info -PgenerateSqlFiles=true`");
    } else {
      LOG.info("writing generated sql to file because generateSqlFiles flag is set");
      writeSqlToFile(SqlFormatter.format(generatedSql), fileName);
    }
  }

  /** Read the SQL string from a file. */
  private static String readSqlFromFile(String fileName) throws IOException {
    LOG.info("reading generated sql from file: {}", fileName);
    InputStream inputStream =
        GeneratedSqlUtils.class.getClassLoader().getResourceAsStream(fileName);

    InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
    BufferedReader reader = new BufferedReader(streamReader);
    StringBuilder fileContents = new StringBuilder();
    String line;
    boolean isFirstLine = true;
    while ((line = reader.readLine()) != null) {
      if (!isFirstLine) {
        fileContents.append(System.lineSeparator());
      }
      isFirstLine = false;
      fileContents.append(line);
    }
    reader.close();
    return fileContents.toString();
  }

  /** Write the generated SQL string to a file. */
  private static void writeSqlToFile(String generatedSql, String fileName) throws IOException {
    Path generatedSqlFile = GENERATED_SQL_FILES_PARENT_DIR.resolve(fileName).toAbsolutePath();
    LOG.info("writing generated sql to file: {}", generatedSqlFile);
    PrintWriter writer = new PrintWriter(generatedSqlFile.toFile(), StandardCharsets.UTF_8);
    writer.println(generatedSql);
    writer.close();
  }
}
