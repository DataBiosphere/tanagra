package bio.terra.tanagra.api2.query;

import bio.terra.tanagra.api2.field.*;
import bio.terra.tanagra.api2.query.count.CountInstance;
import bio.terra.tanagra.api2.query.count.CountQueryRequest;
import bio.terra.tanagra.api2.query.count.CountQueryResult;
import bio.terra.tanagra.api2.query.hint.Hint;
import bio.terra.tanagra.api2.query.hint.HintInstance;
import bio.terra.tanagra.api2.query.hint.HintQueryRequest;
import bio.terra.tanagra.api2.query.hint.HintQueryResult;
import bio.terra.tanagra.api2.query.list.ListInstance;
import bio.terra.tanagra.api2.query.list.ListQueryRequest;
import bio.terra.tanagra.api2.query.list.ListQueryResult;
import bio.terra.tanagra.query.*;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.underlay2.entitymodel.Attribute;
import bio.terra.tanagra.underlay2.indextable.ITEntityMain;
import bio.terra.tanagra.underlay2.indextable.ITInstanceLevelDisplayHints;
import com.google.common.collect.Lists;
import java.util.*;

public final class EntityQueryRunner {
  private EntityQueryRunner() {}

  public static ListQueryResult run(ListQueryRequest apiQueryRequest, QueryExecutor queryExecutor) {
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

    // ==================================================================
    // Execute the SQL query.
    QueryResult queryResult =
        queryExecutor.execute(
            new QueryRequest(
                query.renderSQL(),
                new ColumnHeaderSchema(columnSchemas),
                apiQueryRequest.getPageMarker(),
                apiQueryRequest.getPageSize()));

    // ==================================================================
    // Convert the SQL query results into entity query results. (SQL-> API)
    List<ListInstance> listInstances = new ArrayList<>();
    Iterator<RowResult> rowResultsItr = queryResult.getRowResults().iterator();
    while (rowResultsItr.hasNext()) {
      listInstances.add(
          ListInstance.fromRowResult(rowResultsItr.next(), apiQueryRequest.getSelectFields()));
    }
    return new ListQueryResult(query.renderSQL(), listInstances, queryResult.getNextPageMarker());
  }

  public static CountQueryResult run(
      CountQueryRequest apiQueryRequest, QueryExecutor queryExecutor) {
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

    // ==================================================================
    // Execute the SQL query.
    QueryResult queryResult =
        queryExecutor.execute(
            new QueryRequest(
                query.renderSQL(),
                new ColumnHeaderSchema(selectColumnSchemas),
                apiQueryRequest.getPageMarker(),
                apiQueryRequest.getPageSize()));

    // ==================================================================
    // Convert the SQL query results into entity query results. (SQL-> API)
    List<CountInstance> countInstances = new ArrayList<>();
    Iterator<RowResult> rowResultsItr = queryResult.getRowResults().iterator();
    while (rowResultsItr.hasNext()) {
      countInstances.add(
          CountInstance.fromRowResult(
              rowResultsItr.next(), apiQueryRequest.getGroupByFields(), countIdEntityField));
    }
    // TODO: If there are any attribute fields with excludeDisplay=true, query the entity-level
    // hints to set the displays.
    return new CountQueryResult(query.renderSQL(), countInstances, queryResult.getNextPageMarker());
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
                      fv.getAlias()
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
