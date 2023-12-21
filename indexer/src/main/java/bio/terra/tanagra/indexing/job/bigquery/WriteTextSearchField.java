package bio.terra.tanagra.indexing.job.bigquery;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.indexing.job.BigQueryJob;
import bio.terra.tanagra.query.bigquery.BQTable;
import bio.terra.tanagra.query.bigquery.translator.BQApiTranslator;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlQueryField;
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
    BQApiTranslator bqTranslator = new BQApiTranslator();

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
                        .cloneWithFunctionWrapper("CAST(${fieldSql} AS STRING)");
              } else {
                attributeTextField = indexTable.getAttributeValueField(attribute.getName());
              }

              String idTextSql =
                  "SELECT "
                      + bqTranslator.selectSql(SqlQueryField.of(entityTableIdField, idAlias))
                      + ", "
                      + bqTranslator.selectSql(SqlQueryField.of(attributeTextField, textAlias))
                      + " FROM "
                      + indexTable.getTablePointer().renderForQuery();
              idTextSqls.add(idTextSql);
            });

    // Build a query for the id-text pairs.
    // SELECT id AS idVal, text AS textVal FROM textSearchTerms
    if (sourceTable != null) {
      String idTextSql =
          "SELECT "
              + bqTranslator.selectSql(SqlQueryField.of(sourceTable.getIdField(), idAlias))
              + ", "
              + bqTranslator.selectSql(SqlQueryField.of(sourceTable.getTextField(), textAlias))
              + " FROM "
              + sourceTable.getTablePointer().renderForQuery();
      idTextSqls.add(idTextSql);
    }

    // Build a string concatenation query for all the id-attribute and id-text queries.
    // SELECT idVal, STRING_AGG(textVal) FROM (
    //   SELECT id AS idVal, attrDisp AS textVal FROM entityMain
    //   UNION ALL
    //   SELECT id AS idVal, text AS textVal FROM textSearchTerms
    // ) GROUP BY idVal
    String unionAllSql = idTextSqls.stream().collect(Collectors.joining(" UNION ALL "));
    BQTable unionAllTable = new BQTable(unionAllSql);
    SqlField tempTableIdField = SqlField.of(idAlias);
    SqlField tempTableTextField = SqlField.of(textAlias, "STRING_AGG");
    String selectTextConcatSql =
        "SELECT "
            + bqTranslator.selectSql(SqlQueryField.of(tempTableIdField))
            + ", "
            + bqTranslator.selectSql(SqlQueryField.of(tempTableTextField, textAlias))
            + " FROM "
            + unionAllTable.renderForQuery()
            + " GROUP BY "
            + bqTranslator.groupBySql(SqlQueryField.of(tempTableIdField), null, true);
    LOGGER.info("idTextPairs union query: {}", selectTextConcatSql);

    // Build an update-from-select query for the index entity main table and the id text pairs
    // query.
    String updateTableAlias = "updatetable";
    String tempTableAlias = "temptable";
    String updateFromSelectSql =
        "UPDATE "
            + indexTable.getTablePointer().renderForQuery()
            + " AS "
            + updateTableAlias
            + " SET "
            + bqTranslator.selectSql(
                SqlQueryField.of(indexTable.getTextSearchField()), updateTableAlias)
            + " = "
            + bqTranslator.selectSql(SqlQueryField.of(tempTableTextField), tempTableAlias)
            + " FROM ("
            + selectTextConcatSql
            + ") WHERE "
            + bqTranslator.selectSql(SqlQueryField.of(entityTableIdField), updateTableAlias)
            + " = "
            + bqTranslator.selectSql(SqlQueryField.of(tempTableIdField), tempTableAlias);
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
