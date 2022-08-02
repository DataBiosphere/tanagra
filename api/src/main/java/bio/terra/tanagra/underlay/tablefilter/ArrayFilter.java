package bio.terra.tanagra.underlay.tablefilter;

import bio.terra.tanagra.query.ArrayFilterVariable;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.serialization.tablefilter.UFArrayFilter;
import bio.terra.tanagra.underlay.TableFilter;
import bio.terra.tanagra.underlay.TablePointer;
import java.util.List;
import java.util.stream.Collectors;

public class ArrayFilter extends TableFilter {
  private TableFilter.LogicalOperator operator;
  private List<TableFilter> subfilters;

  private ArrayFilter(LogicalOperator operator, List<TableFilter> subfilters) {
    super();
    this.operator = operator;
    this.subfilters = subfilters;
  }

  public static ArrayFilter fromSerialized(UFArrayFilter serialized, TablePointer tablePointer) {
    if (serialized.operator == null) {
      throw new IllegalArgumentException("Array filter operator is undefined");
    }
    if (serialized.subfilters == null || serialized.subfilters.size() == 0) {
      throw new IllegalArgumentException("Array filter has no sub-filters defined");
    }
    List<TableFilter> subFilters =
        serialized.subfilters.stream()
            .map(sf -> sf.deserializeToInternal(tablePointer))
            .collect(Collectors.toList());
    return new ArrayFilter(serialized.operator, subFilters);
  }

  @Override
  public ArrayFilterVariable buildVariable(TableVariable primaryTable, List<TableVariable> tables) {
    return new ArrayFilterVariable(
        operator,
        subfilters.stream()
            .map(sf -> sf.buildVariable(primaryTable, tables))
            .collect(Collectors.toList()));
  }
}
