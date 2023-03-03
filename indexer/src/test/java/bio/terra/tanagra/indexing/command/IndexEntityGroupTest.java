package bio.terra.tanagra.indexing.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.tanagra.indexing.Indexer;
import bio.terra.tanagra.indexing.jobexecutor.SequencedJobSet;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityGroup;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.utils.FileIO;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
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
    SequencedJobSet jobs = Indexer.getJobSetForEntityGroup(brandIngredient);

    // copy relationship id pairs
    assertEquals(1, jobs.getNumStages());
  }

  @Test
  void criteriaOccurrenceWithHierarchy() throws IOException {
    EntityGroup conditionPersonOccurrence =
        EntityGroup.fromJSON(
            "ConditionPersonOccurrence.json", dataPointers, entities, primaryEntityName);
    SequencedJobSet jobs = Indexer.getJobSetForEntityGroup(conditionPersonOccurrence);

    // copy relationship id pairs (x4 relationships)
    // compute rollup counts (x2 relationships)
    // compute rollup counts with hierarchy (x2 relationships)
    assertEquals(8, jobs.iterator().next().size());
  }
}
