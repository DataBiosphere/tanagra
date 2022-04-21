package bio.terra.tanagra.service.search;

import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT_NAME;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT_TYPE_ID;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT_TYPE_NAME;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT_T_PATH_TYPE_ID;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.RESERVATION;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.RESERVATION_DAY;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR_NAME;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR_RATING;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.loadNauticalUnderlay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.common.exception.BadRequestException;
import bio.terra.tanagra.service.search.Filter.NullFilter;
import bio.terra.tanagra.service.search.SqlVisitor.SelectionVisitor;
import bio.terra.tanagra.testing.BaseSpringUnitTest;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class SqlVisitorTest extends BaseSpringUnitTest {
  private static final Variable S_VAR = Variable.create("s");
  private static final EntityVariable S_SAILOR = EntityVariable.create(SAILOR, S_VAR);
  private static final AttributeVariable S_NAME = AttributeVariable.create(SAILOR_NAME, S_VAR);
  private static final AttributeVariable S_RATING = AttributeVariable.create(SAILOR_RATING, S_VAR);

  private static final Variable R_VAR = Variable.create("r");
  private static final EntityVariable R_RESERVATION = EntityVariable.create(RESERVATION, R_VAR);
  private static final AttributeVariable R_DAY = AttributeVariable.create(RESERVATION_DAY, R_VAR);

  private static final Variable B_VAR = Variable.create("b");
  private static final AttributeVariable B_NAME = AttributeVariable.create(BOAT_NAME, B_VAR);
  private static final AttributeVariable B_TYPE = AttributeVariable.create(BOAT_TYPE_NAME, B_VAR);
  private static final AttributeVariable B_T_PATH_TYPE =
      AttributeVariable.create(BOAT_T_PATH_TYPE_ID, B_VAR);

  private SearchContext getSimpleContext() {
    return SearchContext.builder()
        .underlay(loadNauticalUnderlay())
        .randomNumberGenerator(randomNumberGenerator)
        .build();
  }

  @Test
  void query() {
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
    assertEquals(
        "SELECT s.rating AS rating, s.s_name AS name FROM `my-project-id.nautical`.sailors AS s WHERE s.rating = 62",
        new SqlVisitor(getSimpleContext()).createSql(query));
  }

  @Test
  void selectionExpression() {
    SqlVisitor.SelectionVisitor visitor = new SelectionVisitor(getSimpleContext());
    assertEquals(
        "s.rating AS rt",
        Selection.SelectExpression.builder()
            .expression(Expression.AttributeExpression.create(S_RATING))
            .name("rt")
            .build()
            .accept(visitor));
  }

  @Test
  void selectionCount() {
    SqlVisitor.SelectionVisitor visitor = new SelectionVisitor(getSimpleContext());
    assertEquals(
        "COUNT(s) AS c",
        Selection.Count.builder().entityVariable(S_SAILOR).name("c").build().accept(visitor));
  }

  @Test
  void selectionPrimaryKey() {
    SqlVisitor.SelectionVisitor visitor = new SelectionVisitor(getSimpleContext());
    assertEquals(
        "s.s_id AS primary_id",
        Selection.PrimaryKey.builder()
            .entityVariable(S_SAILOR)
            .name("primary_id")
            .build()
            .accept(visitor));
  }

  @Test
  void selectionExpressionHierarchyPath() {
    SqlVisitor.SelectionVisitor visitor = new SelectionVisitor(getSimpleContext());
    assertEquals(
        "(SELECT boat_types_paths.bt_path FROM `my-project-id.nautical`.boat_types_paths WHERE boat_types_paths.bt_node = b.bt_id) AS path",
        Selection.SelectExpression.builder()
            .expression(Expression.AttributeExpression.create(B_T_PATH_TYPE))
            .name("path")
            .build()
            .accept(visitor));
  }

  @Test
  void filterBinaryFunction() {
    Filter.BinaryFunction lessThanFilter =
        Filter.BinaryFunction.create(
            Expression.AttributeExpression.create(S_RATING),
            Filter.BinaryFunction.Operator.LESS_THAN,
            Expression.Literal.create(DataType.INT64, "62"));
    assertEquals(
        "s.rating < 62", lessThanFilter.accept(new SqlVisitor.FilterVisitor(getSimpleContext())));

    Filter.BinaryFunction greaterThanFilter =
        Filter.BinaryFunction.create(
            Expression.AttributeExpression.create(S_RATING),
            Filter.BinaryFunction.Operator.GREATER_THAN,
            Expression.Literal.create(DataType.INT64, "62"));
    assertEquals(
        "s.rating > 62",
        greaterThanFilter.accept(new SqlVisitor.FilterVisitor(getSimpleContext())));

    Filter.BinaryFunction equalsFilter =
        Filter.BinaryFunction.create(
            Expression.AttributeExpression.create(S_RATING),
            Filter.BinaryFunction.Operator.EQUALS,
            Expression.Literal.create(DataType.INT64, "62"));
    assertEquals(
        "s.rating = 62", equalsFilter.accept(new SqlVisitor.FilterVisitor(getSimpleContext())));
  }

  @Test
  void filterBinaryFunctionDescendantOfInclusive() {
    Filter descendantOfFilter =
        Filter.BinaryFunction.create(
            Expression.AttributeExpression.create(AttributeVariable.create(BOAT_TYPE_ID, B_VAR)),
            Filter.BinaryFunction.Operator.DESCENDANT_OF_INCLUSIVE,
            Expression.Literal.create(DataType.INT64, "43"));
    assertEquals(
        "(b.bt_id = 43 OR b.bt_id IN (SELECT bt_descendant "
            + "FROM `my-project-id.nautical`.boat_types_descendants WHERE bt_ancestor = 43))",
        descendantOfFilter.accept(new SqlVisitor.FilterVisitor(getSimpleContext())));

    Filter nonHierarchicalAttribute =
        Filter.BinaryFunction.create(
            Expression.AttributeExpression.create(AttributeVariable.create(BOAT_NAME, B_VAR)),
            Filter.BinaryFunction.Operator.DESCENDANT_OF_INCLUSIVE,
            Expression.Literal.create(DataType.INT64, "Foo"));
    assertThrows(
        BadRequestException.class,
        () -> nonHierarchicalAttribute.accept(new SqlVisitor.FilterVisitor(getSimpleContext())));
  }

  @Test
  void filterBinaryFunctionChildOfWithTableFilter() {
    Filter childOfFilter =
        Filter.BinaryFunction.create(
            Expression.AttributeExpression.create(AttributeVariable.create(BOAT_TYPE_ID, B_VAR)),
            Filter.BinaryFunction.Operator.CHILD_OF,
            Expression.Literal.create(DataType.INT64, "432"));
    assertEquals(
        "b.bt_id IN (SELECT btc_child "
            + "FROM (SELECT * FROM `my-project-id.nautical`.boat_types_children WHERE btc_is_expired = 'false') "
            + "WHERE btc_parent = 432)",
        childOfFilter.accept(new SqlVisitor.FilterVisitor(getSimpleContext())));

    Filter nonHierarchicalAttribute =
        Filter.BinaryFunction.create(
            Expression.AttributeExpression.create(AttributeVariable.create(BOAT_NAME, B_VAR)),
            Filter.BinaryFunction.Operator.CHILD_OF,
            Expression.Literal.create(DataType.STRING, "Foo"));
    assertThrows(
        BadRequestException.class,
        () -> nonHierarchicalAttribute.accept(new SqlVisitor.FilterVisitor(getSimpleContext())));
  }

  @Test
  void filterArrayFunction() {
    ImmutableList<Filter> operands =
        ImmutableList.of(
            Filter.BinaryFunction.create(
                Expression.AttributeExpression.create(S_RATING),
                Filter.BinaryFunction.Operator.LESS_THAN,
                Expression.Literal.create(DataType.INT64, "62")),
            Filter.BinaryFunction.create(
                Expression.AttributeExpression.create(S_NAME),
                Filter.BinaryFunction.Operator.EQUALS,
                Expression.Literal.create(DataType.STRING, "John")));

    Filter andFilter = Filter.ArrayFunction.create(operands, Filter.ArrayFunction.Operator.AND);
    assertEquals(
        "s.rating < 62 AND s.s_name = 'John'",
        andFilter.accept(new SqlVisitor.FilterVisitor(getSimpleContext())));

    Filter orFilter = Filter.ArrayFunction.create(operands, Filter.ArrayFunction.Operator.OR);
    assertEquals(
        "s.rating < 62 OR s.s_name = 'John'",
        orFilter.accept(new SqlVisitor.FilterVisitor(getSimpleContext())));
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
        "s.s_id IN (SELECT r.s_id FROM `my-project-id.nautical`.reservations AS r WHERE r.day = 'Tuesday')",
        Filter.RelationshipFilter.builder()
            .outerVariable(S_SAILOR)
            .newVariable(R_RESERVATION)
            .filter(
                Filter.BinaryFunction.create(
                    Expression.AttributeExpression.create(R_DAY),
                    Filter.BinaryFunction.Operator.EQUALS,
                    Expression.Literal.create(DataType.STRING, "Tuesday")))
            .build()
            .accept(new SqlVisitor.FilterVisitor(getSimpleContext())));
  }

  @Test
  void filterRelationshipNested() {
    Variable r2 = Variable.create("r2");
    assertEquals(
        "s.s_id IN (SELECT r.s_id FROM `my-project-id.nautical`.reservations AS r WHERE "
            + "r.day = 'Tuesday' AND s.s_id IN (SELECT r2.s_id FROM "
            + "`my-project-id.nautical`.reservations AS r2 WHERE r2.day = 'Wednesday'))",
        Filter.RelationshipFilter.builder()
            .outerVariable(S_SAILOR)
            .newVariable(R_RESERVATION)
            .filter(
                Filter.ArrayFunction.create(
                    ImmutableList.of(
                        Filter.BinaryFunction.create(
                            Expression.AttributeExpression.create(R_DAY),
                            Filter.BinaryFunction.Operator.EQUALS,
                            Expression.Literal.create(DataType.STRING, "Tuesday")),
                        Filter.RelationshipFilter.builder()
                            .outerVariable(S_SAILOR)
                            .newVariable(EntityVariable.create(RESERVATION, r2))
                            .filter(
                                Filter.BinaryFunction.create(
                                    Expression.AttributeExpression.create(
                                        AttributeVariable.create(RESERVATION_DAY, r2)),
                                    Filter.BinaryFunction.Operator.EQUALS,
                                    Expression.Literal.create(DataType.STRING, "Wednesday")))
                            .build()),
                    Filter.ArrayFunction.Operator.AND))
            .build()
            .accept(new SqlVisitor.FilterVisitor(getSimpleContext())));
  }

  @Test
  void filterTextSearch() {
    assertEquals(
        "s.s_id IN (SELECT s_id FROM `my-project-id.nautical`.sailors WHERE CONTAINS_SUBSTR(s_name, 'george'))",
        Filter.TextSearchFilter.create(
                S_SAILOR, Expression.Literal.create(DataType.STRING, "george"))
            .accept(new SqlVisitor.FilterVisitor(getSimpleContext())));
  }

  @Test
  void filterNull() {
    assertEquals(
        "TRUE", NullFilter.INSTANCE.accept(new SqlVisitor.FilterVisitor(getSimpleContext())));
  }

  @Test
  void expressionLiteral() {
    SqlVisitor.ExpressionVisitor expressionVisitor =
        new SqlVisitor.ExpressionVisitor(getSimpleContext());
    assertEquals("42", Expression.Literal.create(DataType.INT64, "42").accept(expressionVisitor));
    assertEquals(
        "'foo'", Expression.Literal.create(DataType.STRING, "foo").accept(expressionVisitor));
  }

  @Test
  void expressionAttribute() {
    SqlVisitor.ExpressionVisitor expressionVisitor =
        new SqlVisitor.ExpressionVisitor(getSimpleContext());
    // SimpleColumn attribute mapping
    assertEquals(
        "b.b_name", Expression.AttributeExpression.create(B_NAME).accept(expressionVisitor));
    // NormalizedColumn attribute mapping
    assertEquals(
        "(SELECT boat_types.bt_name FROM `my-project-id.nautical`.boat_types WHERE boat_types.bt_id = b.bt_id)",
        Expression.AttributeExpression.create(B_TYPE).accept(expressionVisitor));
  }
}
