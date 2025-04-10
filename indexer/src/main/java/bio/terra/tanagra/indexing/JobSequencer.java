package bio.terra.tanagra.indexing;

import bio.terra.tanagra.indexing.job.IndexingJob;
import bio.terra.tanagra.indexing.job.bigquery.CleanHierarchyNodesWithZeroCounts;
import bio.terra.tanagra.indexing.job.bigquery.CreateEntityMain;
import bio.terra.tanagra.indexing.job.bigquery.ValidateDataTypes;
import bio.terra.tanagra.indexing.job.bigquery.ValidateUniqueIds;
import bio.terra.tanagra.indexing.job.bigquery.WriteChildParent;
import bio.terra.tanagra.indexing.job.bigquery.WriteEntityAttributes;
import bio.terra.tanagra.indexing.job.bigquery.WriteEntityLevelDisplayHints;
import bio.terra.tanagra.indexing.job.bigquery.WriteEntitySearchByAttributes;
import bio.terra.tanagra.indexing.job.bigquery.WriteRelationshipIntermediateTable;
import bio.terra.tanagra.indexing.job.bigquery.WriteTextSearchField;
import bio.terra.tanagra.indexing.job.dataflow.WriteAncestorDescendant;
import bio.terra.tanagra.indexing.job.dataflow.WriteInstanceLevelDisplayHints;
import bio.terra.tanagra.indexing.job.dataflow.WriteNumChildrenAndPaths;
import bio.terra.tanagra.indexing.job.dataflow.WriteRollupCounts;
import bio.terra.tanagra.indexing.jobexecutor.JobRunner;
import bio.terra.tanagra.indexing.jobexecutor.ParallelRunner;
import bio.terra.tanagra.indexing.jobexecutor.SequencedJobSet;
import bio.terra.tanagra.indexing.jobexecutor.SerialRunner;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.entitymodel.Relationship;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.EntityGroup;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.GroupItems;
import bio.terra.tanagra.underlay.indextable.ITEntityLevelDisplayHints;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.indextable.ITEntitySearchByAttributes;
import bio.terra.tanagra.underlay.indextable.ITHierarchyAncestorDescendant;
import bio.terra.tanagra.underlay.indextable.ITHierarchyChildParent;
import bio.terra.tanagra.underlay.indextable.ITRelationshipIdPairs;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import bio.terra.tanagra.underlay.sourcetable.STEntityAttributes;
import bio.terra.tanagra.underlay.sourcetable.STHierarchyChildParent;
import bio.terra.tanagra.underlay.sourcetable.STHierarchyRootFilter;
import bio.terra.tanagra.underlay.sourcetable.STRelationshipIdPairs;
import bio.terra.tanagra.underlay.sourcetable.STRelationshipRollupCounts;
import bio.terra.tanagra.underlay.sourcetable.STTextSearchTerms;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class JobSequencer {
  private JobSequencer() {}

  public static SequencedJobSet getJobSetForEntity(
      SZIndexer indexerConfig, Underlay underlay, Entity entity) {
    SequencedJobSet jobSet = new SequencedJobSet(entity.getName());
    jobSet.startNewStage();
    STEntityAttributes sourceEntityAttributes =
        underlay.getSourceSchema().getEntityAttributes(entity.getName());
    ITEntityMain indexEntityMain = underlay.getIndexSchema().getEntityMain(entity.getName());
    jobSet.addJob(
        new ValidateDataTypes(indexerConfig, entity, sourceEntityAttributes, indexEntityMain));

    jobSet.startNewStage();
    jobSet.addJob(new CreateEntityMain(indexerConfig, entity, indexEntityMain));

    jobSet.startNewStage();
    jobSet.addJob(
        new WriteEntityAttributes(indexerConfig, sourceEntityAttributes, indexEntityMain));

    jobSet.startNewStage();
    jobSet.addJob(new ValidateUniqueIds(indexerConfig, entity, indexEntityMain));
    ITEntityLevelDisplayHints indexEntityHints =
        underlay.getIndexSchema().getEntityLevelDisplayHints(entity.getName());
    jobSet.addJob(
        new WriteEntityLevelDisplayHints(indexerConfig, entity, indexEntityMain, indexEntityHints));

    if (entity.hasOptimizeSearchByAttributes()) {
      jobSet.startNewStage();

      entity
          .getOptimizeSearchByAttributeNames()
          .forEach(
              searchTableAttributeNames -> {
                ITEntitySearchByAttributes searchTable =
                    underlay
                        .getIndexSchema()
                        .getEntitySearchByAttributes(entity, searchTableAttributeNames);
                jobSet.addJob(
                    new WriteEntitySearchByAttributes(
                        indexerConfig, entity, indexEntityMain, searchTable));
              });
    }

    if (entity.hasTextSearch() || entity.hasHierarchies()) {
      jobSet.startNewStage();

      if (entity.hasTextSearch()) {
        STTextSearchTerms sourceTextTable =
            underlay.getSourceSchema().hasTextSearchTerms(entity.getName())
                ? underlay.getSourceSchema().getTextSearchTerms(entity.getName())
                : null;
        jobSet.addJob(
            new WriteTextSearchField(indexerConfig, entity, sourceTextTable, indexEntityMain));
      }
      entity
          .getHierarchies()
          .forEach(
              hierarchy -> {
                STHierarchyChildParent sourceChildParent =
                    underlay
                        .getSourceSchema()
                        .getHierarchyChildParent(entity.getName(), hierarchy.getName());
                ITHierarchyChildParent indexChildParent =
                    underlay
                        .getIndexSchema()
                        .getHierarchyChildParent(entity.getName(), hierarchy.getName());
                jobSet.addJob(
                    new WriteChildParent(indexerConfig, sourceChildParent, indexChildParent));

                ITHierarchyAncestorDescendant indexAncestorDescendant =
                    underlay
                        .getIndexSchema()
                        .getHierarchyAncestorDescendant(entity.getName(), hierarchy.getName());
                jobSet.addJob(
                    new WriteAncestorDescendant(
                        indexerConfig, hierarchy, sourceChildParent, indexAncestorDescendant));
              });

      jobSet.startNewStage();
      entity
          .getHierarchies()
          .forEach(
              hierarchy -> {
                ITHierarchyChildParent indexChildParent =
                    underlay
                        .getIndexSchema()
                        .getHierarchyChildParent(entity.getName(), hierarchy.getName());
                ITHierarchyAncestorDescendant indexAncestorDescendant =
                    underlay
                        .getIndexSchema()
                        .getHierarchyAncestorDescendant(entity.getName(), hierarchy.getName());
                STHierarchyRootFilter sourceRootFilter =
                    underlay
                            .getSourceSchema()
                            .hasHierarchyRootFilter(entity.getName(), hierarchy.getName())
                        ? underlay
                            .getSourceSchema()
                            .getHierarchyRootFilter(entity.getName(), hierarchy.getName())
                        : null;
                jobSet.addJob(
                    new WriteNumChildrenAndPaths(
                        indexerConfig,
                        entity,
                        hierarchy,
                        indexChildParent,
                        indexAncestorDescendant,
                        sourceRootFilter,
                        indexEntityMain));
              });
    }
    return jobSet;
  }

  public static SequencedJobSet getJobSetForEntityGroup(
      SZIndexer indexerConfig, Underlay underlay, EntityGroup entityGroup) {
    if (EntityGroup.Type.GROUP_ITEMS.equals(entityGroup.getType())) {
      return getJobSetForGroupItems(indexerConfig, underlay, (GroupItems) entityGroup);
    } else {
      return getJobSetForCriteriaOccurrence(
          indexerConfig, underlay, (CriteriaOccurrence) entityGroup);
    }
  }

  public static SequencedJobSet getJobSetForGroupItems(
      SZIndexer indexerConfig, Underlay underlay, GroupItems groupItems) {
    SequencedJobSet jobSet = new SequencedJobSet(groupItems.getName());
    jobSet.startNewStage();

    // If the relationship lives in an intermediate table, write the table to the index dataset.
    // e.g. To allow joins between brand-ingredient.
    Relationship relationship = groupItems.getGroupItemsRelationship();
    if (relationship.isIntermediateTable()) {
      STRelationshipIdPairs sourceIdPairsTable =
          underlay
              .getSourceSchema()
              .getRelationshipIdPairs(
                  groupItems.getName(),
                  relationship.getEntityA().getName(),
                  relationship.getEntityB().getName());
      ITRelationshipIdPairs indexIdPairsTable =
          underlay
              .getIndexSchema()
              .getRelationshipIdPairs(
                  groupItems.getName(),
                  relationship.getEntityA().getName(),
                  relationship.getEntityB().getName());
      if (indexIdPairsTable.isGeneratedIndexTable()) {
        jobSet.addJob(
            new WriteRelationshipIntermediateTable(
                indexerConfig, sourceIdPairsTable, indexIdPairsTable));
        jobSet.startNewStage();
      }
    }

    // Compute the criteria rollup counts for the group-items relationship.
    // e.g. To show how many ingredients each brand contains.
    ITEntityMain groupEntityIndexTable =
        underlay.getIndexSchema().getEntityMain(groupItems.getGroupEntity().getName());
    ITEntityMain itemsEntityIndexTable =
        underlay.getIndexSchema().getEntityMain(groupItems.getItemsEntity().getName());
    ITRelationshipIdPairs groupItemsIdPairsTable =
        groupItems.getGroupItemsRelationship().isIntermediateTable()
            ? underlay
                .getIndexSchema()
                .getRelationshipIdPairs(
                    groupItems.getName(),
                    groupItems.getGroupEntity().getName(),
                    groupItems.getItemsEntity().getName())
            : null;
    STRelationshipRollupCounts groupItemsRelationshipRollupCountsTable =
        underlay
            .getSourceSchema()
            .getRelationshipRollupCounts(
                groupItems.getName(),
                groupItems.getGroupEntity().getName(),
                groupItems.getItemsEntity().getName())
            .orElse(null);
    jobSet.addJob(
        new WriteRollupCounts(
            indexerConfig,
            groupItems,
            groupItems.getGroupEntity(),
            groupItems.getItemsEntity(),
            groupItems.getGroupItemsRelationship(),
            groupEntityIndexTable,
            itemsEntityIndexTable,
            groupItemsIdPairsTable,
            null,
            null,
            groupItemsRelationshipRollupCountsTable));

    // If the criteria entity has hierarchies, then also compute the criteria rollup counts for each
    // hierarchy.
    // e.g. To show rollup counts for each genotyping.
    if (groupItems.getGroupEntity().hasHierarchies()) {
      groupItems
          .getGroupEntity()
          .getHierarchies()
          .forEach(
              hierarchy ->
                  jobSet.addJob(
                      new WriteRollupCounts(
                          indexerConfig,
                          groupItems,
                          groupItems.getGroupEntity(),
                          groupItems.getItemsEntity(),
                          groupItems.getGroupItemsRelationship(),
                          groupEntityIndexTable,
                          itemsEntityIndexTable,
                          groupItemsIdPairsTable,
                          hierarchy,
                          underlay
                              .getIndexSchema()
                              .getHierarchyAncestorDescendant(
                                  groupItems.getGroupEntity().getName(), hierarchy.getName()),
                          null)));
    }

    if (groupItems.getGroupEntity().hasHierarchies()) {
      AtomicBoolean isNewStage = new AtomicBoolean(true);
      groupItems
          .getGroupEntity()
          .getHierarchies()
          .forEach(
              hierarchy -> {
                if (hierarchy.isCleanHierarchyNodesWithZeroCounts()) {
                  ITHierarchyChildParent indexChildParent =
                      underlay
                          .getIndexSchema()
                          .getHierarchyChildParent(
                              groupItems.getGroupEntity().getName(), hierarchy.getName());
                  ITHierarchyAncestorDescendant indexAncestorDescendant =
                      underlay
                          .getIndexSchema()
                          .getHierarchyAncestorDescendant(
                              groupItems.getGroupEntity().getName(), hierarchy.getName());
                  Attribute idAttribute = groupItems.getGroupEntity().getIdAttribute();
                  if (isNewStage.getAndSet(false)) {
                    jobSet.startNewStage();
                  }
                  jobSet.addJob(
                      new CleanHierarchyNodesWithZeroCounts(
                          indexerConfig,
                          groupItems,
                          groupEntityIndexTable,
                          indexChildParent,
                          indexAncestorDescendant,
                          idAttribute,
                          hierarchy));
                }
              });
    }

    return jobSet;
  }

  public static SequencedJobSet getJobSetForCriteriaOccurrence(
      SZIndexer indexerConfig, Underlay underlay, CriteriaOccurrence criteriaOccurrence) {
    SequencedJobSet jobSet = new SequencedJobSet(criteriaOccurrence.getName());
    jobSet.startNewStage();

    // Write the relationship id-pairs for each occurrence-criteria and occurrence-primary
    // relationship that is not a direct foreign-key mapping.
    // e.g. To allow joins between person-conditionOccurrence, conditionOccurrence-condition.
    criteriaOccurrence
        .getOccurrenceEntities()
        .forEach(
            occurrenceEntity -> {
              Relationship occurrenceCriteriaRelationship =
                  criteriaOccurrence.getOccurrenceCriteriaRelationship(occurrenceEntity.getName());
              if (occurrenceCriteriaRelationship.isIntermediateTable()) {
                STRelationshipIdPairs sourceIdPairsTable =
                    underlay
                        .getSourceSchema()
                        .getRelationshipIdPairs(
                            criteriaOccurrence.getName(),
                            occurrenceEntity.getName(),
                            criteriaOccurrence.getCriteriaEntity().getName());
                ITRelationshipIdPairs indexIdPairsTable =
                    underlay
                        .getIndexSchema()
                        .getRelationshipIdPairs(
                            criteriaOccurrence.getName(),
                            occurrenceEntity.getName(),
                            criteriaOccurrence.getCriteriaEntity().getName());
                jobSet.addJob(
                    new WriteRelationshipIntermediateTable(
                        indexerConfig, sourceIdPairsTable, indexIdPairsTable));
              }

              Relationship occurrencePrimaryRelationship =
                  criteriaOccurrence.getOccurrencePrimaryRelationship(occurrenceEntity.getName());
              if (occurrencePrimaryRelationship.isIntermediateTable()) {
                STRelationshipIdPairs sourceIdPairsTable =
                    underlay
                        .getSourceSchema()
                        .getRelationshipIdPairs(
                            criteriaOccurrence.getName(),
                            occurrenceEntity.getName(),
                            criteriaOccurrence.getPrimaryEntity().getName());
                ITRelationshipIdPairs indexIdPairsTable =
                    underlay
                        .getIndexSchema()
                        .getRelationshipIdPairs(
                            criteriaOccurrence.getName(),
                            occurrenceEntity.getName(),
                            criteriaOccurrence.getPrimaryEntity().getName());
                jobSet.addJob(
                    new WriteRelationshipIntermediateTable(
                        indexerConfig, sourceIdPairsTable, indexIdPairsTable));
              }
            });

    // Write the relationship id-pairs for the primary-criteria relationship if it's not a direct
    // foreign-key mapping.
    // e.g. To allow joins between person-condition.
    Relationship primaryCriteriaRelationship = criteriaOccurrence.getPrimaryCriteriaRelationship();
    if (primaryCriteriaRelationship.isIntermediateTable()) {
      STRelationshipIdPairs sourceIdPairsTable =
          underlay
              .getSourceSchema()
              .getRelationshipIdPairs(
                  criteriaOccurrence.getName(),
                  criteriaOccurrence.getPrimaryEntity().getName(),
                  criteriaOccurrence.getCriteriaEntity().getName());
      ITRelationshipIdPairs indexIdPairsTable =
          underlay
              .getIndexSchema()
              .getRelationshipIdPairs(
                  criteriaOccurrence.getName(),
                  criteriaOccurrence.getPrimaryEntity().getName(),
                  criteriaOccurrence.getCriteriaEntity().getName());
      jobSet.addJob(
          new WriteRelationshipIntermediateTable(
              indexerConfig, sourceIdPairsTable, indexIdPairsTable));
    }

    jobSet.startNewStage();

    // Compute the criteria rollup counts for the criteria-primary relationship.
    // e.g. To show item counts for each condition.
    ITEntityMain criteriaEntityIndexTable =
        underlay.getIndexSchema().getEntityMain(criteriaOccurrence.getCriteriaEntity().getName());
    ITEntityMain primaryEntityIndexTable =
        underlay.getIndexSchema().getEntityMain(criteriaOccurrence.getPrimaryEntity().getName());
    ITRelationshipIdPairs primaryCriteriaIdPairsTable =
        criteriaOccurrence.getPrimaryCriteriaRelationship().isIntermediateTable()
            ? underlay
                .getIndexSchema()
                .getRelationshipIdPairs(
                    criteriaOccurrence.getName(),
                    criteriaOccurrence.getPrimaryEntity().getName(),
                    criteriaOccurrence.getCriteriaEntity().getName())
            : null;
    jobSet.addJob(
        new WriteRollupCounts(
            indexerConfig,
            criteriaOccurrence,
            criteriaOccurrence.getCriteriaEntity(),
            criteriaOccurrence.getPrimaryEntity(),
            criteriaOccurrence.getPrimaryCriteriaRelationship(),
            criteriaEntityIndexTable,
            primaryEntityIndexTable,
            primaryCriteriaIdPairsTable,
            null,
            null,
            null));

    // If the criteria entity has hierarchies, then also compute the criteria rollup counts for each
    // hierarchy.
    // e.g. To show rollup counts for each condition.
    if (criteriaOccurrence.getCriteriaEntity().hasHierarchies()) {
      criteriaOccurrence
          .getCriteriaEntity()
          .getHierarchies()
          .forEach(
              hierarchy ->
                  jobSet.addJob(
                      new WriteRollupCounts(
                          indexerConfig,
                          criteriaOccurrence,
                          criteriaOccurrence.getCriteriaEntity(),
                          criteriaOccurrence.getPrimaryEntity(),
                          criteriaOccurrence.getPrimaryCriteriaRelationship(),
                          criteriaEntityIndexTable,
                          primaryEntityIndexTable,
                          primaryCriteriaIdPairsTable,
                          hierarchy,
                          underlay
                              .getIndexSchema()
                              .getHierarchyAncestorDescendant(
                                  criteriaOccurrence.getCriteriaEntity().getName(),
                                  hierarchy.getName()),
                          null)));
    }

    // Compute instance-level display hints for the occurrence entity attributes that are flagged as
    // such.
    // e.g. To show display hints for a specific measurement entity instance, such as glucose test.
    // TODO: Handle >1 occurrence entity.
    Entity occurrenceEntity = criteriaOccurrence.getOccurrenceEntities().get(0);
    if (criteriaOccurrence.hasInstanceLevelDisplayHints(occurrenceEntity)) {
      // TODO: Handle >1 hierarchy.
      Hierarchy hierarchy =
          criteriaOccurrence.getCriteriaEntity().hasHierarchies()
              ? criteriaOccurrence.getCriteriaEntity().getHierarchies().get(0)
              : null;

      Relationship occurrenceCriteriaRelationship =
          criteriaOccurrence.getOccurrenceCriteriaRelationship(occurrenceEntity.getName());
      Relationship occurrencePrimaryRelationship =
          criteriaOccurrence.getOccurrencePrimaryRelationship(occurrenceEntity.getName());
      jobSet.addJob(
          new WriteInstanceLevelDisplayHints(
              indexerConfig,
              criteriaOccurrence,
              criteriaOccurrence.getOccurrenceEntities().get(0),
              underlay
                  .getIndexSchema()
                  .getEntityMain(criteriaOccurrence.getCriteriaEntity().getName()),
              underlay.getIndexSchema().getEntityMain(occurrenceEntity.getName()),
              underlay
                  .getIndexSchema()
                  .getEntityMain(criteriaOccurrence.getPrimaryEntity().getName()),
              occurrenceCriteriaRelationship.isIntermediateTable()
                  ? underlay
                      .getIndexSchema()
                      .getRelationshipIdPairs(
                          criteriaOccurrence.getName(),
                          occurrenceEntity.getName(),
                          criteriaOccurrence.getCriteriaEntity().getName())
                  : null,
              occurrencePrimaryRelationship.isIntermediateTable()
                  ? underlay
                      .getIndexSchema()
                      .getRelationshipIdPairs(
                          criteriaOccurrence.getName(),
                          occurrenceEntity.getName(),
                          criteriaOccurrence.getPrimaryEntity().getName())
                  : null,
              underlay
                  .getIndexSchema()
                  .getInstanceLevelDisplayHints(
                      criteriaOccurrence.getName(),
                      occurrenceEntity.getName(),
                      criteriaOccurrence.getCriteriaEntity().getName()),
              hierarchy,
              hierarchy != null
                  ? underlay
                      .getIndexSchema()
                      .getHierarchyAncestorDescendant(
                          criteriaOccurrence.getCriteriaEntity().getName(), hierarchy.getName())
                  : null));
    }

    if (criteriaOccurrence.getCriteriaEntity().hasHierarchies()) {
      AtomicBoolean isNewStage = new AtomicBoolean(true);
      criteriaOccurrence
          .getCriteriaEntity()
          .getHierarchies()
          .forEach(
              hierarchy -> {
                if (hierarchy.isCleanHierarchyNodesWithZeroCounts()) {
                  ITHierarchyChildParent indexChildParent =
                      underlay
                          .getIndexSchema()
                          .getHierarchyChildParent(
                              criteriaOccurrence.getCriteriaEntity().getName(),
                              hierarchy.getName());
                  ITHierarchyAncestorDescendant indexAncestorDescendant =
                      underlay
                          .getIndexSchema()
                          .getHierarchyAncestorDescendant(
                              criteriaOccurrence.getCriteriaEntity().getName(),
                              hierarchy.getName());
                  Attribute idAttribute = criteriaOccurrence.getCriteriaEntity().getIdAttribute();
                  if (isNewStage.getAndSet(false)) {
                    jobSet.startNewStage();
                  }
                  jobSet.addJob(
                      new CleanHierarchyNodesWithZeroCounts(
                          indexerConfig,
                          criteriaOccurrence,
                          criteriaEntityIndexTable,
                          indexChildParent,
                          indexAncestorDescendant,
                          idAttribute,
                          hierarchy));
                }
              });
    }

    return jobSet;
  }

  public enum JobExecutor {
    PARALLEL,
    SERIAL;

    public JobRunner getRunner(
        List<SequencedJobSet> jobSets, boolean isDryRun, IndexingJob.RunType runType) {
      return switch (this) {
        case SERIAL -> new SerialRunner(jobSets, isDryRun, runType);
        case PARALLEL -> new ParallelRunner(jobSets, isDryRun, runType);
        default -> throw new IllegalArgumentException("Unknown JobExecution enum type: " + this);
      };
    }
  }
}
