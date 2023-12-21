package bio.terra.tanagra.underlay.sourcetable;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.query2.sql.SqlColumnSchema;
import bio.terra.tanagra.query2.sql.SqlField;
import bio.terra.tanagra.query2.sql.SqlTable;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.serialization.SZEntity;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class STEntityAttributes extends SourceTable {
  private final String entity;
  private final ImmutableMap<String, SqlColumnSchema> attributeValueColumnSchemas;
  private final ImmutableMap<String, SqlColumnSchema> attributeDisplayColumnSchemas;

  public STEntityAttributes(
      SqlTable sqlTable, String entity, List<SZEntity.Attribute> szAttributes) {
    super(sqlTable);
    this.entity = entity;

    Map<String, SqlColumnSchema> attributeValueColumnSchemasBuilder = new HashMap<>();
    Map<String, SqlColumnSchema> attributeDisplayColumnSchemasBuilder = new HashMap<>();
    szAttributes.stream()
        .forEach(
            szAttribute -> {
              attributeValueColumnSchemasBuilder.put(
                  szAttribute.name,
                  new SqlColumnSchema(
                      szAttribute.valueFieldName == null
                          ? szAttribute.name
                          : szAttribute.valueFieldName,
                      ConfigReader.deserializeDataType(szAttribute.dataType)));
              if (szAttribute.displayFieldName != null) {
                attributeDisplayColumnSchemasBuilder.put(
                    szAttribute.name,
                    new SqlColumnSchema(szAttribute.displayFieldName, DataType.STRING));
              }
            });
    this.attributeValueColumnSchemas = ImmutableMap.copyOf(attributeValueColumnSchemasBuilder);
    this.attributeDisplayColumnSchemas = ImmutableMap.copyOf(attributeDisplayColumnSchemasBuilder);
  }

  @Override
  public ImmutableList<SqlColumnSchema> getColumnSchemas() {
    List<SqlColumnSchema> columnSchemasBuilder = new ArrayList<>();
    columnSchemasBuilder.addAll(attributeValueColumnSchemas.values());
    columnSchemasBuilder.addAll(attributeDisplayColumnSchemas.values());
    return ImmutableList.copyOf(columnSchemasBuilder);
  }

  public String getEntity() {
    return entity;
  }

  public SqlField getValueField(String attribute) {
    return new SqlField.Builder()
        .tablePointer(getTablePointer())
        .columnName(attributeValueColumnSchemas.get(attribute).getColumnName())
        .build();
  }

  public SqlField getDisplayField(String attribute) {
    return new SqlField.Builder()
        .tablePointer(getTablePointer())
        .columnName(attributeDisplayColumnSchemas.get(attribute).getColumnName())
        .build();
  }

  public ImmutableMap<String, SqlColumnSchema> getAttributeValueColumnSchemas() {
    return attributeValueColumnSchemas;
  }

  public SqlColumnSchema getAttributeValueColumnSchema(Attribute attribute) {
    return attributeValueColumnSchemas.get(attribute.getName());
  }

  public ImmutableMap<String, SqlColumnSchema> getAttributeDisplayColumnSchemas() {
    return attributeDisplayColumnSchemas;
  }

  public SqlColumnSchema getAttributeDisplayColumnSchema(Attribute attribute) {
    return attributeDisplayColumnSchemas.get(attribute.getName());
  }
}
