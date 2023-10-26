package bio.terra.tanagra.underlay2;

import bio.terra.tanagra.query.DataPointer;
import bio.terra.tanagra.query.bigquery.BigQueryDataset;
import bio.terra.tanagra.underlay2.indextable.*;
import bio.terra.tanagra.underlay2.serialization.*;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class IndexSchema {
  private final ImmutableList<ITEntityMain> entityMainTables;
  private final ImmutableList<ITEntityLevelDisplayHints> entityLevelDisplayHintTables;
  private final ImmutableList<ITHierarchyChildParent> hierarchyChildParentTables;
  private final ImmutableList<ITHierarchyAncestorDescendant> hierarchyAncestorDescendantTables;
  private final ImmutableList<ITRelationshipIdPairs> relationshipIdPairTables;
  private final ImmutableList<ITInstanceLevelDisplayHints> instanceLevelDisplayHintTables;

  private IndexSchema(
      List<ITEntityMain> entityMainTables,
      List<ITEntityLevelDisplayHints> entityLevelDisplayHintTables,
      List<ITHierarchyChildParent> hierarchyChildParentTables,
      List<ITHierarchyAncestorDescendant> hierarchyAncestorDescendantTables,
      List<ITRelationshipIdPairs> relationshipIdPairTables,
      List<ITInstanceLevelDisplayHints> instanceLevelDisplayHintTables) {
    this.entityMainTables = ImmutableList.copyOf(entityMainTables);
    this.entityLevelDisplayHintTables = ImmutableList.copyOf(entityLevelDisplayHintTables);
    this.hierarchyChildParentTables = ImmutableList.copyOf(hierarchyChildParentTables);
    this.hierarchyAncestorDescendantTables =
        ImmutableList.copyOf(hierarchyAncestorDescendantTables);
    this.relationshipIdPairTables = ImmutableList.copyOf(relationshipIdPairTables);
    this.instanceLevelDisplayHintTables = ImmutableList.copyOf(instanceLevelDisplayHintTables);
  }

  public ITEntityMain getEntityMain(String entity) {
    return entityMainTables.stream()
        .filter(entityMain -> entityMain.getEntity().equals(entity))
        .findFirst()
        .get();
  }

  public ITEntityLevelDisplayHints getEntityLevelDisplayHints(String entity) {
    return entityLevelDisplayHintTables.stream()
        .filter(entityLevelDisplayHints -> entityLevelDisplayHints.getEntity().equals(entity))
        .findFirst()
        .get();
  }

  public ITHierarchyChildParent getHierarchyChildParent(String entity, String hierarchy) {
    return hierarchyChildParentTables.stream()
        .filter(
            childParent ->
                childParent.getEntity().equals(entity)
                    && childParent.getHierarchy().equals(hierarchy))
        .findFirst()
        .get();
  }

  public ITHierarchyAncestorDescendant getHierarchyAncestorDescendant(
      String entity, String hierarchy) {
    return hierarchyAncestorDescendantTables.stream()
        .filter(
            ancestorDescendant ->
                ancestorDescendant.getEntity().equals(entity)
                    && ancestorDescendant.getHierarchy().equals(hierarchy))
        .findFirst()
        .get();
  }

  public ITRelationshipIdPairs getRelationshipIdPairs(
      String entityGroup, String entityA, String entityB) {
    return relationshipIdPairTables.stream()
        .filter(
            relationshipIdPairs ->
                relationshipIdPairs.getEntityGroup().equals(entityGroup)
                    && relationshipIdPairs.getEntityA().equals(entityA)
                    && relationshipIdPairs.getEntityB().equals(entityB))
        .findFirst()
        .get();
  }

  public ITInstanceLevelDisplayHints getInstanceLevelDisplayHints(
      String entityGroup, String hintedEntity, String relatedEntity) {
    return instanceLevelDisplayHintTables.stream()
        .filter(
            instanceLevelDisplayHints ->
                instanceLevelDisplayHints.getEntityGroup().equals(entityGroup)
                    && instanceLevelDisplayHints.getHintedEntity().equals(hintedEntity)
                    && instanceLevelDisplayHints.getRelatedEntity().equals(relatedEntity))
        .findFirst()
        .get();
  }

  public static IndexSchema fromConfig(
      SZBigQuery szBigQuery, SZUnderlay szUnderlay, ConfigReader configReader) {
    DataPointer indexDataPointer =
        new BigQueryDataset(
            "index_datapointer",
            szBigQuery.indexData.projectId,
            szBigQuery.indexData.datasetId,
            szBigQuery.queryProjectId,
            null,
            null,
            null,
            null,
            false,
            null);
    NameHelper nameHelper = new NameHelper(szBigQuery.indexData.tablePrefix);

    List<ITEntityMain> entityMainTables = new ArrayList<>();
    List<ITEntityLevelDisplayHints> entityLevelDisplayHintTables = new ArrayList<>();
    List<ITHierarchyChildParent> hierarchyChildParentTables = new ArrayList<>();
    List<ITHierarchyAncestorDescendant> hierarchyAncestorDescendantTables = new ArrayList<>();
    List<ITRelationshipIdPairs> relationshipIdPairTables = new ArrayList<>();
    List<ITInstanceLevelDisplayHints> instanceLevelDisplayHintTables = new ArrayList<>();

    // Build source tables for each entity.
    szUnderlay.entities.stream()
        .forEach(
            entityName ->
                fromConfigEntity(
                    entityName,
                    szUnderlay,
                    configReader,
                    nameHelper,
                    indexDataPointer,
                    entityMainTables,
                    entityLevelDisplayHintTables,
                    hierarchyChildParentTables,
                    hierarchyAncestorDescendantTables));

    // Build source tables for each entity group.
    szUnderlay.groupItemsEntityGroups.stream()
        .forEach(
            groupItemsName ->
                fromConfigGroupItems(
                    groupItemsName,
                    configReader,
                    nameHelper,
                    indexDataPointer,
                    relationshipIdPairTables));
    szUnderlay.criteriaOccurrenceEntityGroups.stream()
        .forEach(
            criteriaOccurrenceName ->
                fromConfigCriteriaOccurrence(
                    criteriaOccurrenceName,
                    szUnderlay.primaryEntity,
                    configReader,
                    nameHelper,
                    indexDataPointer,
                    relationshipIdPairTables,
                    instanceLevelDisplayHintTables));
    return new IndexSchema(
        entityMainTables,
        entityLevelDisplayHintTables,
        hierarchyChildParentTables,
        hierarchyAncestorDescendantTables,
        relationshipIdPairTables,
        instanceLevelDisplayHintTables);
  }

  private static void fromConfigEntity(
      String entityName,
      SZUnderlay szUnderlay,
      ConfigReader configReader,
      NameHelper nameHelper,
      DataPointer indexDataPointer,
      List<ITEntityMain> entityMainTables,
      List<ITEntityLevelDisplayHints> entityLevelDisplayHintTables,
      List<ITHierarchyChildParent> hierarchyChildParentTables,
      List<ITHierarchyAncestorDescendant> hierarchyAncestorDescendantTables) {
    SZEntity szEntity = configReader.readEntity(entityName);

    // EntityMain table.
    Set<String> entityGroupsWithCount = new HashSet<>();
    szUnderlay.groupItemsEntityGroups.stream()
        .forEach(
            groupItemsName -> {
              SZGroupItems szGroupItems = configReader.readGroupItems(groupItemsName);
              if (szGroupItems.groupEntity.equals(entityName)) {
                entityGroupsWithCount.add(groupItemsName);
              }
            });
    szUnderlay.criteriaOccurrenceEntityGroups.stream()
        .forEach(
            criteriaOccurrenceName -> {
              SZCriteriaOccurrence szCriteriaOccurrence =
                  configReader.readCriteriaOccurrence(criteriaOccurrenceName);
              if (szCriteriaOccurrence.criteriaEntity.equals(entityName)) {
                entityGroupsWithCount.add(criteriaOccurrenceName);
              }
            });
    entityMainTables.add(
        new ITEntityMain(
            nameHelper,
            indexDataPointer,
            entityName,
            szEntity.attributes,
            szEntity.hierarchies,
            szEntity.textSearch != null,
            entityGroupsWithCount));

    // EntityLevelDisplayHints table.
    entityLevelDisplayHintTables.add(
        new ITEntityLevelDisplayHints(nameHelper, indexDataPointer, entityName));

    szEntity.hierarchies.stream()
        .forEach(
            szHierarchy -> {
              // HierarchyChildParent table.
              hierarchyChildParentTables.add(
                  new ITHierarchyChildParent(
                      nameHelper, indexDataPointer, entityName, szHierarchy.name));

              // HierarchyAncestorDescendant table.
              hierarchyAncestorDescendantTables.add(
                  new ITHierarchyAncestorDescendant(
                      nameHelper, indexDataPointer, entityName, szHierarchy.name));
            });
  }

  private static void fromConfigGroupItems(
      String groupItemsName,
      ConfigReader configReader,
      NameHelper nameHelper,
      DataPointer indexDataPointer,
      List<ITRelationshipIdPairs> relationshipIdPairTables) {
    SZGroupItems szGroupItems = configReader.readGroupItems(groupItemsName);
    if (szGroupItems.idPairsSqlFile != null) {
      // RelationshipIdPairs table.
      relationshipIdPairTables.add(
          new ITRelationshipIdPairs(
              nameHelper,
              indexDataPointer,
              groupItemsName,
              szGroupItems.groupEntity,
              szGroupItems.itemsEntity));
    }
  }

  private static void fromConfigCriteriaOccurrence(
      String criteriaOccurrenceName,
      String primaryEntityName,
      ConfigReader configReader,
      NameHelper nameHelper,
      DataPointer indexDataPointer,
      List<ITRelationshipIdPairs> relationshipIdPairTables,
      List<ITInstanceLevelDisplayHints> instanceLevelDisplayHintTables) {
    SZCriteriaOccurrence szCriteriaOccurrence =
        configReader.readCriteriaOccurrence(criteriaOccurrenceName);
    if (szCriteriaOccurrence.primaryCriteriaRelationship.idPairsSqlFile != null) {
      // RelationshipIdPairs table.
      relationshipIdPairTables.add(
          new ITRelationshipIdPairs(
              nameHelper,
              indexDataPointer,
              criteriaOccurrenceName,
              primaryEntityName,
              szCriteriaOccurrence.criteriaEntity));
    }
    szCriteriaOccurrence.occurrenceEntities.stream()
        .forEach(
            szOccurrenceEntity -> {
              if (szOccurrenceEntity.criteriaRelationship.idPairsSqlFile != null) {
                // RelationshipIdPairs table.
                relationshipIdPairTables.add(
                    new ITRelationshipIdPairs(
                        nameHelper,
                        indexDataPointer,
                        criteriaOccurrenceName,
                        szOccurrenceEntity.occurrenceEntity,
                        szCriteriaOccurrence.criteriaEntity));
              }
              if (szOccurrenceEntity.primaryRelationship.idPairsSqlFile != null) {
                // RelationshipIdPairs table.
                relationshipIdPairTables.add(
                    new ITRelationshipIdPairs(
                        nameHelper,
                        indexDataPointer,
                        criteriaOccurrenceName,
                        szOccurrenceEntity.occurrenceEntity,
                        primaryEntityName));
              }
              if (szOccurrenceEntity.attributesWithInstanceLevelHints != null) {
                // InstanceLevelDisplayHints table.
                instanceLevelDisplayHintTables.add(
                    new ITInstanceLevelDisplayHints(
                        nameHelper,
                        indexDataPointer,
                        criteriaOccurrenceName,
                        szOccurrenceEntity.occurrenceEntity,
                        szCriteriaOccurrence.criteriaEntity));
              }
            });
  }
}
