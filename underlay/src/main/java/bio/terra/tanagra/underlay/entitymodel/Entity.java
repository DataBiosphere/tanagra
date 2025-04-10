package bio.terra.tanagra.underlay.entitymodel;

import bio.terra.tanagra.exception.NotFoundException;
import bio.terra.tanagra.exception.SystemException;
import com.google.common.collect.ImmutableList;
import jakarta.annotation.Nullable;
import java.util.List;

public final class Entity {
  private final String name;
  private final String displayName;
  private @Nullable final String description;
  private final boolean isPrimary;
  private final ImmutableList<Attribute> attributes;
  private final ImmutableList<Hierarchy> hierarchies;
  private final ImmutableList<Attribute> optimizeGroupByAttributes;
  private final ImmutableList<ImmutableList<String>> optimizeSearchByAttributeNames;
  private final boolean hasTextSearch;
  private final ImmutableList<Attribute> optimizeTextSearchAttributes;
  private final String sourceQueryTableName;

  @SuppressWarnings({"checkstyle:ParameterNumber", "PMD.ExcessiveParameterList"})
  public Entity(
      String name,
      String displayName,
      @Nullable String description,
      boolean isPrimary,
      List<Attribute> attributes,
      List<Hierarchy> hierarchies,
      List<Attribute> optimizeGroupByAttributes,
      List<List<Attribute>> optimizeSearchByAttributes,
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
    this.optimizeSearchByAttributeNames =
        ImmutableList.copyOf(
            optimizeSearchByAttributes.stream()
                .map(
                    attributesList ->
                        ImmutableList.copyOf(
                            attributesList.stream().map(Attribute::getName).toList()))
                .toList());
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
        .filter(Attribute::isId)
        .findFirst()
        .orElseThrow(() -> new SystemException("No id attribute defined"));
  }

  public Attribute getVisitDateAttributeForTemporalQuery() {
    return attributes.stream()
        .filter(Attribute::isVisitDateForTemporalQuery)
        .findFirst()
        .orElseThrow(
            () -> new SystemException("No visit date attribute for temporal queries defined"));
  }

  public Attribute getVisitIdAttributeForTemporalQuery() {
    return attributes.stream()
        .filter(Attribute::isVisitIdForTemporalQuery)
        .findFirst()
        .orElseThrow(
            () -> new SystemException("No visit id attribute for temporal queries defined"));
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

  public boolean hasOptimizeSearchByAttributes() {
    return !optimizeSearchByAttributeNames.isEmpty();
  }

  public ImmutableList<ImmutableList<String>> getOptimizeSearchByAttributeNames() {
    return optimizeSearchByAttributeNames;
  }

  public boolean containsOptimizeSearchByAttributes(List<String> attributeNames) {
    return !attributeNames.isEmpty()
        && optimizeSearchByAttributeNames.stream()
            .anyMatch(attributes -> attributes.containsAll(attributeNames));
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
