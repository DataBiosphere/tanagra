package bio.terra.tanagra.underlay.tablefilter;

import bio.terra.tanagra.query.BinaryFilterVariable;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.serialization.tablefilter.UFBinaryFilter;
import bio.terra.tanagra.underlay.FieldPointer;
import bio.terra.tanagra.underlay.Literal;
import bio.terra.tanagra.underlay.TableFilter;
import bio.terra.tanagra.underlay.TablePointer;
import java.util.List;

public final class BinaryFilter extends TableFilter {
  private final FieldPointer field;
  private final TableFilter.BinaryOperator operator;
  private final Literal value;

  private BinaryFilter(
      FieldPointer fieldPointer, TableFilter.BinaryOperator operator, Literal value) {
    this.field = fieldPointer;
    this.operator = operator;
    this.value = value;
  }

  public static BinaryFilter fromSerialized(UFBinaryFilter serialized, TablePointer tablePointer) {
    if (serialized.getField() == null
        || serialized.getOperator() == null
        || serialized.getValue() == null) {
      throw new IllegalArgumentException("Only some table filter fields are defined");
    }

    FieldPointer fieldPointer = FieldPointer.fromSerialized(serialized.getField(), tablePointer);
    Literal literal = Literal.fromSerialized(serialized.getValue());

    return new BinaryFilter(fieldPointer, serialized.getOperator(), literal);
  }

  @Override
  public Type getType() {
    return Type.BINARY;
  }

  @Override
  public BinaryFilterVariable buildVariable(
      TableVariable primaryTable, List<TableVariable> tables) {
    return new BinaryFilterVariable(field.buildVariable(primaryTable, tables), operator, value);
  }

  public FieldPointer getField() {
    return field;
  }

  public BinaryOperator getOperator() {
    return operator;
  }

  public Literal getValue() {
    return value;
  }
}
