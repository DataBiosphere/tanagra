package bio.terra.tanagra.service.query.api;

import bio.terra.tanagra.generated.model.ApiAttributeValue;
import bio.terra.tanagra.generated.model.ApiEntityInstanceStruct;
import bio.terra.tanagra.service.databaseaccess.CellValue;
import bio.terra.tanagra.service.databaseaccess.ColumnHeaderSchema;
import bio.terra.tanagra.service.databaseaccess.QueryResult;
import bio.terra.tanagra.service.databaseaccess.RowResult;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.annotation.Nullable;

/** Utilities for converting query result classes to API classes. */
public final class QueryResultConverter {
  private QueryResultConverter() {}

  public static List<ApiEntityInstanceStruct> convert(QueryResult queryResult) {
    ImmutableList.Builder<ApiEntityInstanceStruct> structs = ImmutableList.builder();
    for (RowResult rowResult : queryResult.rowResults()) {
      structs.add(convert(rowResult, queryResult.columnHeaderSchema()));
    }
    return structs.build();
  }

  static ApiEntityInstanceStruct convert(
      RowResult rowResult, ColumnHeaderSchema columnHeaderSchema) {
    ApiEntityInstanceStruct result = new ApiEntityInstanceStruct();
    for (int i = 0; i < rowResult.size(); ++i) {
      String name = columnHeaderSchema.columnSchemas().get(i).name();
      ApiAttributeValue apiAttributeValue = convert(rowResult.get(i));
      result.put(name, apiAttributeValue);
    }
    return result;
  }

  /**
   * Converts a CellValue to an {@link ApiAttributeValue}. Returns null if the value of the cell is
   * null.
   */
  @VisibleForTesting
  @Nullable
  static ApiAttributeValue convert(CellValue cellValue) {
    switch (cellValue.dataType()) {
      case STRING:
        return cellValue
            .getString()
            .map(value -> new ApiAttributeValue().stringVal(value))
            .orElse(null);
      case INT64:
        return (cellValue.getLong().isEmpty())
            ? null
            : new ApiAttributeValue().int64Val(cellValue.getLong().getAsLong());
      default:
        throw new UnsupportedOperationException(
            String.format("Unable to convert CellValue DataType '%s'", cellValue.dataType()));
    }
  }
}
