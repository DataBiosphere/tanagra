package bio.terra.tanagra.indexing;

import bio.terra.tanagra.utils.FileUtils;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.Function;

/**
 * This singleton class determines whether to interpret file paths as JAR resources or disk files.
 * This is needed for testing, when we need to read files from JAR resources. For normal operation,
 * we always read from disk directly.
 */
public final class FileIO {
  private static final Function<Path, InputStream> READ_RESOURCE_FILE_FUNCTION =
      filePath -> FileUtils.getResourceFileStream(filePath);
  private static final Function<Path, InputStream> READ_DISK_FILE_FUNCTION =
      filePath -> FileUtils.getFileStream(filePath);

  private static boolean readResourceFiles; // default to false = read disk, not resource, files

  private FileIO() {}

  public static void setToReadResourceFiles() {
    setReadFunctionType(true);
  }

  public static void setToReadDiskFiles() {
    setReadFunctionType(false);
  }

  private static void setReadFunctionType(boolean isResourceFile) {
    synchronized (FileIO.class) {
      readResourceFiles = isResourceFile;
    }
  }

  public static Function<Path, InputStream> getGetFileInputStreamFunction() {
    return readResourceFiles ? READ_RESOURCE_FILE_FUNCTION : READ_DISK_FILE_FUNCTION;
  }
}
