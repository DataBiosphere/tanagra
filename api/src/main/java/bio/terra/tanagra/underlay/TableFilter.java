package bio.terra.tanagra.underlay;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.serialization.UFTableFilter;
import bio.terra.tanagra.serialization.tablefilter.UFArrayFilter;
import bio.terra.tanagra.serialization.tablefilter.UFBinaryFilter;
import bio.terra.tanagra.underlay.tablefilter.ArrayFilter;
import bio.terra.tanagra.underlay.tablefilter.BinaryFilter;
import java.util.List;

public abstract class TableFilter {
  /** Enum for the types of table filters supported by Tanagra. */
  public enum Type {
    BINARY,
    ARRAY
  }

  public abstract Type getType();

  public abstract FilterVariable buildVariable(
      TableVariable primaryTable, List<TableVariable> tables);

  public UFTableFilter serialize() {
    switch (getType()) {
      case BINARY:
        return new UFBinaryFilter((BinaryFilter) this);
      case ARRAY:
        return new UFArrayFilter((ArrayFilter) this);
      default:
        throw new SystemException("Unknown table filter type: " + getType());
    }
  }
}
