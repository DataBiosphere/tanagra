package bio.terra.tanagra.service.accesscontrol;

import java.util.List;

/** Names of Tanagra resources and the actions that are possible for each. */
public enum ResourceType {
  UNDERLAY(List.of(Action.READ, Action.QUERY_INSTANCES, Action.QUERY_COUNTS)),
  STUDY(List.of(Action.READ, Action.CREATE, Action.UPDATE, Action.DELETE)),
  COHORT(List.of(Action.READ, Action.CREATE, Action.UPDATE, Action.DELETE)),
  CONCEPT_SET(List.of(Action.READ, Action.CREATE, Action.UPDATE, Action.DELETE)),
  DATASET(List.of(Action.READ, Action.CREATE, Action.UPDATE, Action.DELETE)),
  COHORT_REVIEW(List.of(Action.READ, Action.CREATE, Action.UPDATE, Action.DELETE));

  private List<Action> actions;

  ResourceType(List<Action> actions) {
    this.actions = actions;
  }

  public boolean hasAction(Action action) {
    return actions.contains(action);
  }
}
