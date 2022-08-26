package bio.terra.tanagra.indexing.command;

import static bio.terra.tanagra.indexing.Indexer.READ_RESOURCE_FILE_FUNCTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.indexing.WorkflowCommand;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityGroup;
import bio.terra.tanagra.underlay.Underlay;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class IndexEntityGroupTest {
  private static Map<String, DataPointer> dataPointers;
  private static Map<String, Entity> entities;
  private static String primaryEntityName;

  @BeforeAll
  static void readDataPointers() throws IOException {
    Underlay underlay = Underlay.fromJSON("config/underlay/Omop.json", READ_RESOURCE_FILE_FUNCTION);
    dataPointers = underlay.getDataPointers();
    entities = underlay.getEntities();
    primaryEntityName = underlay.getPrimaryEntity().getName();
  }

  @Test
  void oneToMany() throws IOException {
    EntityGroup brandIngredient =
        EntityGroup.fromJSON(
            "config/entitygroup/BrandIngredient.json",
            READ_RESOURCE_FILE_FUNCTION,
            dataPointers,
            entities,
            primaryEntityName);
    List<WorkflowCommand> cmds = brandIngredient.getIndexingCommands();

    assertEquals(0, cmds.size(), "no indexing cmds generated");
  }

  @Test
  void criteriaOccurrence() throws IOException {
    EntityGroup conditionPersonOccurrence =
        EntityGroup.fromJSON(
            "config/entitygroup/ConditionPersonOccurrence.json",
            READ_RESOURCE_FILE_FUNCTION,
            dataPointers,
            entities,
            primaryEntityName);
    List<WorkflowCommand> cmds = conditionPersonOccurrence.getIndexingCommands();

    assertEquals(1, cmds.size(), "one indexing cmd generated");

    Optional<WorkflowCommand> precomputeCounts =
        cmds.stream().filter(cmd -> cmd.getClass().equals(PrecomputeCounts.class)).findFirst();
    assertTrue(precomputeCounts.isPresent(), "PrecomputeCounts indexing cmd generated");
    assertEquals(
        "./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.PrecomputeCounts -Dexec.args=\"--outputBigQueryTable=verily-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.criteriaPrimaryRollupCount --allPrimaryNodesQuery=condition_person_occurrence_selectPrimaryIds.sql --occurrencesQuery=condition_person_occurrence_selectOccurrences.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com\"",
        precomputeCounts.get().getCommand());
    assertEquals(2, precomputeCounts.get().getQueryInputs().size(), "two query inputs generated");
  }
}
