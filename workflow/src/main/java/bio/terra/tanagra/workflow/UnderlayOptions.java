package bio.terra.tanagra.workflow;

import bio.terra.tanagra.proto.underlay.Underlay;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;

/** Options used to select an {@link Underlay}. */
public interface UnderlayOptions extends PipelineOptions {
  @Description("Path to an Underlay yaml file for the Underlay to be used.")
  String getUnderlayYaml();

  void setUnderlayYaml(String underlayPath);

  /**
   * Parses and returns the underlay specified in the option.
   *
   * <p>This must only be used during pipeline setup and not during pipeline execution.
   */
  static Underlay readLocalUnderlay(UnderlayOptions options) throws IOException {
    String yamlUnderlay = Files.readString(Path.of(options.getUnderlayYaml()));
    String jsonUnderlay = yamlToJson(yamlUnderlay);
    Underlay.Builder builder = Underlay.newBuilder();
    JsonFormat.parser().merge(jsonUnderlay, builder);
    return builder.build();
  }

  /** Converts a yaml format string to a json format string. */
  private static String yamlToJson(String contents) throws JsonProcessingException {
    ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    ObjectMapper jsonMapper = new ObjectMapper();
    return jsonMapper.writeValueAsString(yamlMapper.readValue(contents, Object.class));
  }
}
