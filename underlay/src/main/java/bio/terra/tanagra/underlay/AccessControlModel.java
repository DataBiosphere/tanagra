package bio.terra.tanagra.underlay;

import bio.terra.tanagra.serialization.UFAccessControlModel;
import java.util.HashMap;
import java.util.Map;

/**
 * Supported models of access control. Each of these corresponds to an implementation of the
 * AccessControlPlugin class. This mapping is not specified here to avoid a circular dependency
 * between the underlay and service Gradle sub-projects.
 */
public final class AccessControlModel {
  public enum Type {
    OPEN,
    VUMC_ADMIN
  }

  private final Type type;
  private final Map<String, String> params;

  private AccessControlModel(Type type, Map<String, String> params) {
    this.type = type;
    this.params = params;
  }

  /** Default access control model is open access (i.e. everything is allowed). */
  public static AccessControlModel getDefault() {
    return new AccessControlModel(Type.OPEN, new HashMap<>());
  }

  public static AccessControlModel fromSerialized(UFAccessControlModel serialized) {
    return new AccessControlModel(serialized.getType(), serialized.getParams());
  }

  public Type getType() {
    return type;
  }

  public Map<String, String> getParams() {
    return params;
  }
}
