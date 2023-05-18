package bio.terra.tanagra.indexing.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import bio.terra.tanagra.indexing.BigQueryIndexingJob;
import bio.terra.tanagra.indexing.Indexer;
import bio.terra.tanagra.indexing.IndexingJob;
import bio.terra.tanagra.indexing.job.BuildNumChildrenAndPaths;
import bio.terra.tanagra.indexing.job.BuildTextSearchStrings;
import bio.terra.tanagra.indexing.job.CreateEntityTable;
import bio.terra.tanagra.indexing.job.DenormalizeEntityInstances;
import bio.terra.tanagra.indexing.job.WriteAncestorDescendantIdPairs;
import bio.terra.tanagra.indexing.job.WriteParentChildIdPairs;
import bio.terra.tanagra.indexing.jobexecutor.SequencedJobSet;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.utils.FileIO;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.BeforeClass;
import org.junit.Test;

public class IndexEntityTest {
  private static Map<String, DataPointer> dataPointers;

  private static Indexer indexer;

  @BeforeClass
  public static void readDataPointers() throws IOException {
    FileIO.setToReadResourceFiles();
    FileIO.setInputParentDir(Path.of("config"));
    Underlay underlay = Underlay.fromJSON("underlay/Omop.json");
    dataPointers = underlay.getDataPointers();

    indexer = Indexer.deserializeUnderlay("underlay/Omop.json");
  }

  @Test
  public void person() throws IOException {
    Entity person = Entity.fromJSON("Person.json", dataPointers);
    SequencedJobSet jobs = indexer.getJobSetForEntity(person);

    assertEquals("two indexing job stages generated", 2, jobs.getNumStages());
    Iterator<List<IndexingJob>> jobStageItr = jobs.iterator();
    IndexingJob job = jobStageItr.next().get(0);
    assertEquals(
        "CreateEntityTable indexing job generated", CreateEntityTable.class, job.getClass());

    job = jobStageItr.next().get(0);
    assertEquals(
        "DenormalizeEntityInstances indexing job generated",
        DenormalizeEntityInstances.class,
        job.getClass());
    assertEquals(
        "broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.person",
        ((BigQueryIndexingJob) job).getEntityIndexTable().getPathForIndexing());
  }

  @Test
  public void condition() throws IOException {
    Entity condition = Entity.fromJSON("Condition.json", dataPointers);
    SequencedJobSet jobs = indexer.getJobSetForEntity(condition);

    assertEquals("three indexing job stages generated", 3, jobs.getNumStages());
    Iterator<List<IndexingJob>> jobStageItr = jobs.iterator();
    IndexingJob job = jobStageItr.next().get(0);
    assertEquals(
        "CreateEntityTable indexing job generated", CreateEntityTable.class, job.getClass());

    job = jobStageItr.next().get(0);
    assertEquals(
        "DenormalizeEntityInstances indexing job generated",
        DenormalizeEntityInstances.class,
        job.getClass());
    assertEquals(
        "broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.condition",
        ((BigQueryIndexingJob) job).getEntityIndexTable().getPathForIndexing());

    List<IndexingJob> jobStage = jobStageItr.next();
    Optional<IndexingJob> buildTextSearchStrings =
        jobStage.stream()
            .filter(jobInStage -> jobInStage.getClass().equals(BuildTextSearchStrings.class))
            .findFirst();
    assertTrue("BuildTextSearchStrings indexing job generated", buildTextSearchStrings.isPresent());

    Optional<IndexingJob> writeParentChildIdPairs =
        jobStage.stream()
            .filter(jobInStage -> jobInStage.getClass().equals(WriteParentChildIdPairs.class))
            .findFirst();
    assertTrue(
        "WriteParentChildIdPairs indexing job generated", writeParentChildIdPairs.isPresent());
    assertEquals(
        "broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.condition_standard_childParent",
        ((WriteParentChildIdPairs) writeParentChildIdPairs.get())
            .getAuxiliaryTable()
            .getPathForIndexing());

    Optional<IndexingJob> writeAncestorDescendantIdPairs =
        jobStage.stream()
            .filter(
                jobInStage -> jobInStage.getClass().equals(WriteAncestorDescendantIdPairs.class))
            .findFirst();
    assertTrue(
        "WriteAncestorDescendantIdPairs indexing job generated",
        writeAncestorDescendantIdPairs.isPresent());
    assertEquals(
        "broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.condition_standard_ancestorDescendant",
        ((WriteAncestorDescendantIdPairs) writeAncestorDescendantIdPairs.get())
            .getAuxiliaryTable()
            .getPathForIndexing());

    Optional<IndexingJob> buildNumChildrenAndPaths =
        jobStage.stream()
            .filter(jobInStage -> jobInStage.getClass().equals(BuildNumChildrenAndPaths.class))
            .findFirst();
    assertTrue(
        "BuildNumChildrenAndPaths indexing job generated", buildNumChildrenAndPaths.isPresent());
    assertEquals(
        "broad-tanagra-dev:aou_synthetic_SR2019q4r4_indexes.condition_standard_pathNumChildren",
        ((BuildNumChildrenAndPaths) buildNumChildrenAndPaths.get())
            .getTempTable()
            .getPathForIndexing());
  }
}
