package bio.terra.tanagra.underlay.entitymodel;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.exception.SystemException;
import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.annotation.Nullable;

public final class Entity {
  private final String name;
  private final String displayName;
  private @Nullable final String description;
  private final boolean isPrimary;
  private final ImmutableList<Attribute> attributes;
  private final ImmutableList<Hierarchy> hierarchies;
  private final ImmutableList<Attribute> optimizeGroupByAttributes;
  private final boolean hasTextSearch;
  private final ImmutableList<Attribute> optimizeTextSearchAttributes;
  private final String sourceQueryTableName;

  @SuppressWarnings({"checkstyle:ParameterNumber", "PMD.ExcessiveParameterList"})
  public Entity(
      String name,
      String displayName,
      String description,
      boolean isPrimary,
      List<Attribute> attributes,
      List<Hierarchy> hierarchies,
      List<Attribute> optimizeGroupByAttributes,
      boolean hasTextSearch,
      List<Attribute> optimizeTextSearchAttributes,
      String sourceQueryTableName) {
    this.name = name;
    this.displayName = displayName;
    this.description = description;
    this.isPrimary = isPrimary;
    this.attributes = ImmutableList.copyOf(attributes);
    this.hierarchies = ImmutableList.copyOf(hierarchies);
    this.optimizeGroupByAttributes = ImmutableList.copyOf(optimizeGroupByAttributes);
    this.hasTextSearch = hasTextSearch;
    this.optimizeTextSearchAttributes = ImmutableList.copyOf(optimizeTextSearchAttributes);
    this.sourceQueryTableName = sourceQueryTableName;
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

  public ImmutableList<Hierarchy> getHierarchies() {
    return hierarchies;
  }

  public boolean hasHierarchies() {
    return !hierarchies.isEmpty();
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

  public ImmutableList<Attribute> getOptimizeGroupByAttributes() {
    return optimizeGroupByAttributes;
  }

  public boolean hasTextSearch() {
    return hasTextSearch;
  }

  public ImmutableList<Attribute> getOptimizeTextSearchAttributes() {
    return optimizeTextSearchAttributes;
  }

  public String getSourceQueryTableName() {
    return sourceQueryTableName;
  }

  public boolean supportsSourceQueries() {
    return sourceQueryTableName != null;
  }
}
