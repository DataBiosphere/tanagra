package bio.terra.tanagra.query.bigquery;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.field.CountDistinctField;
import bio.terra.tanagra.api.field.ValueDisplayField;
import bio.terra.tanagra.api.filter.EntityFilter;
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
import bio.terra.tanagra.api.query.list.OrderBy;
import bio.terra.tanagra.api.shared.DataType;
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
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.indextable.ITEntityLevelDisplayHints;
import bio.terra.tanagra.underlay.indextable.ITEntitySearchByAttributes;
import bio.terra.tanagra.underlay.indextable.ITInstanceLevelDisplayHints;
import bio.terra.tanagra.underlay.indextable.IndexTable;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;

public class BQQueryRunner implements QueryRunner {
  private final BQExecutor bigQueryExecutor;

  public BQQueryRunner(BQExecutorInfrastructure queryInfrastructure) {
    this.bigQueryExecutor = new BQExecutor(queryInfrastructure);
  }

  @Override
  public SqlQueryResult run(SqlQueryRequest sqlQueryRequest) {
    return bigQueryExecutor.run(sqlQueryRequest);
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
        .rowResults()
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
        sqlQueryResult.nextPageMarker() == null
            ? null
            : sqlQueryResult.nextPageMarker().instant(queryInstant);
    return new ListQueryResult(
        sqlQueryRequest.sql(),
        sqlQueryResult.sqlNoParams(),
        listInstances,
        nextPageMarker,
        sqlQueryResult.totalNumRows());
  }

  @Override
  public CountQueryResult run(CountQueryRequest countQueryRequest) {
    // The select fields are the COUNT(id) field + the GROUP BY fields (values only).
    List<ValueDisplayField> selectValueDisplayFields = new ArrayList<>();
    CountDistinctField countDistinctField =
        new CountDistinctField(
            countQueryRequest.getUnderlay(),
            countQueryRequest.getEntity(),
            countQueryRequest.getCountDistinctAttribute());
    selectValueDisplayFields.add(countDistinctField);
    selectValueDisplayFields.addAll(countQueryRequest.getGroupByFields());

    // Always order by the count field.
    OrderBy orderBy = new OrderBy(countDistinctField, countQueryRequest.getOrderByDirection());

    Instant queryInstant =
        countQueryRequest.getPageMarker() == null
            ? Instant.now()
            : countQueryRequest.getPageMarker().getInstant();

    SqlQueryRequest sqlQueryRequest =
        buildQuerySqlAgainstIndexData(
            countQueryRequest.getUnderlay(),
            selectValueDisplayFields,
            countQueryRequest.getFilter() == null
                ? Map.of()
                : Map.of(countQueryRequest.getEntity(), countQueryRequest.getFilter()),
            countQueryRequest.getGroupByFields(),
            List.of(orderBy),
            countQueryRequest.getLimit(),
            countQueryRequest.getPageMarker(),
            countQueryRequest.getPageSize(),
            queryInstant,
            countQueryRequest.getEntityLevelHints() == null
                ? Map.of()
                : Map.of(countQueryRequest.getEntity(), countQueryRequest.getEntityLevelHints()),
            countQueryRequest.isDryRun());

    // Execute the SQL query.
    SqlQueryResult sqlQueryResult = bigQueryExecutor.run(sqlQueryRequest);

    // Process the rows returned.
    BQApiTranslator bqTranslator = new BQApiTranslator();
    List<CountInstance> countInstances = new ArrayList<>();
    sqlQueryResult
        .rowResults()
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
              Long count =
                  bqTranslator
                      .translator(countDistinctField)
                      .parseValueDisplayFromResult(sqlRowResult)
                      .getValue()
                      .getInt64Val();
              countInstances.add(new CountInstance(count != null ? count : 0L, fieldValues));
            });

    return new CountQueryResult(
        sqlQueryRequest.sql(),
        countInstances,
        sqlQueryResult.nextPageMarker() == null
            ? null
            : sqlQueryResult.nextPageMarker().instant(queryInstant),
        sqlQueryResult.totalNumRows());
  }

  @Override
  public HintQueryResult run(HintQueryRequest hintQueryRequest) {
    // Build the SQL query.
    StringBuilder sql = new StringBuilder();
    SqlParams sqlParams = new SqlParams();
    List<String> orderBys;

    if (hintQueryRequest.isEntityLevel()) {
      ITEntityLevelDisplayHints eldhTable =
          hintQueryRequest
              .underlay()
              .getIndexSchema()
              .getEntityLevelDisplayHints(hintQueryRequest.hintedEntity().getName());

      // SELECT * FROM [entity-level hint]
      sql.append("SELECT * FROM ").append(eldhTable.getTablePointer().render());
      orderBys = eldhTable.getOrderBys();

    } else {
      ITInstanceLevelDisplayHints ildhTable =
          hintQueryRequest
              .underlay()
              .getIndexSchema()
              .getInstanceLevelDisplayHints(
                  hintQueryRequest.entityGroup().getName(),
                  hintQueryRequest.hintedEntity().getName(),
                  hintQueryRequest.relatedEntity().getName());

      // SELECT * FROM [instance-level hint]
      sql.append("SELECT * FROM ").append(ildhTable.getTablePointer().render());

      // WHERE [filter on related entity id]
      String relatedEntityIdParam =
          sqlParams.addParam("relatedEntityId", hintQueryRequest.relatedEntityId());
      sql.append(" WHERE ")
          .append(ITInstanceLevelDisplayHints.Column.ENTITY_ID.getSchema().getColumnName())
          .append(" = @")
          .append(relatedEntityIdParam);
      orderBys = ildhTable.getOrderBys();
    }

    // ORDER BY [order by fields]
    sql.append(" ORDER BY ").append(String.join(", ", orderBys));

    // Execute the SQL query.
    SqlQueryRequest sqlQueryRequest =
        new SqlQueryRequest(sql.toString(), sqlParams, null, null, hintQueryRequest.isDryRun());
    SqlQueryResult sqlQueryResult = bigQueryExecutor.run(sqlQueryRequest);

    // Process the rows returned.
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
      enumValColName = ITEntityLevelDisplayHints.Column.ENUM_VALUE.getSchema().getColumnName();
      enumDisplayColName =
          ITEntityLevelDisplayHints.Column.ENUM_DISPLAY.getSchema().getColumnName();
      enumCountColName = ITEntityLevelDisplayHints.Column.ENUM_COUNT.getSchema().getColumnName();
    } else {
      attributeColName =
          ITInstanceLevelDisplayHints.Column.ATTRIBUTE_NAME.getSchema().getColumnName();
      minColName = ITInstanceLevelDisplayHints.Column.MIN.getSchema().getColumnName();
      maxColName = ITInstanceLevelDisplayHints.Column.MAX.getSchema().getColumnName();
      enumValColName = ITInstanceLevelDisplayHints.Column.ENUM_VALUE.getSchema().getColumnName();
      enumDisplayColName =
          ITInstanceLevelDisplayHints.Column.ENUM_DISPLAY.getSchema().getColumnName();
      enumCountColName = ITInstanceLevelDisplayHints.Column.ENUM_COUNT.getSchema().getColumnName();
    }

    List<HintInstance> allHintInstances = new ArrayList<>();
    Map<Attribute, HintInstance> attrHintInstanceMap = new HashMap<>();
    sqlQueryResult
        .rowResults()
        .iterator()
        .forEachRemaining(
            sqlRowResult -> {
              Attribute attribute =
                  hintQueryRequest
                      .hintedEntity()
                      .getAttribute(
                          sqlRowResult.get(attributeColName, DataType.STRING).getStringVal());

              if (attribute.isValueDisplay()) {
                // This is one (value,count) pair of an enum values hint.
                HintInstance hintInstance = attrHintInstanceMap.get(attribute);
                if (hintInstance == null) {
                  hintInstance = new HintInstance(attribute, new HashMap<>());
                  allHintInstances.add(hintInstance);
                  attrHintInstanceMap.put(attribute, hintInstance);
                }
                Entry<ValueDisplay, Long> entry =
                    HintInstance.newValueDisplayInstance(
                        sqlRowResult,
                        attribute,
                        enumValColName,
                        enumDisplayColName,
                        enumCountColName);
                hintInstance.addEnumValueCount(entry.getKey(), entry.getValue());

              } else if (attribute.getRuntimeDataType().equals(DataType.STRING)) {
                // repeated attribute
                HintInstance hintInstance = attrHintInstanceMap.get(attribute);
                if (hintInstance == null) {
                  hintInstance = new HintInstance(attribute, new HashMap<>());
                  allHintInstances.add(hintInstance);
                  attrHintInstanceMap.put(attribute, hintInstance);
                }
                Entry<ValueDisplay, Long> entry =
                    HintInstance.newRepeatedStringEntry(
                        sqlRowResult, attribute, enumDisplayColName, enumCountColName);
                hintInstance.addEnumValueCount(entry.getKey(), entry.getValue());

              } else {
                // This is a range hint.
                HintInstance.newRangeInstance(sqlRowResult, attribute, minColName, maxColName)
                    .ifPresent(allHintInstances::add);
              }
            });

    return new HintQueryResult(sql.toString(), allHintInstances);
  }

  @Override
  public ExportQueryResult run(ExportQueryRequest exportQueryRequest) {
    Pair<String, String> exportFileUrlAndFileName;
    if (exportQueryRequest.isForRawData()) {
      // Export the raw data to GCS.
      exportFileUrlAndFileName =
          bigQueryExecutor.exportRawData(
              exportQueryRequest.getFileContents(),
              exportQueryRequest.getFileDisplayName(),
              exportQueryRequest.isGenerateSignedUrl());
    } else {
      // Build the SQL query.
      SqlQueryRequest sqlQueryRequest =
          buildListQuerySqlAgainstIndexData(exportQueryRequest.getListQueryRequest());

      // Execute the SQL query and export the results to GCS.
      exportFileUrlAndFileName =
          bigQueryExecutor.exportQuery(
              sqlQueryRequest,
              exportQueryRequest.getFileNamePrefix(),
              exportQueryRequest.isGenerateSignedUrl());
    }
    return new ExportQueryResult(
        exportFileUrlAndFileName.getRight(), exportFileUrlAndFileName.getLeft());
  }

  public SqlQueryRequest buildListQuerySqlAgainstIndexData(ListQueryRequest listQueryRequest) {
    return buildListQuerySqlAgainstIndexData(listQueryRequest, Instant.now());
  }

  private SqlQueryRequest buildListQuerySqlAgainstIndexData(
      ListQueryRequest listQueryRequest, Instant queryInstant) {
    return buildQuerySqlAgainstIndexData(
        listQueryRequest.getUnderlay(),
        listQueryRequest.getSelectFields(),
        listQueryRequest.getFilter() == null
            ? Map.of()
            : Map.of(listQueryRequest.getEntity(), listQueryRequest.getFilter()),
        List.of(),
        listQueryRequest.getOrderBys(),
        listQueryRequest.getLimit(),
        listQueryRequest.getPageMarker(),
        listQueryRequest.getPageSize(),
        queryInstant,
        Map.of(),
        listQueryRequest.isDryRun());
  }

  @SuppressWarnings("checkstyle:ParameterNumber")
  private SqlQueryRequest buildQuerySqlAgainstIndexData(
      Underlay underlay,
      List<ValueDisplayField> selectFields,
      Map<Entity, EntityFilter> filters,
      List<ValueDisplayField> groupByFields,
      List<OrderBy> orderBys,
      @Nullable Integer limit,
      @Nullable PageMarker pageMarker,
      @Nullable Integer pageSize,
      Instant queryInstant,
      Map<Entity, HintQueryResult> entityLevelHints,
      boolean isDryRun) {
    if (selectFields.isEmpty()) {
      throw new InvalidQueryException("Query must include at least one select field");
    }

    // Build the SQL query.
    StringBuilder sql = new StringBuilder();
    SqlParams sqlParams = new SqlParams();
    BQApiTranslator bqTranslator = new BQApiTranslator();

    // Build the list of entities we need for the select fields and filters.
    Set<Entity> entities =
        selectFields.stream().map(ValueDisplayField::getEntity).collect(Collectors.toSet());
    if (entities.size() > 1) {
      throw new NotImplementedException("Queries with more than one entity are not yet supported");
    }

    List<String> selectFieldNames = new ArrayList<>();
    List<String> selectFieldSqls = new ArrayList<>();
    List<String> joinTableSqls = new ArrayList<>();
    selectFields.forEach(
        valueDisplayField -> {
          List<SqlQueryField> sqlQueryFields;
          if (groupByFields.contains(valueDisplayField)
              && valueDisplayField instanceof AttributeField) {
            BQAttributeFieldTranslator attributeFieldTranslator =
                (BQAttributeFieldTranslator) bqTranslator.translator(valueDisplayField);
            sqlQueryFields =
                attributeFieldTranslator.buildSqlFieldsForCountSelectAndGroupBy(
                    entityLevelHints.get(valueDisplayField.getEntity()));
            String joinTableSql = attributeFieldTranslator.buildSqlJoinTableForCountQuery();
            if (joinTableSql != null) {
              joinTableSqls.add(joinTableSql);
            }
          } else {
            sqlQueryFields =
                bqTranslator.translator(valueDisplayField).buildSqlFieldsForListSelect();
          }

          sqlQueryFields.forEach(
              sqlQueryField -> {
                selectFieldNames.add(sqlQueryField.getField().getColumnName());
                selectFieldSqls.add(sqlQueryField.renderForSelect());
              });
        });

    // Build the list of entity tables we need to query or join.
    Entity singleEntity = entities.iterator().next();
    EntityFilter singleEntityFilter = filters.get(singleEntity);

    // get a list of attributes filtered on, if implemented for the filter
    List<String> filterAttributeNames =
        singleEntityFilter != null ? singleEntityFilter.getFilterAttributeNames() : List.of();

    // default: use entityMain table
    // check if: entity is optimized for search on filterAttributes &
    // (either search table contains entityMain fields OR all selectFields)
    IndexTable entityTable = underlay.getIndexSchema().getEntityMain(singleEntity.getName());
    if (singleEntity.containsOptimizeSearchByAttributes(filterAttributeNames)) {
      ITEntitySearchByAttributes searchTable =
          underlay.getIndexSchema().getEntitySearchByAttributes(singleEntity, filterAttributeNames);
      if (searchTable.includeEntityMainColumns()
          || new HashSet<>(searchTable.getColumnNames()).containsAll(selectFieldNames)) {
        entityTable = searchTable;
      }
    }

    // SELECT [select fields] FROM [entity main] JOIN [join tables]
    sql.append("SELECT ")
        .append(String.join(", ", selectFieldSqls))
        .append(" FROM ")
        .append(entityTable.getTablePointer().render());

    // JOIN [join tables]
    if (!joinTableSqls.isEmpty()) {
      sql.append(" ").append(String.join(" ", joinTableSqls));
    }

    // WHERE [filter]
    if (singleEntityFilter != null) {
      sql.append(" WHERE ")
          .append(bqTranslator.translator(singleEntityFilter).buildSql(sqlParams, null));
    }

    // GROUP BY [group by fields]
    if (!groupByFields.isEmpty()) {
      List<String> groupByFieldSqls = new ArrayList<>();
      groupByFields.forEach(
          groupBy -> {
            List<SqlQueryField> sqlQueryFields;
            if (groupBy instanceof AttributeField) {
              sqlQueryFields =
                  ((BQAttributeFieldTranslator) bqTranslator.translator(groupBy))
                      .buildSqlFieldsForCountSelectAndGroupBy(
                          entityLevelHints.get(groupBy.getEntity()));
            } else {
              sqlQueryFields = bqTranslator.translator(groupBy).buildSqlFieldsForGroupBy();
            }
            sqlQueryFields.forEach(
                sqlQueryField -> groupByFieldSqls.add(sqlQueryField.renderForGroupBy(null, true)));
          });
      sql.append(" GROUP BY ").append(String.join(", ", groupByFieldSqls));
    }

    // ORDER BY [order by fields]
    if (!orderBys.isEmpty()) {
      // All the order by fields come from the index entity main table.
      List<String> orderByFields = new ArrayList<>();
      orderBys.forEach(
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
                                      null, selectFields.contains(orderBy.getEntityField()))
                                  + ' '
                                  + bqTranslator.orderByDirectionSql(orderBy.getDirection())));
            }
          });
      sql.append(" ORDER BY ").append(String.join(", ", orderByFields));
    }

    // LIMIT [limit]
    if (limit != null) {
      sql.append(" LIMIT ").append(limit);
    }

    // Swap out any un-cacheable functions with SQL parameters.
    String sqlWithOnlyCacheableFunctions =
        BQExecutor.replaceFunctionsThatPreventCaching(sql.toString(), sqlParams, queryInstant);

    return new SqlQueryRequest(
        sqlWithOnlyCacheableFunctions, sqlParams, pageMarker, pageSize, isDryRun);
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
    SqlParams sqlParams = indexDataSqlRequest.sqlParams();

    // Now build the outer SQL query against the source data.
    final String sourceTableAlias = "st";
    // Using a set since source queries can reuse the same join key with many columns from joined
    // table.
    Set<String> selectFields = new LinkedHashSet<>();
    // Map of all joins which allows for removal of repeating join conditions. Key contains both
    // valueFieldName and displayFieldTable which supports joins to different tables on the same
    // attribute value.
    // e.g. displayTableJoins: sourceQuery.valueFieldName, sourceQuery.displayFieldTable -> joinSql
    Table<String, String, String> displayTableJoins = HashBasedTable.create();
    // Map of table join aliases allows for multiple columns from same join condition to reuse
    // aliases. Key contains both valueFieldName and displayFieldTable which supports joins to
    // different tables on the same attribute value.
    // e.g. tableJoinAliases: sourceQuery.valueFieldName, sourceQuery.displayFieldTable ->
    // joinTableAlias
    Table<String, String, String> tableJoinAliases = HashBasedTable.create();
    BQApiTranslator bqTranslator = new BQApiTranslator();
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

                  String valueFieldName = attrSourcePointer.getValueFieldName();
                  String displayFieldTable =
                      attrFieldAgainstSourceData
                          .getAttribute()
                          .getSourceQuery()
                          .getDisplayFieldTable();
                  if (displayTableJoins.get(valueFieldName, displayFieldTable) == null) {
                    // Add new table joins and aliases
                    String joinSql =
                        " LEFT JOIN "
                            + fromFullTablePath(attrSourcePointer.getDisplayFieldTable()).render()
                            + " AS "
                            + joinTableAlias
                            + " ON "
                            + displayTableJoinField.renderForSelect(joinTableAlias)
                            + " = "
                            + valueSqlField.renderForSelect(sourceTableAlias);

                    displayTableJoins.put(valueFieldName, displayFieldTable, joinSql);
                    tableJoinAliases.put(valueFieldName, displayFieldTable, joinTableAlias);
                    selectFields.add(displaySqlField.renderForSelect(joinTableAlias));
                  } else {
                    // Use existing table joins and aliases
                    selectFields.add(
                        displaySqlField.renderForSelect(
                            tableJoinAliases.get(valueFieldName, displayFieldTable)));
                  }

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
    String sql =
        "SELECT "
            + String.join(", ", selectFields)
            + " FROM "
            + fromFullTablePath(listQueryRequest.getEntity().getSourceQueryTableName()).render()
            + " AS "
            + sourceTableAlias
            + String.join("", displayTableJoins.values().stream().toList())
            + " WHERE "
            + sourceIdAttrSqlField.renderForSelect(sourceTableAlias)
            + " IN ("
            + indexDataSqlRequest.sql()
            + ')';
    return new SqlQueryRequest(
        sql,
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
}
