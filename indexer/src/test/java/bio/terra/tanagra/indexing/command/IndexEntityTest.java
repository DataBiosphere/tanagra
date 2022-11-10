package bio.terra.tanagra.indexing.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.indexing.BigQueryIndexingJob;
import bio.terra.tanagra.indexing.Indexer;
import bio.terra.tanagra.indexing.IndexingJob;
import bio.terra.tanagra.indexing.job.BuildNumChildrenAndPaths;
import bio.terra.tanagra.indexing.job.BuildTextSearchStrings;
import bio.terra.tanagra.indexing.job.CreateEntityTable;
import bio.terra.tanagra.indexing.job.DenormalizeEntityInstances;
import bio.terra.tanagra.indexing.job.WriteAncestorDescendantIdPairs;
import bio.terra.tanagra.indexing.job.WriteParentChildIdPairs;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.utils.FileIO;
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
    FileIO.setToReadResourceFiles();
    FileIO.setInputParentDir(Path.of("config"));
    Underlay underlay = Underlay.fromJSON("underlay/Omop.json");
    dataPointers = underlay.getDataPointers();
  }

  @Test
  void person() throws IOException {
    Entity person = Entity.fromJSON("Person.json", dataPointers);
    List<IndexingJob> jobs = Indexer.getJobsForEntity(person);

    assertEquals(2, jobs.size(), "two indexing jobs generated");
    IndexingJob job = jobs.get(0);
    assertEquals(
        CreateEntityTable.class, job.getClass(), "CreateEntityTable indexing job generated");

    job = jobs.get(1);
    assertEquals(
        DenormalizeEntityInstances.class,
        job.getClass(),
        "DenormalizeEntityInstances indexing job generated");
    assertEquals(
        "broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.person",
        ((BigQueryIndexingJob) job).getEntityIndexTable().getPathForIndexing());
  }

  @Test
  void condition() throws IOException {
    Entity condition = Entity.fromJSON("Condition.json", dataPointers);
    List<IndexingJob> jobs = Indexer.getJobsForEntity(condition);

    assertEquals(6, jobs.size(), "six indexing jobs generated");

    Optional<IndexingJob> createEntityTable =
        jobs.stream().filter(job -> job.getClass().equals(CreateEntityTable.class)).findFirst();
    assertTrue(createEntityTable.isPresent(), "CreateEntityTable indexing job generated");

    Optional<IndexingJob> denormalizeEntityInstances =
        jobs.stream()
            .filter(job -> job.getClass().equals(DenormalizeEntityInstances.class))
            .findFirst();
    assertTrue(
        denormalizeEntityInstances.isPresent(),
        "DenormalizeEntityInstances indexing job generated");
    assertEquals(
        "broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.condition",
        ((BigQueryIndexingJob) denormalizeEntityInstances.get())
            .getEntityIndexTable()
            .getPathForIndexing());

    Optional<IndexingJob> buildTextSearchStrings =
        jobs.stream()
            .filter(job -> job.getClass().equals(BuildTextSearchStrings.class))
            .findFirst();
    assertTrue(buildTextSearchStrings.isPresent(), "BuildTextSearchStrings indexing job generated");

    Optional<IndexingJob> writeParentChildIdPairs =
        jobs.stream()
            .filter(job -> job.getClass().equals(WriteParentChildIdPairs.class))
            .findFirst();
    assertTrue(
        writeParentChildIdPairs.isPresent(), "WriteParentChildIdPairs indexing job generated");
    assertEquals(
        "broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.condition_standard_childParent",
        ((WriteParentChildIdPairs) writeParentChildIdPairs.get())
            .getAuxiliaryTable()
            .getPathForIndexing());

    Optional<IndexingJob> writeAncestorDescendantIdPairs =
        jobs.stream()
            .filter(job -> job.getClass().equals(WriteAncestorDescendantIdPairs.class))
            .findFirst();
    assertTrue(
        writeAncestorDescendantIdPairs.isPresent(),
        "WriteAncestorDescendantIdPairs indexing job generated");
    assertEquals(
        "broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.condition_standard_ancestorDescendant",
        ((WriteAncestorDescendantIdPairs) writeAncestorDescendantIdPairs.get())
            .getAuxiliaryTable()
            .getPathForIndexing());

    Optional<IndexingJob> buildNumChildrenAndPaths =
        jobs.stream()
            .filter(job -> job.getClass().equals(BuildNumChildrenAndPaths.class))
            .findFirst();
    assertTrue(
        buildNumChildrenAndPaths.isPresent(), "BuildNumChildrenAndPaths indexing job generated");
    assertEquals(
        "broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.condition_standard_pathNumChildren",
        ((BuildNumChildrenAndPaths) buildNumChildrenAndPaths.get())
            .getTempTable()
            .getPathForIndexing());
  }
}
