package bio.terra.tanagra.underlay.indextable;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.underlay.ColumnSchema;
import bio.terra.tanagra.underlay.NameHelper;
import bio.terra.tanagra.underlay.serialization.SZBigQuery;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class ITInstanceLevelDisplayHints extends IndexTable {
  public static final String TABLE_NAME = "ILDH";
  private final String entityGroup;
  private final String hintedEntity;
  private final String relatedEntity;

  public ITInstanceLevelDisplayHints(
      NameHelper namer,
      SZBigQuery.IndexData bigQueryConfig,
      String entityGroup,
      String hintedEntity,
      String relatedEntity) {
    super(namer, bigQueryConfig);
    this.entityGroup = entityGroup;
    this.hintedEntity = hintedEntity;
    this.relatedEntity = relatedEntity;
  }

  public String getEntityGroup() {
    return entityGroup;
  }

  public String getHintedEntity() {
    return hintedEntity;
  }

  public String getRelatedEntity() {
    return relatedEntity;
  }

  @Override
  public String getTableBaseName() {
    return TABLE_NAME + "_" + hintedEntity + "_" + relatedEntity;
  }

  @Override
  public ImmutableList<ColumnSchema> getColumnSchemas() {
    // Columns are static and don't depend on the entity.
    return ImmutableList.copyOf(
        Arrays.stream(Column.values())
            .map(column -> column.getSchema())
            .collect(Collectors.toList()));
  }

  public enum Column {
    ENTITY_ID(new ColumnSchema("entity_id", DataType.INT64, true)),
    ATTRIBUTE_NAME(new ColumnSchema("attribute_name", DataType.STRING, true)),
    MIN(new ColumnSchema("min", DataType.DOUBLE)),
    MAX(new ColumnSchema("max", DataType.DOUBLE)),
    ENUM_VALUE(new ColumnSchema("enum_value", DataType.INT64)),
    ENUM_DISPLAY(new ColumnSchema("enum_display", DataType.STRING)),
    ENUM_COUNT(new ColumnSchema("enum_count", DataType.INT64));

    private final ColumnSchema columnSchema;

    Column(ColumnSchema columnSchema) {
      this.columnSchema = columnSchema;
    }

    public ColumnSchema getSchema() {
      return columnSchema;
    }
  }
}
