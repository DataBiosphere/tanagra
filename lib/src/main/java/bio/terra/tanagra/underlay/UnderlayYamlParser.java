package bio.terra.tanagra.underlay;

import bio.terra.tanagra.proto.underlay.Underlay;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;

/** Parses YAML formatted Underlays. */
public class UnderlayYamlParser {
  private UnderlayYamlParser() {}

  public static Underlay parse(String yaml) throws IOException {
    ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    ObjectMapper jsonMapper = new ObjectMapper();
    String json = jsonMapper.writeValueAsString(yamlMapper.readValue(yaml, Object.class));

    Underlay.Builder builder = Underlay.newBuilder();
    JsonFormat.parser().merge(json, builder);
    return builder.build();
  }
}
