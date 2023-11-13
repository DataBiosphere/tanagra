package bio.terra.tanagra.underlay;

import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.underlay.serialization.SZBigQuery;
import bio.terra.tanagra.underlay.serialization.SZCriteriaOccurrence;
import bio.terra.tanagra.underlay.serialization.SZEntity;
import bio.terra.tanagra.underlay.serialization.SZGroupItems;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import bio.terra.tanagra.underlay.sourcetable.STEntityAttributes;
import bio.terra.tanagra.underlay.sourcetable.STHierarchyChildParent;
import bio.terra.tanagra.underlay.sourcetable.STHierarchyRootFilter;
import bio.terra.tanagra.underlay.sourcetable.STRelationshipIdPairs;
import bio.terra.tanagra.underlay.sourcetable.STTextSearchTerms;
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;

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

  private SourceSchema(
      List<STEntityAttributes> entityAttributesTables,
      List<STTextSearchTerms> textSearchTermsTables,
      List<STHierarchyChildParent> hierarchyChildParentTables,
      List<STHierarchyRootFilter> hierarchyRootFilterTables,
      List<STRelationshipIdPairs> relationshipIdPairTables) {
    this.entityAttributesTables = ImmutableList.copyOf(entityAttributesTables);
    this.textSearchTermsTables = ImmutableList.copyOf(textSearchTermsTables);
    this.hierarchyChildParentTables = ImmutableList.copyOf(hierarchyChildParentTables);
    this.hierarchyRootFilterTables = ImmutableList.copyOf(hierarchyRootFilterTables);
    this.relationshipIdPairTables = ImmutableList.copyOf(relationshipIdPairTables);
  }

  public STEntityAttributes getEntityAttributes(String entity) {
    return entityAttributesTables.stream()
        .filter(entityAttributes -> entityAttributes.getEntity().equals(entity))
        .findFirst()
        .get();
  }

  public STTextSearchTerms getTextSearchTerms(String entity) {
    return textSearchTermsTables.stream()
        .filter(textSearchTerms -> textSearchTerms.getEntity().equals(entity))
        .findFirst()
        .get();
  }

  public boolean hasTextSearchTerms(String entity) {
    return textSearchTermsTables.stream()
        .filter(textSearchTerms -> textSearchTerms.getEntity().equals(entity))
        .findFirst()
        .isPresent();
  }

  public STHierarchyChildParent getHierarchyChildParent(String entity, String hierarchy) {
    return hierarchyChildParentTables.stream()
        .filter(
            childParent ->
                childParent.getEntity().equals(entity)
                    && childParent.getHierarchy().equals(hierarchy))
        .findFirst()
        .get();
  }

  public STHierarchyRootFilter getHierarchyRootFilter(String entity, String hierarchy) {
    return hierarchyRootFilterTables.stream()
        .filter(
            rootFilter ->
                rootFilter.getEntity().equals(entity)
                    && rootFilter.getHierarchy().equals(hierarchy))
        .findFirst()
        .get();
  }

  public boolean hasHierarchyRootFilter(String entity, String hierarchy) {
    return hierarchyRootFilterTables.stream()
        .filter(
            rootFilter ->
                rootFilter.getEntity().equals(entity)
                    && rootFilter.getHierarchy().equals(hierarchy))
        .findFirst()
        .isPresent();
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
        .get();
  }

  public static SourceSchema fromConfig(
      SZBigQuery szBigQuery, SZUnderlay szUnderlay, ConfigReader configReader) {
    List<STEntityAttributes> entityAttributesTables = new ArrayList<>();
    List<STTextSearchTerms> textSearchTermsTables = new ArrayList<>();
    List<STHierarchyChildParent> hierarchyChildParentTables = new ArrayList<>();
    List<STHierarchyRootFilter> hierarchyRootFilterTables = new ArrayList<>();
    List<STRelationshipIdPairs> relationshipIdPairTables = new ArrayList<>();

    // Build source tables for each entity.
    szUnderlay.entities.stream()
        .forEach(
            entityPath ->
                fromConfigEntity(
                    entityPath,
                    configReader,
                    entityAttributesTables,
                    textSearchTermsTables,
                    hierarchyChildParentTables,
                    hierarchyRootFilterTables));

    // Build source tables for each entity group.
    szUnderlay.groupItemsEntityGroups.stream()
        .forEach(
            groupItemsPath ->
                fromConfigGroupItems(groupItemsPath, configReader, relationshipIdPairTables));
    szUnderlay.criteriaOccurrenceEntityGroups.stream()
        .forEach(
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
        relationshipIdPairTables);
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
    TablePointer allInstancesTable = new TablePointer(allInstancesSql);
    entityAttributesTables.add(
        new STEntityAttributes(allInstancesTable, szEntity.name, szEntity.attributes));

    if (szEntity.textSearch != null && szEntity.textSearch.idTextPairsSqlFile != null) {
      // TextSearchTerms table.
      String idTextPairsSql =
          configReader.readEntitySql(entityPath, szEntity.textSearch.idTextPairsSqlFile);
      TablePointer idTextPairsTable = new TablePointer(idTextPairsSql);
      textSearchTermsTables.add(
          new STTextSearchTerms(idTextPairsTable, szEntity.name, szEntity.textSearch));
    }

    szEntity.hierarchies.stream()
        .forEach(
            szHierarchy -> {
              // HierarchyChildParent table.
              String childParentSql =
                  configReader.readEntitySql(entityPath, szHierarchy.childParentIdPairsSqlFile);
              TablePointer childParentTable = new TablePointer(childParentSql);
              hierarchyChildParentTables.add(
                  new STHierarchyChildParent(childParentTable, szEntity.name, szHierarchy));

              if (szHierarchy.rootNodeIdsSqlFile != null) {
                // HierarchyRootFilter table.
                String rootNodeSql =
                    configReader.readEntitySql(entityPath, szHierarchy.rootNodeIdsSqlFile);
                TablePointer rootNodeTable = new TablePointer(rootNodeSql);
                hierarchyRootFilterTables.add(
                    new STHierarchyRootFilter(rootNodeTable, szEntity.name, szHierarchy));
              }
            });
  }

  private static void fromConfigGroupItems(
      String groupItemsPath,
      ConfigReader configReader,
      List<STRelationshipIdPairs> relationshipIdPairTables) {
    SZGroupItems szGroupItems = configReader.readGroupItems(groupItemsPath);
    if (szGroupItems.idPairsSqlFile != null) {
      // RelationshipIdPairs table.
      String idPairsSql =
          configReader.readEntityGroupSql(groupItemsPath, szGroupItems.idPairsSqlFile);
      TablePointer idPairsTable = new TablePointer(idPairsSql);
      relationshipIdPairTables.add(
          new STRelationshipIdPairs(
              idPairsTable,
              szGroupItems.name,
              szGroupItems.groupEntity,
              szGroupItems.itemsEntity,
              szGroupItems.groupEntityIdFieldName,
              szGroupItems.itemsEntityIdFieldName));
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
      TablePointer idPairsTable = new TablePointer(idPairsSql);
      relationshipIdPairTables.add(
          new STRelationshipIdPairs(
              idPairsTable,
              szCriteriaOccurrence.name,
              primaryEntityName,
              szCriteriaOccurrence.criteriaEntity,
              szCriteriaOccurrence.primaryCriteriaRelationship.primaryEntityIdFieldName,
              szCriteriaOccurrence.primaryCriteriaRelationship.criteriaEntityIdFieldName));
    }
    szCriteriaOccurrence.occurrenceEntities.stream()
        .forEach(
            szOccurrenceEntity -> {
              if (szOccurrenceEntity.criteriaRelationship.idPairsSqlFile != null) {
                // RelationshipIdPairs table.
                String idPairsSql =
                    configReader.readEntityGroupSql(
                        criteriaOccurrencePath,
                        szOccurrenceEntity.criteriaRelationship.idPairsSqlFile);
                TablePointer idPairsTable = new TablePointer(idPairsSql);
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
                        criteriaOccurrencePath,
                        szOccurrenceEntity.primaryRelationship.idPairsSqlFile);
                TablePointer idPairsTable = new TablePointer(idPairsSql);
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
