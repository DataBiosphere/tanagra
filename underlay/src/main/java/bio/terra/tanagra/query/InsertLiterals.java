package bio.terra.tanagra.query;

import com.google.common.collect.ImmutableMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.text.StringSubstitutor;

public class InsertLiterals implements SQLExpression {
    private final TableVariable insertTable;
    private final List<FieldVariable> valueFields;
    private final List<List<Literal>> rowsOfLiterals;

    public InsertLiterals(
            TableVariable insertTable, List<FieldVariable> valueFields, List<List<Literal>> rowsOfLiterals) {
        this.insertTable = insertTable;
        this.valueFields = valueFields;
        this.rowsOfLiterals = rowsOfLiterals;
    }

    @Override
    public String renderSQL() {
        String insertFieldsSQL =
                valueFields.stream()
                        .map(valueField -> valueField.getAliasOrColumnName())
                        .collect(Collectors.joining(", "));

        String rowsOfLiteralsSQL = rowsOfLiterals.stream()
                .map(row -> "(" + row.stream().map(rowField -> rowField.renderSQL()).collect(Collectors.joining(",")) + ")")
                .collect(Collectors.joining(", "));

        String template = "INSERT INTO ${insertTableSQL} (${insertFieldsSQL}) VALUES ${rowsOfLiterals}";
        Map<String, String> params =
                ImmutableMap.<String, String>builder()
                        .put("insertTableSQL", insertTable.renderSQL())
                        .put("insertFieldsSQL", insertFieldsSQL)
                        .put("rowsOfLiterals", rowsOfLiteralsSQL)
                        .build();
        return StringSubstitutor.replace(template, params);
    }
}
