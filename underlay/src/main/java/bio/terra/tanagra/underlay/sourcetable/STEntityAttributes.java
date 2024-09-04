package bio.terra.tanagra.underlay.sourcetable;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.query.bigquery.BQTable;
import bio.terra.tanagra.underlay.ColumnSchema;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.serialization.SZAttribute;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class STEntityAttributes extends SourceTable {
  private final String entity;
  private final ImmutableMap<String, ColumnSchema> attributeValueColumnSchemas;
  private final ImmutableMap<String, ColumnSchema> attributeDisplayColumnSchemas;

  public STEntityAttributes(BQTable bqTable, String entity, List<SZAttribute> szAttributes) {
    super(bqTable);
    this.entity = entity;

    Map<String, ColumnSchema> attributeValueColumnSchemasBuilder = new HashMap<>();
    Map<String, ColumnSchema> attributeDisplayColumnSchemasBuilder = new HashMap<>();
    szAttributes.forEach(
        szAttribute -> {
          attributeValueColumnSchemasBuilder.put(
              szAttribute.name,
              new ColumnSchema(
                  szAttribute.valueFieldName == null
                      ? szAttribute.name
                      : szAttribute.valueFieldName,
                  ConfigReader.deserializeDataType(szAttribute.dataType),
                  szAttribute.isDataTypeRepeated,
                  false));
          if (szAttribute.displayFieldName != null) {
            attributeDisplayColumnSchemasBuilder.put(
                szAttribute.name, new ColumnSchema(szAttribute.displayFieldName, DataType.STRING));
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

  public ImmutableMap<String, ColumnSchema> getAttributeValueColumnSchemas() {
    return attributeValueColumnSchemas;
  }

  public ColumnSchema getAttributeValueColumnSchema(Attribute attribute) {
    return attributeValueColumnSchemas.get(attribute.getName());
  }

  public ImmutableMap<String, ColumnSchema> getAttributeDisplayColumnSchemas() {
    return attributeDisplayColumnSchemas;
  }

  public ColumnSchema getAttributeDisplayColumnSchema(Attribute attribute) {
    return attributeDisplayColumnSchemas.get(attribute.getName());
  }
}
