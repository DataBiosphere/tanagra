package bio.terra.tanagra.service.query.api;

import bio.terra.tanagra.generated.model.ApiAttributeValue;
import bio.terra.tanagra.generated.model.ApiEntityInstanceStruct;
import bio.terra.tanagra.service.databaseaccess.CellValue;
import bio.terra.tanagra.service.databaseaccess.ColumnHeaderSchema;
import bio.terra.tanagra.service.databaseaccess.QueryResult;
import bio.terra.tanagra.service.databaseaccess.RowResult;
import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.annotation.Nullable;

// DO NOT SUBMIT comment & test me.
public final class QueryResultConverter {
  private QueryResultConverter() {}

  public static List<ApiEntityInstanceStruct> convert(QueryResult queryResult) {
    ImmutableList.Builder<ApiEntityInstanceStruct> structs = ImmutableList.builder();
    for (RowResult rowResult : queryResult.rowResults()) {
      structs.add(convert(rowResult, queryResult.columnHeaderSchema()));
    }
    return structs.build();
  }

  public static ApiEntityInstanceStruct convert(
      RowResult rowResult, ColumnHeaderSchema columnHeaderSchema) {
    ApiEntityInstanceStruct result = new ApiEntityInstanceStruct();
    for (int i = 0; i < rowResult.size(); ++i) {
      String name = columnHeaderSchema.columnSchemas().get(i).name();
      ApiAttributeValue apiAttributeValue = convert(rowResult.get(i));
      result.put(name, apiAttributeValue);
    }
    return result;
  }

  @Nullable
  public static ApiAttributeValue convert(CellValue cellValue) {
    if (cellValue.isNull()) {
      return null;
    }
    switch (cellValue.dataType()) {
      case STRING:
        return new ApiAttributeValue().stringVal(cellValue.getString());
      case INT64:
        return new ApiAttributeValue().int64Val(cellValue.getLong());
      default:
        throw new UnsupportedOperationException(
            String.format("Unable to convert CellValue DataType '%s'", cellValue.dataType()));
    }
  }
}
