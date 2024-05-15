package bio.terra.tanagra.service.accesscontrol;

import bio.terra.tanagra.exception.SystemException;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Collection of resource ids and their associated permissions. This is defined as a separate
 * object, instead of just using a map of resource id to permission, so that we can have a flag
 * indicating all resource ids, without having to list them individually.
 */
public final class ResourceCollection {
  private final ResourceType type;
  private final boolean isAllResources;
  private final @Nullable ResourceId parentId;
  private final boolean isSharedPermissions;
  private final Permissions sharedPermissions;
  private final Set<ResourceId> ids;
  private final Map<ResourceId, Permissions> idPermissionsMap;
  private final @Nullable String userFilter;

  @SuppressWarnings("checkstyle:ParameterNumber")
  private ResourceCollection(
      ResourceType type,
      boolean isAllResources,
      @Nullable ResourceId parentId,
      boolean isSharedPermissions,
      Permissions sharedPermissions,
      Set<ResourceId> ids,
      Map<ResourceId, Permissions> idPermissionsMap,
      @Nullable String userFilter) {
    this.type = type;
    this.isAllResources = isAllResources;
    this.parentId = parentId;
    this.isSharedPermissions = isSharedPermissions;
    this.sharedPermissions = sharedPermissions;
    this.ids = ids;
    this.idPermissionsMap = idPermissionsMap;
    this.userFilter = userFilter;
  }

  public static ResourceCollection empty(ResourceType type, @Nullable ResourceId parentId) {
    validateParentForType(type, parentId);
    return new ResourceCollection(
        type, false, parentId, true, Permissions.empty(type), Collections.emptySet(), null, null);
  }

  public static ResourceCollection allResourcesAllPermissions(
      ResourceType type, @Nullable ResourceId parentId) {
    return allResourcesAllPermissions(type, parentId, null);
  }

  public static ResourceCollection allResourcesAllPermissions(
      ResourceType type, @Nullable ResourceId parentId, @Nullable String userFilter) {
    return allResourcesSamePermissions(Permissions.allActions(type), parentId, userFilter);
  }

  public static ResourceCollection allResourcesSamePermissions(
      Permissions permissions, @Nullable ResourceId parentId) {
    return allResourcesSamePermissions(permissions, parentId, null);
  }

  public static ResourceCollection allResourcesSamePermissions(
      Permissions permissions, @Nullable ResourceId parentId, String userFilter) {
    validateParentForType(permissions.getType(), parentId);
    return new ResourceCollection(
        permissions.getType(), true, parentId, true, permissions, null, null, userFilter);
  }

  private static void validateParentForType(ResourceType type, @Nullable ResourceId parentId) {
    if (type.hasParentResourceType()
        && (parentId == null || !type.getParentResourceType().equals(parentId.getType()))) {
      throw new SystemException(
          "Parent resource required for a ResourceCollection of type " + type);
    }
  }

  public static ResourceCollection resourcesSamePermissions(
      Permissions permissions, Set<ResourceId> resources) {
    if (resources.isEmpty()) {
      throw new SystemException("Resources set cannot be empty. Call empty() instead");
    }
    ResourceId firstResource = resources.iterator().next();
    ResourceType type = firstResource.getType();
    ResourceId parentId = firstResource.getParent();
    if (!type.equals(permissions.getType())) {
      throw new SystemException(
          "Resource type mismatch building ResourceCollection with same permissions");
    }
    resources.forEach(
        r -> {
          if (r.getType().hasParentResourceType() && !r.getParent().equals(parentId)) {
            throw new SystemException("Resources must all share the same parent");
          }
        });
    return new ResourceCollection(
        permissions.getType(), false, parentId, true, permissions, resources, null, null);
  }

  public static ResourceCollection resourcesDifferentPermissions(
      Map<ResourceId, Permissions> idPermissionsMap) {
    if (idPermissionsMap.isEmpty()) {
      throw new SystemException(
          "Resources to permissions map cannot be empty. Call empty() instead");
    }
    ResourceId firstResource = idPermissionsMap.keySet().iterator().next();
    ResourceType type = firstResource.getType();
    ResourceId parentId = firstResource.getParent();
    for (Map.Entry<ResourceId, Permissions> entry : idPermissionsMap.entrySet()) {
      if (!entry.getKey().getType().equals(type) || !entry.getValue().getType().equals(type)) {
        throw new SystemException(
            "Resource type mismatch building ResourceCollection with different permissions");
      }
      if (type.hasParentResourceType() && !entry.getKey().getParent().equals(parentId)) {
        throw new SystemException("Resource does not match parent resource");
      }
    }
    return new ResourceCollection(type, false, parentId, false, null, null, idPermissionsMap, null);
  }

  public boolean isEmpty() {
    return !isAllResources
        && ((isSharedPermissions && ids.isEmpty())
            || (!isSharedPermissions && idPermissionsMap.isEmpty()));
  }

  public ResourceType getType() {
    return type;
  }

  public boolean isAllResources() {
    return isAllResources;
  }

  public boolean isSharedPermissions() {
    return isSharedPermissions;
  }

  public ResourceId getParent() {
    return parentId;
  }

  public Permissions getSharedPermissions() {
    return sharedPermissions;
  }

  public Map<ResourceId, Permissions> getIdPermissionsMap() {
    return ImmutableMap.copyOf(idPermissionsMap);
  }

  public Set<ResourceId> getResources() {
    if (isAllResources) {
      throw new SystemException("Resource collection represents all resources");
    } else if (isSharedPermissions) {
      return Collections.unmodifiableSet(ids);
    } else {
      return Collections.unmodifiableSet(idPermissionsMap.keySet());
    }
  }

  public boolean hasUserFilter() {
    return userFilter != null;
  }

  public String getUserFilter() {
    return userFilter;
  }

  public ResourceCollection filter(Permissions permissions) {
    if (isSharedPermissions) {
      return sharedPermissions.contains(permissions)
          ? this
          : ResourceCollection.empty(permissions.getType(), parentId);
    } else {
      Map<ResourceId, Permissions> filtered = new HashMap<>();
      idPermissionsMap.forEach(
          (key, value) -> {
            if (value.contains(permissions)) {
              filtered.put(key, permissions);
            }
          });
      return filtered.isEmpty()
          ? ResourceCollection.empty(permissions.getType(), parentId)
          : ResourceCollection.resourcesDifferentPermissions(filtered);
    }
  }

  public ResourceCollection slice(int offset, int limit) {
    if (isAllResources) {
      return this;
    } else if (isSharedPermissions) {
      Set<ResourceId> idsSlice =
          ids.stream()
              .sorted(Comparator.comparing(ResourceId::getId))
              .skip(offset)
              .limit(limit)
              .collect(Collectors.toSet());
      return idsSlice.isEmpty()
          ? ResourceCollection.empty(type, parentId)
          : ResourceCollection.resourcesSamePermissions(sharedPermissions, idsSlice);
    } else {
      Map<ResourceId, Permissions> idPermissionsMapSlice = new HashMap<>();
      Set<ResourceId> idsSlice =
          idPermissionsMap.keySet().stream()
              .sorted(Comparator.comparing(ResourceId::getId))
              .skip(offset)
              .limit(limit)
              .collect(Collectors.toSet());
      idsSlice.forEach(id -> idPermissionsMapSlice.put(id, idPermissionsMap.get(id)));
      return idsSlice.isEmpty()
          ? ResourceCollection.empty(type, parentId)
          : ResourceCollection.resourcesDifferentPermissions(idPermissionsMapSlice);
    }
  }
}
