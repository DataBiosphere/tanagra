package bio.terra.tanagra.indexing.command;

import bio.terra.tanagra.indexing.WorkflowCommand;
import bio.terra.tanagra.underlay.Entity;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.text.StringSubstitutor;

public class BuildTextSearch extends WorkflowCommand {

  private BuildTextSearch(String command, String description, Map<String, String> queryInputs) {
    super(command, description, queryInputs);
  }

  public static BuildTextSearch forEntity(Entity entity) {
    Map<String, String> queryInputs = new HashMap<>();
    String sqlFileSelectIds = entity.getName() + "_selectIds.sql";
    String sqlFileTextSearch = entity.getName() + "_textSearch.sql";
    queryInputs.put(
        sqlFileSelectIds,
        entity
            .getSourceDataMapping()
            .queryAttributes(List.of(entity.getIdAttribute()))
            .renderSQL());
    queryInputs.put(
        sqlFileTextSearch, entity.getSourceDataMapping().queryTextSearchStrings().renderSQL());

    String template =
        "./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.BuildTextSearchInformation "
            + "-Dexec.args=\"--outputBigQueryTable=${outputTable} "
            + "--allNodesQuery=${sqlFile_selectIds} "
            + "--searchStringsQuery=${sqlFile_textSearch} "
            + "--runner=dataflow --project=broad-tanagra-dev --region=us-central1 "
            + "--serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com\"";
    Map<String, String> params =
        ImmutableMap.<String, String>builder()
            .put("outputTable", entity.getIndexDataMapping().getTablePointer().getPathForIndexing())
            .put("sqlFile_selectIds", sqlFileSelectIds)
            .put("sqlFile_textSearch", sqlFileTextSearch)
            .build();
    String command = StringSubstitutor.replace(template, params);
    String description = entity.getName() + ": BuildTextSearch";

    return new BuildTextSearch(command, description, queryInputs);
  }
}
