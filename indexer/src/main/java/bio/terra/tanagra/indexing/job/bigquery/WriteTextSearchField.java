package bio.terra.tanagra.indexing.job.bigquery;

import bio.terra.tanagra.indexing.job.BigQueryJob;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.query.UnionQuery;
import bio.terra.tanagra.query.UpdateFromSelect;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import bio.terra.tanagra.underlay.sourcetable.STTextSearchTerms;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    return sourceTable.getEntity();
  }

  @Override
  protected String getOutputTableName() {
    return indexTable.getTablePointer().getTableName();
  }

  @Override
  public JobStatus checkStatus() {
    return outputTableHasAtLeastOneRow()
            && outputTableHasAtLeastOneRowWithNotNullField(
                indexTable.getTextSearchField(), indexTable.getTextSearchColumnSchema())
        ? JobStatus.COMPLETE
        : JobStatus.NOT_STARTED;
  }

  @Override
  public void run(boolean isDryRun) {
    List<Query> idTextQueries = new ArrayList<>();
    final String idAlias = "idVal";
    final String textAlias = "textVal";

    // Build a query for each id-attribute pair.
    TableVariable entityMainTableVar = TableVariable.forPrimary(indexTable.getTablePointer());
    List<TableVariable> attributeTableVars = Lists.newArrayList(entityMainTableVar);
    FieldVariable entityMainIdFieldVar =
        indexTable
            .getAttributeValueField(entity.getIdAttribute().getName())
            .buildVariable(entityMainTableVar, attributeTableVars, idAlias);
    entity.getOptimizeTextSearchAttributes().stream()
        .forEach(
            attribute -> {
              FieldPointer attributeTextField;
              if (attribute.isValueDisplay()) {
                attributeTextField = indexTable.getAttributeDisplayField(attribute.getName());
              } else if (!attribute.getDataType().equals(Literal.DataType.STRING)) {
                attributeTextField =
                    indexTable
                        .getAttributeValueField(attribute.getName())
                        .toBuilder()
                        .sqlFunctionWrapper("CAST(${fieldSql} AS STRING)")
                        .build();
              } else {
                attributeTextField = indexTable.getAttributeValueField(attribute.getName());
              }

              FieldVariable attributeTextFieldVar =
                  attributeTextField.buildVariable(
                      entityMainTableVar, attributeTableVars, textAlias);
              idTextQueries.add(
                  new Query.Builder()
                      .select(List.of(entityMainIdFieldVar, attributeTextFieldVar))
                      .tables(attributeTableVars)
                      .build());
            });

    // Build a query for the id-text pairs.
    if (sourceTable != null) {
      TableVariable searchTermsTableVar = TableVariable.forPrimary(sourceTable.getTablePointer());
      List<TableVariable> searchTermsTableVars = Lists.newArrayList(searchTermsTableVar);
      FieldVariable idFieldVar =
          sourceTable
              .getIdField()
              .buildVariable(searchTermsTableVar, searchTermsTableVars, idAlias);
      FieldVariable textFieldVar =
          sourceTable
              .getTextField()
              .buildVariable(searchTermsTableVar, searchTermsTableVars, textAlias);
      idTextQueries.add(
          new Query.Builder()
              .select(List.of(idFieldVar, textFieldVar))
              .tables(searchTermsTableVars)
              .build());
    }

    // Build a string concatenation query for all the id-attribute and id-text queries.
    TablePointer unionQueryTable = new TablePointer(new UnionQuery(idTextQueries).renderSQL());
    FieldPointer unionTableIdField =
        new FieldPointer.Builder().tablePointer(unionQueryTable).columnName(idAlias).build();
    FieldPointer aggregateTextField =
        new FieldPointer.Builder()
            .tablePointer(unionQueryTable)
            .columnName(textAlias)
            .sqlFunctionWrapper("STRING_AGG")
            .build();

    TableVariable unionQueryTableVar = TableVariable.forPrimary(unionQueryTable);
    FieldVariable unionTableIdFieldVar =
        new FieldVariable(unionTableIdField, unionQueryTableVar, idAlias);
    FieldVariable aggregateTextFieldFieldVar =
        new FieldVariable(aggregateTextField, unionQueryTableVar, textAlias);
    Query idTextPairsQuery =
        new Query.Builder()
            .select(List.of(unionTableIdFieldVar, aggregateTextFieldFieldVar))
            .tables(List.of(unionQueryTableVar))
            .groupBy(List.of(unionTableIdFieldVar))
            .build();
    LOGGER.info("idTextPairs union query: {}", idTextPairsQuery.renderSQL());

    // Build an update-from-select query for the index entity main table and the id text pairs
    // query.
    TableVariable updateTable = TableVariable.forPrimary(indexTable.getTablePointer());
    List<TableVariable> updateTableVars = Lists.newArrayList(updateTable);
    FieldVariable updateTableTextFieldVar =
        indexTable.getTextSearchField().buildVariable(updateTable, updateTableVars);
    Map<FieldVariable, FieldVariable> updateFields =
        Map.of(updateTableTextFieldVar, aggregateTextFieldFieldVar);
    FieldVariable updateTableIdFieldVar =
        indexTable
            .getAttributeValueField(entity.getIdAttribute().getName())
            .buildVariable(updateTable, updateTableVars);
    UpdateFromSelect updateQuery =
        new UpdateFromSelect(
            updateTable,
            updateFields,
            idTextPairsQuery,
            updateTableIdFieldVar,
            unionTableIdFieldVar);
    LOGGER.info("update-from-select query: {}", updateQuery.renderSQL());

    // Run the update-from-select to write the text search field in the index entity main table.
    bigQueryExecutor.getBigQueryService().runInsertUpdateQuery(updateQuery.renderSQL(), isDryRun);
  }

  @Override
  public void clean(boolean isDryRun) {
    LOGGER.info(
        "Nothing to clean. CreateEntityTable will delete the output table, which includes all the rows updated by this job.");
  }
}
