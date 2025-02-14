package bio.terra.tanagra.underlay.indextable;

import bio.terra.tanagra.query.bigquery.BQTable;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.underlay.ColumnSchema;
import bio.terra.tanagra.underlay.NameHelper;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.indextable.ITEntityMain.ColumnTemplate;
import bio.terra.tanagra.underlay.serialization.SZBigQuery;
import com.google.common.collect.ImmutableList;

public abstract class IndexTable {
  private final NameHelper namer;
  private final SZBigQuery.IndexData bigQueryConfig;
  private final String sql;

  protected IndexTable(NameHelper namer, SZBigQuery.IndexData bigQueryConfig) {
    this.namer = namer;
    this.bigQueryConfig = bigQueryConfig;
    this.sql = null;
  }

  protected IndexTable(String sql) {
    this.namer = null;
    this.bigQueryConfig = null;
    this.sql = sql;
  }

  public abstract String getTableBaseName();

  public BQTable getTablePointer() {
    return sql == null
        ? new BQTable(
            bigQueryConfig.projectId,
            bigQueryConfig.datasetId,
            namer.getReservedTableName(getTableBaseName()))
        : new BQTable(sql);
  }

  public abstract ImmutableList<ColumnSchema> getColumnSchemas();

  public ImmutableList<String> getColumnNames() {
    return ImmutableList.copyOf(
        getColumnSchemas().stream().map(ColumnSchema::getColumnName).toList());
  }

  public boolean isGeneratedIndexTable() {
    return true;
  }

  public SqlField getAttributeValueField(String attribute) {
    return SqlField.of(attribute);
  }

  public ColumnSchema getAttributeValueColumnSchema(Attribute attribute) {
    return new ColumnSchema(
        attribute.getName(), attribute.getDataType(), attribute.isDataTypeRepeated(), false);
  }

  public SqlField getAttributeDisplayField(String attribute) {
    return SqlField.of(getAttributeDisplayFieldName(attribute));
  }

  public String getAttributeDisplayFieldName(String attribute) {
    return NameHelper.getReservedFieldName(
        ColumnTemplate.ATTRIBUTE_DISPLAY.getColumnNamePrefixed(attribute));
  }
}
