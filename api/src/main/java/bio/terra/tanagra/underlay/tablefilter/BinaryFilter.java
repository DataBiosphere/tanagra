package bio.terra.tanagra.underlay.tablefilter;

import bio.terra.tanagra.query.BinaryFilterVariable;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.serialization.tablefilter.UFBinaryFilter;
import bio.terra.tanagra.underlay.FieldPointer;
import bio.terra.tanagra.underlay.Literal;
import bio.terra.tanagra.underlay.TableFilter;
import bio.terra.tanagra.underlay.TablePointer;
import java.util.List;

public class BinaryFilter extends TableFilter {
  private FieldPointer field;
  private TableFilter.BinaryOperator operator;
  private Literal value;

  private BinaryFilter(
      FieldPointer fieldPointer, TableFilter.BinaryOperator operator, Literal value) {
    super();
    this.field = fieldPointer;
    this.operator = operator;
    this.value = value;
  }

  public static BinaryFilter fromSerialized(UFBinaryFilter serialized, TablePointer tablePointer) {
    if (serialized.field == null || serialized.operator == null || serialized.value == null) {
      throw new IllegalArgumentException("Only some table filter fields are defined");
    }

    FieldPointer fieldPointer = FieldPointer.fromSerialized(serialized.field, tablePointer);
    Literal literal = Literal.fromSerialized(serialized.value);

    return new BinaryFilter(fieldPointer, serialized.operator, literal);
  }

  @Override
  public BinaryFilterVariable buildVariable(
      TableVariable primaryTable, List<TableVariable> tables) {
    return new BinaryFilterVariable(field.buildVariable(primaryTable, tables), operator, value);
  }
}
