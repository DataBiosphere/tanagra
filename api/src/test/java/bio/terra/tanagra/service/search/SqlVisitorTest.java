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
  private static final Attribute ID =
      Attribute.builder().name("id").dataType(DataType.INT64).entity(PERSON).build();
  private static final Attribute HEIGHT =
      Attribute.builder().name("height").dataType(DataType.INT64).entity(PERSON).build();
  private static final Attribute FIRST_NAME =
      Attribute.builder().name("first_name").dataType(DataType.STRING).entity(PERSON).build();

  private static final Entity ADDRESS = Entity.builder().name("address").underlay("foo").build();
  private static final Attribute PERSON_ID_FK =
      Attribute.builder().name("person_id").dataType(DataType.INT64).entity(ADDRESS).build();
  private static final Attribute ZIP_CODE =
      Attribute.builder().name("zip_code").dataType(DataType.INT64).entity(ADDRESS).build();

  private static final Variable P_VAR = Variable.create("p");
  private static final EntityVariable P_PERSON = EntityVariable.create(PERSON, P_VAR);
  private static final AttributeVariable P_ID = AttributeVariable.create(ID, P_VAR);
  private static final AttributeVariable P_HEIGHT = AttributeVariable.create(HEIGHT, P_VAR);
  private static final AttributeVariable P_FIRST_NAME = AttributeVariable.create(FIRST_NAME, P_VAR);

  private static final Variable A_VAR = Variable.create("a");
  private static final AttributeVariable A_PERSON_ID =
      AttributeVariable.create(PERSON_ID_FK, A_VAR);
  private static final AttributeVariable A_ZIP_CODE = AttributeVariable.create(ZIP_CODE, A_VAR);

  @Test
  void query() {
    Query query =
        Query.create(
            ImmutableList.of(
                Selection.SelectExpression.create(Expression.AttributeExpression.create(P_HEIGHT)),
                Selection.SelectExpression.create(
                    Expression.AttributeExpression.create(P_FIRST_NAME))),
            P_PERSON,
            Optional.of(
                Filter.BinaryFunction.create(
                    Expression.AttributeExpression.create(P_HEIGHT),
                    Filter.BinaryFunction.Operator.EQUALS,
                    Expression.Literal.create(DataType.INT64, "62"))));
    assertEquals(
        "SELECT p.height, p.first_name FROM foo.person AS p WHERE p.height = 62",
        new SqlVisitor(SIMPLE_CONTEXT).createSql(query));
  }

  @Test
  void selectionExpression() {
    SqlVisitor.SelectionVisitor visitor = new SelectionVisitor(SIMPLE_CONTEXT);
    assertEquals(
        "p.height AS h",
        Selection.SelectExpression.create(
                Expression.AttributeExpression.create(P_HEIGHT), Optional.of("h"))
            .accept(visitor));
    assertEquals(
        "p.height",
        Selection.SelectExpression.create(
                Expression.AttributeExpression.create(P_HEIGHT), Optional.empty())
            .accept(visitor));
  }

  @Test
  void selectionCount() {
    SqlVisitor.SelectionVisitor visitor = new SelectionVisitor(SIMPLE_CONTEXT);
    assertEquals(
        "COUNT(p) AS c", Selection.Count.create(P_PERSON, Optional.of("c")).accept(visitor));
    assertEquals("COUNT(p)", Selection.Count.create(P_PERSON, Optional.empty()).accept(visitor));
  }

  @Test
  void filterBinaryFunction() {
    Filter.BinaryFunction lessThanFilter =
        Filter.BinaryFunction.create(
            Expression.AttributeExpression.create(P_HEIGHT),
            Filter.BinaryFunction.Operator.LESS_THAN,
            Expression.Literal.create(DataType.INT64, "62"));
    assertEquals(
        "p.height < 62", lessThanFilter.accept(new SqlVisitor.FilterVisitor(SIMPLE_CONTEXT)));

    Filter.BinaryFunction equalsFilter =
        Filter.BinaryFunction.create(
            Expression.AttributeExpression.create(P_HEIGHT),
            Filter.BinaryFunction.Operator.EQUALS,
            Expression.Literal.create(DataType.INT64, "62"));
    assertEquals(
        "p.height = 62", equalsFilter.accept(new SqlVisitor.FilterVisitor(SIMPLE_CONTEXT)));
  }

  @Test
  void filterArrayFunction() {
    ImmutableList<Filter> operands =
        ImmutableList.of(
            Filter.BinaryFunction.create(
                Expression.AttributeExpression.create(P_HEIGHT),
                Filter.BinaryFunction.Operator.LESS_THAN,
                Expression.Literal.create(DataType.INT64, "62")),
            Filter.BinaryFunction.create(
                Expression.AttributeExpression.create(P_FIRST_NAME),
                Filter.BinaryFunction.Operator.EQUALS,
                Expression.Literal.create(DataType.STRING, "John")));

    Filter andFilter = Filter.ArrayFunction.create(operands, Filter.ArrayFunction.Operator.AND);
    assertEquals(
        "(p.height < 62) AND (p.first_name = 'John')",
        andFilter.accept(new SqlVisitor.FilterVisitor(SIMPLE_CONTEXT)));

    Filter orFilter = Filter.ArrayFunction.create(operands, Filter.ArrayFunction.Operator.OR);
    assertEquals(
        "(p.height < 62) OR (p.first_name = 'John')",
        orFilter.accept(new SqlVisitor.FilterVisitor(SIMPLE_CONTEXT)));
  }

  @Test
  void filterArrayFunctionThrowsIfOperandsEmpty() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Filter.ArrayFunction.create(ImmutableList.of(), Filter.ArrayFunction.Operator.AND));
  }

  @Test
  void filterRelationship() {
    assertEquals(
        "p.id IN (SELECT a.person_id FROM foo.address AS a WHERE a.zip_code = 12345)",
        Filter.RelationshipFilter.builder()
            .outerAttribute(P_ID)
            .boundAttribute(A_PERSON_ID)
            .filter(
                Filter.BinaryFunction.create(
                    Expression.AttributeExpression.create(A_ZIP_CODE),
                    Filter.BinaryFunction.Operator.EQUALS,
                    Expression.Literal.create(DataType.INT64, "12345")))
            .build()
            .accept(new SqlVisitor.FilterVisitor(SIMPLE_CONTEXT)));
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
        "p.height", Expression.AttributeExpression.create(P_HEIGHT).accept(expressionVisitor));
  }
}
