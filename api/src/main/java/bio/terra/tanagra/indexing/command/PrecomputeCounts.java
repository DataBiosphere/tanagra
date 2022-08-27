package bio.terra.tanagra.indexing.command;

import bio.terra.tanagra.indexing.WorkflowCommand;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.text.StringSubstitutor;

public final class PrecomputeCounts extends WorkflowCommand {

  private PrecomputeCounts(String command, String description, Map<String, String> queryInputs) {
    super(command, description, queryInputs);
  }

  public static PrecomputeCounts forEntityGroup(CriteriaOccurrence entityGroup) {
    Map<String, String> queryInputs = new HashMap<>();
    String sqlFileSelectCriteriaIds = entityGroup.getName() + "_selectCriteriaIds.sql";
    Entity criteriaEntity = entityGroup.getCriteriaEntity();
    queryInputs.put(
        sqlFileSelectCriteriaIds,
        criteriaEntity
            .getSourceDataMapping()
            .queryAttributes(Map.of("node", criteriaEntity.getIdAttribute()))
            .renderSQL());

    String sqlFileSelectCriteriaPrimaryPairs =
        entityGroup.getName() + "_selectCriteriaPrimaryPairs.sql";
    queryInputs.put(
        sqlFileSelectCriteriaPrimaryPairs,
        entityGroup.queryCriteriaPrimaryPairs("node", "what_to_count").renderSQL());

    String template =
        "./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.PrecomputeCounts "
            + "-Dexec.args=\"--outputBigQueryTable=${outputTable} "
            + "--allPrimaryNodesQuery=${sqlFile_selectCriteriaIds} "
            + "--occurrencesQuery=${sqlFile_selectCriteriaPrimaryPairs} "
            // --ancestorDescendantRelationshipsQuery=${sqlFile_criteriaAncestorDescendant}
            + "--runner=dataflow --project=broad-tanagra-dev --region=us-central1 "
            + "--serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com\"";
    Map<String, String> params =
        ImmutableMap.<String, String>builder()
            .put(
                "outputTable",
                entityGroup
                    .getCriteriaPrimaryRollupCountAuxiliaryDataMapping()
                    .getTablePointer()
                    .getPathForIndexing())
            .put("sqlFile_selectCriteriaIds", sqlFileSelectCriteriaIds)
            .put("sqlFile_selectCriteriaPrimaryPairs", sqlFileSelectCriteriaPrimaryPairs)
            .build();
    String command = StringSubstitutor.replace(template, params);
    String description = entityGroup.getName() + ": PrecomputeCounts";

    return new PrecomputeCounts(command, description, queryInputs);
  }
}
