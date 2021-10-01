package bio.terra.tanagra.service.databaseaccess.postgres;

import bio.terra.tanagra.service.databaseaccess.CellValue;
import bio.terra.tanagra.service.databaseaccess.ColumnHeaderSchema;
import bio.terra.tanagra.service.databaseaccess.ColumnSchema;
import bio.terra.tanagra.service.databaseaccess.EagerCellValue;
import bio.terra.tanagra.service.databaseaccess.EagerRowResult;
import bio.terra.tanagra.service.databaseaccess.RowResult;
import bio.terra.tanagra.service.search.DataType;
import com.google.common.collect.ImmutableList;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

/** A {@link RowMapper} forparsing {@link ResultSet} into {@link RowResult}s. */
class RowResultMapper implements RowMapper<RowResult> {

  private final ColumnHeaderSchema columnHeaderSchema;

  RowResultMapper(ColumnHeaderSchema columnHeaderSchema) {
    this.columnHeaderSchema = columnHeaderSchema;
  }

  @Override
  public RowResult mapRow(ResultSet rs, int rowNum) throws SQLException {
    ImmutableList.Builder<CellValue> cellValues = ImmutableList.builder();
    for (ColumnSchema columnSchema : columnHeaderSchema.columnSchemas()) {
      cellValues.add(createCellValue(columnSchema, rs));
    }
    return EagerRowResult.builder()
        .cellValues(cellValues.build())
        .columnHeaderSchema(columnHeaderSchema)
        .build();
  }

  private CellValue createCellValue(ColumnSchema columnSchema, ResultSet rs) throws SQLException {
    switch (columnSchema.dataType()) {
      case STRING:
        return EagerCellValue.of(rs.getString(columnSchema.name()));
      case INT64:
        long longVal = rs.getLong(columnSchema.name());
        if (rs.wasNull()) {
          return EagerCellValue.ofNull(DataType.INT64);
        } else {
          return EagerCellValue.of(longVal);
        }
      default:
        throw new UnsupportedOperationException(
            String.format("Unable to convert DataType to CellValue '%s'", columnSchema.dataType()));
    }
  }
}
