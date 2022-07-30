package bio.terra.tanagra.indexing.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.tanagra.indexing.WorkflowCommand;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Underlay;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class IndexEntityTest {
  static Map<String, DataPointer> dataPointers;

  @BeforeAll
  static void readDataPointers() {
    Underlay underlay = Underlay.fromJSON("config/underlay/Omop.json");
    dataPointers = underlay.getDataPointers();
  }

  @Test
  void simple() {
    Entity person = Entity.fromJSON("config/entity/Person.json", dataPointers);
    List<WorkflowCommand> cmds = person.getIndexingCommands();

    assertEquals(1, cmds.size(), "one indexing cmd generated");
    WorkflowCommand cmd = cmds.get(0);
    assertEquals(
        DenormalizeAllNodes.class, cmd.getClass(), "DenormalizeAllNodes indexing cmd generated");
    assertEquals(
        "WriteAllNodes bash command needs: `verily-tanagra-dev.aou_synthetic_index`.person, person",
        cmd.getCommand());
    assertEquals(1, cmd.getQueryInputs().size(), "one query input generated");
  }
}
