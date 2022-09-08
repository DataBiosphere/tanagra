package bio.terra.tanagra.utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility methods for manipulating files on disk. */
public final class FileUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);

  private FileUtils() {}

  /**
   * Build a stream to a resource file.
   *
   * @return the new file stream
   * @throws RuntimeException if the resource file doesn't exist
   */
  public static InputStream getResourceFileStream(Path resourceFilePath) {
    InputStream inputStream =
        FileUtils.class.getClassLoader().getResourceAsStream(resourceFilePath.toString());
    if (inputStream == null) {
      throw new IllegalArgumentException("Resource file not found: " + resourceFilePath);
    }
    return inputStream;
  }

  /**
   * Build a stream to a file on disk.
   *
   * @return the new file stream
   * @throws RuntimeException if the file doesn't exist
   */
  public static InputStream getFileStream(Path filePath) {
    try {
      return Files.newInputStream(Path.of(filePath.toAbsolutePath().toString()));
    } catch (IOException ioEx) {
      throw new IllegalArgumentException("Error opening file stream: " + filePath, ioEx);
    }
  }

  /** Create the file and any parent directories if they don't already exist. */
  @SuppressFBWarnings(
      value = "RV_RETURN_VALUE_IGNORED",
      justification =
          "A file not found exception will be thrown anyway in the calling method if the mkdirs or createNewFile calls fail.")
  public static void createFile(Path path) throws IOException {
    File file = path.toFile();
    if (!file.exists()) {
      file.getParentFile().mkdirs();
      file.createNewFile();
    }
  }

  /**
   * Write a string directly to a file.
   *
   * @param path the file path to write to
   * @param fileContents the string to write
   * @return the file path that was written to
   */
  @SuppressFBWarnings(
      value = "RV_RETURN_VALUE_IGNORED",
      justification =
          "A file not found exception will be thrown anyway in this same method if the mkdirs or createNewFile calls fail.")
  public static Path writeStringToFile(Path path, String fileContents) throws IOException {
    LOGGER.debug("Writing to file: {}", path);

    // create the file and any parent directories if they don't already exist
    createFile(path);

    return Files.write(path, fileContents.getBytes(StandardCharsets.UTF_8));
  }
}
