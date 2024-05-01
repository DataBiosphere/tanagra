package bio.terra.tanagra.indexing.job.bigquery;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.indexing.job.BigQueryJob;
import bio.terra.tanagra.query.bigquery.BQTable;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlQueryField;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import bio.terra.tanagra.underlay.sourcetable.STTextSearchTerms;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WriteTextSearchField extends BigQueryJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(WriteTextSearchField.class);

  private final Entity entity;
  private final @Nullable STTextSearchTerms sourceTable;
  private final ITEntityMain indexTable;

  public WriteTextSearchField(
      SZIndexer indexerConfig,
      Entity entity,
      @Nullable STTextSearchTerms sourceTable,
      ITEntityMain indexTable) {
    super(indexerConfig);
    this.entity = entity;
    this.sourceTable = sourceTable;
    this.indexTable = indexTable;
  }

  @Override
  public String getEntity() {
    return entity.getName();
  }

  @Override
  protected String getOutputTableName() {
    return indexTable.getTablePointer().getTableName();
  }

  @Override
  public JobStatus checkStatus() {
    return outputTableHasAtLeastOneRow()
            && outputTableHasAtLeastOneRowWithNotNullField(
                indexTable.getTablePointer(), indexTable.getTextSearchField())
        ? JobStatus.COMPLETE
        : JobStatus.NOT_STARTED;
  }

  @Override
  public void run(boolean isDryRun) {
    List<String> idTextSqls = new ArrayList<>();
    final String idAlias = "idVal";
    final String textAlias = "textVal";

    // Build a query for each id-attribute pair.
    // SELECT id AS idVal, attrDisp AS textVal FROM entityMain
    SqlField entityTableIdField =
        indexTable.getAttributeValueField(entity.getIdAttribute().getName());
    entity
        .getOptimizeTextSearchAttributes()
        .forEach(
            attribute -> {
              SqlField attributeTextField;
              if (attribute.isValueDisplay()) {
                attributeTextField = indexTable.getAttributeDisplayField(attribute.getName());
              } else if (!attribute.getDataType().equals(DataType.STRING)) {
                attributeTextField =
                    indexTable
                        .getAttributeValueField(attribute.getName())
                        .cloneWithFunctionWrapper("CAST(${fieldSql} AS STRING)");
              } else {
                attributeTextField = indexTable.getAttributeValueField(attribute.getName());
              }

              String idTextSql =
                  "SELECT "
                      + SqlQueryField.of(entityTableIdField, idAlias).renderForSelect()
                      + ", "
                      + SqlQueryField.of(attributeTextField, textAlias).renderForSelect()
                      + " FROM "
                      + indexTable.getTablePointer().render();
              idTextSqls.add(idTextSql);
            });

    // Build a query for the id-text pairs.
    // SELECT id AS idVal, text AS textVal FROM textSearchTerms
    if (sourceTable != null) {
      String idTextSql =
          "SELECT "
              + SqlQueryField.of(sourceTable.getIdField(), idAlias).renderForSelect()
              + ", "
              + SqlQueryField.of(sourceTable.getTextField(), textAlias).renderForSelect()
              + " FROM "
              + sourceTable.getTablePointer().render();
      idTextSqls.add(idTextSql);
    }

    // Build a string concatenation query for all the id-attribute and id-text queries.
    // SELECT idVal, STRING_AGG(textVal) FROM (
    //   SELECT id AS idVal, attrDisp AS textVal FROM entityMain
    //   UNION ALL
    //   SELECT id AS idVal, text AS textVal FROM textSearchTerms
    // ) GROUP BY idVal
    String unionAllSql = String.join(" UNION ALL ", idTextSqls);
    BQTable unionAllTable = new BQTable(unionAllSql);
    SqlField tempTableIdField = SqlField.of(idAlias);
    SqlField tempTableTextField = SqlField.of(textAlias, "STRING_AGG");
    String selectTextConcatSql =
        "SELECT "
            + SqlQueryField.of(tempTableIdField).renderForSelect()
            + ", "
            + SqlQueryField.of(tempTableTextField, textAlias).renderForSelect()
            + " FROM "
            + unionAllTable.render()
            + " GROUP BY "
            + SqlQueryField.of(tempTableIdField).renderForGroupBy(null, true);
    LOGGER.info("idTextPairs union query: {}", selectTextConcatSql);

    // Build an update-from-select query for the index entity main table and the id text pairs
    // query.
    String updateTableAlias = "updatetable";
    String tempTableAlias = "temptable";
    String updateFromSelectSql =
        "UPDATE "
            + indexTable.getTablePointer().render()
            + " AS "
            + updateTableAlias
            + " SET "
            + SqlQueryField.of(indexTable.getTextSearchField()).renderForSelect(updateTableAlias)
            + " = "
            + SqlQueryField.of(SqlField.of(textAlias)).renderForSelect(tempTableAlias)
            + " FROM ("
            + selectTextConcatSql
            + ") AS "
            + tempTableAlias
            + " WHERE "
            + SqlQueryField.of(entityTableIdField).renderForSelect(updateTableAlias)
            + " = "
            + SqlQueryField.of(tempTableIdField).renderForSelect(tempTableAlias);
    LOGGER.info("update-from-select query: {}", updateFromSelectSql);

    // Run the update-from-select to write the text search field in the index entity main table.
    googleBigQuery.runInsertUpdateQuery(updateFromSelectSql, isDryRun);
  }

  @Override
  public void clean(boolean isDryRun) {
    LOGGER.info(
        "Nothing to clean. CreateEntityTable will delete the output table, which includes all the rows updated by this job.");
  }
}
