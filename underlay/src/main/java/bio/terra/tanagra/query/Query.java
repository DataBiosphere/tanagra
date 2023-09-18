package bio.terra.tanagra.query;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.filtervariable.HavingFilterVariable;
import com.google.common.collect.ImmutableMap;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.text.StringSubstitutor;

public class Query implements SQLExpression {
  public static final String TANAGRA_FIELD_PREFIX = "t_";

  private final List<FieldVariable> select;
  private final List<TableVariable> tables;
  private final FilterVariable where;
  private final List<OrderByVariable> orderBy;
  private final List<FieldVariable> groupBy;
  private final HavingFilterVariable having;
  private final Integer limit;

  private final Integer offset;

  private Query(Builder builder) {
    this.select = builder.select;
    this.tables = builder.tables;
    this.where = builder.where;
    this.orderBy = builder.orderBy;
    this.groupBy = builder.groupBy;
    this.having = builder.having;
    this.limit = builder.limit;
    this.offset = builder.offset;
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

    // render the primary TableVariable
    String template = "SELECT ${selectSQL} FROM ${primaryTableFromSQL}";
    Map<String, String> params =
        ImmutableMap.<String, String>builder()
            .put("selectSQL", selectSQL)
            .put("primaryTableFromSQL", getPrimaryTable().renderSQL())
            .build();
    String sql = StringSubstitutor.replace(template, params);

    // render the join TableVariables
    if (tables.size() > 1) {
      String joinTablesFromSQL =
          tables.stream()
              .map(tv -> tv.isPrimary() ? "" : tv.renderSQL())
              .collect(Collectors.joining(" "));
      template = "${sql} ${joinTablesFromSQL}";
      params =
          ImmutableMap.<String, String>builder()
              .put("sql", sql)
              .put("joinTablesFromSQL", joinTablesFromSQL)
              .build();
      sql = StringSubstitutor.replace(template, params);
    }

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

    if (groupBy != null && !groupBy.isEmpty()) {
      // render each GROUP BY FieldVariable and join them into a single string
      String groupBySQL =
          groupBy.stream()
              .map(fv -> fv.renderSqlForOrderOrGroupBy(select.contains(fv)))
              .collect(Collectors.joining(", "));

      template = "${sql} GROUP BY ${groupBySQL}";
      params =
          ImmutableMap.<String, String>builder()
              .put("sql", sql)
              .put("groupBySQL", groupBySQL)
              .build();
      sql = StringSubstitutor.replace(template, params);
    }

    if (having != null) {
      sql += " " + having.renderSQL();
    }

    if (orderBy != null && !orderBy.isEmpty()) {
      // render each ORDER BY FieldVariable and join them into a single string
      String orderBySQL =
          orderBy.stream()
              .map(
                  obv ->
                      obv.isRandom()
                          ? obv.renderSQL()
                          : obv.setIsIncludedInSelect(select.contains(obv.getFieldVariable()))
                              .renderSQL())
              .collect(Collectors.joining(", "));

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

    if (offset != null) {
      template = "${sql} OFFSET ${offset}";
      params =
          ImmutableMap.<String, String>builder()
              .put("sql", sql)
              .put("offset", String.valueOf(offset))
              .build();
      sql = StringSubstitutor.replace(template, params);
    }

    return sql;
  }

  public List<FieldVariable> getSelect() {
    return Collections.unmodifiableList(select);
  }

  public TableVariable getPrimaryTable() {
    List<TableVariable> primaryTable =
        tables.stream().filter(table -> table.isPrimary()).collect(Collectors.toList());
    if (primaryTable.size() != 1) {
      throw new SystemException(
          "Query can only have one primary table, but found " + primaryTable.size());
    }
    return primaryTable.get(0);
  }

  public static class Builder {
    private List<FieldVariable> select;
    private List<TableVariable> tables;
    private FilterVariable where;
    private List<OrderByVariable> orderBy;
    private List<FieldVariable> groupBy;
    private HavingFilterVariable having;
    private Integer limit;
    private Integer offset;

    public Builder select(List<FieldVariable> select) {
      this.select = new ArrayList<>(select);
      return this;
    }

    public Builder addSelect(FieldVariable newSelect) {
      if (this.select == null) {
        this.select = new ArrayList<>();
      }
      this.select.add(newSelect);
      return this;
    }

    public Builder tables(List<TableVariable> tables) {
      this.tables = new ArrayList<>(tables);
      return this;
    }

    public Builder addTable(TableVariable newTable) {
      if (this.tables == null) {
        this.tables = new ArrayList<>();
      }
      if (!this.tables.contains(newTable)) {
        this.tables.add(newTable);
      }
      return this;
    }

    public Builder where(FilterVariable where) {
      this.where = where;
      return this;
    }

    public Builder orderBy(List<OrderByVariable> orderBy) {
      this.orderBy = orderBy;
      return this;
    }

    public Builder groupBy(List<FieldVariable> groupBy) {
      this.groupBy = groupBy;
      return this;
    }

    public Builder having(HavingFilterVariable having) {
      this.having = having;
      return this;
    }

    public Builder limit(Integer limit) {
      this.limit = limit;
      return this;
    }

    public Builder offset(Integer offset) {
      this.offset = offset;
      return this;
    }

    public Query build() {
      return new Query(this);
    }

    public List<FieldVariable> getSelect() {
      return select;
    }

    public List<TableVariable> getTables() {
      return tables;
    }
  }
}
