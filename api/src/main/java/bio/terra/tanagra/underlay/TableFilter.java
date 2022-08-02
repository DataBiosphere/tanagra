package bio.terra.tanagra.underlay;

import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.SQLExpression;
import bio.terra.tanagra.query.TableVariable;
import java.util.List;

public abstract class TableFilter {
  /** Enum for the types of table filters supported by Tanagra. */
  public enum Type {
    BINARY,
    ARRAY;
  }

  public enum BinaryOperator implements SQLExpression {
    EQUALS("="),
    LESS_THAN("<"),
    GREATER_THAN(">"),
    LESS_THAN_OR_EQUAL("<="),
    GREATER_THAN_OR_EQUAL(">=");

    private String sql;

    BinaryOperator(String sql) {
      this.sql = sql;
    }

    @Override
    public String renderSQL() {
      return sql;
    }
  }

  public enum LogicalOperator implements SQLExpression {
    AND,
    OR;

    @Override
    public String renderSQL() {
      return name();
    }
  }

  public TableFilter() {}

  public abstract FilterVariable buildVariable(
      TableVariable primaryTable, List<TableVariable> tables);
}
