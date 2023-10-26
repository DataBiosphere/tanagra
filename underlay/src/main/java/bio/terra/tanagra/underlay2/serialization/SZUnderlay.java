package bio.terra.tanagra.underlay2.serialization;

import java.util.Map;
import java.util.Set;

public class SZUnderlay {
  public String name;
  public String primaryEntity;
  public Set<String> entities;
  public Set<String> groupItemsEntityGroups;
  public Set<String> criteriaOccurrenceEntityGroups;
  public Metadata metadata;

  public static class Metadata {
    public String displayName;
    public String description;
    public Map<String, String> properties;
  }
}
