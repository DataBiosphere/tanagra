package bio.terra.tanagra.underlay2;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.underlay2.indexschema.EntityLevelDisplayHints;
import bio.terra.tanagra.underlay2.indexschema.EntityMain;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public final class Entity {
  private final String name;
  private final String displayName;
  private @Nullable final String description;
  private final boolean isPrimary;
  private final ImmutableList<Attribute> attributes;
  private final ImmutableList<Attribute> optimizeGroupByAttributes;
  private final ImmutableList<Hierarchy> hierarchies;
  private @Nullable final TextSearch textSearch;
  private final TablePointer sourceEntityTable;
  private final TablePointer indexEntityTable;
  private final TablePointer indexEntityLevelDisplayHintTable;

  @SuppressWarnings("checkstyle:ParameterNumber")
  public Entity(
      String name,
      String displayName,
      String description,
      boolean isPrimary,
      List<Attribute> attributes,
      List<Attribute> optimizeGroupByAttributes,
      List<Hierarchy> hierarchies,
      TextSearch textSearch,
      TablePointer sourceEntityTable) {
    this.name = name;
    this.displayName = displayName;
    this.description = description;
    this.isPrimary = isPrimary;
    this.attributes = ImmutableList.copyOf(attributes);
    this.optimizeGroupByAttributes = ImmutableList.copyOf(optimizeGroupByAttributes);
    this.hierarchies = ImmutableList.copyOf(hierarchies);
    this.textSearch = textSearch;
    this.sourceEntityTable = sourceEntityTable;

    // Resolve index tables.
    this.indexEntityTable = EntityMain.getTable(name);
    this.indexEntityLevelDisplayHintTable = EntityLevelDisplayHints.getTable(name);
  }

  public String getName() {
    return name;
  }

  public String getDisplayName() {
    return displayName;
  }

  public @Nullable String getDescription() {
    return description;
  }

  public boolean isPrimary() {
    return isPrimary;
  }

  public ImmutableList<Attribute> getAttributes() {
    return attributes;
  }

  public Attribute getAttribute(String name) {
    return attributes.stream()
        .filter(a -> name.equals(a.getName()))
        .findFirst()
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Attribute not found: entity=" + this.name + ", attribute=" + name));
  }

  public Attribute getIdAttribute() {
    return attributes.stream()
        .filter(a -> a.isId())
        .findFirst()
        .orElseThrow(() -> new SystemException("No id attribute defined"));
  }

  public ImmutableList<Attribute> getOptimizeGroupByAttributes() {
    return optimizeGroupByAttributes;
  }

  public ImmutableList<Hierarchy> getHierarchies() {
    return hierarchies;
  }

  public Hierarchy getHierarchy(String name) {
    return hierarchies.stream()
        .filter(h -> name.equals(h.getName()))
        .findFirst()
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Hierarchy not found: entity=" + this.name + ", hierarchy=" + name));
  }

  public boolean hasTextSearch() {
    return textSearch != null;
  }

  public TextSearch getTextSearch() {
    if (!hasTextSearch()) {
      throw new SystemException("No text search defined for entity: " + name);
    }
    return textSearch;
  }

  public TablePointer getSourceEntityTable() {
    return sourceEntityTable;
  }

  public TablePointer getIndexEntityTable() {
    return indexEntityTable;
  }

  public TablePointer getIndexEntityLevelDisplayHintTable() {
    return indexEntityLevelDisplayHintTable;
  }

  public ImmutableList<FieldPointer> getIndexEntityLevelDisplayHintFields() {
    return ImmutableList.copyOf(
        EntityLevelDisplayHints.getColumns().stream()
            .map(
                c ->
                    new FieldPointer.Builder()
                        .tablePointer(indexEntityLevelDisplayHintTable)
                        .columnName(c.getColumnName())
                        .build())
            .collect(Collectors.toList()));
  }

  public ImmutableList<ColumnSchema> getIndexEntityLevelDisplayHintColumnSchemas() {
    return ImmutableList.copyOf(EntityLevelDisplayHints.getColumns());
  }
}
