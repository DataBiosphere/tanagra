package bio.terra.tanagra.underlay;

import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.indextable.ITEntityLevelDisplayHints;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.indextable.ITEntitySearchByAttribute;
import bio.terra.tanagra.underlay.indextable.ITHierarchyAncestorDescendant;
import bio.terra.tanagra.underlay.indextable.ITHierarchyChildParent;
import bio.terra.tanagra.underlay.indextable.ITInstanceLevelDisplayHints;
import bio.terra.tanagra.underlay.indextable.ITRelationshipIdPairs;
import bio.terra.tanagra.underlay.serialization.SZBigQuery;
import bio.terra.tanagra.underlay.serialization.SZCriteriaOccurrence;
import bio.terra.tanagra.underlay.serialization.SZEntity;
import bio.terra.tanagra.underlay.serialization.SZGroupItems;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressFBWarnings(
    value = "NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD",
    justification =
        "Jackson object mapper writes the POJO fields during deserialization. Need to put this at the class level, because method-level does not handle internal lambdas.")
public final class IndexSchema {
  private final ImmutableList<ITEntityMain> entityMainTables;
  private final ImmutableList<ITEntityLevelDisplayHints> entityLevelDisplayHintTables;
  private final ImmutableList<ITEntitySearchByAttribute> entitySearchByAttributeTables;
  private final ImmutableList<ITHierarchyChildParent> hierarchyChildParentTables;
  private final ImmutableList<ITHierarchyAncestorDescendant> hierarchyAncestorDescendantTables;
  private final ImmutableList<ITRelationshipIdPairs> relationshipIdPairTables;
  private final ImmutableList<ITInstanceLevelDisplayHints> instanceLevelDisplayHintTables;

  private IndexSchema(
      List<ITEntityMain> entityMainTables,
      List<ITEntityLevelDisplayHints> entityLevelDisplayHintTables,
      List<ITEntitySearchByAttribute> entitySearchByAttributeTables,
      List<ITHierarchyChildParent> hierarchyChildParentTables,
      List<ITHierarchyAncestorDescendant> hierarchyAncestorDescendantTables,
      List<ITRelationshipIdPairs> relationshipIdPairTables,
      List<ITInstanceLevelDisplayHints> instanceLevelDisplayHintTables) {
    this.entityMainTables = ImmutableList.copyOf(entityMainTables);
    this.entityLevelDisplayHintTables = ImmutableList.copyOf(entityLevelDisplayHintTables);
    this.entitySearchByAttributeTables = ImmutableList.copyOf(entitySearchByAttributeTables);
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
        .orElseThrow();
  }

  public ITEntityLevelDisplayHints getEntityLevelDisplayHints(String entity) {
    return entityLevelDisplayHintTables.stream()
        .filter(entityLevelDisplayHints -> entityLevelDisplayHints.getEntity().equals(entity))
        .findFirst()
        .orElseThrow();
  }

  // function used during indexing: when table clustered on all attributes are needed
  public ITEntitySearchByAttribute getEntitySearchByAttributeTable(
      Entity entity, List<Attribute> attributes) {
    List<String> attrNames = attributes.stream().map(Attribute::getName).toList();
    return entitySearchByAttributeTables.stream()
        .filter(
            searchTable ->
                searchTable.getEntity().equals(entity.getName())
                    && searchTable.getAttributeNames().containsAll(attrNames))
        .findFirst()
        .orElseThrow();
  }

  /**
   * function used during filter translation: Currently filterableGroup creates an BOOLEAN_AND of
   * attribute filters for search config with multiple attributes. eg: genomic_region is searched
   * with contig=x AND position>=y AND position<=z There are three sub-filters that are queried on
   * the same table
   */
  public ITEntitySearchByAttribute getEntitySearchByAttributeTable(
      Entity entity, Attribute attribute) {
    return entitySearchByAttributeTables.stream()
        .filter(
            searchTable ->
                searchTable.getEntity().equals(entity.getName())
                    && searchTable.getAttributeNames().contains(attribute.getName()))
        .findFirst()
        .orElseThrow();
  }

  public ITHierarchyChildParent getHierarchyChildParent(String entity, String hierarchy) {
    return hierarchyChildParentTables.stream()
        .filter(
            childParent ->
                childParent.getEntity().equals(entity)
                    && childParent.getHierarchy().equals(hierarchy))
        .findFirst()
        .orElseThrow();
  }

  public ITHierarchyAncestorDescendant getHierarchyAncestorDescendant(
      String entity, String hierarchy) {
    return hierarchyAncestorDescendantTables.stream()
        .filter(
            ancestorDescendant ->
                ancestorDescendant.getEntity().equals(entity)
                    && ancestorDescendant.getHierarchy().equals(hierarchy))
        .findFirst()
        .orElseThrow();
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
        .orElseThrow();
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
        .orElseThrow();
  }

  public static IndexSchema fromConfig(
      SZBigQuery szBigQuery,
      SZUnderlay szUnderlay,
      ConfigReader configReader,
      SourceSchema sourceSchema) {
    NameHelper nameHelper = new NameHelper(szBigQuery.indexData.tablePrefix);

    List<ITEntityMain> entityMainTables = new ArrayList<>();
    List<ITEntityLevelDisplayHints> entityLevelDisplayHintTables = new ArrayList<>();
    List<ITEntitySearchByAttribute> entitySearchByAttributeTables = new ArrayList<>();
    List<ITHierarchyChildParent> hierarchyChildParentTables = new ArrayList<>();
    List<ITHierarchyAncestorDescendant> hierarchyAncestorDescendantTables = new ArrayList<>();
    List<ITRelationshipIdPairs> relationshipIdPairTables = new ArrayList<>();
    List<ITInstanceLevelDisplayHints> instanceLevelDisplayHintTables = new ArrayList<>();

    // Build index tables for each entity.
    szUnderlay.entities.forEach(
        entityPath ->
            fromConfigEntity(
                entityPath,
                szUnderlay,
                configReader,
                nameHelper,
                szBigQuery.indexData,
                entityMainTables,
                entityLevelDisplayHintTables,
                entitySearchByAttributeTables,
                hierarchyChildParentTables,
                hierarchyAncestorDescendantTables));

    // Build index tables for each entity group.
    szUnderlay.groupItemsEntityGroups.forEach(
        groupItemsPath ->
            fromConfigGroupItems(
                groupItemsPath,
                configReader,
                sourceSchema,
                nameHelper,
                szBigQuery.indexData,
                relationshipIdPairTables));
    szUnderlay.criteriaOccurrenceEntityGroups.forEach(
        criteriaOccurrencePath ->
            fromConfigCriteriaOccurrence(
                criteriaOccurrencePath,
                szUnderlay.primaryEntity,
                configReader,
                nameHelper,
                szBigQuery.indexData,
                relationshipIdPairTables,
                instanceLevelDisplayHintTables));
    return new IndexSchema(
        entityMainTables,
        entityLevelDisplayHintTables,
        entitySearchByAttributeTables,
        hierarchyChildParentTables,
        hierarchyAncestorDescendantTables,
        relationshipIdPairTables,
        instanceLevelDisplayHintTables);
  }

  @SuppressWarnings("checkstyle:ParameterNumber")
  private static void fromConfigEntity(
      String entityPath,
      SZUnderlay szUnderlay,
      ConfigReader configReader,
      NameHelper nameHelper,
      SZBigQuery.IndexData szBigQueryIndexData,
      List<ITEntityMain> entityMainTables,
      List<ITEntityLevelDisplayHints> entityLevelDisplayHintTables,
      List<ITEntitySearchByAttribute> entitySearchByAttributeTables,
      List<ITHierarchyChildParent> hierarchyChildParentTables,
      List<ITHierarchyAncestorDescendant> hierarchyAncestorDescendantTables) {
    SZEntity szEntity = configReader.readEntity(entityPath);

    // EntityMain table.
    Set<String> entityGroupsWithCount = new HashSet<>();
    szUnderlay.groupItemsEntityGroups.forEach(
        groupItemsPath -> {
          SZGroupItems szGroupItems = configReader.readGroupItems(groupItemsPath);
          if (szGroupItems.groupEntity.equals(szEntity.name)) {
            entityGroupsWithCount.add(szGroupItems.name);
          }
        });
    szUnderlay.criteriaOccurrenceEntityGroups.forEach(
        criteriaOccurrencePath -> {
          SZCriteriaOccurrence szCriteriaOccurrence =
              configReader.readCriteriaOccurrence(criteriaOccurrencePath);
          if (szCriteriaOccurrence.criteriaEntity.equals(szEntity.name)) {
            entityGroupsWithCount.add(szCriteriaOccurrence.name);
          }
        });
    entityMainTables.add(
        new ITEntityMain(
            nameHelper,
            szBigQueryIndexData,
            szEntity.name,
            szEntity.attributes,
            szEntity.hierarchies,
            szEntity.textSearch != null,
            entityGroupsWithCount));

    // EntityLevelDisplayHints table.
    entityLevelDisplayHintTables.add(
        new ITEntityLevelDisplayHints(nameHelper, szBigQueryIndexData, szEntity.name));

    // EntitySearchByAttribute tables.
    if (szEntity.optimizeSearchByAttributes != null) {
      szEntity.optimizeSearchByAttributes.forEach(
          attributeSearch ->
              entitySearchByAttributeTables.add(
                  new ITEntitySearchByAttribute(
                      nameHelper, szBigQueryIndexData, szEntity, attributeSearch)));
    }

    szEntity.hierarchies.forEach(
        szHierarchy -> {
          // HierarchyChildParent table.
          hierarchyChildParentTables.add(
              new ITHierarchyChildParent(
                  nameHelper, szBigQueryIndexData, szEntity.name, szHierarchy.name));

          // HierarchyAncestorDescendant table.
          hierarchyAncestorDescendantTables.add(
              new ITHierarchyAncestorDescendant(
                  nameHelper, szBigQueryIndexData, szEntity.name, szHierarchy.name));
        });
  }

  private static void fromConfigGroupItems(
      String groupItemsPath,
      ConfigReader configReader,
      SourceSchema sourceSchema,
      NameHelper nameHelper,
      SZBigQuery.IndexData szBigQueryIndexData,
      List<ITRelationshipIdPairs> relationshipIdPairTables) {
    SZGroupItems szGroupItems = configReader.readGroupItems(groupItemsPath);
    if (szGroupItems.idPairsSqlFile != null) {
      // RelationshipIdPairs table.
      if (szGroupItems.useSourceIdPairsSql) {
        relationshipIdPairTables.add(
            new ITRelationshipIdPairs(
                sourceSchema,
                szGroupItems.name,
                szGroupItems.groupEntity,
                szGroupItems.itemsEntity));
      } else {
        relationshipIdPairTables.add(
            new ITRelationshipIdPairs(
                nameHelper,
                szBigQueryIndexData,
                szGroupItems.name,
                szGroupItems.groupEntity,
                szGroupItems.itemsEntity));
      }
    }
  }

  public static void fromConfigCriteriaOccurrence(
      String criteriaOccurrencePath,
      String primaryEntityName,
      ConfigReader configReader,
      NameHelper nameHelper,
      SZBigQuery.IndexData szBigQueryIndexData,
      List<ITRelationshipIdPairs> relationshipIdPairTables,
      List<ITInstanceLevelDisplayHints> instanceLevelDisplayHintTables) {
    SZCriteriaOccurrence szCriteriaOccurrence =
        configReader.readCriteriaOccurrence(criteriaOccurrencePath);
    if (szCriteriaOccurrence.primaryCriteriaRelationship.idPairsSqlFile != null) {
      // RelationshipIdPairs table.
      relationshipIdPairTables.add(
          new ITRelationshipIdPairs(
              nameHelper,
              szBigQueryIndexData,
              szCriteriaOccurrence.name,
              primaryEntityName,
              szCriteriaOccurrence.criteriaEntity));
    }
    szCriteriaOccurrence.occurrenceEntities.forEach(
        szOccurrenceEntity -> {
          if (szOccurrenceEntity.criteriaRelationship.idPairsSqlFile != null) {
            // RelationshipIdPairs table.
            relationshipIdPairTables.add(
                new ITRelationshipIdPairs(
                    nameHelper,
                    szBigQueryIndexData,
                    szCriteriaOccurrence.name,
                    szOccurrenceEntity.occurrenceEntity,
                    szCriteriaOccurrence.criteriaEntity));
          }
          if (szOccurrenceEntity.primaryRelationship.idPairsSqlFile != null) {
            // RelationshipIdPairs table.
            relationshipIdPairTables.add(
                new ITRelationshipIdPairs(
                    nameHelper,
                    szBigQueryIndexData,
                    szCriteriaOccurrence.name,
                    szOccurrenceEntity.occurrenceEntity,
                    primaryEntityName));
          }
          if (szOccurrenceEntity.attributesWithInstanceLevelHints != null) {
            // InstanceLevelDisplayHints table.
            instanceLevelDisplayHintTables.add(
                new ITInstanceLevelDisplayHints(
                    nameHelper,
                    szBigQueryIndexData,
                    szCriteriaOccurrence.name,
                    szOccurrenceEntity.occurrenceEntity,
                    szCriteriaOccurrence.criteriaEntity));
          }
        });
  }
}
