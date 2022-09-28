package bio.terra.tanagra.indexing.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.indexing.BigQueryIndexingJob;
import bio.terra.tanagra.indexing.FileIO;
import bio.terra.tanagra.indexing.IndexingJob;
import bio.terra.tanagra.indexing.job.BuildNumChildrenAndPaths;
import bio.terra.tanagra.indexing.job.BuildTextSearchStrings;
import bio.terra.tanagra.indexing.job.DenormalizeEntityInstances;
import bio.terra.tanagra.indexing.job.WriteAncestorDescendantIdPairs;
import bio.terra.tanagra.indexing.job.WriteParentChildIdPairs;
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
    FileIO.setToReadResourceFiles();
    FileIO.setInputParentDir(Path.of("config"));
    Underlay underlay = Underlay.fromJSON("underlay/Omop.json");
    dataPointers = underlay.getDataPointers();
  }

  @Test
  void person() throws IOException {
    Entity person = Entity.fromJSON("Person.json", dataPointers);
    List<IndexingJob> jobs = person.getIndexingJobs();

    assertEquals(1, jobs.size(), "one indexing job generated");
    IndexingJob job = jobs.get(0);
    assertEquals(
        DenormalizeEntityInstances.class,
        job.getClass(),
        "DenormalizeEntityInstances indexing job generated");
    assertEquals(
        "broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.person",
        ((BigQueryIndexingJob) job).getOutputTablePointer().getPathForIndexing());
  }

  @Test
  void condition() throws IOException {
    Entity condition = Entity.fromJSON("Condition.json", dataPointers);
    List<IndexingJob> jobs = condition.getIndexingJobs();

    assertEquals(5, jobs.size(), "five indexing jobs generated");

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
            .getOutputTablePointer()
            .getPathForIndexing());

    Optional<IndexingJob> buildTextSearchStrings =
        jobs.stream()
            .filter(job -> job.getClass().equals(BuildTextSearchStrings.class))
            .findFirst();
    assertTrue(buildTextSearchStrings.isPresent(), "BuildTextSearchStrings indexing job generated");
    assertEquals(
        "broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.condition_textsearch",
        ((BigQueryIndexingJob) buildTextSearchStrings.get())
            .getOutputTablePointer()
            .getPathForIndexing());

    Optional<IndexingJob> writeParentChildIdPairs =
        jobs.stream()
            .filter(job -> job.getClass().equals(WriteParentChildIdPairs.class))
            .findFirst();
    assertTrue(
        writeParentChildIdPairs.isPresent(), "WriteParentChildIdPairs indexing job generated");
    assertEquals(
        "broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.condition_standard_childParent",
        ((BigQueryIndexingJob) writeParentChildIdPairs.get())
            .getOutputTablePointer()
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
        ((BigQueryIndexingJob) writeAncestorDescendantIdPairs.get())
            .getOutputTablePointer()
            .getPathForIndexing());

    Optional<IndexingJob> buildNumChildrenAndPaths =
        jobs.stream()
            .filter(job -> job.getClass().equals(BuildNumChildrenAndPaths.class))
            .findFirst();
    assertTrue(
        buildNumChildrenAndPaths.isPresent(), "BuildNumChildrenAndPaths indexing job generated");
    assertEquals(
        "broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.condition_standard_pathNumChildren",
        ((BigQueryIndexingJob) buildNumChildrenAndPaths.get())
            .getOutputTablePointer()
            .getPathForIndexing());
  }
}
