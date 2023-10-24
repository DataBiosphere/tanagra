package bio.terra.tanagra.api2.query;

import bio.terra.tanagra.api2.field.EntityIdCountField;
import bio.terra.tanagra.api2.query.count.CountInstance;
import bio.terra.tanagra.api2.query.count.CountQueryRequest;
import bio.terra.tanagra.api2.query.count.CountQueryResult;
import bio.terra.tanagra.api2.query.hint.HintInstance;
import bio.terra.tanagra.api2.query.hint.HintQueryRequest;
import bio.terra.tanagra.api2.query.hint.HintQueryResult;
import bio.terra.tanagra.api2.query.list.ListInstance;
import bio.terra.tanagra.api2.query.list.ListQueryRequest;
import bio.terra.tanagra.api2.query.list.ListQueryResult;
import bio.terra.tanagra.query.*;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.underlay2.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay2.indexschema.EntityLevelDisplayHints;
import bio.terra.tanagra.underlay2.indexschema.InstanceLevelDisplayHints;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public final class EntityQueryRunner {
  private EntityQueryRunner() {}

  public static ListQueryResult run(ListQueryRequest apiQueryRequest, QueryExecutor queryExecutor) {
    // ==================================================================
    // Convert the entity query into a SQL query. (API -> SQL)
    TableVariable entityTableVar =
        TableVariable.forPrimary(apiQueryRequest.getEntity().getIndexEntityTable());
    List<TableVariable> tableVars = Lists.newArrayList(entityTableVar);

    // Build the SELECT field variables and column schemas.
    List<FieldVariable> selectFieldVars = new ArrayList<>();
    List<ColumnSchema> columnSchemas = new ArrayList<>();
    apiQueryRequest.getSelectFields().stream()
        .forEach(
            entityField -> {
              selectFieldVars.addAll(entityField.buildFieldVariables(entityTableVar, tableVars));
              columnSchemas.addAll(entityField.getColumnSchemas());
            });

    // Build the ORDER BY field variables.
    List<OrderByVariable> orderByVars = new ArrayList<>();
    apiQueryRequest.getOrderBys().stream()
        .forEach(
            apiOrderBy -> {
              List<FieldVariable> fieldVars =
                  apiOrderBy.getEntityField().buildFieldVariables(entityTableVar, tableVars);
              fieldVars.stream()
                  .forEach(
                      fv -> orderByVars.add(new OrderByVariable(fv, apiOrderBy.getDirection())));
            });

    // Build the WHERE filter variables.
    FilterVariable filterVar =
        apiQueryRequest.getFilter() == null
            ? null
            : apiQueryRequest.getFilter().getFilterVariable(entityTableVar, tableVars);

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
    TableVariable entityTableVar =
        TableVariable.forPrimary(apiQueryRequest.getEntity().getIndexEntityTable());
    List<TableVariable> tableVars = Lists.newArrayList(entityTableVar);

    // Build the GROUP BY field variables and column schemas.
    List<FieldVariable> groupByFieldVars = new ArrayList<>();
    List<ColumnSchema> groupByColumnSchemas = new ArrayList<>();
    apiQueryRequest.getGroupByFields().stream()
        .forEach(
            entityField -> {
              groupByFieldVars.addAll(entityField.buildFieldVariables(entityTableVar, tableVars));
              groupByColumnSchemas.addAll(entityField.getColumnSchemas());
            });

    // Build the COUNT field variable and column schema.
    EntityIdCountField countIdEntityField = new EntityIdCountField(apiQueryRequest.getEntity());
    List<FieldVariable> countIdFieldVars =
        countIdEntityField.buildFieldVariables(entityTableVar, tableVars);
    List<ColumnSchema> countIdColumnSchemas = countIdEntityField.getColumnSchemas();

    // Build the WHERE filter variable.
    FilterVariable filterVar =
        apiQueryRequest.getFilter() == null
            ? null
            : apiQueryRequest.getFilter().getFilterVariable(entityTableVar, tableVars);

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
    TablePointer dhTable =
        apiQueryRequest.isEntityLevel()
            ? apiQueryRequest.getEntity().getIndexEntityLevelDisplayHintTable()
            : ((CriteriaOccurrence) apiQueryRequest.getEntityGroup())
                .getIndexInstanceLevelDisplayHintTable(
                    apiQueryRequest.getRelatedEntity().getName());
    TableVariable dhTableVar = TableVariable.forPrimary(dhTable);
    List<TableVariable> tableVars = Lists.newArrayList(dhTableVar);

    // Build the SELECT field variables and column schemas.
    List<ColumnSchema> selectColumnSchemas =
        apiQueryRequest.isEntityLevel()
            ? EntityLevelDisplayHints.getColumns()
            : InstanceLevelDisplayHints.getColumns();
    List<FieldVariable> selectFieldVars =
        selectColumnSchemas.stream()
            .map(
                cs -> {
                  FieldPointer fieldPointer =
                      new FieldPointer.Builder()
                          .tablePointer(dhTableVar.getTablePointer())
                          .columnName(cs.getColumnName())
                          .build();
                  return fieldPointer.buildVariable(dhTableVar, tableVars);
                })
            .collect(Collectors.toList());

    Query.Builder queryBuilder = new Query.Builder().select(selectFieldVars).tables(tableVars);
    if (!apiQueryRequest.isEntityLevel()) {
      // Build the WHERE filter variable.
      FieldVariable entityIdFieldVar =
          selectFieldVars.stream()
              .filter(
                  fv ->
                      fv.getAlias()
                          .equals(
                              InstanceLevelDisplayHints.Column.ENTITY_ID
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
            new QueryRequest(query.renderSQL(), new ColumnHeaderSchema(selectColumnSchemas)));

    // ==================================================================
    // Convert the SQL query results into entity query results. (SQL-> API)
    List<HintInstance> hintInstances = new ArrayList<>();
    Iterator<RowResult> rowResultsItr = queryResult.getRowResults().iterator();
    while (rowResultsItr.hasNext()) {
      HintInstance.fromRowResult(
          rowResultsItr.next(),
          hintInstances,
          apiQueryRequest.getEntity(),
          apiQueryRequest.isEntityLevel());
    }
    return new HintQueryResult(query.renderSQL(), hintInstances);
  }
}
