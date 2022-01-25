package bio.terra.tanagra.service.underlay;

import bio.terra.tanagra.proto.underlay.AttributeId;
import bio.terra.tanagra.proto.underlay.AttributeMapping.SimpleColumn;
import bio.terra.tanagra.proto.underlay.Dataset;
import bio.terra.tanagra.proto.underlay.EntityMapping;
import bio.terra.tanagra.proto.underlay.FilterableAttribute;
import bio.terra.tanagra.proto.underlay.FilterableRelationship;
import bio.terra.tanagra.proto.underlay.RelationshipMapping;
import bio.terra.tanagra.service.search.Attribute;
import bio.terra.tanagra.service.search.DataType;
import bio.terra.tanagra.service.search.Entity;
import bio.terra.tanagra.service.search.Relationship;
import bio.terra.tanagra.service.underlay.AttributeMapping.LookupColumn;
import bio.terra.tanagra.service.underlay.Hierarchy.ChildrenTable;
import bio.terra.tanagra.service.underlay.Hierarchy.DescendantsTable;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.protobuf.Message;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Utilities for converting protobuf representations to underlay classes. */
@VisibleForTesting
public final class UnderlayConversion {
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
    Map<String, Relationship> relationships = buildRelationships(underlayProto, entities);
    Map<ColumnId, Column> columns = buildColumns(underlayProto);
    Map<Entity, Column> primaryKeys = buildPrimaryKeys(underlayProto, entities, columns);
    Map<Entity, TableFilter> tableFilters =
        buildTableFilters(underlayProto, entities, columns, primaryKeys);
    Map<Attribute, AttributeMapping> attributeMappings =
        buildAttributeMapping(underlayProto, entities, attributes, columns, primaryKeys);
    Map<Relationship, Object> relationshipMappings =
        buildRelationshipMapping(underlayProto, relationships, columns, primaryKeys);
    Map<Attribute, Hierarchy> hierarchies =
        buildHierarchies(underlayProto, entities, attributes, columns);
    Map<Entity, EntityFiltersSchema> entityFiltersSchemas =
        buildEntityFiltersSchemas(underlayProto, entities, attributes, relationships);

    return Underlay.builder()
        .name(underlayProto.getName())
        .entities(entities)
        .attributes(attributes)
        .relationships(relationships)
        .primaryKeys(primaryKeys)
        .tableFilters(tableFilters)
        .attributeMappings(attributeMappings)
        .relationshipMappings(relationshipMappings)
        .hierarchies(hierarchies)
        .entityFiltersSchemas(entityFiltersSchemas)
        .build();
  }

  /** Builds a map of Relationship names to Relationships. */
  private static Map<String, Relationship> buildRelationships(
      bio.terra.tanagra.proto.underlay.Underlay underlayProto, Map<String, Entity> entities) {
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
    return relationships;
  }

  /** Builds a map of {@link ColumnId}s to their corresponding {@link Column}s. */
  private static Map<ColumnId, Column> buildColumns(
      bio.terra.tanagra.proto.underlay.Underlay underlayProto) {
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
    return columns;
  }

  /** Builds a map of entities to the primary key column on their primary table. */
  private static Map<Entity, Column> buildPrimaryKeys(
      bio.terra.tanagra.proto.underlay.Underlay underlayProto,
      Map<String, Entity> entities,
      Map<ColumnId, Column> columns) {
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
    return primaryKeys;
  }

  /** Builds a map of entities to the optional filter on their primary table. */
  private static Map<Entity, TableFilter> buildTableFilters(
      bio.terra.tanagra.proto.underlay.Underlay underlayProto,
      Map<String, Entity> entities,
      Map<ColumnId, Column> columns,
      Map<Entity, Column> primaryKeys) {
    Map<Entity, TableFilter> tableFilters = new HashMap<>();
    for (EntityMapping entityMapping : underlayProto.getEntityMappingsList()) {
      Entity entity = entities.get(entityMapping.getEntity());
      if (entity == null) {
        throw new IllegalArgumentException(
            String.format("Unknown entity being mapped: %s", entityMapping));
      }
      if (!entityMapping.hasTableFilter()) {
        continue;
      }

      // convert the TableFilter proto and do entity table-specific validation
      TableFilter tableFilter = convert(entityMapping.getTableFilter(), columns);
      switch (entityMapping.getTableFilter().getFilterCase()) {
        case ARRAY_COLUMN_FILTER:
          validateForEntityMapping(
              tableFilter.arrayColumnFilter(), primaryKeys.get(entity), entityMapping);
          break;
        case BINARY_COLUMN_FILTER:
          validateForEntityMapping(
              tableFilter.binaryColumnFilter(), primaryKeys.get(entity), entityMapping);
          break;
        case FILTER_NOT_SET:
        default:
          throw new IllegalArgumentException(
              String.format("Unknown table filter type: %s", entityMapping));
      }

      if (tableFilters.put(entity, tableFilter) != null) {
        throw new IllegalArgumentException(
            String.format("Duplicate entity being mapped: %s", entityMapping));
      }
    }
    return tableFilters;
  }

  /** Builds a map from {@link Attribute}s to their corresponding {@link AttributeMapping}. */
  private static Map<Attribute, AttributeMapping> buildAttributeMapping(
      bio.terra.tanagra.proto.underlay.Underlay underlayProto,
      Map<String, Entity> entities,
      com.google.common.collect.Table<Entity, String, Attribute> attributes,
      Map<ColumnId, Column> columns,
      Map<Entity, Column> primaryKeys) {
    Map<Attribute, AttributeMapping> attributeMappings = new HashMap<>();
    for (bio.terra.tanagra.proto.underlay.AttributeMapping attributeMapping :
        underlayProto.getAttributeMappingsList()) {
      Attribute attribute =
          retrieve(
              attributeMapping.getAttribute(), entities, attributes, "attribute", attributeMapping);

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
        case LOOKUP_COLUMN:
          bio.terra.tanagra.proto.underlay.AttributeMapping.LookupColumn lookupColumn =
              attributeMapping.getLookupColumn();
          Column lookupAttribute =
              retrieve(lookupColumn.getLookupColumn(), columns, "lookup column", lookupColumn);
          Column lookupTableKey =
              retrieve(lookupColumn.getLookupTableKey(), columns, "lookup table key", lookupColumn);
          Column primaryTableKey =
              retrieve(
                  lookupColumn.getPrimaryTableLookupKey(),
                  columns,
                  "primary table lookup key",
                  lookupColumn);
          Preconditions.checkArgument(
              primaryKeys.get(attribute.entity()).table().equals(primaryTableKey.table()),
              "Normalized mapping primary table key column must be on the same table as the Entity's primary table.");
          newMapping =
              LookupColumn.builder()
                  .attribute(attribute)
                  .lookupColumn(lookupAttribute)
                  .lookupTableKey(lookupTableKey)
                  .primaryTableLookupKey(primaryTableKey)
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
    return attributeMappings;
  }

  /** Builds a map of {@link Relationship} to their corresponding relationship mapping. */
  private static Map<Relationship, Object> buildRelationshipMapping(
      bio.terra.tanagra.proto.underlay.Underlay underlayProto,
      Map<String, Relationship> relationships,
      Map<ColumnId, Column> columns,
      Map<Entity, Column> primaryKeys) {
    Map<Relationship, Object> relationshipMappings = new HashMap<>();
    for (RelationshipMapping relationshipMapping : underlayProto.getRelationshipMappingsList()) {
      Relationship relationship = relationships.get(relationshipMapping.getName());
      if (relationship == null) {
        throw new IllegalArgumentException(
            String.format(
                "Unknown relationship name in RelationshipMapping %s", relationshipMapping));
      }

      switch (relationshipMapping.getMappingCase()) {
        case FOREIGN_KEY:
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
          Preconditions.checkArgument(
              primaryKeys.get(relationship.entity1()).table().equals(primaryKey.table()),
              "The first entity's primary table does not match the primary key in the foreign key mapping. %s",
              relationshipMapping);
          Preconditions.checkArgument(
              primaryKeys.get(relationship.entity2()).table().equals(foreignKey.table()),
              "The second entity's primary table does not match the foreign key in the foreign key mapping. %s",
              relationshipMapping);
          if (relationshipMappings.put(
                  relationship,
                  ForeignKey.builder().primaryKey(primaryKey).foreignKey(foreignKey).build())
              != null) {
            throw new IllegalArgumentException(
                String.format(
                    "Duplicate foreign key relationship mapping. %s", relationshipMapping));
          }
          break;
        case INTERMEDIATE_TABLE:
          Column entity1EntityTableKey =
              retrieve(
                  relationshipMapping.getIntermediateTable().getEntity1EntityTableKey(),
                  columns,
                  "entity1 entity table key",
                  relationshipMapping);
          Column entity1IntermediateTableKey =
              retrieve(
                  relationshipMapping.getIntermediateTable().getEntity1IntermediateTableKey(),
                  columns,
                  "entity1 intermediate table key",
                  relationshipMapping);
          Column entity2EntityTableKey =
              retrieve(
                  relationshipMapping.getIntermediateTable().getEntity2EntityTableKey(),
                  columns,
                  "entity2 entity table key",
                  relationshipMapping);
          Column entity2IntermediateTableKey =
              retrieve(
                  relationshipMapping.getIntermediateTable().getEntity2IntermediateTableKey(),
                  columns,
                  "entity2 intermediate table key",
                  relationshipMapping);

          Preconditions.checkArgument(
              entity1IntermediateTableKey.table().equals(entity2IntermediateTableKey.table()),
              "The first and second entities' intermediate table keys are not in the same table in the relationship table mapping. %s",
              relationshipMapping);
          Preconditions.checkArgument(
              primaryKeys.get(relationship.entity1()).table().equals(entity1EntityTableKey.table()),
              "The first entity's primary table does not match the entity table key in the relationship table mapping. %s",
              relationshipMapping);
          Preconditions.checkArgument(
              primaryKeys.get(relationship.entity2()).table().equals(entity2EntityTableKey.table()),
              "The second entity's primary table does not match the entity table key in the relationship table mapping. %s",
              relationshipMapping);
          if (relationshipMappings.put(
                  relationship,
                  IntermediateTable.builder()
                      .entity1EntityTableKey(entity1EntityTableKey)
                      .entity1IntermediateTableKey(entity1IntermediateTableKey)
                      .entity2EntityTableKey(entity2EntityTableKey)
                      .entity2IntermediateTableKey(entity2IntermediateTableKey)
                      .build())
              != null) {
            throw new IllegalArgumentException(
                String.format(
                    "Duplicate foreign key relationship mapping. %s", relationshipMapping));
          }
          break;
        default:
          throw new IllegalArgumentException(
              String.format("Unknown relationship mapping type %s", relationshipMapping));
      }
    }
    return relationshipMappings;
  }

  private static Map<Attribute, Hierarchy> buildHierarchies(
      bio.terra.tanagra.proto.underlay.Underlay underlayProto,
      Map<String, Entity> entities,
      com.google.common.collect.Table<Entity, String, Attribute> attributes,
      Map<ColumnId, Column> columns) {
    Map<Attribute, Hierarchy> hierarchies = new HashMap<>();
    for (bio.terra.tanagra.proto.underlay.Hierarchy hierarchyProto :
        underlayProto.getHierarchiesList()) {
      Attribute attribute =
          retrieve(
              hierarchyProto.getAttribute(), entities, attributes, "attribute", hierarchyProto);

      Column ancestor =
          retrieve(
              hierarchyProto.getDescendantsTable().getAncestor(),
              columns,
              "ancestor",
              hierarchyProto);
      Column descendant =
          retrieve(
              hierarchyProto.getDescendantsTable().getDescendant(),
              columns,
              "descendants",
              hierarchyProto);
      DescendantsTable.Builder descendantsTable =
          DescendantsTable.builder().ancestor(ancestor).descendant(descendant);

      Column parent =
          retrieve(
              hierarchyProto.getChildrenTable().getParent(), columns, "parent", hierarchyProto);
      Column child =
          retrieve(hierarchyProto.getChildrenTable().getChild(), columns, "child", hierarchyProto);
      ChildrenTable.Builder childrenTable = ChildrenTable.builder().parent(parent).child(child);
      if (hierarchyProto.getChildrenTable().hasTableFilter()) {
        childrenTable.tableFilter(
            convert(hierarchyProto.getChildrenTable().getTableFilter(), columns));
      }

      Hierarchy hierarchy =
          Hierarchy.builder()
              .descendantsTable(descendantsTable.build())
              .childrenTable(childrenTable.build())
              .build();

      if (hierarchies.put(attribute, hierarchy) != null) {
        throw new IllegalArgumentException(
            String.format("Duplicate attribute hierarchies not allowed: %s", attribute));
      }
    }
    return hierarchies;
  }

  /** Build a map from entities to their {@link EntityFiltersSchema} for filters. */
  private static Map<Entity, EntityFiltersSchema> buildEntityFiltersSchemas(
      bio.terra.tanagra.proto.underlay.Underlay underlayProto,
      Map<String, Entity> entities,
      com.google.common.collect.Table<Entity, String, Attribute> attributes,
      Map<String, Relationship> relationships) {
    Map<Entity, EntityFiltersSchema> entityFiltersSchemas = new HashMap<>();
    for (bio.terra.tanagra.proto.underlay.EntityFiltersSchema filtersSchemaProto :
        underlayProto.getEntityFiltersSchemasList()) {
      EntityFiltersSchema entityFiltersSchema =
          buildEntityFiltersSchema(
              filtersSchemaProto, entities, attributes, relationships, entityFiltersSchemas);
      if (entityFiltersSchemas.put(entityFiltersSchema.entity(), entityFiltersSchema) != null) {
        throw new IllegalArgumentException(
            String.format(
                "Duplicate entity '%s' in entity filter: %s",
                entityFiltersSchema.entity().name(), filtersSchemaProto));
      }
    }
    return entityFiltersSchemas;
  }

  /** Build an {@link EntityFiltersSchema} from a a proto entity filter schema. */
  private static EntityFiltersSchema buildEntityFiltersSchema(
      bio.terra.tanagra.proto.underlay.EntityFiltersSchema filtersSchemaProto,
      Map<String, Entity> entities,
      com.google.common.collect.Table<Entity, String, Attribute> attributes,
      Map<String, Relationship> relationships,
      Map<Entity, EntityFiltersSchema> entityFiltersSchemas) {
    Entity entity = entities.get(filtersSchemaProto.getEntity());
    if (entity == null) {
      throw new IllegalArgumentException(
          String.format(
              "Unknown entity in entity filter schema: %s", filtersSchemaProto.toString()));
    }
    Map<Attribute, FilterableAttribute> filterableAttributes = new HashMap<>();
    for (FilterableAttribute filterableAttribute : filtersSchemaProto.getAttributesList()) {
      Attribute attribute = attributes.get(entity, filterableAttribute.getAttributeName());
      if (attribute == null) {
        throw new IllegalArgumentException(
            String.format(
                "Unknown attribute '%s' in entity filter schema: %s",
                filterableAttribute.getAttributeName(), entityFiltersSchemas));
      }
      if (filterableAttributes.put(attribute, filterableAttribute) != null) {
        throw new IllegalArgumentException(
            String.format(
                "Duplicate filterable attribute '%s' in entity filter schema: %s'",
                filterableAttribute.getAttributeName(), filtersSchemaProto));
      }
    }
    Set<Relationship> filterableRelationships = new HashSet<>();
    for (FilterableRelationship filterableRelationship :
        filtersSchemaProto.getRelationshipsList()) {
      Relationship relationship = relationships.get(filterableRelationship.getRelationshipName());
      if (relationship == null) {
        throw new IllegalArgumentException(
            String.format(
                "Unable to find relationship '%s' in entity filter schema: '%s'",
                filterableRelationship.getRelationshipName(), filtersSchemaProto));
      }
      if (!relationship.hasEntity(entity)) {
        throw new IllegalArgumentException(
            String.format(
                "Filterable relationship entity does not match the relationship's entities '%s': %s",
                relationship, entityFiltersSchemas));
      }
      if (!filterableRelationships.add(relationship)) {
        throw new IllegalArgumentException(
            String.format(
                "Duplicate relationship '%s' in entity filter schema: '%s'",
                filterableRelationship.getRelationshipName(), filtersSchemaProto));
      }
    }
    return EntityFiltersSchema.builder()
        .entity(entity)
        .filterableAttributes(filterableAttributes)
        .filterableRelationships(filterableRelationships)
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

  private static TableFilter convert(
      bio.terra.tanagra.proto.underlay.TableFilter tableFilterProto,
      Map<ColumnId, Column> columns) {
    switch (tableFilterProto.getFilterCase()) {
      case ARRAY_COLUMN_FILTER:
        return TableFilter.builder()
            .arrayColumnFilter(convert(tableFilterProto.getArrayColumnFilter(), columns))
            .build();
      case BINARY_COLUMN_FILTER:
        return TableFilter.builder()
            .binaryColumnFilter(convert(tableFilterProto.getBinaryColumnFilter(), columns))
            .build();
      case FILTER_NOT_SET:
      default:
        throw new IllegalArgumentException(
            String.format("Unknown table filter type: %s", tableFilterProto));
    }
  }

  private static ArrayColumnFilter convert(
      bio.terra.tanagra.proto.underlay.ArrayColumnFilter arrayColumnFilterProto,
      Map<ColumnId, Column> columns) {
    List<ArrayColumnFilter> arrayColumnFilters =
        arrayColumnFilterProto.getArrayColumnFiltersList().stream()
            .map(acf -> convert(acf, columns))
            .collect(Collectors.toList());
    List<BinaryColumnFilter> binaryColumnFilters =
        arrayColumnFilterProto.getBinaryColumnFiltersList().stream()
            .map(bcf -> convert(bcf, columns))
            .collect(Collectors.toList());

    if (arrayColumnFilters.isEmpty() && binaryColumnFilters.isEmpty()) {
      throw new IllegalArgumentException(
          String.format("Array column filter contains no sub-filters: %s", arrayColumnFilterProto));
    }

    return ArrayColumnFilter.builder()
        .operator(convert(arrayColumnFilterProto.getOperator()))
        .arrayColumnFilters(arrayColumnFilters)
        .binaryColumnFilters(binaryColumnFilters)
        .build();
  }

  private static ArrayColumnFilterOperator convert(
      bio.terra.tanagra.proto.underlay.ArrayColumnFilterOperator operator) {
    switch (operator) {
      case AND:
        return ArrayColumnFilterOperator.AND;
      case OR:
        return ArrayColumnFilterOperator.OR;
      default:
        throw new UnsupportedOperationException(
            String.format("Unsupported ArrayColumnFilterOperator %s", operator.name()));
    }
  }

  private static void validateForEntityMapping(
      ArrayColumnFilter arrayColumnFilter, Column entityPrimaryKey, EntityMapping entityMapping) {
    arrayColumnFilter
        .arrayColumnFilters()
        .forEach(acf -> validateForEntityMapping(acf, entityPrimaryKey, entityMapping));
    arrayColumnFilter
        .binaryColumnFilters()
        .forEach(bcf -> validateForEntityMapping(bcf, entityPrimaryKey, entityMapping));
  }

  private static void validateForEntityMapping(
      BinaryColumnFilter binaryColumnFilter, Column entityPrimaryKey, EntityMapping entityMapping) {
    if (!binaryColumnFilter.column().table().equals(entityPrimaryKey.table())) {
      throw new IllegalArgumentException(
          String.format(
              "Binary table filter column is not in the same table as the entity primary key: %s",
              entityMapping));
    }
  }

  private static BinaryColumnFilter convert(
      bio.terra.tanagra.proto.underlay.BinaryColumnFilter binaryColumnFilterProto,
      Map<ColumnId, Column> columns) {
    ColumnId filterColumnId = convert(binaryColumnFilterProto.getColumn());
    Column filterColumn = columns.get(filterColumnId);
    if (filterColumn == null) {
      throw new IllegalArgumentException(
          String.format("Unknown table filter column: %s", binaryColumnFilterProto));
    }

    ColumnValue columnValue;
    switch (binaryColumnFilterProto.getValueCase()) {
      case INT64_VAL:
        if (!filterColumn.dataType().equals(DataType.INT64)) {
          throw new IllegalArgumentException(
              String.format(
                  "Binary table filter value is a different data type than the column: %s",
                  binaryColumnFilterProto));
        }
        columnValue = ColumnValue.builder().int64Val(binaryColumnFilterProto.getInt64Val()).build();
        break;
      case STRING_VAL:
        if (!filterColumn.dataType().equals(DataType.STRING)) {
          throw new IllegalArgumentException(
              String.format(
                  "Binary table filter value is a different data type than the column: %s",
                  binaryColumnFilterProto));
        }
        columnValue =
            ColumnValue.builder().stringVal(binaryColumnFilterProto.getStringVal()).build();
        break;
      case VALUE_NOT_SET:
        columnValue = null;
        break;
      default:
        throw new IllegalArgumentException(
            "Unknown binary column filter value type: " + binaryColumnFilterProto.getValueCase());
    }

    return BinaryColumnFilter.builder()
        .column(filterColumn)
        .operator(convert(binaryColumnFilterProto.getOperator()))
        .value(columnValue)
        .build();
  }

  private static BinaryColumnFilterOperator convert(
      bio.terra.tanagra.proto.underlay.BinaryColumnFilterOperator operator) {
    switch (operator) {
      case EQUALS:
        return BinaryColumnFilterOperator.EQUALS;
      case NOT_EQUALS:
        return BinaryColumnFilterOperator.NOT_EQUALS;
      case LESS_THAN:
        return BinaryColumnFilterOperator.LESS_THAN;
      case GREATER_THAN:
        return BinaryColumnFilterOperator.GREATER_THAN;
      default:
        throw new UnsupportedOperationException(
            String.format("Unsupported BinaryColumnFilterOperator %s", operator.name()));
    }
  }

  /**
   * Retrieves the {@link Attribute} corresponding to the proto {@link AttributeId}, or throws if
   * none is found.
   */
  private static Attribute retrieve(
      AttributeId attributeId,
      Map<String, Entity> entities,
      com.google.common.collect.Table<Entity, String, Attribute> attributes,
      String attributeIdField,
      Message parentMessage) {
    Entity entity = entities.get(attributeId.getEntity());
    if (entity == null) {
      throw new IllegalArgumentException(
          String.format("Unknown entity in %s AttributeId in %s", attributeIdField, parentMessage));
    }
    Attribute attribute = attributes.get(entity, attributeId.getAttribute());
    if (attribute == null) {
      throw new IllegalArgumentException(
          String.format(
              "Unknown attribute in %s AttributeId in %s", attributeIdField, parentMessage));
    }
    return attribute;
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
