package bio.terra.tanagra.service.search.testing;

import bio.terra.tanagra.service.search.Attribute;
import bio.terra.tanagra.service.search.Entity;
import bio.terra.tanagra.service.search.UnderlaySqlResolver;

/** A simple {@link UnderlaySqlResolver} for testing. */
public class SimpleUnderlaySqlResolver implements UnderlaySqlResolver {

  /**
   * Resolve all entities as directly related to '{underlay name}.{entity name}', as if there were a
   * direct correspondence between each entity and a dataset.table for it.
   */
  @Override
  public String resolveTable(Entity entity) {
    return String.format("%s.%s", entity.underlay(), entity.name());
  }

  /**
   * Resolve all attributes as directly related to '{entity name}.{attribute name}', as if there
   * were a direct correspondence between each entity and a table and each attribute as a column.
   */
  @Override
  public String resolve(Attribute attribute) {
    return String.format("%s.%s", attribute.entity().name(), attribute.name());
  }
}
