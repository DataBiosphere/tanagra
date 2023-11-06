// package bio.terra.tanagra.indexing.job;
//
// import bio.terra.tanagra.query.ColumnHeaderSchema;
// import bio.terra.tanagra.query.ColumnSchema;
// import bio.terra.tanagra.query.FieldPointer;
// import bio.terra.tanagra.query.FieldVariable;
// import bio.terra.tanagra.query.FilterVariable;
// import bio.terra.tanagra.query.Literal;
// import bio.terra.tanagra.query.Query;
// import bio.terra.tanagra.query.QueryRequest;
// import bio.terra.tanagra.query.QueryResult;
// import bio.terra.tanagra.query.TablePointer;
// import bio.terra.tanagra.query.TableVariable;
// import bio.terra.tanagra.query.UpdateFromSelect;
// import bio.terra.tanagra.query.bigquery.BigQueryDataset;
// import bio.terra.tanagra.query.bigquery.BigQueryExecutor;
// import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
// import bio.terra.tanagra.utils.GoogleBigQuery;
// import com.google.cloud.bigquery.BigQueryException;
// import com.google.common.collect.Lists;
// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import org.apache.http.HttpStatus;
//
// public abstract class BigQueryIndexingJob implements IndexingJob {
//  private final BigQueryExecutor bigQueryExecutor;
//  private final GoogleBigQuery googleBigQuery;
//
//  protected BigQueryIndexingJob(BigQueryExecutor bigQueryExecutor, GoogleBigQuery googleBigQuery)
// {
//    this.bigQueryExecutor = bigQueryExecutor;
//    this.googleBigQuery = googleBigQuery;
//  }
//
//  // -----Helper methods for checking whether a job has run already.-------
//  protected boolean checkOneRowExists(TablePointer tablePointer) {
//    // Check if the table has at least one row.
//    BigQueryDataset dataPointer = getBQDataPointer(tablePointer);
//    int numRows =
//        dataPointer
//            .getBigQueryService()
//            .getNumRows(
//                dataPointer.getProjectId(),
//                dataPointer.getDatasetId(),
//                tablePointer.getTableName());
//    return numRows > 0;
//  }
//
//  protected boolean checkOneNotNullIdRowExists() {
//    // Check if the table has at least 1 id row where id IS NOT NULL
//    FieldPointer idField = getEntity().getIdAttribute().getIndexValueField();
//    ColumnSchema idColumnSchema = getEntity().getIdAttribute().getIndexValueColumnSchema();
//    return checkOneNotNullRowExists(idField, idColumnSchema);
//  }
//
//  protected boolean checkOneNotNullRowExists(FieldPointer fieldPointer, ColumnSchema columnSchema)
// {
//    // Check if the table has at least 1 row with a non-null field value.
//    TableVariable outputTableVar = TableVariable.forPrimary(fieldPointer.getTablePointer());
//    List<TableVariable> tableVars = Lists.newArrayList(outputTableVar);
//
//    FieldVariable fieldVar = fieldPointer.buildVariable(outputTableVar, tableVars);
//    FilterVariable fieldNotNull =
//        new BinaryFilterVariable(
//            fieldVar, BinaryFilterVariable.BinaryOperator.IS_NOT, new Literal((String) null));
//    Query query =
//        new Query.Builder()
//            .select(List.of(fieldVar))
//            .tables(tableVars)
//            .where(fieldNotNull)
//            .limit(1)
//            .build();
//
//    ColumnHeaderSchema columnHeaderSchema = new ColumnHeaderSchema(List.of(columnSchema));
//    QueryRequest queryRequest = new QueryRequest(query.renderSQL(), columnHeaderSchema);
//    QueryResult queryResult =
//        getBQDataPointer(fieldPointer.getTablePointer()).getQueryExecutor().execute(queryRequest);
//
//    return queryResult.getRowResults().iterator().hasNext();
//  }
//
//  // -----Helper methods for running insert/update jobs in BigQuery directly (i.e. not via
//  // Dataflow).-------
//  protected void updateEntityTableFromSelect(
//      Query selectQuery,
//      Map<String, FieldVariable> updateFieldMap,
//      String selectIdFieldName,
//      boolean isDryRun) {
//    // Build a TableVariable for the (output) entity table that we want to update.
//    List<TableVariable> outputTables = new ArrayList<>();
//    TableVariable entityTable = TableVariable.forPrimary(getEntity().getIndexEntityTable());
//    outputTables.add(entityTable);
//
//    // Build a map of (output) update FieldVariable -> (input) selected FieldVariable.
//    Map<FieldVariable, FieldVariable> updateFields = new HashMap<>();
//    for (Map.Entry<String, FieldVariable> updateToSelect : updateFieldMap.entrySet()) {
//      String updateFieldName = updateToSelect.getKey();
//      FieldVariable selectField = updateToSelect.getValue();
//      FieldVariable updateField =
//          new FieldPointer.Builder()
//              .tablePointer(entityTable.getTablePointer())
//              .columnName(updateFieldName)
//              .build()
//              .buildVariable(entityTable, outputTables);
//      updateFields.put(updateField, selectField);
//    }
//
//    // Build a FieldVariable for the id field in the (output) entity table.
//    FieldVariable updateIdField =
//        getEntity().getIdAttribute().getIndexValueField().buildVariable(entityTable,
// outputTables);
//
//    // Get the FieldVariable for the id field in the input table.
//    FieldVariable selectIdField =
//        selectQuery.getSelect().stream()
//            .filter(fv -> fv.getAliasOrColumnName().equals(selectIdFieldName))
//            .findFirst()
//            .get();
//
//    UpdateFromSelect updateQuery =
//        new UpdateFromSelect(entityTable, updateFields, selectQuery, updateIdField,
// selectIdField);
//    LOGGER.info("Generated SQL: {}", updateQuery.renderSQL());
//    try {
//      insertUpdateTableFromSelect(updateQuery.renderSQL(), isDryRun);
//    } catch (BigQueryException bqEx) {
//      if (bqEx.getCode() == HttpStatus.SC_NOT_FOUND) {
//        LOGGER.info(
//            "Query dry run failed because table has not been created yet: {}",
//            bqEx.getError().getMessage());
//      } else {
//        throw bqEx;
//      }
//    }
//  }
//
//  protected void insertUpdateTableFromSelect(String sql, boolean isDryRun) {
//    getBQDataPointer(getEntity().getIndexEntityTable())
//        .getBigQueryService()
//        .runInsertUpdateQuery(sql, isDryRun);
//  }
// }
