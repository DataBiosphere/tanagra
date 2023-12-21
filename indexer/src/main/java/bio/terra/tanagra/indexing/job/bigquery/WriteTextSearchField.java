package bio.terra.tanagra.indexing.job.bigquery;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.indexing.job.BigQueryJob;
import bio.terra.tanagra.query2.bigquery.BQTranslator;
import bio.terra.tanagra.query2.sql.SqlField;
import bio.terra.tanagra.query2.sql.SqlQueryField;
import bio.terra.tanagra.query2.sql.SqlTable;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import bio.terra.tanagra.underlay.sourcetable.STTextSearchTerms;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
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
            && outputTableHasAtLeastOneRowWithNotNullField(indexTable.getTextSearchField())
        ? JobStatus.COMPLETE
        : JobStatus.NOT_STARTED;
  }

  @Override
  public void run(boolean isDryRun) {
    List<String> idTextSqls = new ArrayList<>();
    final String idAlias = "idVal";
    final String textAlias = "textVal";
    BQTranslator bqTranslator = new BQTranslator();

    // Build a query for each id-attribute pair.
    // SELECT id AS idVal, attrDisp AS textVal FROM entityMain
    SqlField entityTableIdField =
        indexTable.getAttributeValueField(entity.getIdAttribute().getName());
    entity.getOptimizeTextSearchAttributes().stream()
        .forEach(
            attribute -> {
              SqlField attributeTextField;
              if (attribute.isValueDisplay()) {
                attributeTextField = indexTable.getAttributeDisplayField(attribute.getName());
              } else if (!attribute.getDataType().equals(DataType.STRING)) {
                attributeTextField =
                    indexTable
                        .getAttributeValueField(attribute.getName())
                        .toBuilder()
                        .sqlFunctionWrapper("CAST(${fieldSql} AS STRING)")
                        .build();
              } else {
                attributeTextField = indexTable.getAttributeValueField(attribute.getName());
              }

              String idTextSql =
                  "SELECT "
                      + bqTranslator.selectSql(SqlQueryField.of(entityTableIdField, idAlias), null)
                      + ", "
                      + bqTranslator.selectSql(
                          SqlQueryField.of(attributeTextField, textAlias), null)
                      + " FROM "
                      + indexTable.getTablePointer().renderSQL();
              idTextSqls.add(idTextSql);
            });

    // Build a query for the id-text pairs.
    // SELECT id AS idVal, text AS textVal FROM textSearchTerms
    if (sourceTable != null) {
      String idTextSql =
          "SELECT "
              + bqTranslator.selectSql(SqlQueryField.of(sourceTable.getIdField(), idAlias), null)
              + ", "
              + bqTranslator.selectSql(
                  SqlQueryField.of(sourceTable.getTextField(), textAlias), null)
              + " FROM "
              + sourceTable.getTablePointer().renderSQL();
      idTextSqls.add(idTextSql);
    }

    // Build a string concatenation query for all the id-attribute and id-text queries.
    // SELECT idVal, STRING_AGG(textVal) FROM (
    //   SELECT id AS idVal, attrDisp AS textVal FROM entityMain
    //   UNION ALL
    //   SELECT id AS idVal, text AS textVal FROM textSearchTerms
    // ) GROUP BY idVal
    String unionAllSql = idTextSqls.stream().collect(Collectors.joining(" UNION ALL "));
    SqlTable unionAllTable = new SqlTable(unionAllSql);
    SqlField tempTableIdField =
        new SqlField.Builder().tablePointer(unionAllTable).columnName(idAlias).build();
    SqlField tempTableTextField =
        new SqlField.Builder()
            .tablePointer(unionAllTable)
            .columnName(textAlias)
            .sqlFunctionWrapper("STRING_AGG")
            .build();
    String selectTextConcatSql =
        "SELECT "
            + bqTranslator.selectSql(SqlQueryField.of(tempTableIdField, null), null)
            + ", "
            + bqTranslator.selectSql(SqlQueryField.of(tempTableTextField, textAlias), null)
            + " FROM "
            + unionAllTable.renderSQL()
            + " GROUP BY "
            + bqTranslator.groupBySql(SqlQueryField.of(tempTableIdField, null), null, true);
    LOGGER.info("idTextPairs union query: {}", selectTextConcatSql);

    // Build an update-from-select query for the index entity main table and the id text pairs
    // query.
    String updateTableAlias = "updatetable";
    String tempTableAlias = "temptable";
    String updateFromSelectSql =
        "UPDATE "
            + indexTable.getTablePointer().renderSQL()
            + " AS "
            + updateTableAlias
            + " SET "
            + bqTranslator.selectSql(
                SqlQueryField.of(indexTable.getTextSearchField(), null), updateTableAlias)
            + " = "
            + bqTranslator.selectSql(SqlQueryField.of(tempTableTextField, null), tempTableAlias)
            + " FROM ("
            + selectTextConcatSql
            + ") WHERE "
            + bqTranslator.selectSql(SqlQueryField.of(entityTableIdField, null), updateTableAlias)
            + " = "
            + bqTranslator.selectSql(SqlQueryField.of(tempTableIdField, null), tempTableAlias);
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
