package bio.terra.tanagra.query;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.text.StringSubstitutor;

public class InsertFromSelect implements SQLExpression {
  private final TableVariable insertTable;
  private final List<String> insertColumns;
  private final Query selectQuery;

  public InsertFromSelect(
      TableVariable insertTable, List<String> insertColumns, Query selectQuery) {
    this.insertTable = insertTable;
    this.insertColumns = insertColumns;
    this.selectQuery = selectQuery;
  }

  @Override
  public String renderSQL() {
    // List the insert column names in the same order as the select fields.
    String insertColumnsSQL = insertColumns.stream().collect(Collectors.joining(", "));

    String template = "INSERT INTO ${insertTableSQL} (${insertColumnsSQL}) ${selectQuerySQL}";
    Map<String, String> params =
        ImmutableMap.<String, String>builder()
            .put("insertTableSQL", insertTable.renderSQL())
            .put("insertColumnsSQL", insertColumnsSQL)
            .put("selectQuerySQL", selectQuery.renderSQL())
            .build();
    return StringSubstitutor.replace(template, params);
  }
}
