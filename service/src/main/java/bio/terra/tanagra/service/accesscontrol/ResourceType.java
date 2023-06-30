package bio.terra.tanagra.service.accesscontrol;

import java.util.Collections;
import java.util.Set;

/** Names of Tanagra resources and the actions that are possible for each. */
public enum ResourceType {
  UNDERLAY(Set.of(Action.READ, Action.QUERY_INSTANCES, Action.QUERY_COUNTS)),
  STUDY(
      Set.of(
          Action.READ,
          Action.CREATE,
          Action.UPDATE,
          Action.DELETE,
          Action.CREATE_COHORT,
          Action.CREATE_CONCEPT_SET)),
  COHORT(
      Set.of(
          Action.READ,
          Action.UPDATE,
          Action.DELETE,
          Action.CREATE_REVIEW,
          Action.CREATE_ANNOTATION_KEY),
      STUDY),
  CONCEPT_SET(Set.of(Action.READ, Action.UPDATE, Action.DELETE), STUDY),
  COHORT_REVIEW(
      Set.of(
          Action.READ, Action.UPDATE, Action.DELETE, Action.QUERY_INSTANCES, Action.QUERY_COUNTS),
      COHORT),
  ANNOTATION_KEY(Set.of(Action.READ, Action.UPDATE, Action.DELETE), COHORT);

  private final Set<Action> actions;
  private final ResourceType parentResourceType;

  ResourceType(Set<Action> actions) {
    this.actions = actions;
    this.parentResourceType = null;
  }

  ResourceType(Set<Action> actions, ResourceType parentResourceType) {
    this.actions = actions;
    this.parentResourceType = parentResourceType;
  }

  public Set<Action> getActions() {
    return Collections.unmodifiableSet(actions);
  }

  public boolean hasParentResourceType() {
    return parentResourceType != null;
  }

  public ResourceType getParentResourceType() {
    return parentResourceType;
  }
}
