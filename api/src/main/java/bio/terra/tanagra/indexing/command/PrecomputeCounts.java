package bio.terra.tanagra.indexing.command;

import bio.terra.tanagra.indexing.WorkflowCommand;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.text.StringSubstitutor;

public final class PrecomputeCounts extends WorkflowCommand {

  private PrecomputeCounts(String command, String description, Map<String, String> queryInputs) {
    super(command, description, queryInputs);
  }

  public static PrecomputeCounts forEntityGroup(CriteriaOccurrence entityGroup) {
    Map<String, String> queryInputs = new HashMap<>();
    String sqlFileSelectPrimaryIds = entityGroup.getName() + "_selectPrimaryIds.sql";
    Entity primaryEntity = entityGroup.getPrimaryEntity();
    queryInputs.put(
        sqlFileSelectPrimaryIds,
        primaryEntity
            .getSourceDataMapping()
            .queryAttributes(List.of(primaryEntity.getIdAttribute()))
            .renderSQL());

    String sqlFileSelectOccurrences = entityGroup.getName() + "_selectOccurrences.sql";
    queryInputs.put(
        sqlFileSelectOccurrences,
        entityGroup
            .getOccurrenceToPrimaryRelationshipMapping()
            .queryIdPairs("node", "what_to_count")
            .renderSQL());

    String template =
        "./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.PrecomputeCounts "
            + "-Dexec.args=\"--outputBigQueryTable=${outputTable} "
            + "--allPrimaryNodesQuery=${sqlFile_selectPrimaryIds} "
            + "--occurrencesQuery=${sqlFile_selectOccurrences} "
            // --ancestorDescendantRelationshipsQuery=${sqlFile_criteriaAncestorDescendent}
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
            .put("sqlFile_selectPrimaryIds", sqlFileSelectPrimaryIds)
            .put("sqlFile_selectOccurrences", sqlFileSelectOccurrences)
            .build();
    String command = StringSubstitutor.replace(template, params);
    String description = entityGroup.getName() + ": PrecomputeCounts";

    return new PrecomputeCounts(command, description, queryInputs);
  }
}
