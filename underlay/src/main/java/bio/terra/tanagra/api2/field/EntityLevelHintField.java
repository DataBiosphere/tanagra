package bio.terra.tanagra.api2.field;

import bio.terra.tanagra.api2.query.ValueDisplay;
import bio.terra.tanagra.api2.query.hint.Hint;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.*;
import bio.terra.tanagra.underlay2.Underlay;
import bio.terra.tanagra.underlay2.entitymodel.Entity;
import bio.terra.tanagra.underlay2.indextable.ITEntityLevelDisplayHints;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class EntityLevelHintField extends HintField {
  private final ITEntityLevelDisplayHints indexTable;

  public EntityLevelHintField(Underlay underlay, Entity entity) {
    this.indexTable = underlay.getIndexSchema().getEntityLevelDisplayHints(entity.getName());
  }

  @Override
  public List<FieldVariable> buildFieldVariables(
      TableVariable elHintTableVar, List<TableVariable> tableVars) {
    return Arrays.stream(ITEntityLevelDisplayHints.Column.values())
        .map(
            column ->
                new FieldPointer.Builder()
                    .tablePointer(indexTable.getTablePointer())
                    .columnName(column.getSchema().getColumnName())
                    .build()
                    .buildVariable(elHintTableVar, tableVars))
        .collect(Collectors.toList());
  }

  @Override
  public List<ColumnSchema> getColumnSchemas() {
    return indexTable.getColumnSchemas();
  }

  @Override
  public Hint parseFromRowResult(RowResult rowResult) {
    String attributeName =
        getCellValueOrThrow(
                rowResult,
                ITEntityLevelDisplayHints.Column.ATTRIBUTE_NAME.getSchema().getColumnName())
            .orElseThrow(
                () ->
                    new SystemException(
                        "Null attribute name in entity-level display hints table: "
                            + indexTable.getEntity()))
            .getStringVal();
    Optional<Literal> min =
        getCellValueOrThrow(
            rowResult, ITEntityLevelDisplayHints.Column.MIN.getSchema().getColumnName());
    boolean isRangeHint = min.isPresent();

    if (isRangeHint) {
      Optional<Literal> max =
          getCellValueOrThrow(
              rowResult, ITEntityLevelDisplayHints.Column.MAX.getSchema().getColumnName());
      return new Hint(attributeName, min.get().getDoubleVal(), max.get().getDoubleVal());
    } else {
      Optional<Literal> enumVal =
          getCellValueOrThrow(
              rowResult, ITEntityLevelDisplayHints.Column.ENUM_VALUE.getSchema().getColumnName());
      Optional<Literal> enumDisplay =
          getCellValueOrThrow(
              rowResult, ITEntityLevelDisplayHints.Column.ENUM_DISPLAY.getSchema().getColumnName());
      Optional<Literal> enumCount =
          getCellValueOrThrow(
              rowResult, ITEntityLevelDisplayHints.Column.ENUM_COUNT.getSchema().getColumnName());
      return new Hint(
          attributeName,
          new ValueDisplay(enumVal.get(), enumDisplay.get().getStringVal()),
          enumCount.get().getInt64Val());
    }
  }
}
