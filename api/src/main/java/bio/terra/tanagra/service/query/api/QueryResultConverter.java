package bio.terra.tanagra.service.query.api;

import bio.terra.tanagra.generated.model.ApiAttributeValue;
import bio.terra.tanagra.generated.model.ApiEntityCountStruct;
import bio.terra.tanagra.generated.model.ApiEntityInstanceStruct;
import bio.terra.tanagra.service.databaseaccess.CellValue;
import bio.terra.tanagra.service.databaseaccess.ColumnHeaderSchema;
import bio.terra.tanagra.service.databaseaccess.QueryResult;
import bio.terra.tanagra.service.databaseaccess.RowResult;
import bio.terra.tanagra.service.query.QueryService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/** Utilities for converting query result classes to API classes. */
public final class QueryResultConverter {
  private QueryResultConverter() {}

  public static List<ApiEntityInstanceStruct> convertToEntityInstances(QueryResult queryResult) {
    ImmutableList.Builder<ApiEntityInstanceStruct> structs = ImmutableList.builder();
    for (RowResult rowResult : queryResult.rowResults()) {
      structs.add(convertToEntityInstance(rowResult, queryResult.columnHeaderSchema()));
    }
    return structs.build();
  }

  static ApiEntityInstanceStruct convertToEntityInstance(
      RowResult rowResult, ColumnHeaderSchema columnHeaderSchema) {
    ApiEntityInstanceStruct result = new ApiEntityInstanceStruct();
    result.putAll(convertRowToAttributeMap(rowResult, columnHeaderSchema));
    return result;
  }

  public static List<ApiEntityCountStruct> convertToEntityCounts(QueryResult queryResult) {
    ImmutableList.Builder<ApiEntityCountStruct> structs = ImmutableList.builder();
    for (RowResult rowResult : queryResult.rowResults()) {
      structs.add(convertToEntityCount(rowResult, queryResult.columnHeaderSchema()));
    }
    return structs.build();
  }

  static ApiEntityCountStruct convertToEntityCount(
      RowResult rowResult, ColumnHeaderSchema columnHeaderSchema) {
    ApiEntityCountStruct result = new ApiEntityCountStruct();
    Map<String, ApiAttributeValue> attributeMap =
        convertRowToAttributeMap(rowResult, columnHeaderSchema);
    ApiAttributeValue count = attributeMap.remove(QueryService.COUNT_ALIAS);
    result.setCount(Math.toIntExact(count.getInt64Val()));
    result.putAll(attributeMap);
    return result;
  }

  static Map<String, ApiAttributeValue> convertRowToAttributeMap(
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
