package bio.terra.tanagra.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for using Jackson to de/serialize JSON. This class maintains a singleton instance
 * of the Jackson {@link ObjectMapper}, to avoid re-loading the modules multiple times for a single
 * CLI command.
 */
public class JacksonMapper {
  private static final Logger logger = LoggerFactory.getLogger(JacksonMapper.class);

  private static ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  /** Getter for the singleton instance of the default Jackson {@link ObjectMapper} instance. */
  private static ObjectMapper getMapper() {
    return objectMapper.enable(JsonParser.Feature.ALLOW_COMMENTS);
  }

  /**
   * Getter for an instance of the Jackson {@link ObjectMapper}, with the specified Jackson features
   * enabled. If no Jackson features are specified (i.e. the list of mapper featuers is empty), then
   * this method is equivalent to the {@link #getMapper()} method.
   */
  private static ObjectMapper getMapper(List<MapperFeature> mapperFeatures) {
    // if no Jackson features are specified, just return the default mapper object
    if (mapperFeatures.size() == 0) {
      return getMapper();
    }

    // create a copy of the default mapper and enable any Jackson features specified
    ObjectMapper objectMapperWithFeatures = getMapper().copy();
    for (MapperFeature mapperFeature : mapperFeatures) {
      objectMapperWithFeatures.enable(mapperFeature);
    }
    return objectMapperWithFeatures;
  }

  /**
   * Read a JSON-formatted resource file into a Java object using the Jackson object mapper.
   *
   * @param resourceFilePath the resource file to read in
   * @param javaObjectClass the Java object class
   * @param <T> the Java object class to map the file contents to
   * @return an instance of the Java object class
   * @throws IOException if the stream to read in does not exist or is not readable
   */
  public static <T> T readFileIntoJavaObject(String resourceFilePath, Class<T> javaObjectClass)
      throws IOException {
    return readFileIntoJavaObject(resourceFilePath, javaObjectClass, Collections.emptyList());
  }

  /**
   * Read a JSON-formatted resource file into a Java object using the Jackson object mapper.
   *
   * @param resourceFilePath the resource file to read in
   * @param javaObjectClass the Java object class
   * @param mapperFeatures list of Jackson mapper features to enable
   * @param <T> the Java object class to map the file contents to
   * @return an instance of the Java object class
   * @throws IOException if the stream to read in does not exist or is not readable
   */
  public static <T> T readFileIntoJavaObject(
      String resourceFilePath, Class<T> javaObjectClass, List<MapperFeature> mapperFeatures)
      throws IOException {
    InputStream inputStream = getResourceFileStream(resourceFilePath);

    // use Jackson to map the file contents to an instance of the specified class
    ObjectMapper objectMapper = getMapper(mapperFeatures);

    // enable any Jackson features specified
    for (MapperFeature mapperFeature : mapperFeatures) {
      objectMapper.enable(mapperFeature);
    }

    try {
      return objectMapper.readValue(inputStream, javaObjectClass);
    } finally {
      inputStream.close();
    }
  }

  /**
   * Build a stream to a resource file contents.
   *
   * @return the new file stream
   * @throws FileNotFoundException if the resource file doesn't exist
   */
  private static InputStream getResourceFileStream(String resourceFilePath)
      throws FileNotFoundException {
    InputStream inputStream =
        JacksonMapper.class.getClassLoader().getResourceAsStream(resourceFilePath);
    if (inputStream == null) {
      throw new FileNotFoundException("Resource file not found: " + resourceFilePath);
    }
    return inputStream;
  }
}
