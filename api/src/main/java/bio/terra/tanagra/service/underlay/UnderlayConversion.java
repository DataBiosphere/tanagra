package bio.terra.tanagra.service.underlay;

import bio.terra.tanagra.proto.underlay.AttributeMapping.NormalizedColumn;
import bio.terra.tanagra.proto.underlay.AttributeMapping.SimpleColumn;
import bio.terra.tanagra.proto.underlay.Dataset;
import bio.terra.tanagra.proto.underlay.EntityMapping;
import bio.terra.tanagra.proto.underlay.RelationshipMapping;
import bio.terra.tanagra.service.search.Attribute;
import bio.terra.tanagra.service.search.DataType;
import bio.terra.tanagra.service.search.Entity;
import bio.terra.tanagra.service.search.Relationship;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.protobuf.Message;
import java.util.HashMap;
import java.util.Map;

/** Utilities for converting protobuf representations to underlay classes. */
final class UnderlayConversion {
  private UnderlayConversion() {}

  /** Creates an Underlay from a protobuf representation. */
  public static Underlay convert(bio.terra.tanagra.proto.underlay.Underlay underlayProto) {
    Map<String, Entity> entities = new HashMap<>();
    com.google.common.collect.Table<Entity, String, Attribute> attributes = HashBasedTable.create();
    for (bio.terra.tanagra.proto.underlay.Entity entityProto : underlayProto.getEntitiesList()) {
      Entity entity = convert(entityProto, underlayProto);
      if (entities.put(entity.name(), entity) != null) {
        throw new IllegalArgumentException(
            String.format("Duplicate entity name not allowed: %s", entity));
      }
      for (bio.terra.tanagra.proto.underlay.Attribute attributeProto :
          entityProto.getAttributesList()) {
        Attribute attribute = convert(attributeProto, entity);
        if (attributes.put(attribute.entity(), attribute.name(), attribute) != null) {
          throw new IllegalArgumentException(
              String.format("Duplicate attributes not allowed: %s", attribute));
        }
      }
    }
    Map<String, Relationship> relationships = new HashMap<>();
    for (bio.terra.tanagra.proto.underlay.Relationship relationshipProto :
        underlayProto.getRelationshipsList()) {
      Entity entity1 = entities.get(relationshipProto.getEntity1());
      if (entity1 == null) {
        throw new IllegalArgumentException(
            String.format("Unknown entity1 in relationship: %s", relationshipProto));
      }
      Entity entity2 = entities.get(relationshipProto.getEntity2());
      if (entity2 == null) {
        throw new IllegalArgumentException(
            String.format("Unknown entity2 in relationship: %s", relationshipProto));
      }
      Relationship relationship =
          Relationship.builder()
              .name(relationshipProto.getName())
              .entity1(entity1)
              .entity2(entity2)
              .build();
      if (relationships.put(relationship.name(), relationship) != null) {
        throw new IllegalArgumentException(
            String.format("Duplicate relationship name not allowed: %s", relationshipProto));
      }
    }

    Map<ColumnId, Column> columns = new HashMap<>();
    for (Dataset datasetProto : underlayProto.getDatasetsList()) {
      BigQueryDataset dataset = convert(datasetProto);
      for (bio.terra.tanagra.proto.underlay.Table tableProto : datasetProto.getTablesList()) {
        Table table = convert(tableProto, dataset);
        for (bio.terra.tanagra.proto.underlay.Column columnProto : tableProto.getColumnsList()) {
          Column column = convert(columnProto, table);
          ColumnId columnId =
              ColumnId.builder()
                  .dataset(dataset.name())
                  .table(table.name())
                  .column(column.name())
                  .build();
          if (columns.put(columnId, column) != null) {
            throw new IllegalArgumentException(
                String.format("Duplicate column identifier not allowed: %s", column));
          }
        }
      }
    }

    Map<Entity, Column> primaryKeys = new HashMap<>();
    for (EntityMapping entityMapping : underlayProto.getEntityMappingsList()) {
      Entity entity = entities.get(entityMapping.getEntity());
      if (entity == null) {
        throw new IllegalArgumentException(
            String.format("Unknown entity being mapped: %s", entityMapping));
      }
      ColumnId primaryColumnId = convert(entityMapping.getPrimaryKey());
      Column primaryColumn = columns.get(primaryColumnId);
      if (primaryColumn == null) {
        throw new IllegalArgumentException(
            String.format("Unknown primary key column: %s", entityMapping));
      }

      if (primaryKeys.put(entity, primaryColumn) != null) {
        throw new IllegalArgumentException(
            String.format("Duplicate entity being mapped: %s", entityMapping));
      }
    }

    Map<Attribute, AttributeMapping> attributeMappings = new HashMap<>();
    for (bio.terra.tanagra.proto.underlay.AttributeMapping attributeMapping :
        underlayProto.getAttributeMappingsList()) {
      Entity entity = entities.get(attributeMapping.getAttribute().getEntity());
      if (entity == null) {
        throw new IllegalArgumentException(
            String.format("Unknown entity in AttributeMapping %s", attributeMapping));
      }
      Attribute attribute = attributes.get(entity, attributeMapping.getAttribute().getAttribute());
      if (attribute == null) {
        throw new IllegalArgumentException(
            String.format("Unknown attribute in AttributeMapping %s", attributeMapping));
      }

      AttributeMapping newMapping;
      switch (attributeMapping.getMappingCase()) {
        case SIMPLE_COLUMN:
          SimpleColumn simpleColumn = attributeMapping.getSimpleColumn();
          Column column = retrieve(simpleColumn.getColumnId(), columns, "column id", simpleColumn);
          Preconditions.checkArgument(
              primaryKeys.get(attribute.entity()).table().equals(column.table()),
              "Simple attribute mappings column must be on the same table as the Entity's primary table.");
          newMapping = AttributeMapping.SimpleColumn.create(attribute, column);
          break;
        case NORMALIZED_COLUMN:
          NormalizedColumn normalizedColumn = attributeMapping.getNormalizedColumn();
          Column factColumn =
              retrieve(normalizedColumn.getFactColumn(), columns, "fact column", normalizedColumn);
          Column factTableKey =
              retrieve(
                  normalizedColumn.getFactTableKey(), columns, "fact table key", normalizedColumn);
          Column primaryTableKey =
              retrieve(
                  normalizedColumn.getPrimaryTableKey(),
                  columns,
                  "primary table key",
                  normalizedColumn);
          Preconditions.checkArgument(
              primaryKeys.get(attribute.entity()).table().equals(primaryTableKey.table()),
              "Normalized mapping primary table key column must be on the same table as the Entity's primary table.");
          newMapping =
              AttributeMapping.NormalizedColumn.builder()
                  .attribute(attribute)
                  .factColumn(factColumn)
                  .factTableKey(factTableKey)
                  .primaryTableKey(primaryTableKey)
                  .build();
          break;
        default:
          throw new IllegalArgumentException(
              String.format("Unknown attribute mapping type %s", attributeMapping));
      }
      if (attributeMappings.put(attribute, newMapping) != null) {
        throw new IllegalArgumentException(
            String.format("Attribute mapped to multiple times %s", attributeMapping));
      }
    }
    Map<Relationship, ForeignKey> foreignKeys = new HashMap<>();
    for (RelationshipMapping relationshipMapping : underlayProto.getRelationshipMappingsList()) {
      // TODO support other relationship mappings.
      Column primaryKey =
          retrieve(
              relationshipMapping.getForeignKey().getPrimaryKey(),
              columns,
              "primary key",
              relationshipMapping);
      Column foreignKey =
          retrieve(
              relationshipMapping.getForeignKey().getForeignKey(),
              columns,
              "foreign key",
              relationshipMapping);
      Relationship relationship = relationships.get(relationshipMapping.getName());
      if (relationship == null) {
        throw new IllegalArgumentException(
            String.format(
                "Unknown relationship name in RelationshipMapping %s", relationshipMapping));
      }
      Preconditions.checkArgument(
          primaryKeys.get(relationship.entity1()).table().equals(primaryKey.table()),
          "The first entity's primary table does not match the primary key in the foreign key mapping. %s",
          relationshipMapping);
      Preconditions.checkArgument(
          primaryKeys.get(relationship.entity2()).table().equals(foreignKey.table()),
          "The second entity's primary table does not match the foreign key in the foreign key mapping. %s",
          relationshipMapping);
      if (foreignKeys.put(
              relationship,
              ForeignKey.builder().primaryKey(primaryKey).foreignKey(foreignKey).build())
          != null) {
        throw new IllegalArgumentException(
            String.format("Duplicate foreign key relationship mapping. %s", relationshipMapping));
      }
    }

    return Underlay.builder()
        .name(underlayProto.getName())
        .entities(entities)
        .attributes(attributes)
        .relationships(relationships)
        .primaryKeys(primaryKeys)
        .attributeMappings(attributeMappings)
        .foreignKeys(foreignKeys)
        .build();
  }

  private static Entity convert(
      bio.terra.tanagra.proto.underlay.Entity entityProto,
      bio.terra.tanagra.proto.underlay.Underlay underlayProto) {
    return Entity.builder().underlay(underlayProto.getName()).name(entityProto.getName()).build();
  }

  private static Attribute convert(
      bio.terra.tanagra.proto.underlay.Attribute attributeProto, Entity entity) {
    return Attribute.builder()
        .name(attributeProto.getName())
        .dataType(convert(attributeProto.getDataType()))
        .entity(entity)
        .build();
  }

  private static DataType convert(bio.terra.tanagra.proto.underlay.DataType dataType) {
    switch (dataType) {
      case INT64:
        return DataType.INT64;
      case STRING:
        return DataType.STRING;
      default:
        throw new UnsupportedOperationException(
            String.format("Unsupported DataType %s", dataType.name()));
    }
  }

  // TODO support more dataset types.
  private static BigQueryDataset convert(Dataset datasetProto) {
    return BigQueryDataset.builder()
        .name(datasetProto.getName())
        .projectId(datasetProto.getBigQueryDataset().getProjectId())
        .datasetId(datasetProto.getBigQueryDataset().getDatasetId())
        .build();
  }

  private static Table convert(
      bio.terra.tanagra.proto.underlay.Table tableProto, BigQueryDataset bigQueryDataset) {
    return Table.create(tableProto.getName(), bigQueryDataset);
  }

  private static Column convert(bio.terra.tanagra.proto.underlay.Column columnProto, Table table) {
    return Column.builder()
        .name(columnProto.getName())
        .table(table)
        .dataType(convert(columnProto.getDataType()))
        .build();
  }

  private static ColumnId convert(bio.terra.tanagra.proto.underlay.ColumnId columnIdProto) {
    return ColumnId.builder()
        .dataset(columnIdProto.getDataset())
        .table(columnIdProto.getTable())
        .column(columnIdProto.getColumn())
        .build();
  }

  /**
   * Converts a proto ColumnId to a service ColumnId and looks it up in {@code columns}. Throws if
   * this fails.
   */
  private static Column retrieve(
      bio.terra.tanagra.proto.underlay.ColumnId columnIdProto,
      Map<ColumnId, Column> columns,
      String columnIdField,
      Message parentMessage) {
    ColumnId columnId = convert(columnIdProto);
    Column column = columns.get(columnId);
    if (column == null) {
      throw new IllegalArgumentException(
          String.format("Unknown %s column: %s", columnIdField, parentMessage));
    }
    return column;
  }
}
