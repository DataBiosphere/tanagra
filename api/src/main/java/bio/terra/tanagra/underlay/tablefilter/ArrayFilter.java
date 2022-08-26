package bio.terra.tanagra.underlay.tablefilter;

import bio.terra.tanagra.query.ArrayFilterVariable;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.serialization.tablefilter.UFArrayFilter;
import bio.terra.tanagra.underlay.TableFilter;
import bio.terra.tanagra.underlay.TablePointer;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class ArrayFilter extends TableFilter {
  private final TableFilter.LogicalOperator operator;
  private final List<TableFilter> subfilters;

  private ArrayFilter(LogicalOperator operator, List<TableFilter> subfilters) {
    this.operator = operator;
    this.subfilters = subfilters;
  }

  public static ArrayFilter fromSerialized(UFArrayFilter serialized, TablePointer tablePointer) {
    if (serialized.getOperator() == null) {
      throw new IllegalArgumentException("Array filter operator is undefined");
    }
    if (serialized.getSubfilters() == null || serialized.getSubfilters().size() == 0) {
      throw new IllegalArgumentException("Array filter has no sub-filters defined");
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

  public LogicalOperator getOperator() {
    return operator;
  }

  public List<TableFilter> getSubfilters() {
    return Collections.unmodifiableList(subfilters);
  }
}
