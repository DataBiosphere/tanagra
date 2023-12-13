package bio.terra.tanagra.query2.bigquery;

import static bio.terra.tanagra.query2.sql.SqlGeneration.orderByDirectionSql;
import static bio.terra.tanagra.query2.sql.SqlGeneration.orderBySql;
import static bio.terra.tanagra.query2.sql.SqlGeneration.selectSql;

import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.api.query.list.ListQueryResult;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query2.ListQueryRunner;
import bio.terra.tanagra.query2.sql.SqlParams;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BigQueryListRunner implements ListQueryRunner {
  public BigQueryListRunner() {}

  @Override
  public ListQueryResult run(ListQueryRequest listQueryRequest) {
    // Build the SQL query.
    StringBuilder sql = new StringBuilder();
    SqlParams sqlParams = new SqlParams();

    // All the select fields come from the index entity main table.
    ITEntityMain entityMain =
        listQueryRequest
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(listQueryRequest.getEntity().getName());
    String entityMainAlias = entityMain.getEntity().toLowerCase().substring(0, 1);
    List<String> selectFields = new ArrayList<>();
    listQueryRequest.getSelectFields().stream()
        .forEach(
            valueDisplayField ->
                BigQueryFieldSqlUtils.getFieldsAndAliases(
                        listQueryRequest, valueDisplayField, false)
                    .stream()
                    .forEach(field -> selectFields.add(selectSql(field, entityMainAlias))));

    // SELECT [select fields] FROM [entity main] AS [entity main alias]
    sql.append("SELECT ")
        .append(selectFields.stream().collect(Collectors.joining(", ")))
        .append(" FROM ")
        .append(entityMain.getTablePointer().renderSQL())
        .append(" AS ")
        .append(entityMainAlias);

    // WHERE [filter]
    if (listQueryRequest.getFilter() != null) {
      FieldPointer idField =
          entityMain.getAttributeValueField(
              listQueryRequest.getEntity().getIdAttribute().getName());
      sql.append(" WHERE ")
          .append(
              BigQueryFilterSqlUtils.buildFilterSql(
                  listQueryRequest.getUnderlay(),
                  listQueryRequest.getFilter(),
                  entityMainAlias,
                  sqlParams,
                  idField));
    }

    // ORDER BY [order by fields]
    if (!listQueryRequest.getOrderBys().isEmpty()) {
      // All the order by fields come from the index entity main table.
      List<String> orderByFields = new ArrayList<>();
      listQueryRequest.getOrderBys().stream()
          .forEach(
              orderBy ->
                  BigQueryFieldSqlUtils.getFieldsAndAliases(
                          listQueryRequest, orderBy.getEntityField(), true)
                      .stream()
                      .forEach(
                          field ->
                              orderByFields.add(
                                  orderBySql(
                                          field,
                                          entityMainAlias,
                                          listQueryRequest
                                              .getSelectFields()
                                              .contains(orderBy.getEntityField()))
                                      + ' '
                                      + orderByDirectionSql(orderBy.getDirection()))));

      sql.append(" ORDER BY ").append(orderByFields.stream().collect(Collectors.joining(", ")));
    }

    // LIMIT [limit]
    if (listQueryRequest.getLimit() != null) {
      sql.append(" LIMIT ").append(listQueryRequest.getLimit());
    }

    // TODO: Execute the SQL query.

    return new ListQueryResult(sql.toString(), List.of(), null);
  }
}
