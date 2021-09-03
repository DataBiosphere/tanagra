package bio.terra.tanagra.service.search;

import bio.terra.tanagra.service.databaseaccess.ColumnHeaderSchema;
import bio.terra.tanagra.service.databaseaccess.ColumnSchema;
import bio.terra.tanagra.service.databaseaccess.QueryExecutor;
import bio.terra.tanagra.service.databaseaccess.QueryRequest;
import bio.terra.tanagra.service.databaseaccess.QueryResult;
import bio.terra.tanagra.service.underlay.Table;
import bio.terra.tanagra.service.underlay.Underlay;
import com.google.common.collect.ImmutableList;

// DO NOT SUBMIT comment me.
public class SearchEngine {
  private final QueryExecutor.Factory queryExecutorFactory;

  public SearchEngine(QueryExecutor.Factory queryExecutorFactory) {
    this.queryExecutorFactory = queryExecutorFactory;
  }

  public QueryResult execute(Query query, SearchContext searchContext) {
    // TODO write business logic to use the appropriate indexes once we have indexes.
    // TODO add query parameterization support.
    String sql = new SqlVisitor(searchContext).createSql(query);
    QueryRequest queryRequest =
        QueryRequest.builder()
            .sql(sql)
            .columnHeaderSchema(createColumnHeaderSchema(query, searchContext.underlay()))
            .build();

    Table primaryTable =
        searchContext.underlay().primaryKeys().get(query.primaryEntity().entity()).table();

    QueryExecutor queryExecutor = queryExecutorFactory.get(primaryTable);
    return queryExecutor.execute(queryRequest);
  }

  private static ColumnHeaderSchema createColumnHeaderSchema(Query query, Underlay underlay) {
    ImmutableList<ColumnSchema> columnSchemas =
        query.selections().stream()
            .map(selection -> deriveSchema(selection, underlay))
            .collect(ImmutableList.toImmutableList());
    return ColumnHeaderSchema.builder().columnSchemas(columnSchemas).build();
  }

  private static ColumnSchema deriveSchema(Selection selection, Underlay underlay) {
    DataType dataType = selection.accept(new DataTypeVisitor.SelectionVisitor(underlay));

    String name = null; // DO NOT SUBMIT. change select.
    name = ((Selection.SelectExpression) selection).alias().get();
    return ColumnSchema.builder().name(name).dataType(dataType).build();
  }
}
