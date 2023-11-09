package bio.terra.tanagra.api.query;

import bio.terra.tanagra.api.field.EntityIdCountField;
import bio.terra.tanagra.api.field.EntityLevelHintField;
import bio.terra.tanagra.api.field.HintField;
import bio.terra.tanagra.api.field.InstanceLevelHintField;
import bio.terra.tanagra.api.query.count.CountInstance;
import bio.terra.tanagra.api.query.count.CountQueryRequest;
import bio.terra.tanagra.api.query.count.CountQueryResult;
import bio.terra.tanagra.api.query.hint.Hint;
import bio.terra.tanagra.api.query.hint.HintInstance;
import bio.terra.tanagra.api.query.hint.HintQueryRequest;
import bio.terra.tanagra.api.query.hint.HintQueryResult;
import bio.terra.tanagra.api.query.list.ListInstance;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.api.query.list.ListQueryResult;
import bio.terra.tanagra.query.ColumnHeaderSchema;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.OrderByVariable;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.QueryExecutor;
import bio.terra.tanagra.query.QueryRequest;
import bio.terra.tanagra.query.QueryResult;
import bio.terra.tanagra.query.RowResult;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.indextable.ITInstanceLevelDisplayHints;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EntityQueryRunner {
  private static final Logger LOGGER = LoggerFactory.getLogger(EntityQueryRunner.class);

  private EntityQueryRunner() {}

  public static ListQueryResult run(ListQueryRequest apiQueryRequest, QueryExecutor queryExecutor) {
    // ==================================================================
    // Execute the SQL query.
    QueryRequest queryRequest = buildQueryRequest(apiQueryRequest);
    QueryResult queryResult = queryExecutor.execute(queryRequest);
    LOGGER.debug("ListQuery returned {} total rows", queryResult.getTotalNumRows());

    // ==================================================================
    // Convert the SQL query results into entity query results. (SQL-> API)
    List<ListInstance> listInstances = new ArrayList<>();
    Iterator<RowResult> rowResultsItr = queryResult.getRowResults().iterator();
    while (rowResultsItr.hasNext()) {
      listInstances.add(
          ListInstance.fromRowResult(rowResultsItr.next(), apiQueryRequest.getSelectFields()));
    }
    return new ListQueryResult(
        queryRequest.getSql(), listInstances, queryResult.getNextPageMarker());
  }

  public static QueryRequest buildQueryRequest(ListQueryRequest apiQueryRequest) {
    // ==================================================================
    // Convert the entity query into a SQL query. (API -> SQL)
    ITEntityMain indexTable =
        apiQueryRequest
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(apiQueryRequest.getEntity().getName());
    TableVariable tableVar = TableVariable.forPrimary(indexTable.getTablePointer());
    List<TableVariable> tableVars = Lists.newArrayList(tableVar);

    // Build the SELECT field variables and column schemas.
    List<FieldVariable> selectFieldVars = new ArrayList<>();
    List<ColumnSchema> columnSchemas = new ArrayList<>();
    apiQueryRequest.getSelectFields().stream()
        .forEach(
            entityField -> {
              selectFieldVars.addAll(entityField.buildFieldVariables(tableVar, tableVars));
              columnSchemas.addAll(entityField.getColumnSchemas());
            });

    // Build the ORDER BY field variables.
    List<OrderByVariable> orderByVars = new ArrayList<>();
    apiQueryRequest.getOrderBys().stream()
        .forEach(
            apiOrderBy -> {
              List<FieldVariable> fieldVars =
                  apiOrderBy.getEntityField().buildFieldVariables(tableVar, tableVars);
              fieldVars.stream()
                  .forEach(
                      fv -> orderByVars.add(new OrderByVariable(fv, apiOrderBy.getDirection())));
            });

    // Build the WHERE filter variables.
    FilterVariable filterVar =
        apiQueryRequest.getFilter() == null
            ? null
            : apiQueryRequest.getFilter().getFilterVariable(tableVar, tableVars);

    Query query =
        new Query.Builder()
            .select(selectFieldVars)
            .tables(tableVars)
            .where(filterVar)
            .orderBy(orderByVars)
            .limit(apiQueryRequest.getLimit())
            .build();
    return new QueryRequest(
        query.renderSQL(),
        new ColumnHeaderSchema(columnSchemas),
        apiQueryRequest.getPageMarker(),
        apiQueryRequest.getPageSize());
  }

  public static CountQueryResult run(
      CountQueryRequest apiQueryRequest, QueryExecutor queryExecutor) {
    // ==================================================================
    // Execute the SQL query.
    Pair<QueryRequest, EntityIdCountField> queryRequestAndCountField =
        buildQueryRequestAndCountField(apiQueryRequest);
    QueryRequest queryRequest = queryRequestAndCountField.getLeft();
    QueryResult queryResult = queryExecutor.execute(queryRequest);
    LOGGER.debug("CountQuery returned {} total rows", queryResult.getTotalNumRows());

    // ==================================================================
    // Convert the SQL query results into entity query results. (SQL-> API)
    List<CountInstance> countInstances = new ArrayList<>();
    Iterator<RowResult> rowResultsItr = queryResult.getRowResults().iterator();
    while (rowResultsItr.hasNext()) {
      countInstances.add(
          CountInstance.fromRowResult(
              rowResultsItr.next(),
              apiQueryRequest.getGroupByFields(),
              queryRequestAndCountField.getRight()));
    }
    // TODO: If there are any attribute fields with excludeDisplay=true, query the entity-level
    // hints to set the displays.
    return new CountQueryResult(
        queryRequest.getSql(), countInstances, queryResult.getNextPageMarker());
  }

  public static QueryRequest buildQueryRequest(CountQueryRequest apiQueryRequest) {
    return buildQueryRequestAndCountField(apiQueryRequest).getLeft();
  }

  private static Pair<QueryRequest, EntityIdCountField> buildQueryRequestAndCountField(
      CountQueryRequest apiQueryRequest) {
    // ==================================================================
    // Convert the entity query into a SQL query. (API -> SQL)
    ITEntityMain indexTable =
        apiQueryRequest
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(apiQueryRequest.getEntity().getName());
    TableVariable tableVar = TableVariable.forPrimary(indexTable.getTablePointer());
    List<TableVariable> tableVars = Lists.newArrayList(tableVar);

    // Build the GROUP BY field variables and column schemas.
    List<FieldVariable> groupByFieldVars = new ArrayList<>();
    List<ColumnSchema> groupByColumnSchemas = new ArrayList<>();
    apiQueryRequest.getGroupByFields().stream()
        .forEach(
            entityField -> {
              groupByFieldVars.addAll(entityField.buildFieldVariables(tableVar, tableVars));
              groupByColumnSchemas.addAll(entityField.getColumnSchemas());
            });

    // Build the COUNT field variable and column schema.
    EntityIdCountField countIdEntityField =
        new EntityIdCountField(apiQueryRequest.getUnderlay(), apiQueryRequest.getEntity());
    List<FieldVariable> countIdFieldVars =
        countIdEntityField.buildFieldVariables(tableVar, tableVars);
    List<ColumnSchema> countIdColumnSchemas = countIdEntityField.getColumnSchemas();

    // Build the WHERE filter variable.
    FilterVariable filterVar =
        apiQueryRequest.getFilter() == null
            ? null
            : apiQueryRequest.getFilter().getFilterVariable(tableVar, tableVars);

    // Define the SELECT field variables and column schemas to be the union of the GROUP BY and
    // COUNT ones.
    List<FieldVariable> selectFieldVars = new ArrayList<>();
    selectFieldVars.addAll(groupByFieldVars);
    selectFieldVars.addAll(countIdFieldVars);
    List<ColumnSchema> selectColumnSchemas = new ArrayList<>();
    selectColumnSchemas.addAll(groupByColumnSchemas);
    selectColumnSchemas.addAll(countIdColumnSchemas);

    Query query =
        new Query.Builder()
            .select(selectFieldVars)
            .tables(tableVars)
            .where(filterVar)
            .groupBy(groupByFieldVars)
            .build();
    return Pair.of(
        new QueryRequest(
            query.renderSQL(),
            new ColumnHeaderSchema(selectColumnSchemas),
            apiQueryRequest.getPageMarker(),
            apiQueryRequest.getPageSize()),
        countIdEntityField);
  }

  public static HintQueryResult run(HintQueryRequest apiQueryRequest, QueryExecutor queryExecutor) {
    // ==================================================================
    // Convert the entity query into a SQL query. (API -> SQL)
    TablePointer indexTable =
        apiQueryRequest.isEntityLevel()
            ? apiQueryRequest
                .getUnderlay()
                .getIndexSchema()
                .getEntityLevelDisplayHints(apiQueryRequest.getHintedEntity().getName())
                .getTablePointer()
            : apiQueryRequest
                .getUnderlay()
                .getIndexSchema()
                .getInstanceLevelDisplayHints(
                    apiQueryRequest.getEntityGroup().getName(),
                    apiQueryRequest.getHintedEntity().getName(),
                    apiQueryRequest.getRelatedEntity().getName())
                .getTablePointer();
    TableVariable tableVar = TableVariable.forPrimary(indexTable);
    List<TableVariable> tableVars = Lists.newArrayList(tableVar);

    // Build the SELECT field variables and column schemas.
    HintField hintField =
        apiQueryRequest.isEntityLevel()
            ? new EntityLevelHintField(
                apiQueryRequest.getUnderlay(), apiQueryRequest.getHintedEntity())
            : new InstanceLevelHintField(
                apiQueryRequest.getUnderlay(),
                apiQueryRequest.getEntityGroup(),
                apiQueryRequest.getHintedEntity(),
                apiQueryRequest.getRelatedEntity());
    List<FieldVariable> fieldVars = hintField.buildFieldVariables(tableVar, tableVars);
    List<ColumnSchema> columnSchemas = hintField.getColumnSchemas();

    Query.Builder queryBuilder = new Query.Builder().select(fieldVars).tables(tableVars);
    if (!apiQueryRequest.isEntityLevel()) {
      // Build the WHERE filter variable.
      FieldVariable entityIdFieldVar =
          fieldVars.stream()
              .filter(
                  fv ->
                      fv.getAliasOrColumnName()
                          .equals(
                              ITInstanceLevelDisplayHints.Column.ENTITY_ID
                                  .getSchema()
                                  .getColumnName()))
              .findFirst()
              .get();
      BinaryFilterVariable filterVar =
          new BinaryFilterVariable(
              entityIdFieldVar,
              BinaryFilterVariable.BinaryOperator.EQUALS,
              apiQueryRequest.getRelatedEntityId());
      queryBuilder.where(filterVar);
    }
    Query query = queryBuilder.build();

    // ==================================================================
    // Execute the SQL query.
    QueryResult queryResult =
        queryExecutor.execute(
            new QueryRequest(query.renderSQL(), new ColumnHeaderSchema(columnSchemas)));
    LOGGER.debug("HintQuery returned {} total rows", queryResult.getTotalNumRows());

    // ==================================================================
    // Convert the SQL query results into entity query results. (SQL-> API)
    List<HintInstance> hintInstances = new ArrayList<>();
    Iterator<RowResult> rowResultsItr = queryResult.getRowResults().iterator();
    while (rowResultsItr.hasNext()) {
      Hint hint = hintField.parseFromRowResult(rowResultsItr.next());
      Attribute attribute = apiQueryRequest.getHintedEntity().getAttribute(hint.getAttribute());
      if (hint.isRangeHint()) {
        // This is a numeric range hint, which is contained in a single row.
        hintInstances.add(new HintInstance(attribute, hint.getMin(), hint.getMax()));
      } else {
        // This is part of an enum values hint, which is spread across multiple rows, one per value.
        Optional<HintInstance> existingHintInstance =
            hintInstances.stream().filter(hi -> hi.getAttribute().equals(attribute)).findFirst();
        if (existingHintInstance.isEmpty()) {
          // Add a new hint instance.
          hintInstances.add(
              new HintInstance(attribute, Map.of(hint.getEnumVal(), hint.getEnumCount())));
        } else {
          // Update an existing hint instance.
          existingHintInstance.get().addEnumValueCount(hint.getEnumVal(), hint.getEnumCount());
        }
      }
    }
    return new HintQueryResult(query.renderSQL(), hintInstances);
  }
}
