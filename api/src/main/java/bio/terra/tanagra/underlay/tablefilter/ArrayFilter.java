package bio.terra.tanagra.underlay.tablefilter;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.query.filtervariable.ArrayFilterVariable;
import bio.terra.tanagra.serialization.tablefilter.UFArrayFilter;
import bio.terra.tanagra.underlay.TableFilter;
import bio.terra.tanagra.underlay.TablePointer;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class ArrayFilter extends TableFilter {
  private final ArrayFilterVariable.LogicalOperator operator;
  private final List<TableFilter> subfilters;

  private ArrayFilter(ArrayFilterVariable.LogicalOperator operator, List<TableFilter> subfilters) {
    this.operator = operator;
    this.subfilters = subfilters;
  }

  public static ArrayFilter fromSerialized(UFArrayFilter serialized, TablePointer tablePointer) {
    if (serialized.getOperator() == null) {
      throw new InvalidConfigException("Array filter operator is undefined");
    }
    if (serialized.getSubfilters() == null || serialized.getSubfilters().size() == 0) {
      throw new InvalidConfigException("Array filter has no sub-filters defined");
    }
    List<TableFilter> subFilters =
        serialized.getSubfilters().stream()
            .map(sf -> sf.deserializeToInternal(tablePointer))
            .collect(Collectors.toList());
    return new ArrayFilter(serialized.getOperator(), subFilters);
  }

  @Override
  public Type getType() {
    return Type.ARRAY;
  }

  @Override
  public ArrayFilterVariable buildVariable(TableVariable primaryTable, List<TableVariable> tables) {
    return new ArrayFilterVariable(
        operator,
        subfilters.stream()
            .map(sf -> sf.buildVariable(primaryTable, tables))
            .collect(Collectors.toList()));
  }

  public ArrayFilterVariable.LogicalOperator getOperator() {
    return operator;
  }

  public List<TableFilter> getSubfilters() {
    return Collections.unmodifiableList(subfilters);
  }
}
