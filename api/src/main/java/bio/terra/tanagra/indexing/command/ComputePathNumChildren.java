package bio.terra.tanagra.indexing.command;

import bio.terra.tanagra.indexing.WorkflowCommand;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.HierarchyMapping;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.text.StringSubstitutor;

public final class ComputePathNumChildren extends WorkflowCommand {

  private ComputePathNumChildren(
      String command, String description, Map<String, String> queryInputs) {
    super(command, description, queryInputs);
  }

  public static ComputePathNumChildren forHierarchy(Entity entity, String hierarchyName) {
    HierarchyMapping hierarchySourceMapping =
        entity.getSourceDataMapping().getHierarchyMapping(hierarchyName);
    HierarchyMapping hierarchyIndexMapping =
        entity.getIndexDataMapping().getHierarchyMapping(hierarchyName);

    Map<String, String> queryInputs = new HashMap<>();
    String filePrefix = entity.getName() + "_" + hierarchyName;
    String sqlFileSelectIds = filePrefix + "_selectIds.sql";
    String sqlFileSelectParentChildPairs = filePrefix + "_selectParentChildPairs.sql";
    String sqlFileSelectPossibleRootNodes = filePrefix + "_selectPossibleRootNodes.sql";
    queryInputs.put(
        sqlFileSelectIds,
        entity
            .getSourceDataMapping()
            .queryAttributes(Map.of("node", entity.getIdAttribute()))
            .renderSQL());
    queryInputs.put(
        sqlFileSelectParentChildPairs,
        hierarchySourceMapping.queryChildParentPairs("child", "parent").renderSQL());
    if (hierarchySourceMapping.hasRootNodesFilter()) {
      queryInputs.put(
          sqlFileSelectPossibleRootNodes,
          hierarchySourceMapping.queryPossibleRootNodes("node").renderSQL());
    }

    String template =
        "./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.BuildPathsForHierarchy "
            + "-Dexec.args=\"--outputBigQueryTable=${outputTable} "
            + "--allNodesQuery=${sqlFile_selectIds} "
            + "--parentChildQuery=${sqlFile_selectParentChildPairs} "
            + (hierarchySourceMapping.hasRootNodesFilter()
                ? "--rootNodesFilterQuery=${sqlFile_selectPossibleRootNodes} "
                : "")
            + "--runner=dataflow --project=broad-tanagra-dev --region=us-central1 "
            + "--serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com\"";
    Map<String, String> params =
        ImmutableMap.<String, String>builder()
            .put(
                "outputTable",
                hierarchyIndexMapping.getPathNumChildren().getTablePointer().getPathForIndexing())
            .put("sqlFile_selectIds", sqlFileSelectIds)
            .put("sqlFile_selectParentChildPairs", sqlFileSelectParentChildPairs)
            .put("sqlFile_selectPossibleRootNodes", sqlFileSelectPossibleRootNodes)
            .build();
    String command = StringSubstitutor.replace(template, params);
    String description =
        entity.getName() + " (Hierarchy = " + hierarchyName + "): ComputePathNumChildren";

    return new ComputePathNumChildren(command, description, queryInputs);
  }
}
