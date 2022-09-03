package bio.terra.tanagra.query;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.text.StringSubstitutor;

public class Query implements SQLExpression {
  private final List<FieldVariable> select;
  private final List<TableVariable> tables;
  private final FilterVariable where;
  private final List<FieldVariable> orderBy;
  private final List<FieldVariable> groupBy;
  private final Integer limit;

  private Query(Builder builder) {
    this.select = builder.select;
    this.tables = builder.tables;
    this.where = builder.where;
    this.orderBy = builder.orderBy;
    this.groupBy = builder.groupBy;
    this.limit = builder.limit;
  }

  @Override
  public String renderSQL() {
    // generate a unique alias for each TableVariable
    TableVariable.generateAliases(tables);

    // render each SELECT FieldVariable and join them into a single string
    String selectSQL =
        select.stream()
            .sorted(Comparator.comparing(FieldVariable::getAlias))
            .map(fv -> fv.renderSQL())
            .collect(Collectors.joining(", "));

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
      template = "${sql} WHERE ${whereSQL}";
      params =
          ImmutableMap.<String, String>builder()
              .put("sql", sql)
              .put("whereSQL", where.renderSQL())
              .build();
      sql = StringSubstitutor.replace(template, params);
    }

    // render each GROUP BY FieldVariable and join them into a single string
    if (groupBy != null) {
      // render each GROUP BY FieldVariable and join them into a single string
      String groupBySQL =
          groupBy.stream().map(fv -> fv.renderSqlForOrderBy()).collect(Collectors.joining(", "));

      template = "${sql} GROUP BY ${groupBySQL}";
      params =
          ImmutableMap.<String, String>builder()
              .put("sql", sql)
              .put("groupBySQL", groupBySQL)
              .build();
      sql = StringSubstitutor.replace(template, params);
    }

    // render each ORDER BY FieldVariable and join them into a single string
    if (orderBy != null) {
      // render each ORDER BY FieldVariable and join them into a single string
      String orderBySQL =
          orderBy.stream().map(fv -> fv.renderSqlForOrderBy()).collect(Collectors.joining(", "));

      template = "${sql} ORDER BY ${orderBySQL}";
      params =
          ImmutableMap.<String, String>builder()
              .put("sql", sql)
              .put("orderBySQL", orderBySQL)
              .build();
      sql = StringSubstitutor.replace(template, params);
    }

    if (limit != null) {
      template = "${sql} LIMIT ${limit}";
      params =
          ImmutableMap.<String, String>builder()
              .put("sql", sql)
              .put("limit", String.valueOf(limit))
              .build();
      sql = StringSubstitutor.replace(template, params);
    }

    return sql;
  }

  public List<FieldVariable> getSelect() {
    return Collections.unmodifiableList(select);
  }

  public static class Builder {
    private List<FieldVariable> select;
    private List<TableVariable> tables;
    private FilterVariable where;
    private List<FieldVariable> orderBy;
    private List<FieldVariable> groupBy;
    private Integer limit;

    public Builder select(List<FieldVariable> select) {
      this.select = select;
      return this;
    }

    public Builder tables(List<TableVariable> tables) {
      this.tables = tables;
      return this;
    }

    public Builder where(FilterVariable where) {
      this.where = where;
      return this;
    }

    public Builder orderBy(List<FieldVariable> orderBy) {
      this.orderBy = orderBy;
      return this;
    }

    public Builder groupBy(List<FieldVariable> groupBy) {
      this.groupBy = groupBy;
      return this;
    }

    public Builder limit(Integer limit) {
      this.limit = limit;
      return this;
    }

    public Query build() {
      return new Query(this);
    }
  }
}
