package bio.terra.tanagra.indexing.command;

import static bio.terra.tanagra.indexing.Indexer.READ_RESOURCE_FILE_FUNCTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.indexing.WorkflowCommand;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Underlay;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class IndexEntityTest {
  private static Map<String, DataPointer> dataPointers;

  @BeforeAll
  static void readDataPointers() throws IOException {
    Underlay underlay =
        Underlay.fromJSON(Path.of("config/underlay/Omop.json"), READ_RESOURCE_FILE_FUNCTION);
    dataPointers = underlay.getDataPointers();
  }

  @Test
  void person() throws IOException {
    Entity person =
        Entity.fromJSON(
            Path.of("config/entity/Person.json"), READ_RESOURCE_FILE_FUNCTION, dataPointers);
    List<WorkflowCommand> cmds = person.getIndexingCommands();

    assertEquals(1, cmds.size(), "one indexing cmd generated");
    WorkflowCommand cmd = cmds.get(0);
    assertEquals(
        DenormalizeAllNodes.class, cmd.getClass(), "DenormalizeAllNodes indexing cmd generated");
    assertEquals(
        "./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.WriteAllNodes -Dexec.args=\"--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.person --allNodesQuery=person_selectAll.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com\"",
        cmd.getCommand());
    assertEquals(1, cmd.getQueryInputs().size(), "one query input generated");
  }

  @Test
  void condition() throws IOException {
    Entity condition =
        Entity.fromJSON(
            Path.of("config/entity/Condition.json"), READ_RESOURCE_FILE_FUNCTION, dataPointers);
    List<WorkflowCommand> cmds = condition.getIndexingCommands();

    assertEquals(5, cmds.size(), "five indexing cmds generated");

    Optional<WorkflowCommand> denormalizeAllNodes =
        cmds.stream().filter(cmd -> cmd.getClass().equals(DenormalizeAllNodes.class)).findFirst();
    assertTrue(denormalizeAllNodes.isPresent(), "DenormalizeAllNodes indexing cmd generated");
    assertEquals(
        "./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.WriteAllNodes -Dexec.args=\"--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.condition --allNodesQuery=condition_selectAll.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com\"",
        denormalizeAllNodes.get().getCommand());
    assertEquals(1, denormalizeAllNodes.get().getQueryInputs().size(), "one query input generated");

    Optional<WorkflowCommand> buildTextSearch =
        cmds.stream().filter(cmd -> cmd.getClass().equals(BuildTextSearch.class)).findFirst();
    assertTrue(buildTextSearch.isPresent(), "BuildTextSearch indexing cmd generated");
    assertEquals(
        "./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.BuildTextSearchInformation -Dexec.args=\"--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.condition --allNodesQuery=condition_selectIds.sql --searchStringsQuery=condition_textSearch.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com\"",
        buildTextSearch.get().getCommand());
    assertEquals(2, buildTextSearch.get().getQueryInputs().size(), "two query inputs generated");

    Optional<WorkflowCommand> writeParentChild =
        cmds.stream().filter(cmd -> cmd.getClass().equals(WriteParentChild.class)).findFirst();
    assertTrue(writeParentChild.isPresent(), "WriteParentChild indexing cmd generated");
    assertEquals(
        "./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.WriteParentChildRelationships -Dexec.args=\"--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.condition_standard_childParent --parentChildQuery=condition_standard_selectParentChildPairs.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com\"",
        writeParentChild.get().getCommand());
    assertEquals(1, writeParentChild.get().getQueryInputs().size(), "one query input generated");

    Optional<WorkflowCommand> computeAncestorDescendant =
        cmds.stream()
            .filter(cmd -> cmd.getClass().equals(ComputeAncestorDescendant.class))
            .findFirst();
    assertTrue(
        computeAncestorDescendant.isPresent(), "ComputeAncestorDescendant indexing cmd generated");
    assertEquals(
        "./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.FlattenHierarchy -Dexec.args=\"--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.condition_standard_ancestorDescendant --parentChildQuery=condition_standard_selectParentChildPairs.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com\"",
        computeAncestorDescendant.get().getCommand());
    assertEquals(
        1, computeAncestorDescendant.get().getQueryInputs().size(), "one query input generated");

    Optional<WorkflowCommand> computePathNumChildren =
        cmds.stream()
            .filter(cmd -> cmd.getClass().equals(ComputePathNumChildren.class))
            .findFirst();
    assertTrue(computePathNumChildren.isPresent(), "ComputePathNumChildren indexing cmd generated");
    assertEquals(
        "./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.BuildPathsForHierarchy -Dexec.args=\"--outputBigQueryTable=broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.condition --allNodesQuery=condition_standard_selectIds.sql --parentChildQuery=condition_standard_selectParentChildPairs.sql --rootNodesFilterQuery=condition_standard_selectPossibleRootNodes.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com\"",
        computePathNumChildren.get().getCommand());
    assertEquals(
        3, computePathNumChildren.get().getQueryInputs().size(), "three query inputs generated");
  }
}
