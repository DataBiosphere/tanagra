package bio.terra.tanagra.query2.bigquery;

import bio.terra.tanagra.api.field.valuedisplay.AttributeField;
import bio.terra.tanagra.api.field.valuedisplay.EntityIdCountField;
import bio.terra.tanagra.api.field.valuedisplay.ValueDisplayField;
import bio.terra.tanagra.api.query.ValueDisplay;
import bio.terra.tanagra.api.query.count.CountInstance;
import bio.terra.tanagra.api.query.count.CountQueryRequest;
import bio.terra.tanagra.api.query.count.CountQueryResult;
import bio.terra.tanagra.api.query.hint.HintInstance;
import bio.terra.tanagra.api.query.hint.HintQueryRequest;
import bio.terra.tanagra.api.query.hint.HintQueryResult;
import bio.terra.tanagra.api.query.list.ListInstance;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.api.query.list.ListQueryResult;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.query2.QueryRunner;
import bio.terra.tanagra.query2.bigquery.fieldtranslator.BQAttributeFieldTranslator;
import bio.terra.tanagra.query2.sql.SqlParams;
import bio.terra.tanagra.query2.sql.SqlQueryRequest;
import bio.terra.tanagra.query2.sql.SqlQueryResult;
import bio.terra.tanagra.underlay.indextable.ITEntityLevelDisplayHints;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.indextable.ITInstanceLevelDisplayHints;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BQQueryRunner implements QueryRunner {
  private final BQExecutor bigQueryExecutor;

  public BQQueryRunner(String queryProjectId, String datasetLocation) {
    this.bigQueryExecutor = new BQExecutor(queryProjectId, datasetLocation);
  }

  @Override
  public ListQueryResult run(ListQueryRequest listQueryRequest) {
    // Build the SQL query.
    StringBuilder sql = new StringBuilder();
    SqlParams sqlParams = new SqlParams();
    BQTranslator bqTranslator = new BQTranslator();

    // All the select fields come from the index entity main table.
    if (listQueryRequest.getSelectFields().isEmpty()) {
      throw new InvalidQueryException("List query must include at least one select field");
    }
    ITEntityMain entityMain =
        listQueryRequest
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(listQueryRequest.getEntity().getName());
    List<String> selectFields = new ArrayList<>();
    listQueryRequest.getSelectFields().stream()
        .forEach(
            valueDisplayField ->
                bqTranslator.translator(valueDisplayField).buildSqlFieldsForListSelect().stream()
                    .forEach(sqlField -> selectFields.add(bqTranslator.selectSql(sqlField, null))));

    // SELECT [select fields] FROM [entity main]
    sql.append("SELECT ")
        .append(selectFields.stream().collect(Collectors.joining(", ")))
        .append(" FROM ")
        .append(entityMain.getTablePointer().renderSQL());

    // WHERE [filter]
    if (listQueryRequest.getFilter() != null) {
      sql.append(" WHERE ")
          .append(bqTranslator.translator(listQueryRequest.getFilter()).buildSql(sqlParams, null));
    }

    // ORDER BY [order by fields]
    if (!listQueryRequest.getOrderBys().isEmpty()) {
      // All the order by fields come from the index entity main table.
      List<String> orderByFields = new ArrayList<>();
      listQueryRequest.getOrderBys().stream()
          .forEach(
              orderBy ->
                  bqTranslator.translator(orderBy.getEntityField()).buildSqlFieldsForOrderBy()
                      .stream()
                      .forEach(
                          sqlField ->
                              orderByFields.add(
                                  bqTranslator.orderBySql(
                                          sqlField,
                                          null,
                                          listQueryRequest
                                              .getSelectFields()
                                              .contains(orderBy.getEntityField()))
                                      + ' '
                                      + bqTranslator.orderByDirectionSql(orderBy.getDirection()))));
      sql.append(" ORDER BY ").append(orderByFields.stream().collect(Collectors.joining(", ")));
    }

    // LIMIT [limit]
    if (listQueryRequest.getLimit() != null) {
      sql.append(" LIMIT ").append(listQueryRequest.getLimit());
    }

    // Execute the SQL query.
    SqlQueryRequest sqlQueryRequest =
        new SqlQueryRequest(
            sql.toString(),
            sqlParams,
            listQueryRequest.getPageMarker(),
            listQueryRequest.getPageSize(),
            listQueryRequest.isDryRun());
    SqlQueryResult sqlQueryResult = bigQueryExecutor.run(sqlQueryRequest);

    // Process the rows returned.
    List<ListInstance> listInstances = new ArrayList<>();
    sqlQueryResult
        .getRowResults()
        .iterator()
        .forEachRemaining(
            sqlRowResult -> {
              Map<ValueDisplayField, ValueDisplay> fieldValues = new HashMap<>();
              listQueryRequest.getSelectFields().stream()
                  .forEach(
                      valueDisplayField ->
                          fieldValues.put(
                              valueDisplayField,
                              bqTranslator
                                  .translator(valueDisplayField)
                                  .parseValueDisplayFromResult(sqlRowResult)));
              listInstances.add(new ListInstance(fieldValues));
            });

    return new ListQueryResult(sql.toString(), listInstances, sqlQueryResult.getNextPageMarker());
  }

  @Override
  public CountQueryResult run(CountQueryRequest countQueryRequest) {
    // Build the SQL query.
    StringBuilder sql = new StringBuilder();
    SqlParams sqlParams = new SqlParams();
    BQTranslator bqTranslator = new BQTranslator();

    // The select fields are the COUNT(id) field + the GROUP BY fields (values only).
    List<ValueDisplayField> selectValueDisplayFields = new ArrayList<>();
    EntityIdCountField entityIdCountField =
        new EntityIdCountField(countQueryRequest.getUnderlay(), countQueryRequest.getEntity());
    selectValueDisplayFields.add(entityIdCountField);
    selectValueDisplayFields.addAll(countQueryRequest.getGroupByFields());

    // All the select fields come from the index entity main table.
    ITEntityMain entityMain =
        countQueryRequest
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(countQueryRequest.getEntity().getName());
    List<String> selectFields = new ArrayList<>();
    selectValueDisplayFields.stream()
        .forEach(
            valueDisplayField ->
                bqTranslator.translator(valueDisplayField).buildSqlFieldsForCountSelect().stream()
                    .forEach(sqlField -> selectFields.add(bqTranslator.selectSql(sqlField, null))));

    // SELECT [id count field],[group by fields] FROM [entity main]
    sql.append("SELECT ")
        .append(selectFields.stream().collect(Collectors.joining(", ")))
        .append(" FROM ")
        .append(entityMain.getTablePointer().renderSQL());

    // WHERE [filter]
    if (countQueryRequest.getFilter() != null) {
      sql.append(" WHERE ")
          .append(bqTranslator.translator(countQueryRequest.getFilter()).buildSql(sqlParams, null));
    }

    // GROUP BY [group by fields]
    if (!countQueryRequest.getGroupByFields().isEmpty()) {
      List<String> groupByFields = new ArrayList<>();
      countQueryRequest.getGroupByFields().stream()
          .forEach(
              groupBy ->
                  bqTranslator.translator(groupBy).buildSqlFieldsForGroupBy().stream()
                      .forEach(
                          sqlField ->
                              groupByFields.add(bqTranslator.groupBySql(sqlField, null, true))));
      sql.append(" GROUP BY ").append(groupByFields.stream().collect(Collectors.joining(", ")));
    }

    // Execute the SQL query.
    SqlQueryRequest sqlQueryRequest =
        new SqlQueryRequest(
            sql.toString(),
            sqlParams,
            countQueryRequest.getPageMarker(),
            countQueryRequest.getPageSize(),
            countQueryRequest.isDryRun());
    SqlQueryResult sqlQueryResult = bigQueryExecutor.run(sqlQueryRequest);

    // Process the rows returned.
    List<CountInstance> countInstances = new ArrayList<>();
    sqlQueryResult
        .getRowResults()
        .iterator()
        .forEachRemaining(
            sqlRowResult -> {
              Map<ValueDisplayField, ValueDisplay> fieldValues = new HashMap<>();
              countQueryRequest.getGroupByFields().stream()
                  .forEach(
                      valueDisplayField -> {
                        ValueDisplay valueDisplay;
                        if (valueDisplayField instanceof AttributeField) {
                          // Use the entity-level display hints to fill in any attribute display
                          // fields.
                          valueDisplay =
                              ((BQAttributeFieldTranslator)
                                      bqTranslator.translator((AttributeField) valueDisplayField))
                                  .parseValueDisplayFromCountResult(
                                      sqlRowResult, countQueryRequest.getEntityLevelHints());
                        } else {
                          // Just parse the field result normally.
                          valueDisplay =
                              bqTranslator
                                  .translator(valueDisplayField)
                                  .parseValueDisplayFromResult(sqlRowResult);
                        }
                        fieldValues.put(valueDisplayField, valueDisplay);
                      });
              long count =
                  bqTranslator
                      .translator(entityIdCountField)
                      .parseValueDisplayFromResult(sqlRowResult)
                      .getValue()
                      .getInt64Val();
              countInstances.add(new CountInstance(count, fieldValues));
            });

    return new CountQueryResult(sql.toString(), countInstances, sqlQueryResult.getNextPageMarker());
  }

  @Override
  public HintQueryResult run(HintQueryRequest hintQueryRequest) {
    // Build the SQL query.
    StringBuilder sql = new StringBuilder();
    SqlParams sqlParams = new SqlParams();

    if (hintQueryRequest.isEntityLevel()) {
      ITEntityLevelDisplayHints eldhTable =
          hintQueryRequest
              .getUnderlay()
              .getIndexSchema()
              .getEntityLevelDisplayHints(hintQueryRequest.getHintedEntity().getName());

      // SELECT * FROM [entity-level hint]
      sql.append("SELECT * FROM ").append(eldhTable.getTablePointer().renderSQL());
    } else {
      ITInstanceLevelDisplayHints ildhTable =
          hintQueryRequest
              .getUnderlay()
              .getIndexSchema()
              .getInstanceLevelDisplayHints(
                  hintQueryRequest.getEntityGroup().getName(),
                  hintQueryRequest.getHintedEntity().getName(),
                  hintQueryRequest.getRelatedEntity().getName());

      // SELECT * FROM [instance-level hint]
      sql.append("SELECT * FROM ").append(ildhTable.getTablePointer().renderSQL());

      // WHERE [filter on related entity id]
      String relatedEntityIdParam =
          sqlParams.addParam("relatedEntityId", hintQueryRequest.getRelatedEntityId());
      sql.append(" WHERE ")
          .append(ITInstanceLevelDisplayHints.Column.ENTITY_ID.getSchema().getColumnName())
          .append(" = @")
          .append(relatedEntityIdParam);
    }

    // Execute the SQL query.
    SqlQueryRequest sqlQueryRequest =
        new SqlQueryRequest(sql.toString(), sqlParams, null, null, hintQueryRequest.isDryRun());
    SqlQueryResult sqlQueryResult = bigQueryExecutor.run(sqlQueryRequest);

    // TODO: Process the rows returned.
    List<HintInstance> hintInstances = new ArrayList<>();
    sqlQueryResult.getRowResults().iterator().forEachRemaining(sqlRowResult -> {});

    return new HintQueryResult(sql.toString(), hintInstances);
  }
}
