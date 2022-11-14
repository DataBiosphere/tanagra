package bio.terra.tanagra.api.accesscontrol;

import static bio.terra.tanagra.api.accesscontrol.Action.*;

import java.util.List;

/** Names of Tanagra resources and the actions that are possible for each. */
public enum ResourceType {
  UNDERLAY(List.of(READ, QUERY_INSTANCES, QUERY_COUNTS)),
  STUDY(List.of(READ, CREATE, UPDATE, DELETE)),
  COHORT(List.of(READ, CREATE, UPDATE, DELETE)),
  CONCEPT_SET(List.of(READ, CREATE, UPDATE, DELETE)),
  DATASET(List.of(READ, CREATE, UPDATE, DELETE)),
  COHORT_REVIEW(List.of(READ, CREATE, UPDATE, DELETE));

  private List<Action> actions;

  ResourceType(List<Action> actions) {
    this.actions = actions;
  }

  public boolean hasAction(Action action) {
    return actions.contains(action);
  }
}
