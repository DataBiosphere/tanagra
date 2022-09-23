package bio.terra.tanagra.indexing.command;

import bio.terra.tanagra.indexing.WorkflowCommand;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.underlay.Entity;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.apache.commons.text.StringSubstitutor;

public final class DenormalizeAllNodes extends WorkflowCommand {

  private DenormalizeAllNodes(String command, String description, Map<String, String> queryInputs) {
    super(command, description, queryInputs);
  }

  public static DenormalizeAllNodes forEntity(Entity entity) {
    String sqlFileSelectAll = entity.getName() + "_selectAll.sql";
    Query selectAllAttributes =
        entity.getSourceDataMapping().queryAttributes(entity.getAttributes());
    Map<String, String> queryInputs = Map.of(sqlFileSelectAll, selectAllAttributes.renderSQL());

    String template =
        "./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.WriteAllNodes "
            + "-Dexec.args=\"--outputBigQueryTable=${outputTable} "
            + "--allNodesQuery=${sqlFile_selectAll} "
            + "--runner=dataflow --project=broad-tanagra-dev --region=us-central1 "
            + "--serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com\"";
    Map<String, String> params =
        ImmutableMap.<String, String>builder()
            .put("outputTable", entity.getIndexDataMapping().getTablePointer().getPathForIndexing())
            .put("sqlFile_selectAll", sqlFileSelectAll)
            .build();
    String command = StringSubstitutor.replace(template, params);
    String description = entity.getName() + ": DenormalizeAllNodes";

    return new DenormalizeAllNodes(command, description, queryInputs);
  }
}
