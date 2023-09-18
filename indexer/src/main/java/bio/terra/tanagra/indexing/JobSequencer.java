package bio.terra.tanagra.indexing;

import bio.terra.tanagra.indexing.job.*;
import bio.terra.tanagra.indexing.jobexecutor.SequencedJobSet;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityGroup;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay.entitygroup.GroupItems;

public final class JobSequencer {
  private JobSequencer() {}

  public static SequencedJobSet getJobSetForEntity(Entity entity) {
    SequencedJobSet jobSet = new SequencedJobSet(entity.getName());
    jobSet.startNewStage();
    jobSet.addJob(new CreateEntityTable(entity));

    jobSet.startNewStage();
    jobSet.addJob(new DenormalizeEntityInstances(entity));
    jobSet.addJob(new ComputeEntityLevelDisplayHints(entity));

    if (entity.getTextSearch().isEnabled() || entity.hasHierarchies()) {
      jobSet.startNewStage();
    }

    if (entity.getTextSearch().isEnabled()) {
      jobSet.addJob(new BuildTextSearchStrings(entity));
    }
    entity.getHierarchies().stream()
        .forEach(
            hierarchy -> {
              jobSet.addJob(new WriteParentChildIdPairs(entity, hierarchy.getName()));
              jobSet.addJob(new WriteAncestorDescendantIdPairs(entity, hierarchy.getName()));
              jobSet.addJob(new BuildNumChildrenAndPaths(entity, hierarchy.getName()));
            });

    return jobSet;
  }

  public static SequencedJobSet getJobSetForEntityGroup(EntityGroup entityGroup) {
    SequencedJobSet jobSet = new SequencedJobSet(entityGroup.getName());
    jobSet.startNewStage();

    if (EntityGroup.Type.CRITERIA_OCCURRENCE.equals(entityGroup.getType())) {
      CriteriaOccurrence criteriaOccurrence = (CriteriaOccurrence) entityGroup;

      // Write the relationship id-pairs for each occurrence-criteria and occurrence-primary
      // relationship that is not a direct foreign-key mapping.
      // e.g. To allow joins between person-conditionOccurrence, conditionOccurrence-condition.
      for (Entity occurrenceEntity : criteriaOccurrence.getOccurrenceEntities()) {
        if (criteriaOccurrence
                .getOccurrenceCriteriaRelationship(occurrenceEntity)
                .getMapping(Underlay.MappingType.INDEX)
                .getForeignKeyAttribute()
            == null) {
          jobSet.addJob(
              new WriteRelationshipIdPairs(
                  criteriaOccurrence.getOccurrenceCriteriaRelationship(occurrenceEntity)));
        }
        if (criteriaOccurrence
                .getOccurrencePrimaryRelationship(occurrenceEntity)
                .getMapping(Underlay.MappingType.INDEX)
                .getForeignKeyAttribute()
            == null) {
          jobSet.addJob(
              new WriteRelationshipIdPairs(
                  criteriaOccurrence.getOccurrencePrimaryRelationship(occurrenceEntity)));
        }
      }

      // Compute the criteria rollup counts for the criteria-primary relationship.
      // e.g. To show item counts for each condition.
      jobSet.addJob(
          new ComputeRollupCounts(
              criteriaOccurrence.getCriteriaEntity(),
              criteriaOccurrence.getCriteriaPrimaryRelationship(),
              null));

      // If the criteria entity has hierarchies, then also compute the criteria rollup counts for
      // each hierarchy.
      // e.g. To show rollup counts for each condition.
      if (criteriaOccurrence.getCriteriaEntity().hasHierarchies()) {
        criteriaOccurrence.getCriteriaEntity().getHierarchies().stream()
            .forEach(
                hierarchy -> {
                  jobSet.addJob(
                      new ComputeRollupCounts(
                          criteriaOccurrence.getCriteriaEntity(),
                          criteriaOccurrence.getCriteriaPrimaryRelationship(),
                          hierarchy));
                });
      }

      // Compute display hints for the occurrence entity attributes that are flagged as modifiers.
      // e.g. To show display hints for a specific measurement entity instance, such as blood
      // pressure.
      if (!criteriaOccurrence.getModifierAttributes().isEmpty()) {
        jobSet.addJob(
            new ComputeModifierDisplayHints(
                criteriaOccurrence, criteriaOccurrence.getOccurrenceEntities().get(0)));
      }
    } else if (EntityGroup.Type.GROUP_ITEMS.equals(entityGroup.getType())) {
      GroupItems groupItems = (GroupItems) entityGroup;

      // Write the relationship id-pairs for the group-items relationship.
      // e.g. To allow joins between brand-ingredient.
      jobSet.addJob(new WriteRelationshipIdPairs(groupItems.getGroupItemsRelationship()));

      // Compute the criteria rollup counts for the group-items relationship.
      // e.g. To show how many ingredients each brand contains.
      jobSet.addJob(
          new ComputeRollupCounts(
              groupItems.getGroupEntity(), groupItems.getGroupItemsRelationship(), null));
    }

    return jobSet;
  }
}
