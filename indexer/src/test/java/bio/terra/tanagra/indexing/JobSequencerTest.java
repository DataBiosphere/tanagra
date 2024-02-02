package bio.terra.tanagra.indexing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import bio.terra.tanagra.indexing.job.IndexingJob;
import bio.terra.tanagra.indexing.job.bigquery.CreateEntityMain;
import bio.terra.tanagra.indexing.job.bigquery.ValidateDataTypes;
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
import org.junit.Test;

public class JobSequencerTest {
  @Test
  public void person() {
    ConfigReader configReader = ConfigReader.fromJarResources();
    SZIndexer szIndexer = configReader.readIndexer("sd020230331_verily");
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
  }

  @Test
  public void condition() {
    ConfigReader configReader = ConfigReader.fromJarResources();
    SZIndexer szIndexer = configReader.readIndexer("sd020230331_verily");
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

  @Test
  public void brandIngredient() {
    ConfigReader configReader = ConfigReader.fromJarResources();
    SZIndexer szIndexer = configReader.readIndexer("sd020230331_verily");
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

  @Test
  public void conditionPerson() {
    ConfigReader configReader = ConfigReader.fromJarResources();
    SZIndexer szIndexer = configReader.readIndexer("sd020230331_verily");
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

  @Test
  public void filtered() {
    ConfigReader configReader = ConfigReader.fromJarResources();
    SZIndexer szIndexer = configReader.readIndexer("sd020230331_verily");
    SZUnderlay szUnderlay = configReader.readUnderlay(szIndexer.underlay);
    Underlay underlay = Underlay.fromConfig(szIndexer.bigQuery, szUnderlay, configReader);
    SequencedJobSet jobs =
        JobSequencer.getJobSetForEntity(szIndexer, underlay, underlay.getEntity("person"))
            .filterJobs(
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
