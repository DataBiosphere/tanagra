package bio.terra.tanagra.indexing.command;

import static bio.terra.tanagra.indexing.Indexer.READ_RESOURCE_FILE_FUNCTION;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.tanagra.indexing.WorkflowCommand;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Underlay;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class IndexEntityTest {
  private static Map<String, DataPointer> dataPointers;

  @BeforeAll
  static void readDataPointers() throws IOException {
    Underlay underlay = Underlay.fromJSON("config/underlay/Omop.json", READ_RESOURCE_FILE_FUNCTION);
    dataPointers = underlay.getDataPointers();
  }

  @Test
  void simple() throws IOException {
    Entity person =
        Entity.fromJSON("config/entity/Person.json", READ_RESOURCE_FILE_FUNCTION, dataPointers);
    List<WorkflowCommand> cmds = person.getIndexingCommands();

    assertEquals(1, cmds.size(), "one indexing cmd generated");
    WorkflowCommand cmd = cmds.get(0);
    assertEquals(
        DenormalizeAllNodes.class, cmd.getClass(), "DenormalizeAllNodes indexing cmd generated");
    assertEquals(
        "./gradlew workflow:execute -DmainClass=bio.terra.tanagra.workflow.WriteAllNodes -Dexec.args=\"--outputBigQueryTable=verily-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.person --allNodesQuery=person_selectAll.sql --runner=dataflow --project=broad-tanagra-dev --region=us-central1 --serviceAccount=tanagra@broad-tanagra-dev.iam.gserviceaccount.com\"",
        cmd.getCommand());
    assertEquals(1, cmd.getQueryInputs().size(), "one query input generated");
  }
}
