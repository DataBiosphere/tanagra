package bio.terra.tanagra.service.search.testing;

import bio.terra.tanagra.service.search.AttributeVariable;
import bio.terra.tanagra.service.search.EntityVariable;
import bio.terra.tanagra.service.search.UnderlaySqlResolver;

/** A simple {@link UnderlaySqlResolver} for testing. */
public class SimpleUnderlaySqlResolver implements UnderlaySqlResolver {

  /**
   * Resolve all entities as directly related to '{underlay name}.{entity name} AS {variable}', as
   * if there were a direct correspondence between each entity and a dataset.table for it.
   */
  @Override
  public String resolveTable(EntityVariable entityVariable) {
    return String.format(
        "%s.%s AS %s",
        entityVariable.entity().underlay(),
        entityVariable.entity().name(),
        entityVariable.variable().name());
  }

  /**
   * Resolve all attributes as directly related to '{variable}.{attribute name}', as if there were a
   * direct correspondence between each entity variable and a table and each attribute as a column.
   */
  @Override
  public String resolve(AttributeVariable attributeVariable) {
    return String.format(
        "%s.%s", attributeVariable.variable().name(), attributeVariable.attribute().name());
  }
}
