package bio.terra.tanagra.indexing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import bio.terra.tanagra.indexing.job.BigQueryIndexingJob;
import bio.terra.tanagra.indexing.job.IndexingJob;
import bio.terra.tanagra.indexing.job.bigquery.*;
import bio.terra.tanagra.indexing.job.dataflow.BuildNumChildrenAndPaths;
import bio.terra.tanagra.indexing.job.dataflow.WriteAncestorDescendantIdPairs;
import bio.terra.tanagra.indexing.jobexecutor.SequencedJobSet;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityGroup;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public class JobSequencerTest {
  @Test
  public void person() throws IOException {
    Entity person = Entity.fromJSON("Person.json", dataPointers);
    SequencedJobSet jobs = JobSequencer.getJobSetForEntity(person);

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
    SequencedJobSet jobs = JobSequencer.getJobSetForEntity(condition);

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

  @Test
  public void oneToMany() throws IOException {
    EntityGroup brandIngredient =
        EntityGroup.fromJSON("BrandIngredient.json", dataPointers, entities, primaryEntityName);
    SequencedJobSet jobs = JobSequencer.getJobSetForEntityGroup(brandIngredient);

    // copy relationship id pairs
    assertEquals(1, jobs.getNumStages());
  }

  @Test
  public void criteriaOccurrenceWithHierarchy() throws IOException {
    EntityGroup conditionPersonOccurrence =
        EntityGroup.fromJSON(
            "ConditionPersonOccurrence.json", dataPointers, entities, primaryEntityName);
    SequencedJobSet jobs = JobSequencer.getJobSetForEntityGroup(conditionPersonOccurrence);

    // copy relationship id pairs (x2 relationships)
    // compute rollup counts (x1 criteria-primary relationship)
    // compute rollup counts with hierarchy (x1 criteria-primary relationship)
    assertEquals(4, jobs.iterator().next().size());
  }
}
