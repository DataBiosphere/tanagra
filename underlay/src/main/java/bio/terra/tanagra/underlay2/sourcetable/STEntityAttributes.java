package bio.terra.tanagra.underlay2.sourcetable;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.underlay2.serialization.SZEntity;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.*;

public class STEntityAttributes extends SourceTable {
  private final String entity;
  private final ImmutableMap<String, ColumnSchema> attributeValueColumnSchemas;
  private final ImmutableMap<String, ColumnSchema> attributeDisplayColumnSchemas;

  public STEntityAttributes(
      TablePointer tablePointer, String entity, Set<SZEntity.Attribute> szAttributes) {
    super(tablePointer);
    this.entity = entity;

    Map<String, ColumnSchema> attributeValueColumnSchemasBuilder = new HashMap<>();
    Map<String, ColumnSchema> attributeDisplayColumnSchemasBuilder = new HashMap<>();
    List<ColumnSchema> columnSchemas = new ArrayList<>();
    szAttributes.stream()
        .forEach(
            szAttribute -> {
              attributeValueColumnSchemasBuilder.put(
                  szAttribute.name,
                  new ColumnSchema(
                      szAttribute.valueFieldName,
                      CellValue.SQLDataType.fromUnderlayDataType(szAttribute.dataType)));
              if (szAttribute.displayFieldName != null) {
                attributeDisplayColumnSchemasBuilder.put(
                    szAttribute.name,
                    new ColumnSchema(szAttribute.displayFieldName, CellValue.SQLDataType.STRING));
              }
            });
    this.attributeValueColumnSchemas = ImmutableMap.copyOf(attributeValueColumnSchemasBuilder);
    this.attributeDisplayColumnSchemas = ImmutableMap.copyOf(attributeDisplayColumnSchemasBuilder);
  }

  @Override
  public ImmutableList<ColumnSchema> getColumnSchemas() {
    List<ColumnSchema> columnSchemasBuilder = new ArrayList<>();
    columnSchemasBuilder.addAll(attributeValueColumnSchemas.values());
    columnSchemasBuilder.addAll(attributeDisplayColumnSchemas.values());
    return ImmutableList.copyOf(columnSchemasBuilder);
  }

  public String getEntity() {
    return entity;
  }

  public FieldPointer getValueField(String attribute) {
    return new FieldPointer.Builder()
        .tablePointer(getTablePointer())
        .columnName(attributeValueColumnSchemas.get(attribute).getColumnName())
        .build();
  }

  public FieldPointer getDisplayField(String attribute) {
    return new FieldPointer.Builder()
        .tablePointer(getTablePointer())
        .columnName(attributeDisplayColumnSchemas.get(attribute).getColumnName())
        .build();
  }
}
