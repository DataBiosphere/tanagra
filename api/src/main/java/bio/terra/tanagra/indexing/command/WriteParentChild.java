package bio.terra.tanagra.indexing.command;

import bio.terra.tanagra.indexing.WorkflowCommand;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.HierarchyMapping;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.text.StringSubstitutor;

public final class WriteParentChild extends WorkflowCommand {

  private WriteParentChild(String command, String description, Map<String, String> queryInputs) {
    super(command, description, queryInputs);
  }

  public static WriteParentChild forHierarchy(Entity entity, String hierarchyName) {
    HierarchyMapping hierarchySourceMapping =
        entity.getSourceDataMapping().getHierarchyMapping(hierarchyName);
    HierarchyMapping hierarchyIndexMapping =
        entity.getIndexDataMapping().getHierarchyMapping(hierarchyName);

    Map<String, String> queryInputs = new HashMap<>();
    String sqlFileSelectParentChildPairs =
        entity.getName() + "_" + hierarchyName + "_selectParentChildPairs.sql";
    queryInputs.put(
        sqlFileSelectParentChildPairs,
        hierarchySourceMapping.queryChildParentPairs("child", "parent").renderSQL());

    String template =
        "./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.WriteParentChildRelationships "
            + "-Dexec.args=\"--outputBigQueryTable=${outputTable} "
            + "--parentChildQuery=${sqlFile_selectParentChildPairs} "
            + "--runner=dataflow --project=broad-tanagra-dev --region=us-central1 "
            + "--serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com\"";
    Map<String, String> params =
        ImmutableMap.<String, String>builder()
            .put(
                "outputTable",
                hierarchyIndexMapping.getChildParent().getTablePointer().getPathForIndexing())
            .put("sqlFile_selectParentChildPairs", sqlFileSelectParentChildPairs)
            .build();
    String command = StringSubstitutor.replace(template, params);
    String description =
        entity.getName() + " (Hierarchy = " + hierarchyName + "): WriteParentChild";

    return new WriteParentChild(command, description, queryInputs);
  }
}
