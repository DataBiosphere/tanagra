package bio.terra.tanagra.service.accesscontrol;

import bio.terra.tanagra.exception.SystemException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class Permissions {
  private final ResourceType type;
  private final Set<Action> actions;
  private final boolean isAllActions;

  private Permissions(ResourceType type, Set<Action> actions, boolean isAllActions) {
    this.type = type;
    this.actions = actions;
    this.isAllActions = isAllActions;
  }

  public static Permissions empty(ResourceType type) {
    return forActions(type);
  }

  public static Permissions allActions(ResourceType type) {
    return new Permissions(type, null, true);
  }

  public static Permissions forActions(ResourceType type, Action... actions) {
    Set<Action> actionsSet = new HashSet<>(Arrays.asList(actions));
    return Permissions.forActions(type, actionsSet);
  }

  public static Permissions forActions(ResourceType type, Set<Action> actions) {
    if (!type.getActions().containsAll(actions)) {
      throw new SystemException("Permissions don't match resource type " + type);
    }
    return new Permissions(type, actions, false);
  }

  public ResourceType getType() {
    return type;
  }

  public boolean isAllActions() {
    return isAllActions || actions.equals(type.getActions());
  }

  public Set<Action> getActions() {
    return isAllActions() ? type.getActions() : Collections.unmodifiableSet(actions);
  }

  public boolean contains(Permissions permissions) {
    return getActions().containsAll(permissions.getActions());
  }

  public String logString() {
    return isAllActions
        ? "ALL"
        : actions.stream().map(Action::name).collect(Collectors.joining(","));
  }
}
