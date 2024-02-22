package bio.terra.tanagra.indexing.job.bigquery;

import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.indexing.job.BigQueryJob;
import bio.terra.tanagra.query.bigquery.translator.BQApiTranslator;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlQueryField;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.EntityGroup;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.indextable.ITHierarchyAncestorDescendant;
import bio.terra.tanagra.underlay.indextable.ITHierarchyChildParent;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableResult;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CleanHierarchyNodesWithZeroCounts extends BigQueryJob {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(CleanHierarchyNodesWithZeroCounts.class);
  private final EntityGroup entityGroup;
  private final ITEntityMain indexEntityTable;
  private final ITHierarchyChildParent indexChildParentTable;
  private final ITHierarchyAncestorDescendant indexAncestorDescendantTable;
  private final Attribute idAttribute;
  private final Hierarchy hierarchy;

  public CleanHierarchyNodesWithZeroCounts(
      SZIndexer indexerConfig,
      EntityGroup entityGroup,
      ITEntityMain indexEntityTable,
      ITHierarchyChildParent indexChildParentTable,
      ITHierarchyAncestorDescendant indexAncestorDescendantTable,
      Attribute idAttribute,
      Hierarchy hierarchy) {
    super(indexerConfig);
    this.entityGroup = entityGroup;
    this.indexEntityTable = indexEntityTable;
    this.indexChildParentTable = indexChildParentTable;
    this.indexAncestorDescendantTable = indexAncestorDescendantTable;
    this.idAttribute = idAttribute;
    this.hierarchy = hierarchy;
  }

  @Override
  public String getEntityGroup() {
    return entityGroup.getName();
  }

  @Override
  protected String getOutputTableName() {
    return indexEntityTable.getTablePointer().getTableName();
  }

  @Override
  protected Optional<Table> getOutputTable() {
    return googleBigQuery.getTable(
        indexerConfig.bigQuery.indexData.projectId,
        indexerConfig.bigQuery.indexData.datasetId,
        getOutputTableName());
  }

  @Override
  public JobStatus checkStatus() {
    return getOutputTable().isPresent() && outputTableHasNoNodesWithZeroCounts()
        ? JobStatus.COMPLETE
        : JobStatus.NOT_STARTED;
  }

  @Override
  public void run(boolean isDryRun) {
    cleanChildParent(isDryRun);
    cleanAncestorDescendant(isDryRun);
    updateNumChildren(isDryRun);
    cleanHierarchyNodesWithZeroCounts(isDryRun);
  }

  private void cleanHierarchyNodesWithZeroCounts(boolean isDryRun) {
    /* Build a delete-from query for the index entity main table that have zero counts
    for both hierarchy and non hierarchy fields */
    String cleanHierarchyNodesWithZeroCounts = "DELETE FROM " + generateSqlBody();
    LOGGER.info("main-entity-delete-from query: {}", cleanHierarchyNodesWithZeroCounts);

    // Run the delete-from to remove entities that have zero counts
    googleBigQuery.runInsertUpdateQuery(cleanHierarchyNodesWithZeroCounts, isDryRun);
  }

  private void cleanChildParent(boolean isDryRun) {
    SqlField childField = indexChildParentTable.getChildField();
    SqlField idField = indexEntityTable.getAttributeValueField(idAttribute.getName());
    // Build a delete-from query for the child parent relationships that have zero counts
    String cleanChildParent =
        "DELETE FROM "
            + indexChildParentTable.getTablePointer().render()
            + " WHERE "
            + SqlQueryField.of(childField).renderForSelect()
            + " IN ( SELECT "
            + SqlQueryField.of(idField).renderForSelect()
            + " FROM "
            + generateSqlBody()
            + ")";
    LOGGER.info("child-parent-delete-from query: {}", cleanChildParent);

    // Run the delete-from to remove the child parent relationships that have zero counts
    googleBigQuery.runInsertUpdateQuery(cleanChildParent, isDryRun);
  }

  private void cleanAncestorDescendant(boolean isDryRun) {
    SqlField descendantField = indexAncestorDescendantTable.getDescendantField();
    SqlField idField = indexEntityTable.getAttributeValueField(idAttribute.getName());
    // Build a delete-from query for the ancestor descendant relationships that have zero counts
    String cleanAncestorDescendant =
        "DELETE FROM "
            + indexAncestorDescendantTable.getTablePointer().render()
            + " WHERE "
            + SqlQueryField.of(descendantField).renderForSelect()
            + " IN ( SELECT "
            + SqlQueryField.of(idField).renderForSelect()
            + " FROM "
            + generateSqlBody()
            + ")";
    LOGGER.info("ancestor-descendant-delete-from query: {}", cleanAncestorDescendant);

    // Run the delete-from query for the ancestor descendant relationships that have zero counts
    googleBigQuery.runInsertUpdateQuery(cleanAncestorDescendant, isDryRun);
  }

  private void updateNumChildren(boolean isDryRun) {
    String updateTableAlias = "updatetable";
    String tempTableAlias = "temptable";
    final String countAlias = "count";
    SqlField childField = indexChildParentTable.getChildField();
    SqlField parentField = indexChildParentTable.getParentField();
    SqlField idField = indexEntityTable.getAttributeValueField(idAttribute.getName());
    SqlField entityNumChildrenField =
        indexEntityTable.getHierarchyNumChildrenField(hierarchy.getName());

    String innerSelect =
        "SELECT "
            + SqlQueryField.of(parentField).renderForSelect()
            + ", COUNT("
            + SqlQueryField.of(childField).renderForSelect()
            + ") AS "
            + countAlias
            + " FROM "
            + indexChildParentTable.getTablePointer().render()
            + " GROUP BY "
            + SqlQueryField.of(parentField).renderForSelect();

    String updateFromSelectSql =
        "UPDATE "
            + indexEntityTable.getTablePointer().render()
            + " AS "
            + updateTableAlias
            + " SET "
            + SqlQueryField.of(entityNumChildrenField).renderForSelect(updateTableAlias)
            + " = "
            + SqlQueryField.of(SqlField.of(countAlias)).renderForSelect(tempTableAlias)
            + " FROM ("
            + innerSelect
            + ") AS "
            + tempTableAlias
            + " WHERE "
            + SqlQueryField.of(idField).renderForSelect(updateTableAlias)
            + " = "
            + SqlQueryField.of(parentField).renderForSelect(tempTableAlias);

    LOGGER.info("update-num-children-from-select query: {}", updateFromSelectSql);

    // Run the update-from-select to update the count for num children.
    googleBigQuery.runInsertUpdateQuery(updateFromSelectSql, isDryRun);
  }

  private boolean outputTableHasNoNodesWithZeroCounts() {
    // Check if the table has rows with zero counts for both hierarchy and non hierarchy fields
    String selectCountSql = "SELECT COUNT(*) AS count FROM " + generateSqlBody();
    TableResult tableResult = googleBigQuery.queryBigQuery(selectCountSql);
    return tableResult.iterateAll().iterator().next().get("count").getLongValue() == 0;
  }

  private String generateSqlBody() {
    // Get the hierarchy and non hierarchy count fields
    SqlField entityTableCountField =
        indexEntityTable.getEntityGroupCountField(entityGroup.getName(), hierarchy.getName());
    SqlField entityTableNoHierCountField =
        indexEntityTable.getEntityGroupCountField(entityGroup.getName(), null);

    BQApiTranslator bqTranslator = new BQApiTranslator();
    return indexEntityTable.getTablePointer().render()
        + " WHERE "
        + SqlQueryField.of(entityTableCountField).renderForSelect()
        + bqTranslator.binaryOperatorSql(BinaryOperator.EQUALS)
        + "0"
        + " AND "
        + SqlQueryField.of(entityTableNoHierCountField).renderForSelect()
        + bqTranslator.binaryOperatorSql(BinaryOperator.EQUALS)
        + "0";
  }
}
