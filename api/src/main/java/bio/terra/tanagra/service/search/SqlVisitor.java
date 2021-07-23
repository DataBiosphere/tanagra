package bio.terra.tanagra.service.search;

import bio.terra.tanagra.service.search.Expression.AttributeExpression;
import bio.terra.tanagra.service.search.Filter.ArrayFunction;
import bio.terra.tanagra.service.search.Filter.BinaryFunction;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.text.StringSubstitutor;

/** Visitors for walking query constructs to create SQL. */
// TODO consider how this may need to be split for different SQL backends.
public class SqlVisitor {
  private final SearchContext searchContext;

  public SqlVisitor(SearchContext searchContext) {
    this.searchContext = searchContext;
  }

  public String createSql(Query query) {
    String selections =
        query.selections().stream()
            .map(selectField -> selectField.accept(new SelectionVisitor(searchContext)))
            .collect(Collectors.joining(", "));
    Optional<String> filterSql =
        query.filter().map(predicate -> predicate.accept(new FilterVisitor(searchContext)));

    String template = "SELECT ${selections} FROM ${table} WHERE ${filter}";
    Map<String, String> params =
        ImmutableMap.<String, String>builder()
            .put("selections", selections)
            .put("table", searchContext.underlaySqlResolver().resolveTable(query.primaryEntity()))
            .put("filter", filterSql.orElse("TRUE"))
            .build();
    return StringSubstitutor.replace(template, params);
  }

  /** A {@link Selection.Visitor} for creating SQL for selections. */
  static class SelectionVisitor implements Selection.Visitor<String> {
    private final SearchContext searchContext;

    SelectionVisitor(SearchContext searchContext) {
      this.searchContext = searchContext;
    }

    @Override
    public String selectExpression(Selection.SelectExpression selectExpression) {
      String expression =
          selectExpression.expression().accept(new ExpressionVisitor(searchContext));
      return expression.concat(aliasSuffix(selectExpression.alias()));
    }

    @Override
    public String count(Selection.Count count) {
      // TODO consider aliasing entities.
      return String.format("COUNT(%s)%s", count.entity().name(), aliasSuffix(count.alias()));
    }

    /** Returns " AS alias" or else "" if the alias is not present. */
    private static String aliasSuffix(Optional<String> alias) {
      return alias.map(" AS "::concat).orElse("");
    }
  }

  /** A {@link Filter.Visitor} for creating SQL for filters. */
  static class FilterVisitor implements Filter.Visitor<String> {
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
      // TODO consider parameterizing output to avoid injection.
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
