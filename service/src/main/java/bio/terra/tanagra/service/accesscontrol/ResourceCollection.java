package bio.terra.tanagra.service.accesscontrol;

import bio.terra.tanagra.exception.SystemException;
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

  private ResourceCollection(
      ResourceType type,
      boolean isAllResources,
      @Nullable ResourceId parentId,
      boolean isSharedPermissions,
      Permissions sharedPermissions,
      Set<ResourceId> ids,
      Map<ResourceId, Permissions> idPermissionsMap) {
    this.type = type;
    this.isAllResources = isAllResources;
    this.parentId = parentId;
    this.isSharedPermissions = isSharedPermissions;
    this.sharedPermissions = sharedPermissions;
    this.ids = ids;
    this.idPermissionsMap = idPermissionsMap;
  }

  public static ResourceCollection empty(ResourceType type, @Nullable ResourceId parentId) {
    validateParentForType(type, parentId);
    return new ResourceCollection(
        type, false, parentId, true, Permissions.empty(type), Collections.emptySet(), null);
  }

  public static ResourceCollection allResourcesAllPermissions(
      ResourceType type, @Nullable ResourceId parentId) {
    return allResourcesSamePermissions(Permissions.allActions(type), parentId);
  }

  public static ResourceCollection allResourcesSamePermissions(
      Permissions permissions, @Nullable ResourceId parentId) {
    validateParentForType(permissions.getType(), parentId);
    return new ResourceCollection(
        permissions.getType(), true, parentId, true, permissions, null, null);
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
        permissions.getType(), false, parentId, true, permissions, resources, null);
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
    return new ResourceCollection(type, false, parentId, false, null, null, idPermissionsMap);
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

  public ResourceId getParent() {
    return parentId;
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

  public Permissions getPermissions(ResourceId resource) {
    if (!contains(resource)) {
      return Permissions.empty(resource.getType());
    } else if (isSharedPermissions) {
      return sharedPermissions;
    } else {
      return idPermissionsMap.get(resource);
    }
  }

  public boolean contains(ResourceId resource) {
    if (resource.getType().hasParentResourceType() && !parentId.equals(resource.getParent())) {
      return false;
    } else if (isAllResources) {
      return true;
    } else if (isSharedPermissions) {
      return ids.contains(resource);
    } else {
      return idPermissionsMap.containsKey(resource);
    }
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
