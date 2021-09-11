package bio.terra.tanagra.service.search;

import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR_NAME;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR_RATING;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.loadNauticalUnderlay;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.tanagra.model.DataType;
import bio.terra.tanagra.service.databaseaccess.ColumnHeaderSchema;
import bio.terra.tanagra.service.databaseaccess.ColumnSchema;
import bio.terra.tanagra.service.databaseaccess.QueryExecutorStub;
import bio.terra.tanagra.service.databaseaccess.QueryRequest;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class SearchEngineTest {
  private static final SearchContext SIMPLE_CONTEXT =
      SearchContext.builder().underlay(loadNauticalUnderlay()).build();

  private static final Variable S_VAR = Variable.create("s");
  private static final EntityVariable S_SAILOR = EntityVariable.create(SAILOR, S_VAR);
  private static final AttributeVariable S_NAME = AttributeVariable.create(SAILOR_NAME, S_VAR);
  private static final AttributeVariable S_RATING = AttributeVariable.create(SAILOR_RATING, S_VAR);

  @Test
  void execute() {
    QueryExecutorStub queryExecutorStub = new QueryExecutorStub();
    SearchEngine searchEngine = new SearchEngine((primaryTable -> queryExecutorStub));

    Query query =
        Query.builder()
            .selections(
                ImmutableList.of(
                    Selection.SelectExpression.builder()
                        .expression(Expression.AttributeExpression.create(S_RATING))
                        .name("rating")
                        .build(),
                    Selection.SelectExpression.builder()
                        .expression(Expression.AttributeExpression.create(S_NAME))
                        .name("name")
                        .build()))
            .primaryEntity(S_SAILOR)
            .filter(
                Optional.of(
                    Filter.BinaryFunction.create(
                        Expression.AttributeExpression.create(S_RATING),
                        Filter.BinaryFunction.Operator.EQUALS,
                        Expression.Literal.create(DataType.INT64, "62"))))
            .build();
    searchEngine.execute(query, SIMPLE_CONTEXT);
    assertEquals(
        QueryRequest.builder()
            .sql(
                "SELECT s.rating AS rating, s.s_name AS name "
                    + "FROM `my-project-id.nautical`.sailors AS s WHERE s.rating = 62")
            .columnHeaderSchema(
                ColumnHeaderSchema.builder()
                    .columnSchemas(
                        ImmutableList.of(
                            ColumnSchema.builder().dataType(DataType.INT64).name("rating").build(),
                            ColumnSchema.builder().dataType(DataType.STRING).name("name").build()))
                    .build())
            .build(),
        queryExecutorStub.getLatestQueryRequest());
  }
}
