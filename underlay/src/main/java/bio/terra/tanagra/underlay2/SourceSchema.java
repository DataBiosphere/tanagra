package bio.terra.tanagra.underlay2;

import bio.terra.tanagra.query.DataPointer;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.query.bigquery.BigQueryDataset;
import bio.terra.tanagra.underlay2.serialization.SZBigQuery;
import bio.terra.tanagra.underlay2.serialization.SZCriteriaOccurrence;
import bio.terra.tanagra.underlay2.serialization.SZEntity;
import bio.terra.tanagra.underlay2.serialization.SZGroupItems;
import bio.terra.tanagra.underlay2.serialization.SZUnderlay;
import bio.terra.tanagra.underlay2.sourcetable.STEntityAttributes;
import bio.terra.tanagra.underlay2.sourcetable.STHierarchyChildParent;
import bio.terra.tanagra.underlay2.sourcetable.STHierarchyRootFilter;
import bio.terra.tanagra.underlay2.sourcetable.STRelationshipIdPairs;
import bio.terra.tanagra.underlay2.sourcetable.STTextSearchTerms;
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
    DataPointer sourceDataPointer =
        new BigQueryDataset(
            "source_datapointer",
            szBigQuery.sourceData.projectId,
            szBigQuery.sourceData.datasetId,
            szBigQuery.queryProjectId,
            null,
            null,
            null,
            null,
            false,
            null);

    List<STEntityAttributes> entityAttributesTables = new ArrayList<>();
    List<STTextSearchTerms> textSearchTermsTables = new ArrayList<>();
    List<STHierarchyChildParent> hierarchyChildParentTables = new ArrayList<>();
    List<STHierarchyRootFilter> hierarchyRootFilterTables = new ArrayList<>();
    List<STRelationshipIdPairs> relationshipIdPairTables = new ArrayList<>();

    // Build source tables for each entity.
    szUnderlay.entities.stream()
        .forEach(
            entityName ->
                fromConfigEntity(
                    entityName,
                    configReader,
                    sourceDataPointer,
                    entityAttributesTables,
                    textSearchTermsTables,
                    hierarchyChildParentTables,
                    hierarchyRootFilterTables));

    // Build source tables for each entity group.
    szUnderlay.groupItemsEntityGroups.stream()
        .forEach(
            groupItemsName ->
                fromConfigGroupItems(
                    groupItemsName, configReader, sourceDataPointer, relationshipIdPairTables));
    szUnderlay.criteriaOccurrenceEntityGroups.stream()
        .forEach(
            criteriaOccurrenceName ->
                fromConfigCriteriaOccurrence(
                    criteriaOccurrenceName,
                    szUnderlay.primaryEntity,
                    configReader,
                    sourceDataPointer,
                    relationshipIdPairTables));
    return new SourceSchema(
        entityAttributesTables,
        textSearchTermsTables,
        hierarchyChildParentTables,
        hierarchyRootFilterTables,
        relationshipIdPairTables);
  }

  private static void fromConfigEntity(
      String entityName,
      ConfigReader configReader,
      DataPointer sourceDataPointer,
      List<STEntityAttributes> entityAttributesTables,
      List<STTextSearchTerms> textSearchTermsTables,
      List<STHierarchyChildParent> hierarchyChildParentTables,
      List<STHierarchyRootFilter> hierarchyRootFilterTables) {
    SZEntity szEntity = configReader.readEntity(entityName);

    // EntityAttributes table.
    String allInstancesSql = configReader.readEntitySql(entityName, szEntity.allInstancesSqlFile);
    TablePointer allInstancesTable = TablePointer.fromRawSql(allInstancesSql, sourceDataPointer);
    entityAttributesTables.add(
        new STEntityAttributes(allInstancesTable, entityName, szEntity.attributes));

    if (szEntity.textSearch != null && szEntity.textSearch.idTextPairsSqlFile != null) {
      // TextSearchTerms table.
      String idTextPairsSql =
          configReader.readEntitySql(entityName, szEntity.textSearch.idTextPairsSqlFile);
      TablePointer idTextPairsTable = TablePointer.fromRawSql(idTextPairsSql, sourceDataPointer);
      textSearchTermsTables.add(
          new STTextSearchTerms(idTextPairsTable, entityName, szEntity.textSearch));
    }

    szEntity.hierarchies.stream()
        .forEach(
            szHierarchy -> {
              // HierarchyChildParent table.
              String childParentSql =
                  configReader.readEntitySql(entityName, szHierarchy.childParentIdPairsSqlFile);
              TablePointer childParentTable =
                  TablePointer.fromRawSql(childParentSql, sourceDataPointer);
              hierarchyChildParentTables.add(
                  new STHierarchyChildParent(childParentTable, entityName, szHierarchy));

              if (szHierarchy.rootNodeIdsSqlFile != null) {
                // HierarchyRootFilter table.
                String rootNodeSql =
                    configReader.readEntitySql(entityName, szHierarchy.rootNodeIdsSqlFile);
                TablePointer rootNodeTable =
                    TablePointer.fromRawSql(rootNodeSql, sourceDataPointer);
                hierarchyRootFilterTables.add(
                    new STHierarchyRootFilter(rootNodeTable, entityName, szHierarchy));
              }
            });
  }

  private static void fromConfigGroupItems(
      String groupItemsName,
      ConfigReader configReader,
      DataPointer sourceDataPointer,
      List<STRelationshipIdPairs> relationshipIdPairTables) {
    SZGroupItems szGroupItems = configReader.readGroupItems(groupItemsName);
    if (szGroupItems.idPairsSqlFile != null) {
      // RelationshipIdPairs table.
      String idPairsSql =
          configReader.readEntityGroupSql(groupItemsName, szGroupItems.idPairsSqlFile);
      TablePointer idPairsTable = TablePointer.fromRawSql(idPairsSql, sourceDataPointer);
      relationshipIdPairTables.add(
          new STRelationshipIdPairs(
              idPairsTable,
              groupItemsName,
              szGroupItems.groupEntity,
              szGroupItems.itemsEntity,
              szGroupItems.groupEntityIdFieldName,
              szGroupItems.itemsEntityIdFieldName));
    }
  }

  private static void fromConfigCriteriaOccurrence(
      String criteriaOccurrenceName,
      String primaryEntityName,
      ConfigReader configReader,
      DataPointer sourceDataPointer,
      List<STRelationshipIdPairs> relationshipIdPairTables) {
    SZCriteriaOccurrence szCriteriaOccurrence =
        configReader.readCriteriaOccurrence(criteriaOccurrenceName);
    if (szCriteriaOccurrence.primaryCriteriaRelationship.idPairsSqlFile != null) {
      // RelationshipIdPairs table.
      String idPairsSql =
          configReader.readEntityGroupSql(
              criteriaOccurrenceName,
              szCriteriaOccurrence.primaryCriteriaRelationship.idPairsSqlFile);
      TablePointer idPairsTable = TablePointer.fromRawSql(idPairsSql, sourceDataPointer);
      relationshipIdPairTables.add(
          new STRelationshipIdPairs(
              idPairsTable,
              criteriaOccurrenceName,
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
                        criteriaOccurrenceName,
                        szOccurrenceEntity.criteriaRelationship.idPairsSqlFile);
                TablePointer idPairsTable = TablePointer.fromRawSql(idPairsSql, sourceDataPointer);
                relationshipIdPairTables.add(
                    new STRelationshipIdPairs(
                        idPairsTable,
                        criteriaOccurrenceName,
                        szOccurrenceEntity.occurrenceEntity,
                        szCriteriaOccurrence.criteriaEntity,
                        szOccurrenceEntity.criteriaRelationship.occurrenceEntityIdFieldName,
                        szOccurrenceEntity.criteriaRelationship.criteriaEntityIdFieldName));
              }
              if (szOccurrenceEntity.primaryRelationship.idPairsSqlFile != null) {
                // RelationshipIdPairs table.
                String idPairsSql =
                    configReader.readEntityGroupSql(
                        criteriaOccurrenceName,
                        szOccurrenceEntity.primaryRelationship.idPairsSqlFile);
                TablePointer idPairsTable = TablePointer.fromRawSql(idPairsSql, sourceDataPointer);
                relationshipIdPairTables.add(
                    new STRelationshipIdPairs(
                        idPairsTable,
                        criteriaOccurrenceName,
                        szOccurrenceEntity.occurrenceEntity,
                        primaryEntityName,
                        szOccurrenceEntity.primaryRelationship.occurrenceEntityIdFieldName,
                        szOccurrenceEntity.primaryRelationship.primaryEntityIdFieldName));
              }
            });
  }
}
