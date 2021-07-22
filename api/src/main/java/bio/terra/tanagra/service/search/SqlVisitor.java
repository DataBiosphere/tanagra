package bio.terra.tanagra.service.search;

import bio.terra.tanagra.service.search.Expression.AttributeExpression;
import bio.terra.tanagra.service.search.Filter.ArrayFunction;
import bio.terra.tanagra.service.search.Filter.BinaryFunction;
import bio.terra.tanagra.service.search.Filter.Visitor;
import java.util.stream.Collectors;

/** Visitors for walking query constructs to create SQL. */
// TODO consider how this may need to be split for different SQL backends.
public class SqlVisitor {
  /** A {@link Visitor} for creating SQL for filters. */
  static class FilterVisitor implements Visitor<String> {
    private final SearchContext searchContext;

    FilterVisitor(SearchContext searchContext) {
      this.searchContext = searchContext;
    }

    @Override
    public String visitArrayFunction(ArrayFunction arrayFunction) {
      String operatorDelimiter = String.format(" %s ", convert(arrayFunction.operator()));
      // e.g. (operand0) OR (operand1)
      return String.join(
          operatorDelimiter,
          arrayFunction.operands().stream()
              // Recursively evaluate each operand, wrapping it in parens.
              .map(f -> String.format("(%s)", f.accept(this)))
              .collect(Collectors.toList()));
    }

    private static String convert(ArrayFunction.Operator operator) {
      switch (operator) {
        case AND:
          return "AND";
        case OR:
          return "OR";
        default:
          throw new UnsupportedOperationException(
              String.format("Unable to convert ArrayFunction.Operator %s to SQL string", operator));
      }
    }

    @Override
    public String visitBinaryComparision(BinaryFunction binaryFunction) {
      ExpressionVisitor expressionVisitor = new ExpressionVisitor(searchContext);

      String leftSql = binaryFunction.left().accept(expressionVisitor);
      String rightSql = binaryFunction.right().accept(expressionVisitor);
      switch (binaryFunction.operator()) {
        case LESS_THAN:
          return String.format("%s < %s", leftSql, rightSql);
        case EQUALS:
          return String.format("%s = %s", leftSql, rightSql);
        default:
          throw new UnsupportedOperationException(
              String.format("Unsupported BinaryFunction.Operator %s", binaryFunction.operator()));
      }
    }
  }

  /** A {@link Expression.Visitor} for creating SQL for expressions. */
  static class ExpressionVisitor implements Expression.Visitor<String> {
    private final SearchContext searchContext;

    ExpressionVisitor(SearchContext searchContext) {
      this.searchContext = searchContext;
    }

    @Override
    public String visitLiteral(Expression.Literal literal) {
      // TODO consider parametrizing output to avoid injection.
      if (DataType.STRING.equals(literal.dataType())) {
        return String.format("'%s'", literal.value());
      }
      return literal.value();
    }

    @Override
    public String visitAttribute(AttributeExpression attributeExpression) {
      // TODO consider aliasing entities.
      return searchContext.underlaySqlResolver().resolve(attributeExpression.attribute());
    }
  }
}
