package bio.terra.tanagra.service.underlay;

import bio.terra.tanagra.service.search.Attribute;
import bio.terra.tanagra.service.search.Entity;
import bio.terra.tanagra.service.search.UnderlaySqlResolver;
import java.util.Optional;

public class Underlay {

  private final String name;

  public Underlay(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public Optional<Entity> getEntity(String entityName) {
    return Optional.empty();
  }

  public Optional<Attribute> getAttribute(Entity entity, String attributeName) {
    return Optional.empty();
  }

  public UnderlaySqlResolver getUnderlaySqlResolver() {
    return null;
  }
}
