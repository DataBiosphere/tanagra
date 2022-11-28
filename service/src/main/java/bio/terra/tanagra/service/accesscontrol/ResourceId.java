package bio.terra.tanagra.service.accesscontrol;

/**
 * Strongly typed wrapper around a String identifying a Tanagra resource (e.g. underlay, study,
 * cohort). In the future, we may need to support other data types.
 *
 * <p>Strongly instead of Stringly typing this makes its usage clearer and more type safe.
 */
public class ResourceId {
  private final String id;

  public ResourceId(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }
}
