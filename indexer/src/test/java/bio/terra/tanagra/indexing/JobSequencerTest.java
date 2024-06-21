package bio.terra.tanagra.indexing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import bio.terra.tanagra.indexing.job.IndexingJob;
import bio.terra.tanagra.indexing.job.bigquery.CleanHierarchyNodesWithZeroCounts;
import bio.terra.tanagra.indexing.job.bigquery.CreateEntityMain;
import bio.terra.tanagra.indexing.job.bigquery.ValidateDataTypes;
import bio.terra.tanagra.indexing.job.bigquery.ValidateUniqueIds;
import bio.terra.tanagra.indexing.job.bigquery.WriteChildParent;
import bio.terra.tanagra.indexing.job.bigquery.WriteEntityAttributes;
import bio.terra.tanagra.indexing.job.bigquery.WriteEntityLevelDisplayHints;
import bio.terra.tanagra.indexing.job.bigquery.WriteRelationshipIntermediateTable;
import bio.terra.tanagra.indexing.job.bigquery.WriteTextSearchField;
import bio.terra.tanagra.indexing.job.dataflow.WriteAncestorDescendant;
import bio.terra.tanagra.indexing.job.dataflow.WriteNumChildrenAndPaths;
import bio.terra.tanagra.indexing.job.dataflow.WriteRollupCounts;
import bio.terra.tanagra.indexing.jobexecutor.SequencedJobSet;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.GroupItems;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class JobSequencerTest {
  public void person() {
    ConfigReader configReader = ConfigReader.fromJarResources();
    SZIndexer szIndexer = configReader.readIndexer("sd20230331_verily");
    SZUnderlay szUnderlay = configReader.readUnderlay(szIndexer.underlay);
    Underlay underlay = Underlay.fromConfig(szIndexer.bigQuery, szUnderlay, configReader);
    SequencedJobSet jobs =
        JobSequencer.getJobSetForEntity(szIndexer, underlay, underlay.getEntity("person"));

    assertEquals(4, jobs.getNumStages());
    Iterator<List<IndexingJob>> jobStageItr = jobs.iterator();

    IndexingJob job = jobStageItr.next().get(0);
    assertEquals(ValidateDataTypes.class, job.getClass());

    job = jobStageItr.next().get(0);
    assertEquals(CreateEntityMain.class, job.getClass());

    job = jobStageItr.next().get(0);
    assertEquals(WriteEntityAttributes.class, job.getClass());

    job = jobStageItr.next().get(0);
    assertEquals(ValidateUniqueIds.class, job.getClass());
  }

  public void condition() {
    ConfigReader configReader = ConfigReader.fromJarResources();
    SZIndexer szIndexer = configReader.readIndexer("sd20230331_verily");
    SZUnderlay szUnderlay = configReader.readUnderlay(szIndexer.underlay);
    Underlay underlay = Underlay.fromConfig(szIndexer.bigQuery, szUnderlay, configReader);
    SequencedJobSet jobs =
        JobSequencer.getJobSetForEntity(szIndexer, underlay, underlay.getEntity("condition"));

    assertEquals(6, jobs.getNumStages());
    Iterator<List<IndexingJob>> jobStageItr = jobs.iterator();

    IndexingJob job = jobStageItr.next().get(0);
    assertEquals(ValidateDataTypes.class, job.getClass());

    job = jobStageItr.next().get(0);
    assertEquals(CreateEntityMain.class, job.getClass());

    job = jobStageItr.next().get(0);
    assertEquals(WriteEntityAttributes.class, job.getClass());

    List<IndexingJob> jobStage = jobStageItr.next();
    Optional<IndexingJob> validateUniqueIds =
        jobStage.stream()
            .filter(jobInStage -> jobInStage.getClass().equals(ValidateUniqueIds.class))
            .findFirst();
    assertTrue(validateUniqueIds.isPresent());

    Optional<IndexingJob> writeEntityLevelDisplayHints =
        jobStage.stream()
            .filter(jobInStage -> jobInStage.getClass().equals(WriteEntityLevelDisplayHints.class))
            .findFirst();
    assertTrue(writeEntityLevelDisplayHints.isPresent());

    jobStage = jobStageItr.next();
    Optional<IndexingJob> buildTextSearchStrings =
        jobStage.stream()
            .filter(jobInStage -> jobInStage.getClass().equals(WriteTextSearchField.class))
            .findFirst();
    assertTrue(buildTextSearchStrings.isPresent());

    Optional<IndexingJob> writeParentChildIdPairs =
        jobStage.stream()
            .filter(jobInStage -> jobInStage.getClass().equals(WriteChildParent.class))
            .findFirst();
    assertTrue(writeParentChildIdPairs.isPresent());

    Optional<IndexingJob> writeAncestorDescendantIdPairs =
        jobStage.stream()
            .filter(jobInStage -> jobInStage.getClass().equals(WriteAncestorDescendant.class))
            .findFirst();
    assertTrue(writeAncestorDescendantIdPairs.isPresent());

    jobStage = jobStageItr.next();
    Optional<IndexingJob> buildNumChildrenAndPaths =
        jobStage.stream()
            .filter(jobInStage -> jobInStage.getClass().equals(WriteNumChildrenAndPaths.class))
            .findFirst();
    assertTrue(buildNumChildrenAndPaths.isPresent());
  }

  public void brandIngredient() {
    ConfigReader configReader = ConfigReader.fromJarResources();
    SZIndexer szIndexer = configReader.readIndexer("sd20230331_verily");
    SZUnderlay szUnderlay = configReader.readUnderlay(szIndexer.underlay);
    Underlay underlay = Underlay.fromConfig(szIndexer.bigQuery, szUnderlay, configReader);
    SequencedJobSet jobs =
        JobSequencer.getJobSetForGroupItems(
            szIndexer, underlay, (GroupItems) underlay.getEntityGroup("brandIngredient"));

    assertEquals(2, jobs.getNumStages());
    Iterator<List<IndexingJob>> jobStageItr = jobs.iterator();
    IndexingJob job = jobStageItr.next().get(0);
    assertEquals(WriteRelationshipIntermediateTable.class, job.getClass());

    job = jobStageItr.next().get(0);
    assertEquals(WriteRollupCounts.class, job.getClass());
  }

  public void conditionPerson() {
    ConfigReader configReader = ConfigReader.fromJarResources();
    SZIndexer szIndexer = configReader.readIndexer("sd20230331_verily");
    SZUnderlay szUnderlay = configReader.readUnderlay(szIndexer.underlay);
    Underlay underlay = Underlay.fromConfig(szIndexer.bigQuery, szUnderlay, configReader);
    SequencedJobSet jobs =
        JobSequencer.getJobSetForCriteriaOccurrence(
            szIndexer, underlay, (CriteriaOccurrence) underlay.getEntityGroup("conditionPerson"));

    assertEquals(2, jobs.getNumStages());
    Iterator<List<IndexingJob>> jobStageItr = jobs.iterator();
    IndexingJob job = jobStageItr.next().get(0);
    assertEquals(WriteRelationshipIntermediateTable.class, job.getClass());

    assertEquals(2, jobStageItr.next().size());
  }

  public void conditionPersonCleanHierarchyNodesWithZeroCounts() {
    ConfigReader configReader = ConfigReader.fromJarResources();
    SZIndexer szIndexer = configReader.readIndexer("aou/SC2023Q3R2");
    SZUnderlay szUnderlay = configReader.readUnderlay(szIndexer.underlay);
    Underlay underlay = Underlay.fromConfig(szIndexer.bigQuery, szUnderlay, configReader);
    SequencedJobSet jobs =
        JobSequencer.getJobSetForCriteriaOccurrence(
            szIndexer, underlay, (CriteriaOccurrence) underlay.getEntityGroup("conditionPerson"));

    assertEquals(3, jobs.getNumStages());
    Iterator<List<IndexingJob>> jobStageItr = jobs.iterator();

    // Assert stage 1 job is WriteRelationshipIntermediateTable
    List<IndexingJob> stage1Jobs = jobStageItr.next();
    assertEquals(1, stage1Jobs.size());
    assertEquals(WriteRelationshipIntermediateTable.class, stage1Jobs.get(0).getClass());

    // Assert stage 2 job is WriteRollupCounts
    List<IndexingJob> stage2Jobs = jobStageItr.next();
    assertEquals(2, stage2Jobs.size());
    assertEquals(WriteRollupCounts.class, stage2Jobs.get(0).getClass());
    assertEquals(WriteRollupCounts.class, stage2Jobs.get(1).getClass());

    // Assert stage 3 job is CleanHierarchyNodesWithZeroCounts
    List<IndexingJob> stage3Jobs = jobStageItr.next();
    assertEquals(1, stage3Jobs.size());
    assertEquals(CleanHierarchyNodesWithZeroCounts.class, stage3Jobs.get(0).getClass());
  }

  public void filtered() {
    ConfigReader configReader = ConfigReader.fromJarResources();
    SZIndexer szIndexer = configReader.readIndexer("sd20230331_verily");
    SZUnderlay szUnderlay = configReader.readUnderlay(szIndexer.underlay);
    Underlay underlay = Underlay.fromConfig(szIndexer.bigQuery, szUnderlay, configReader);
    SequencedJobSet jobs =
        JobSequencer.getJobSetForEntity(szIndexer, underlay, underlay.getEntity("person"));
    jobs.filterJobs(
        List.of(
            "bio.terra.tanagra.indexing.job.bigquery.ValidateDataTypes",
            "bio.terra.tanagra.indexing.job.bigquery.WriteEntityAttributes"));

    assertEquals(2, jobs.getNumStages());
    Iterator<List<IndexingJob>> jobStageItr = jobs.iterator();
    IndexingJob job = jobStageItr.next().get(0);
    assertEquals(ValidateDataTypes.class, job.getClass());

    job = jobStageItr.next().get(0);
    assertEquals(WriteEntityAttributes.class, job.getClass());
  }
}
