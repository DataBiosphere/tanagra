package bio.terra.tanagra.api.field.hint;

import bio.terra.tanagra.api.query.ValueDisplay;
import bio.terra.tanagra.api.query.hint.Hint;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.RowResult;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.EntityGroup;
import bio.terra.tanagra.underlay.indextable.ITInstanceLevelDisplayHints;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class InstanceLevelHintField extends HintField {
  private final ITInstanceLevelDisplayHints indexTable;

  public InstanceLevelHintField(
      Underlay underlay, EntityGroup entityGroup, Entity hintedEntity, Entity relatedEntity) {
    this.indexTable =
        underlay
            .getIndexSchema()
            .getInstanceLevelDisplayHints(
                entityGroup.getName(), hintedEntity.getName(), relatedEntity.getName());
  }

  @Override
  public List<FieldVariable> buildFieldVariables(
      TableVariable ilHintTableVar, List<TableVariable> tableVars) {
    return Arrays.stream(ITInstanceLevelDisplayHints.Column.values())
        .map(
            column ->
                new FieldPointer.Builder()
                    .tablePointer(indexTable.getTablePointer())
                    .columnName(column.getSchema().getColumnName())
                    .build()
                    .buildVariable(ilHintTableVar, tableVars))
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
                ITInstanceLevelDisplayHints.Column.ATTRIBUTE_NAME.getSchema().getColumnName())
            .orElseThrow(
                () ->
                    new SystemException(
                        "Null attribute name in instance-level display hints table: "
                            + indexTable.getHintedEntity()))
            .getStringVal();
    Optional<Literal> min =
        getCellValueOrThrow(
            rowResult, ITInstanceLevelDisplayHints.Column.MIN.getSchema().getColumnName());
    boolean isRangeHint = min.isPresent();

    if (isRangeHint) {
      Optional<Literal> max =
          getCellValueOrThrow(
              rowResult, ITInstanceLevelDisplayHints.Column.MAX.getSchema().getColumnName());
      return new Hint(attributeName, min.get().getDoubleVal(), max.get().getDoubleVal());
    } else {
      Optional<Literal> enumVal =
          getCellValueOrThrow(
              rowResult, ITInstanceLevelDisplayHints.Column.ENUM_VALUE.getSchema().getColumnName());
      Optional<Literal> enumDisplay =
          getCellValueOrThrow(
              rowResult,
              ITInstanceLevelDisplayHints.Column.ENUM_DISPLAY.getSchema().getColumnName());
      Optional<Literal> enumCount =
          getCellValueOrThrow(
              rowResult, ITInstanceLevelDisplayHints.Column.ENUM_COUNT.getSchema().getColumnName());
      return new Hint(
          attributeName,
          new ValueDisplay(enumVal.get(), enumDisplay.get().getStringVal()),
          enumCount.get().getInt64Val());
    }
  }
}
