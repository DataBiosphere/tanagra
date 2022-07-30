package bio.terra.tanagra.indexing.command;

import bio.terra.tanagra.indexing.WorkflowCommand;
import bio.terra.tanagra.underlay.Entity;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.apache.commons.text.StringSubstitutor;

public class DenormalizeAllNodes extends WorkflowCommand {

  private DenormalizeAllNodes(String command, String description, Map<String, String> queryInputs) {
    super(command, description, queryInputs);
  }

  public static DenormalizeAllNodes forEntity(Entity entity) {
    String template =
        "WriteAllNodes bash command needs: ${indexMapping_TablePointer}, ${entity_name}";
    Map<String, String> params =
        ImmutableMap.<String, String>builder()
            .put("entity_name", entity.getName())
            .put(
                "indexMapping_TablePointer",
                entity.getIndexDataMapping().getTablePointer().getSQL())
            .build();
    String command = StringSubstitutor.replace(template, params);
    String description = entity.getName() + ": " + DenormalizeAllNodes.class.getName();
    Map<String, String> queryInputs =
        Map.of("selectAllAttributes", entity.getSourceDataMapping().selectAllQuery());

    return new DenormalizeAllNodes(command, description, queryInputs);
  }
}
