package bio.terra.tanagra.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for using Jackson to de/serialize JSON. This class maintains a singleton instance
 * of the Jackson {@link ObjectMapper}, to avoid re-loading the modules multiple times for a single
 * CLI command.
 */
public final class JacksonMapper {
  private static final Logger LOGGER = LoggerFactory.getLogger(JacksonMapper.class);

  private static JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();

  private JacksonMapper() {}

  /** Getter for the singleton instance of the default Jackson {@link JsonMapper} instance. */
  private static ObjectMapper getMapper() {
    return jsonMapper.enable(JsonParser.Feature.ALLOW_COMMENTS);
  }

  /**
   * Getter for an instance of the Jackson {@link ObjectMapper}, with the specified Jackson features
   * enabled. If no Jackson features are specified (i.e. the list of mapper featuers is empty), then
   * this method is equivalent to the {@link #getMapper()} method.
   */
  private static ObjectMapper getMapper(List<MapperFeature> mapperFeatures) {
    // if no Jackson features are specified, just return the default mapper object
    if (mapperFeatures.isEmpty()) {
      return getMapper();
    }

    // create a copy of the default mapper and enable any Jackson features specified
    JsonMapper.Builder objectMapperWithFeatures =
        JsonMapper.builder().findAndAddModules().enable(JsonParser.Feature.ALLOW_COMMENTS);
    for (MapperFeature mapperFeature : mapperFeatures) {
      objectMapperWithFeatures.enable(mapperFeature);
    }
    return objectMapperWithFeatures.build();
  }

  /**
   * Read a JSON-formatted file into a Java object using the Jackson object mapper.
   *
   * @param inputStream the file to read in
   * @param javaObjectClass the Java object class
   * @param <T> the Java object class to map the file contents to
   * @return an instance of the Java object class
   * @throws IOException if the stream to read in does not exist or is not readable
   */
  public static <T> T readFileIntoJavaObject(InputStream inputStream, Class<T> javaObjectClass)
      throws IOException {
    return readFileIntoJavaObject(
        inputStream,
        javaObjectClass,
        Collections.emptyList(),
        List.of(Pair.of(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)));
  }

  /**
   * Read a JSON-formatted file into a Java object using the Jackson object mapper.
   *
   * @param inputStream the file to read in
   * @param javaObjectClass the Java object class
   * @param mapperFeatures list of Jackson mapper features to enable
   * @param <T> the Java object class to map the file contents to
   * @return an instance of the Java object class
   * @throws IOException if the stream to read in does not exist or is not readable
   */
  private static <T> T readFileIntoJavaObject(
      InputStream inputStream,
      Class<T> javaObjectClass,
      List<MapperFeature> mapperFeatures,
      List<Pair<DeserializationFeature, Boolean>> deserializationFeatures)
      throws IOException {
    // Use Jackson to map the file contents to an instance of the specified class.
    ObjectMapper objectMapper = getMapper(mapperFeatures);

    deserializationFeatures.forEach(df -> objectMapper.configure(df.getKey(), df.getValue()));

    try (inputStream) {
      return objectMapper.readValue(inputStream, javaObjectClass);
    }
  }

  /**
   * Write a Java object to a JSON-formatted file using the Jackson object mapper.
   *
   * @param path the file path to write to
   * @param javaObject the Java object to write
   * @param <T> the Java object class to write
   */
  @SuppressFBWarnings(
      value = "RV_RETURN_VALUE_IGNORED",
      justification =
          "A file not found exception will be thrown anyway in this same method if the mkdirs or createNewFile calls fail.")
  public static <T> void writeJavaObjectToFile(Path path, T javaObject) throws IOException {
    // use Jackson to map the object to a JSON-formatted text block
    ObjectMapper objectMapper = getMapper();
    ObjectWriter objectWriter = objectMapper.writerWithDefaultPrettyPrinter();

    // create the file and any parent directories if they don't already exist
    FileUtils.createFile(path);

    LOGGER.debug("Serializing object with Jackson to file: {}", path);
    objectWriter.writeValue(path.toFile(), javaObject);
  }

  public static <T> String serializeJavaObject(T javaObject) throws JsonProcessingException {
    if (javaObject == null) {
      return null;
    }
    return getMapper().writeValueAsString(javaObject);
  }

  public static <T> T deserializeJavaObject(String jsonStr, Class<T> javaObjectClass)
      throws JsonProcessingException {
    if (jsonStr == null || jsonStr.isEmpty()) {
      return null;
    }
    return getMapper().readValue(jsonStr, javaObjectClass);
  }
}
