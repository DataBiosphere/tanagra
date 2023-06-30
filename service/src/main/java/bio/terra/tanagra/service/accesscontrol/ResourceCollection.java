package bio.terra.tanagra.service.accesscontrol;

import bio.terra.tanagra.exception.SystemException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Collection of resource ids and their associated permissions. This is defined as a separate
 * object, instead of just using a map of resource id to permission, so that we can have a flag
 * indicating all resource ids, without having to list them individually.
 */
public final class ResourceCollection {
  private final ResourceType type;
  private final boolean isAllResources;
  private final boolean isSharedPermissions;
  private final Permissions sharedPermissions;
  private final Set<ResourceId> ids;
  private final Map<ResourceId, Permissions> idPermissionsMap;

  private ResourceCollection(
      ResourceType type,
      boolean isAllResources,
      boolean isSharedPermissions,
      Permissions sharedPermissions,
      Set<ResourceId> ids,
      Map<ResourceId, Permissions> idPermissionsMap) {
    this.type = type;
    this.isAllResources = isAllResources;
    this.isSharedPermissions = isSharedPermissions;
    this.sharedPermissions = sharedPermissions;
    this.ids = ids;
    this.idPermissionsMap = idPermissionsMap;
  }

  public static ResourceCollection empty(ResourceType type) {
    return new ResourceCollection(
        type, false, true, Permissions.empty(type), Collections.emptySet(), null);
  }

  public static ResourceCollection allResourcesAllPermissions(ResourceType type) {
    return allResourcesSamePermissions(Permissions.allActions(type));
  }

  public static ResourceCollection allResourcesSamePermissions(Permissions permissions) {
    return new ResourceCollection(permissions.getType(), true, true, permissions, null, null);
  }

  public static ResourceCollection resourcesSamePermissions(
      Permissions permissions, Set<ResourceId> resources) {
    resources.stream()
        .forEach(
            r -> {
              if (!r.getType().equals(permissions.getType())) {
                throw new SystemException(
                    "Resource type mismatch building ResourceCollection with same permissions");
              }
            });
    return new ResourceCollection(permissions.getType(), false, true, permissions, resources, null);
  }

  public static ResourceCollection resourcesDifferentPermissions(
      Map<ResourceId, Permissions> idPermissionsMap) {
    if (idPermissionsMap.isEmpty()) {
      throw new SystemException(
          "Resources to permissions map cannot be empty. Call empty() instead");
    }
    ResourceType type = null;
    for (Map.Entry<ResourceId, Permissions> entry : idPermissionsMap.entrySet()) {
      if (type == null) {
        type = entry.getKey().getType();
      }
      if (!entry.getKey().getType().equals(type) || !entry.getValue().getType().equals(type)) {
        throw new SystemException(
            "Resource type mismatch building ResourceCollection with different permissions");
      }
    }
    return new ResourceCollection(type, false, false, null, null, idPermissionsMap);
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
    if (isAllResources) {
      return sharedPermissions;
    } else if (isSharedPermissions) {
      return ids.contains(resource) ? sharedPermissions : Permissions.empty(resource.getType());
    } else {
      return idPermissionsMap.containsKey(resource)
          ? idPermissionsMap.get(resource)
          : Permissions.empty(resource.getType());
    }
  }

  public boolean contains(ResourceId resource) {
    if (isAllResources) {
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
          : ResourceCollection.empty(permissions.getType());
    } else {
      Map<ResourceId, Permissions> filtered = new HashMap<>();
      idPermissionsMap.entrySet().stream()
          .forEach(
              entry -> {
                if (entry.getValue().contains(permissions)) {
                  filtered.put(entry.getKey(), permissions);
                }
              });
      return ResourceCollection.resourcesDifferentPermissions(filtered);
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
      return ResourceCollection.resourcesSamePermissions(sharedPermissions, idsSlice);
    } else {
      Map<ResourceId, Permissions> idPermissionsMapSlice = new HashMap<>();
      Set<ResourceId> idsSlice =
          idPermissionsMap.keySet().stream()
              .sorted(Comparator.comparing(ResourceId::getId))
              .skip(offset)
              .limit(limit)
              .collect(Collectors.toSet());
      idsSlice.stream().forEach(id -> idPermissionsMapSlice.put(id, idPermissionsMap.get(id)));
      return ResourceCollection.resourcesDifferentPermissions(idPermissionsMapSlice);
    }
  }
}
