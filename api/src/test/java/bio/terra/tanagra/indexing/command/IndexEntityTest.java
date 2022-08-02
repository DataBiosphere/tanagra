package bio.terra.tanagra.indexing.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.tanagra.indexing.WorkflowCommand;
import bio.terra.tanagra.serialization.UFEntity;
import bio.terra.tanagra.serialization.UFUnderlay;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.utils.FileUtils;
import bio.terra.tanagra.utils.JacksonMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class IndexEntityTest {
  static Map<String, DataPointer> dataPointers;

  @BeforeAll
  static void readDataPointers() throws IOException {
    UFUnderlay serialized =
        JacksonMapper.readFileIntoJavaObject(
            FileUtils.getResourceFileStream("config/underlay/Omop.json"), UFUnderlay.class);
    Underlay underlay = Underlay.deserialize(serialized, true);
    dataPointers = underlay.getDataPointers();
  }

  @Test
  void simple() throws IOException {
    UFEntity serialized =
        JacksonMapper.readFileIntoJavaObject(
            FileUtils.getResourceFileStream("config/entity/Person.json"), UFEntity.class);
    Entity person = Entity.deserialize(serialized, dataPointers);
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
