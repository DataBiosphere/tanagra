package bio.terra.tanagra.service.accesscontrol;

import java.util.Collection;
import java.util.Collections;

/**
 * Collection of Tanagra resource IDs. This is defined as a separate object, instead of just using a
 * Java list of ResourceIds, so that we can have a flag indicating all resource ids, without having
 * to actually list them all out.
 */
public final class ResourceIdCollection {
  private final boolean isAllResourceIds;
  private final Collection<ResourceId> resourceIds;

  private ResourceIdCollection(boolean isAllResourceIds, Collection<ResourceId> resourceIds) {
    this.isAllResourceIds = isAllResourceIds;
    this.resourceIds = resourceIds;
  }

  public static ResourceIdCollection forCollection(Collection<ResourceId> resourceIds) {
    return new ResourceIdCollection(false, resourceIds);
  }

  public static ResourceIdCollection allResourceIds() {
    return new ResourceIdCollection(true, null);
  }

  public static ResourceIdCollection empty() {
    return new ResourceIdCollection(false, null);
  }

  public boolean isAllResourceIds() {
    return isAllResourceIds;
  }

  public Collection<ResourceId> getResourceIds() {
    return resourceIds == null
        ? Collections.emptySet()
        : Collections.unmodifiableCollection(resourceIds);
  }

  public boolean isEmpty() {
    return !isAllResourceIds && (resourceIds == null || resourceIds.isEmpty());
  }
}
