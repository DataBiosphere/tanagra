package bio.terra.tanagra.service.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.tanagra.service.search.SqlVisitor.SelectionVisitor;
import bio.terra.tanagra.service.search.testing.SimpleUnderlaySqlResolver;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class SqlVisitorTest {
  private static final SearchContext SIMPLE_CONTEXT =
      SearchContext.builder().underlaySqlResolver(new SimpleUnderlaySqlResolver()).build();

  private static final Entity PERSON = Entity.builder().name("person").underlay("foo").build();
  private static final Attribute HEIGHT =
      Attribute.builder().name("height").dataType(DataType.INT64).entity(PERSON).build();
  private static final Attribute FIRST_NAME =
      Attribute.builder().name("first_name").dataType(DataType.STRING).entity(PERSON).build();

  @Test
  void query() {
    Query query =
        Query.create(
            ImmutableList.of(
                Selection.SelectExpression.create(Expression.AttributeExpression.create(HEIGHT)),
                Selection.SelectExpression.create(
                    Expression.AttributeExpression.create(FIRST_NAME))),
            PERSON,
            Optional.of(
                Filter.BinaryFunction.create(
                    Expression.AttributeExpression.create(HEIGHT),
                    Filter.BinaryFunction.Operator.EQUALS,
                    Expression.Literal.create(DataType.INT64, "62"))));
    assertEquals(
        "SELECT person.height, person.first_name FROM foo.person WHERE person.height = 62",
        new SqlVisitor(SIMPLE_CONTEXT).createSql(query));
  }

  @Test
  void selectionExpression() {
    SqlVisitor.SelectionVisitor visitor = new SelectionVisitor(SIMPLE_CONTEXT);
    assertEquals(
        "person.height AS h",
        Selection.SelectExpression.create(
                Expression.AttributeExpression.create(HEIGHT), Optional.of("h"))
            .accept(visitor));
    assertEquals(
        "person.height",
        Selection.SelectExpression.create(
                Expression.AttributeExpression.create(HEIGHT), Optional.empty())
            .accept(visitor));
  }

  @Test
  void selectionCount() {
    SqlVisitor.SelectionVisitor visitor = new SelectionVisitor(SIMPLE_CONTEXT);
    assertEquals(
        "COUNT(person) AS c", Selection.Count.create(PERSON, Optional.of("c")).accept(visitor));
    assertEquals("COUNT(person)", Selection.Count.create(PERSON, Optional.empty()).accept(visitor));
  }

  @Test
  void filterBinaryFunction() {
    Filter.BinaryFunction lessThanFilter =
        Filter.BinaryFunction.create(
            Expression.AttributeExpression.create(HEIGHT),
            Filter.BinaryFunction.Operator.LESS_THAN,
            Expression.Literal.create(DataType.INT64, "62"));
    assertEquals(
        "person.height < 62", lessThanFilter.accept(new SqlVisitor.FilterVisitor(SIMPLE_CONTEXT)));

    Filter.BinaryFunction equalsFilter =
        Filter.BinaryFunction.create(
            Expression.AttributeExpression.create(HEIGHT),
            Filter.BinaryFunction.Operator.EQUALS,
            Expression.Literal.create(DataType.INT64, "62"));
    assertEquals(
        "person.height = 62", equalsFilter.accept(new SqlVisitor.FilterVisitor(SIMPLE_CONTEXT)));
  }

  @Test
  void filterArrayFunction() {
    ImmutableList<Filter> operands =
        ImmutableList.of(
            Filter.BinaryFunction.create(
                Expression.AttributeExpression.create(HEIGHT),
                Filter.BinaryFunction.Operator.LESS_THAN,
                Expression.Literal.create(DataType.INT64, "62")),
            Filter.BinaryFunction.create(
                Expression.AttributeExpression.create(FIRST_NAME),
                Filter.BinaryFunction.Operator.EQUALS,
                Expression.Literal.create(DataType.STRING, "John")));

    Filter andFilter = Filter.ArrayFunction.create(operands, Filter.ArrayFunction.Operator.AND);
    assertEquals(
        "(person.height < 62) AND (person.first_name = 'John')",
        andFilter.accept(new SqlVisitor.FilterVisitor(SIMPLE_CONTEXT)));

    Filter orFilter = Filter.ArrayFunction.create(operands, Filter.ArrayFunction.Operator.OR);
    assertEquals(
        "(person.height < 62) OR (person.first_name = 'John')",
        orFilter.accept(new SqlVisitor.FilterVisitor(SIMPLE_CONTEXT)));
  }

  @Test
  void filterArrayFunctionThrowsIfOperandsEmpty() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Filter.ArrayFunction.create(ImmutableList.of(), Filter.ArrayFunction.Operator.AND));
  }

  @Test
  void expressionLiteral() {
    SqlVisitor.ExpressionVisitor expressionVisitor =
        new SqlVisitor.ExpressionVisitor(SIMPLE_CONTEXT);
    assertEquals("42", Expression.Literal.create(DataType.INT64, "42").accept(expressionVisitor));
    assertEquals(
        "'foo'", Expression.Literal.create(DataType.STRING, "foo").accept(expressionVisitor));
  }

  @Test
  void expressionAttribute() {
    SqlVisitor.ExpressionVisitor expressionVisitor =
        new SqlVisitor.ExpressionVisitor(SIMPLE_CONTEXT);
    assertEquals(
        "person.height", Expression.AttributeExpression.create(HEIGHT).accept(expressionVisitor));
  }
}
