package bio.terra.tanagra.service.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.tanagra.service.underlay.NauticalUnderlayUtils;
import bio.terra.tanagra.service.underlay.Underlay;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class DataTypeVisitorTest {
  private static final Underlay NAUTICAL_UNDERLAY = NauticalUnderlayUtils.loadNauticalUnderlay();

  @Test
  void selectionExpression() {
    assertEquals(
        DataType.STRING,
        Selection.SelectExpression.builder()
            .expression(Expression.Literal.create(DataType.STRING, "foo"))
            .name("x")
            .build()
            .accept(new DataTypeVisitor.SelectionVisitor(NAUTICAL_UNDERLAY)));
  }

  @Test
  void selectionPrimaryKey() {
    assertEquals(
        DataType.INT64,
        Selection.PrimaryKey.builder()
            .entityVariable(
                EntityVariable.create(NauticalUnderlayUtils.SAILOR, Variable.create("s")))
            .name("s_id")
            .build()
            .accept(new DataTypeVisitor.SelectionVisitor(NAUTICAL_UNDERLAY)));
  }

  @Test
  void selectionCount() {
    assertEquals(
        DataType.INT64,
        Selection.Count.builder()
            .entityVariable(
                EntityVariable.create(NauticalUnderlayUtils.SAILOR, Variable.create("s")))
            .name("c")
            .build()
            .accept(new DataTypeVisitor.SelectionVisitor(NAUTICAL_UNDERLAY)));
  }

  @Test
  void expressionLiteral() {
    assertEquals(
        DataType.INT64,
        Expression.Literal.create(DataType.INT64, "42")
            .accept(new DataTypeVisitor.ExpressionVisitor()));
  }

  @Test
  void expressionAttribute() {
    assertEquals(
        DataType.STRING,
        Expression.AttributeExpression.create(
                AttributeVariable.create(NauticalUnderlayUtils.SAILOR_NAME, Variable.create("s")))
            .accept(new DataTypeVisitor.ExpressionVisitor()));
  }
}
