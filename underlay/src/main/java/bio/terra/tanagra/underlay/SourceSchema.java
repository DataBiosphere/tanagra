package bio.terra.tanagra.underlay;

import bio.terra.tanagra.query.bigquery.BQTable;
import bio.terra.tanagra.underlay.serialization.SZBigQuery;
import bio.terra.tanagra.underlay.serialization.SZCriteriaOccurrence;
import bio.terra.tanagra.underlay.serialization.SZEntity;
import bio.terra.tanagra.underlay.serialization.SZGroupItems;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import bio.terra.tanagra.underlay.sourcetable.*;
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressFBWarnings(
    value = "NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD",
    justification =
        "Jackson object mapper writes the POJO fields during deserialization. Need to put this at the class level, because method-level does not handle internal lambdas.")
public final class SourceSchema {
  private final ImmutableList<STEntityAttributes> entityAttributesTables;
  private final ImmutableList<STTextSearchTerms> textSearchTermsTables;
  private final ImmutableList<STHierarchyChildParent> hierarchyChildParentTables;
  private final ImmutableList<STHierarchyRootFilter> hierarchyRootFilterTables;
  private final ImmutableList<STRelationshipIdPairs> relationshipIdPairTables;
  private final ImmutableList<STRelationshipRollupCounts> relationshipRollupCountTables;

  private SourceSchema(
      List<STEntityAttributes> entityAttributesTables,
      List<STTextSearchTerms> textSearchTermsTables,
      List<STHierarchyChildParent> hierarchyChildParentTables,
      List<STHierarchyRootFilter> hierarchyRootFilterTables,
      List<STRelationshipIdPairs> relationshipIdPairTables,
      List<STRelationshipRollupCounts> relationshipRollupCountTables) {
    this.entityAttributesTables = ImmutableList.copyOf(entityAttributesTables);
    this.textSearchTermsTables = ImmutableList.copyOf(textSearchTermsTables);
    this.hierarchyChildParentTables = ImmutableList.copyOf(hierarchyChildParentTables);
    this.hierarchyRootFilterTables = ImmutableList.copyOf(hierarchyRootFilterTables);
    this.relationshipIdPairTables = ImmutableList.copyOf(relationshipIdPairTables);
    this.relationshipRollupCountTables = ImmutableList.copyOf(relationshipRollupCountTables);
  }

  public STEntityAttributes getEntityAttributes(String entity) {
    return entityAttributesTables.stream()
        .filter(entityAttributes -> entityAttributes.getEntity().equals(entity))
        .findFirst()
        .orElseThrow();
  }

  public STTextSearchTerms getTextSearchTerms(String entity) {
    return textSearchTermsTables.stream()
        .filter(textSearchTerms -> textSearchTerms.getEntity().equals(entity))
        .findFirst()
        .orElseThrow();
  }

  public boolean hasTextSearchTerms(String entity) {
    return textSearchTermsTables.stream()
        .anyMatch(textSearchTerms -> textSearchTerms.getEntity().equals(entity));
  }

  public STHierarchyChildParent getHierarchyChildParent(String entity, String hierarchy) {
    return hierarchyChildParentTables.stream()
        .filter(
            childParent ->
                childParent.getEntity().equals(entity)
                    && childParent.getHierarchy().equals(hierarchy))
        .findFirst()
        .orElseThrow();
  }

  public STHierarchyRootFilter getHierarchyRootFilter(String entity, String hierarchy) {
    return hierarchyRootFilterTables.stream()
        .filter(
            rootFilter ->
                rootFilter.getEntity().equals(entity)
                    && rootFilter.getHierarchy().equals(hierarchy))
        .findFirst()
        .orElseThrow();
  }

  public boolean hasHierarchyRootFilter(String entity, String hierarchy) {
    return hierarchyRootFilterTables.stream()
        .anyMatch(
            rootFilter ->
                rootFilter.getEntity().equals(entity)
                    && rootFilter.getHierarchy().equals(hierarchy));
  }

  public STRelationshipIdPairs getRelationshipIdPairs(
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

  public Optional<STRelationshipRollupCounts> getRelationshipRollupCounts(
      String entityGroup, String entity, String countedEntity) {
    return relationshipRollupCountTables.stream()
        .filter(
            relationshipRollupCounts ->
                relationshipRollupCounts.getEntityGroup().equals(entityGroup)
                    && relationshipRollupCounts.getEntity().equals(entity)
                    && relationshipRollupCounts.getCountedEntity().equals(countedEntity))
        .findFirst();
  }

  public static SourceSchema fromConfig(
      SZBigQuery szBigQuery, SZUnderlay szUnderlay, ConfigReader configReader) {
    List<STEntityAttributes> entityAttributesTables = new ArrayList<>();
    List<STTextSearchTerms> textSearchTermsTables = new ArrayList<>();
    List<STHierarchyChildParent> hierarchyChildParentTables = new ArrayList<>();
    List<STHierarchyRootFilter> hierarchyRootFilterTables = new ArrayList<>();
    List<STRelationshipIdPairs> relationshipIdPairTables = new ArrayList<>();
    List<STRelationshipRollupCounts> relationshipRollupCountTables = new ArrayList<>();

    // Build source tables for each entity.
    szUnderlay.entities.forEach(
        entityPath ->
            fromConfigEntity(
                entityPath,
                configReader,
                entityAttributesTables,
                textSearchTermsTables,
                hierarchyChildParentTables,
                hierarchyRootFilterTables));

    // Build source tables for each entity group.
    szUnderlay.groupItemsEntityGroups.forEach(
        groupItemsPath ->
            fromConfigGroupItems(
                groupItemsPath,
                configReader,
                relationshipIdPairTables,
                relationshipRollupCountTables));
    szUnderlay.criteriaOccurrenceEntityGroups.forEach(
        criteriaOccurrencePath ->
            fromConfigCriteriaOccurrence(
                criteriaOccurrencePath,
                szUnderlay.primaryEntity,
                configReader,
                relationshipIdPairTables));
    return new SourceSchema(
        entityAttributesTables,
        textSearchTermsTables,
        hierarchyChildParentTables,
        hierarchyRootFilterTables,
        relationshipIdPairTables,
        relationshipRollupCountTables);
  }

  private static void fromConfigEntity(
      String entityPath,
      ConfigReader configReader,
      List<STEntityAttributes> entityAttributesTables,
      List<STTextSearchTerms> textSearchTermsTables,
      List<STHierarchyChildParent> hierarchyChildParentTables,
      List<STHierarchyRootFilter> hierarchyRootFilterTables) {
    SZEntity szEntity = configReader.readEntity(entityPath);

    // EntityAttributes table.
    String allInstancesSql = configReader.readEntitySql(entityPath, szEntity.allInstancesSqlFile);
    BQTable allInstancesTable = new BQTable(allInstancesSql);
    entityAttributesTables.add(
        new STEntityAttributes(allInstancesTable, szEntity.name, szEntity.attributes));

    if (szEntity.textSearch != null && szEntity.textSearch.idTextPairsSqlFile != null) {
      // TextSearchTerms table.
      String idTextPairsSql =
          configReader.readEntitySql(entityPath, szEntity.textSearch.idTextPairsSqlFile);
      BQTable idTextPairsTable = new BQTable(idTextPairsSql);
      textSearchTermsTables.add(
          new STTextSearchTerms(idTextPairsTable, szEntity.name, szEntity.textSearch));
    }

    szEntity.hierarchies.forEach(
        szHierarchy -> {
          // HierarchyChildParent table.
          String childParentSql =
              configReader.readEntitySql(entityPath, szHierarchy.childParentIdPairsSqlFile);
          BQTable childParentTable = new BQTable(childParentSql);
          hierarchyChildParentTables.add(
              new STHierarchyChildParent(childParentTable, szEntity.name, szHierarchy));

          if (szHierarchy.rootNodeIdsSqlFile != null) {
            // HierarchyRootFilter table.
            String rootNodeSql =
                configReader.readEntitySql(entityPath, szHierarchy.rootNodeIdsSqlFile);
            BQTable rootNodeTable = new BQTable(rootNodeSql);
            hierarchyRootFilterTables.add(
                new STHierarchyRootFilter(rootNodeTable, szEntity.name, szHierarchy));
          }
        });
  }

  private static void fromConfigGroupItems(
      String groupItemsPath,
      ConfigReader configReader,
      List<STRelationshipIdPairs> relationshipIdPairTables,
      List<STRelationshipRollupCounts> relationshipRollupCountTables) {
    SZGroupItems szGroupItems = configReader.readGroupItems(groupItemsPath);
    if (szGroupItems.idPairsSqlFile != null) {
      // RelationshipIdPairs table.
      String idPairsSql =
          configReader.readEntityGroupSql(groupItemsPath, szGroupItems.idPairsSqlFile);
      BQTable idPairsTable = new BQTable(idPairsSql);
      relationshipIdPairTables.add(
          new STRelationshipIdPairs(
              idPairsTable,
              szGroupItems.name,
              szGroupItems.groupEntity,
              szGroupItems.itemsEntity,
              szGroupItems.groupEntityIdFieldName,
              szGroupItems.itemsEntityIdFieldName));
    }
    if (szGroupItems.rollupCountsSql != null) {
      // RelationshipRollupCounts table.
      String rollupCountsSql =
          configReader.readEntityGroupSql(groupItemsPath, szGroupItems.rollupCountsSql.sqlFile);
      BQTable rollupCountsTable = new BQTable(rollupCountsSql);
      relationshipRollupCountTables.add(
          new STRelationshipRollupCounts(
              rollupCountsTable,
              szGroupItems.name,
              szGroupItems.groupEntity,
              szGroupItems.itemsEntity,
              szGroupItems.rollupCountsSql.entityIdFieldName,
              szGroupItems.rollupCountsSql.rollupCountFieldName));
    }
  }

  private static void fromConfigCriteriaOccurrence(
      String criteriaOccurrencePath,
      String primaryEntityName,
      ConfigReader configReader,
      List<STRelationshipIdPairs> relationshipIdPairTables) {
    SZCriteriaOccurrence szCriteriaOccurrence =
        configReader.readCriteriaOccurrence(criteriaOccurrencePath);
    if (szCriteriaOccurrence.primaryCriteriaRelationship.idPairsSqlFile != null) {
      // RelationshipIdPairs table.
      String idPairsSql =
          configReader.readEntityGroupSql(
              criteriaOccurrencePath,
              szCriteriaOccurrence.primaryCriteriaRelationship.idPairsSqlFile);
      BQTable idPairsTable = new BQTable(idPairsSql);
      relationshipIdPairTables.add(
          new STRelationshipIdPairs(
              idPairsTable,
              szCriteriaOccurrence.name,
              primaryEntityName,
              szCriteriaOccurrence.criteriaEntity,
              szCriteriaOccurrence.primaryCriteriaRelationship.primaryEntityIdFieldName,
              szCriteriaOccurrence.primaryCriteriaRelationship.criteriaEntityIdFieldName));
    }
    szCriteriaOccurrence.occurrenceEntities.forEach(
        szOccurrenceEntity -> {
          if (szOccurrenceEntity.criteriaRelationship.idPairsSqlFile != null) {
            // RelationshipIdPairs table.
            String idPairsSql =
                configReader.readEntityGroupSql(
                    criteriaOccurrencePath, szOccurrenceEntity.criteriaRelationship.idPairsSqlFile);
            BQTable idPairsTable = new BQTable(idPairsSql);
            relationshipIdPairTables.add(
                new STRelationshipIdPairs(
                    idPairsTable,
                    szCriteriaOccurrence.name,
                    szOccurrenceEntity.occurrenceEntity,
                    szCriteriaOccurrence.criteriaEntity,
                    szOccurrenceEntity.criteriaRelationship.occurrenceEntityIdFieldName,
                    szOccurrenceEntity.criteriaRelationship.criteriaEntityIdFieldName));
          }
          if (szOccurrenceEntity.primaryRelationship.idPairsSqlFile != null) {
            // RelationshipIdPairs table.
            String idPairsSql =
                configReader.readEntityGroupSql(
                    criteriaOccurrencePath, szOccurrenceEntity.primaryRelationship.idPairsSqlFile);
            BQTable idPairsTable = new BQTable(idPairsSql);
            relationshipIdPairTables.add(
                new STRelationshipIdPairs(
                    idPairsTable,
                    szCriteriaOccurrence.name,
                    szOccurrenceEntity.occurrenceEntity,
                    primaryEntityName,
                    szOccurrenceEntity.primaryRelationship.occurrenceEntityIdFieldName,
                    szOccurrenceEntity.primaryRelationship.primaryEntityIdFieldName));
          }
        });
  }
}
