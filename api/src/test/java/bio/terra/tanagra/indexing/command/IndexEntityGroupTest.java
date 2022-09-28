package bio.terra.tanagra.indexing.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.indexing.BigQueryIndexingJob;
import bio.terra.tanagra.indexing.FileIO;
import bio.terra.tanagra.indexing.IndexingJob;
import bio.terra.tanagra.indexing.job.ComputeRollupCounts;
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
    List<IndexingJob> jobs = brandIngredient.getIndexingJobs();

    assertEquals(0, jobs.size(), "no indexing jobs generated");
  }

  @Test
  void criteriaOccurrenceWithHierarchy() throws IOException {
    EntityGroup conditionPersonOccurrence =
        EntityGroup.fromJSON(
            "ConditionPersonOccurrence.json", dataPointers, entities, primaryEntityName);
    List<IndexingJob> jobs = conditionPersonOccurrence.getIndexingJobs();

    assertEquals(1, jobs.size(), "one indexing job generated");

    Optional<IndexingJob> computeRollupCounts =
        jobs.stream().filter(job -> job.getClass().equals(ComputeRollupCounts.class)).findFirst();
    assertTrue(computeRollupCounts.isPresent(), "ComputeRollupCounts indexing job generated");
    assertEquals(
        "broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.condition_person_occurrence_criteriaPrimaryRollupCount",
        ((BigQueryIndexingJob) computeRollupCounts.get())
            .getOutputTablePointer()
            .getPathForIndexing());
  }
}
