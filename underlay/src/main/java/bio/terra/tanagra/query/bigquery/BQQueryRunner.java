package bio.terra.tanagra.query.bigquery;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.field.EntityIdCountField;
import bio.terra.tanagra.api.field.ValueDisplayField;
import bio.terra.tanagra.api.query.PageMarker;
import bio.terra.tanagra.api.query.count.CountInstance;
import bio.terra.tanagra.api.query.count.CountQueryRequest;
import bio.terra.tanagra.api.query.count.CountQueryResult;
import bio.terra.tanagra.api.query.export.ExportQueryRequest;
import bio.terra.tanagra.api.query.export.ExportQueryResult;
import bio.terra.tanagra.api.query.hint.HintInstance;
import bio.terra.tanagra.api.query.hint.HintQueryRequest;
import bio.terra.tanagra.api.query.hint.HintQueryResult;
import bio.terra.tanagra.api.query.list.ListInstance;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.api.query.list.ListQueryResult;
import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.ValueDisplay;
import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.query.QueryRunner;
import bio.terra.tanagra.query.bigquery.translator.BQApiTranslator;
import bio.terra.tanagra.query.bigquery.translator.field.BQAttributeFieldTranslator;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.SqlQueryField;
import bio.terra.tanagra.query.sql.SqlQueryRequest;
import bio.terra.tanagra.query.sql.SqlQueryResult;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.indextable.ITEntityLevelDisplayHints;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.indextable.ITInstanceLevelDisplayHints;
import com.google.common.annotations.VisibleForTesting;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;

public class BQQueryRunner implements QueryRunner {
  private final BQExecutor bigQueryExecutor;

  public BQQueryRunner(String queryProjectId, String datasetLocation) {
    this.bigQueryExecutor = new BQExecutor(queryProjectId, datasetLocation);
  }

  @Override
  public ListQueryResult run(ListQueryRequest listQueryRequest) {
    // Build the SQL query.
    Instant queryInstant =
        listQueryRequest.getPageMarker() == null
            ? Instant.now()
            : listQueryRequest.getPageMarker().getInstant();
    SqlQueryRequest sqlQueryRequest =
        listQueryRequest.isAgainstSourceData()
            ? buildListQuerySqlAgainstSourceData(listQueryRequest)
            : buildListQuerySqlAgainstIndexData(listQueryRequest, queryInstant);

    // Execute the SQL query.
    SqlQueryResult sqlQueryResult = bigQueryExecutor.run(sqlQueryRequest);

    // Process the rows returned.
    BQApiTranslator bqTranslator = new BQApiTranslator();
    List<ListInstance> listInstances = new ArrayList<>();
    sqlQueryResult
        .getRowResults()
        .iterator()
        .forEachRemaining(
            sqlRowResult -> {
              Map<ValueDisplayField, ValueDisplay> fieldValues = new HashMap<>();
              listQueryRequest
                  .getSelectFields()
                  .forEach(
                      valueDisplayField ->
                          fieldValues.put(
                              valueDisplayField,
                              bqTranslator
                                  .translator(valueDisplayField)
                                  .parseValueDisplayFromResult(sqlRowResult)));
              listInstances.add(new ListInstance(fieldValues));
            });

    PageMarker nextPageMarker =
        sqlQueryResult.getNextPageMarker() == null
            ? null
            : sqlQueryResult.getNextPageMarker().instant(queryInstant);
    return new ListQueryResult(
        sqlQueryRequest.getSql(),
        sqlQueryResult.getSqlNoParams(),
        listInstances,
        nextPageMarker,
        sqlQueryResult.getTotalNumRows());
  }

  public SqlQueryRequest buildListQuerySqlAgainstIndexData(ListQueryRequest listQueryRequest) {
    return buildListQuerySqlAgainstIndexData(listQueryRequest, Instant.now());
  }

  @VisibleForTesting
  public SqlQueryRequest buildListQuerySqlAgainstIndexData(
      ListQueryRequest listQueryRequest, Instant queryInstant) {
    // Build the SQL query.
    StringBuilder sql = new StringBuilder();
    SqlParams sqlParams = new SqlParams();
    BQApiTranslator bqTranslator = new BQApiTranslator();

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
    listQueryRequest
        .getSelectFields()
        .forEach(
            valueDisplayField ->
                bqTranslator
                    .translator(valueDisplayField)
                    .buildSqlFieldsForListSelect()
                    .forEach(sqlField -> selectFields.add(sqlField.renderForSelect())));

    // SELECT [select fields] FROM [entity main]
    sql.append("SELECT ")
        .append(String.join(", ", selectFields))
        .append(" FROM ")
        .append(entityMain.getTablePointer().render());

    // WHERE [filter]
    if (listQueryRequest.getFilter() != null) {
      sql.append(" WHERE ")
          .append(bqTranslator.translator(listQueryRequest.getFilter()).buildSql(sqlParams, null));
    }

    // ORDER BY [order by fields]
    if (!listQueryRequest.getOrderBys().isEmpty()) {
      // All the order by fields come from the index entity main table.
      List<String> orderByFields = new ArrayList<>();
      listQueryRequest
          .getOrderBys()
          .forEach(
              orderBy -> {
                if (orderBy.isRandom()) {
                  orderByFields.add(bqTranslator.orderByRandSql());
                } else {
                  bqTranslator
                      .translator(orderBy.getEntityField())
                      .buildSqlFieldsForOrderBy()
                      .forEach(
                          sqlField ->
                              orderByFields.add(
                                  sqlField.renderForOrderBy(
                                          null,
                                          listQueryRequest
                                              .getSelectFields()
                                              .contains(orderBy.getEntityField()))
                                      + ' '
                                      + bqTranslator.orderByDirectionSql(orderBy.getDirection())));
                }
              });
      sql.append(" ORDER BY ").append(String.join(", ", orderByFields));
    }

    // LIMIT [limit]
    if (listQueryRequest.getLimit() != null) {
      sql.append(" LIMIT ").append(listQueryRequest.getLimit());
    }

    // Swap out any un-cacheable functions with SQL parameters.
    String sqlWithOnlyCacheableFunctions =
        BQExecutor.replaceFunctionsThatPreventCaching(sql.toString(), sqlParams, queryInstant);

    return new SqlQueryRequest(
        sqlWithOnlyCacheableFunctions,
        sqlParams,
        listQueryRequest.getPageMarker(),
        listQueryRequest.getPageSize(),
        listQueryRequest.isDryRun());
  }

  @VisibleForTesting
  public SqlQueryRequest buildListQuerySqlAgainstSourceData(ListQueryRequest listQueryRequest) {
    // Make sure the entity is enabled for source queries, and all the selected fields are
    // attributes.
    if (!listQueryRequest.getEntity().supportsSourceQueries()) {
      throw new InvalidConfigException(
          "Entity " + listQueryRequest.getEntity().getName() + " does not support source queries");
    }
    listQueryRequest
        .getSelectFields()
        .forEach(
            selectField -> {
              if (!(selectField instanceof AttributeField)) {
                throw new InvalidQueryException(
                    "Only attribute fields can be selected in a query against the source data");
              }
            });

    // Build the inner SQL query against the index data first.
    // Select only the id attribute, which we need to JOIN to the source table.
    // SELECT id FROM [index table] WHERE [filter]
    StringBuilder sql = new StringBuilder(50);
    BQApiTranslator bqTranslator = new BQApiTranslator();
    AttributeField indexIdAttributeField =
        new AttributeField(
            listQueryRequest.getUnderlay(),
            listQueryRequest.getEntity(),
            listQueryRequest.getEntity().getIdAttribute(),
            true);
    ListQueryRequest indexDataQueryRequest =
        ListQueryRequest.dryRunAgainstIndexData(
            listQueryRequest.getUnderlay(),
            listQueryRequest.getEntity(),
            List.of(indexIdAttributeField),
            listQueryRequest.getFilter(),
            null,
            null);
    Instant queryInstant =
        listQueryRequest.getPageMarker() == null
            ? Instant.now()
            : listQueryRequest.getPageMarker().getInstant();
    SqlQueryRequest indexDataSqlRequest =
        buildListQuerySqlAgainstIndexData(indexDataQueryRequest, queryInstant);
    SqlParams sqlParams = indexDataSqlRequest.getSqlParams();

    // Now build the outer SQL query against the source data.
    final String sourceTableAlias = "st";
    List<String> selectFields = new ArrayList<>();
    List<String> displayTableJoins = new ArrayList<>();
    listQueryRequest
        .getSelectFields()
        .forEach(
            valueDisplayField -> {
              AttributeField attrFieldAgainstSourceData =
                  AttributeField.againstSourceDataset((AttributeField) valueDisplayField);
              Attribute.SourceQuery attrSourcePointer =
                  attrFieldAgainstSourceData.getAttribute().getSourceQuery();

              List<SqlQueryField> valueAndDisplayFields =
                  bqTranslator.translator(attrFieldAgainstSourceData).buildSqlFieldsForListSelect();
              SqlQueryField valueSqlField = valueAndDisplayFields.get(0);
              selectFields.add(valueSqlField.renderForSelect(sourceTableAlias));

              if (valueAndDisplayFields.size() > 1) {
                SqlQueryField displaySqlField = valueAndDisplayFields.get(1);
                if (attrSourcePointer.hasDisplayFieldTableJoin()) {
                  SqlQueryField displayTableJoinField =
                      SqlQueryField.of(
                          SqlField.of(attrSourcePointer.getDisplayFieldTableJoinFieldName()));
                  String joinTableAlias = "dt" + displayTableJoins.size();

                  StringBuilder joinSql = new StringBuilder();
                  joinSql
                      .append(" LEFT JOIN ")
                      .append(fromFullTablePath(attrSourcePointer.getDisplayFieldTable()).render())
                      .append(" AS ")
                      .append(joinTableAlias)
                      .append(" ON ")
                      .append(displayTableJoinField.renderForSelect(joinTableAlias))
                      .append(" = ")
                      .append(valueSqlField.renderForSelect(sourceTableAlias));
                  displayTableJoins.add(joinSql.toString());
                  selectFields.add(displaySqlField.renderForSelect(joinTableAlias));
                } else {
                  selectFields.add(displaySqlField.renderForSelect(sourceTableAlias));
                }
              }
            });
    if (selectFields.isEmpty()) {
      throw new InvalidQueryException("List query must include at least one select field");
    }
    AttributeField sourceIdAttrField = AttributeField.againstSourceDataset(indexIdAttributeField);
    SqlQueryField sourceIdAttrSqlField =
        bqTranslator.translator(sourceIdAttrField).buildSqlFieldsForListSelect().get(0);

    // SELECT [select fields] FROM [source table]
    // JOIN [display table] ON [display join field]
    // WHERE [source id field] IN [inner query against index data]
    sql.append("SELECT ")
        .append(String.join(", ", selectFields))
        .append(" FROM ")
        .append(fromFullTablePath(listQueryRequest.getEntity().getSourceQueryTableName()).render())
        .append(" AS ")
        .append(sourceTableAlias)
        .append(String.join("", displayTableJoins))
        .append(" WHERE ")
        .append(sourceIdAttrSqlField.renderForSelect(sourceTableAlias))
        .append(" IN (")
        .append(indexDataSqlRequest.getSql())
        .append(')');
    return new SqlQueryRequest(
        sql.toString(),
        sqlParams,
        listQueryRequest.getPageMarker(),
        listQueryRequest.getPageSize(),
        listQueryRequest.isDryRun());
  }

  @VisibleForTesting
  public static BQTable fromFullTablePath(String fullTablePath) {
    String[] bqDatasetParsed = fullTablePath.split("\\.");
    return new BQTable(bqDatasetParsed[0], bqDatasetParsed[1], bqDatasetParsed[2]);
  }

  @Override
  public CountQueryResult run(CountQueryRequest countQueryRequest) {
    // Build the SQL query.
    StringBuilder sql = new StringBuilder();
    SqlParams sqlParams = new SqlParams();
    BQApiTranslator bqTranslator = new BQApiTranslator();

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
    selectValueDisplayFields.forEach(
        valueDisplayField ->
            bqTranslator
                .translator(valueDisplayField)
                .buildSqlFieldsForCountSelect()
                .forEach(sqlField -> selectFields.add(sqlField.renderForSelect())));

    // SELECT [id count field],[group by fields] FROM [entity main]
    sql.append("SELECT ")
        .append(String.join(", ", selectFields))
        .append(" FROM ")
        .append(entityMain.getTablePointer().render());

    // WHERE [filter]
    if (countQueryRequest.getFilter() != null) {
      sql.append(" WHERE ")
          .append(bqTranslator.translator(countQueryRequest.getFilter()).buildSql(sqlParams, null));
    }

    // GROUP BY [group by fields]
    if (!countQueryRequest.getGroupByFields().isEmpty()) {
      List<String> groupByFields = new ArrayList<>();
      countQueryRequest
          .getGroupByFields()
          .forEach(
              groupBy ->
                  bqTranslator
                      .translator(groupBy)
                      .buildSqlFieldsForGroupBy()
                      .forEach(
                          sqlField -> groupByFields.add(sqlField.renderForGroupBy(null, true))));
      sql.append(" GROUP BY ").append(String.join(", ", groupByFields));
    }

    // Swap out any un-cacheable functions with SQL parameters.
    Instant queryInstant =
        countQueryRequest.getPageMarker() == null
            ? Instant.now()
            : countQueryRequest.getPageMarker().getInstant();
    String sqlWithOnlyCacheableFunctions =
        BQExecutor.replaceFunctionsThatPreventCaching(sql.toString(), sqlParams, queryInstant);

    // Execute the SQL query.
    SqlQueryRequest sqlQueryRequest =
        new SqlQueryRequest(
            sqlWithOnlyCacheableFunctions,
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
              countQueryRequest
                  .getGroupByFields()
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

    return new CountQueryResult(
        sql.toString(),
        countInstances,
        sqlQueryResult.getNextPageMarker() == null
            ? null
            : sqlQueryResult.getNextPageMarker().instant(queryInstant),
        sqlQueryResult.getTotalNumRows());
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
      sql.append("SELECT * FROM ").append(eldhTable.getTablePointer().render());
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
      sql.append("SELECT * FROM ").append(ildhTable.getTablePointer().render());

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

    // Process the rows returned.
    List<HintInstance> hintInstances = new ArrayList<>();
    Map<Attribute, Map<ValueDisplay, Long>> enumValues = new HashMap<>();
    sqlQueryResult
        .getRowResults()
        .iterator()
        .forEachRemaining(
            sqlRowResult -> {
              String attributeColName;
              String minColName;
              String maxColName;
              String enumValColName;
              String enumDisplayColName;
              String enumCountColName;
              if (hintQueryRequest.isEntityLevel()) {
                attributeColName =
                    ITEntityLevelDisplayHints.Column.ATTRIBUTE_NAME.getSchema().getColumnName();
                minColName = ITEntityLevelDisplayHints.Column.MIN.getSchema().getColumnName();
                maxColName = ITEntityLevelDisplayHints.Column.MAX.getSchema().getColumnName();
                enumValColName =
                    ITEntityLevelDisplayHints.Column.ENUM_VALUE.getSchema().getColumnName();
                enumDisplayColName =
                    ITEntityLevelDisplayHints.Column.ENUM_DISPLAY.getSchema().getColumnName();
                enumCountColName =
                    ITEntityLevelDisplayHints.Column.ENUM_COUNT.getSchema().getColumnName();
              } else {
                attributeColName =
                    ITInstanceLevelDisplayHints.Column.ATTRIBUTE_NAME.getSchema().getColumnName();
                minColName = ITInstanceLevelDisplayHints.Column.MIN.getSchema().getColumnName();
                maxColName = ITInstanceLevelDisplayHints.Column.MAX.getSchema().getColumnName();
                enumValColName =
                    ITInstanceLevelDisplayHints.Column.ENUM_VALUE.getSchema().getColumnName();
                enumDisplayColName =
                    ITInstanceLevelDisplayHints.Column.ENUM_DISPLAY.getSchema().getColumnName();
                enumCountColName =
                    ITInstanceLevelDisplayHints.Column.ENUM_COUNT.getSchema().getColumnName();
              }

              Attribute attribute =
                  hintQueryRequest
                      .getHintedEntity()
                      .getAttribute(
                          sqlRowResult.get(attributeColName, DataType.STRING).getStringVal());
              if (attribute.isValueDisplay()
                  || attribute.getRuntimeDataType().equals(DataType.STRING)) {
                // This is one (value,count) pair of an enum values hint.
                Literal enumVal = sqlRowResult.get(enumValColName, DataType.INT64);
                String enumDisplay =
                    sqlRowResult.get(enumDisplayColName, DataType.STRING).getStringVal();
                Long enumCount = sqlRowResult.get(enumCountColName, DataType.INT64).getInt64Val();
                Map<ValueDisplay, Long> enumValuesForAttr =
                    enumValues.containsKey(attribute) ? enumValues.get(attribute) : new HashMap<>();
                enumValuesForAttr.put(new ValueDisplay(enumVal, enumDisplay), enumCount);
                enumValues.put(attribute, enumValuesForAttr);
              } else {
                // This is a range hint.
                Double min = sqlRowResult.get(minColName, DataType.DOUBLE).getDoubleVal();
                Double max = sqlRowResult.get(maxColName, DataType.DOUBLE).getDoubleVal();
                if (min != null && max != null) {
                  hintInstances.add(new HintInstance(attribute, min, max));
                }
              }
            });
    // Assemble the value/count pairs into a single enum values hint for each attribute.
    enumValues.forEach(
        (attribute, enumValuesForAttr) ->
            hintInstances.add(new HintInstance(attribute, enumValuesForAttr)));

    return new HintQueryResult(sql.toString(), hintInstances);
  }

  @Override
  public ExportQueryResult run(ExportQueryRequest exportQueryRequest) {
    // Build the SQL query.
    SqlQueryRequest sqlQueryRequest =
        buildListQuerySqlAgainstIndexData(exportQueryRequest.getListQueryRequest());

    // Execute the SQL query and export the results to GCS.
    Pair<String, String> exportFileUrlAndFileName =
        bigQueryExecutor.export(
            sqlQueryRequest,
            exportQueryRequest.getFileNamePrefix(),
            exportQueryRequest.getGcsProjectId(),
            exportQueryRequest.getAvailableBqDatasetIds(),
            exportQueryRequest.getAvailableGcsBucketNames(),
            exportQueryRequest.isGenerateSignedUrl());

    return new ExportQueryResult(
        exportFileUrlAndFileName.getRight(), exportFileUrlAndFileName.getLeft());
  }
}
