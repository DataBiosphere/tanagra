package bio.terra.tanagra.indexing.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.indexing.FileIO;
import bio.terra.tanagra.indexing.WorkflowCommand;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityGroup;
import bio.terra.tanagra.underlay.Underlay;
import java.io.IOException;
import java.nio.file.Path;
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
    FileIO.setToReadResourceFiles();
    FileIO.setInputParentDir(Path.of("config"));
    Underlay underlay = Underlay.fromJSON("underlay/Omop.json");
    dataPointers = underlay.getDataPointers();
    entities = underlay.getEntities();
    primaryEntityName = underlay.getPrimaryEntity().getName();
  }

  @Test
  void oneToMany() throws IOException {
    EntityGroup brandIngredient =
        EntityGroup.fromJSON("BrandIngredient.json", dataPointers, entities, primaryEntityName);
    List<WorkflowCommand> cmds = brandIngredient.getIndexingCommands();

    assertEquals(0, cmds.size(), "no indexing cmds generated");
  }

  @Test
  void criteriaOccurrenceWithHierarchy() throws IOException {
    EntityGroup conditionPersonOccurrence =
        EntityGroup.fromJSON(
            "ConditionPersonOccurrence.json", dataPointers, entities, primaryEntityName);
    List<WorkflowCommand> cmds = conditionPersonOccurrence.getIndexingCommands();

    assertEquals(1, cmds.size(), "one indexing cmd generated");

    Optional<WorkflowCommand> precomputeCounts =
        cmds.stream().filter(cmd -> cmd.getClass().equals(PrecomputeCounts.class)).findFirst();
    assertTrue(precomputeCounts.isPresent(), "PrecomputeCounts indexing cmd generated");
    assertEquals(
        "./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.PrecomputeCounts -Dexec.args=\"--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.condition_person_occurrence_criteriaPrimaryRollupCount --allPrimaryNodesQuery=condition_person_occurrence_selectCriteriaIds.sql --occurrencesQuery=condition_person_occurrence_selectCriteriaPrimaryPairs.sql --ancestorDescendantRelationshipsQuery=condition_person_occurrence_selectCriteriaAncestorDescendantPairs.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com\"",
        precomputeCounts.get().getCommand());
    assertEquals(3, precomputeCounts.get().getQueryInputs().size(), "three query inputs generated");
  }
}
