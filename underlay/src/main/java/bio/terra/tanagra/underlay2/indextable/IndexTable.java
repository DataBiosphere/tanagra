package bio.terra.tanagra.underlay2.indextable;

import bio.terra.tanagra.query.*;
import bio.terra.tanagra.underlay2.NameHelper;
import com.google.common.collect.ImmutableList;

public abstract class IndexTable {
  protected final NameHelper namer;
  protected final DataPointer dataPointer;

  protected IndexTable(NameHelper namer, DataPointer dataPointer) {
    this.namer = namer;
    this.dataPointer = dataPointer;
  }

  public abstract String getTableBaseName();

  public abstract ImmutableList<ColumnSchema> getColumnSchemas();

  public TablePointer getTablePointer() {
    return TablePointer.fromTableName(getTableBaseName(), dataPointer);
  }
}
