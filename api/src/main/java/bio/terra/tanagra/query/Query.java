package bio.terra.tanagra.query;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.text.StringSubstitutor;

public class Query implements SQLExpression {
  private List<FieldVariable> select;
  private List<TableVariable> tables;
  private FilterVariable where;
  private List<FieldVariable> orderBy;
  private List<FieldVariable> groupBy;

  public Query(List<FieldVariable> select, List<TableVariable> tables) {
    this.select = select;
    this.tables = tables;
  }

  public Query(List<FieldVariable> select, List<TableVariable> tables, FilterVariable where) {
    this.select = select;
    this.tables = tables;
    this.where = where;
  }

  public Query(
      List<FieldVariable> select,
      List<TableVariable> tables,
      FilterVariable where,
      List<FieldVariable> orderBy,
      List<FieldVariable> groupBy) {
    this.select = select;
    this.tables = tables;
    this.where = where;
    this.orderBy = orderBy;
    this.groupBy = groupBy;
  }

  @Override
  public String renderSQL() {
    // TODO: we should be able to build list of distinct TableVariables, by iterating through all
    // the FieldVariables and FilterVariable. then no need to pass in the list of TableVariables or
    // pass it around when building the lists of FieldVariables

    // generate a unique alias for each TableVariable
    TableVariable.generateAliases(tables);

    // render each SELECT FieldVariable and join them into a single string
    String selectSQL = select.stream().map(fv -> fv.renderSQL()).collect(Collectors.joining(", "));

    // render each TableVariable and join them into a single string
    String fromSQL = tables.stream().map(tv -> tv.renderSQL()).collect(Collectors.joining(" "));

    String template = "SELECT ${selectSQL} FROM ${fromSQL}";
    Map<String, String> params =
        ImmutableMap.<String, String>builder()
            .put("selectSQL", selectSQL)
            .put("fromSQL", fromSQL)
            .build();
    String sql = StringSubstitutor.replace(template, params);

    // render the FilterVariable
    if (where != null) {
      template = "${selectFromSQL} WHERE ${whereSQL}";
      params =
          ImmutableMap.<String, String>builder()
              .put("selectFromSQL", sql)
              .put("whereSQL", where.renderSQL())
              .build();
      sql = StringSubstitutor.replace(template, params);
    }

    // render each ORDER BY FieldVariable and join them into a single string

    // render each GROUP BY FieldVariable and join them into a single string

    return sql;
  }
}
