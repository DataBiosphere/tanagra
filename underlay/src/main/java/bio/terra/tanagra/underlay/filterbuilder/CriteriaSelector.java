package bio.terra.tanagra.underlay.filterbuilder;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.filterbuilder.FilterBuilder;
import java.lang.reflect.InvocationTargetException;

public class CriteriaSelector {
  private final String name;
  private final String filterBuilderClass;
  private final String configSerialized;
  private final boolean isEnabledForCohorts;
  private final boolean isEnabledForDataFeatureSets;

  public CriteriaSelector(
      String name,
      String filterBuilderClass,
      String configSerialized,
      boolean isEnabledForCohorts,
      boolean isEnabledForDataFeatureSets) {
    this.name = name;
    this.filterBuilderClass = filterBuilderClass;
    this.configSerialized = configSerialized;
    this.isEnabledForCohorts = isEnabledForCohorts;
    this.isEnabledForDataFeatureSets = isEnabledForDataFeatureSets;
  }

  public FilterBuilder getFilterBuilder() {
    try {
      return (FilterBuilder)
          Class.forName(filterBuilderClass)
              .getDeclaredConstructor(String.class)
              .newInstance(configSerialized);
    } catch (InstantiationException
        | IllegalAccessException
        | InvocationTargetException
        | NoSuchMethodException
        | ClassNotFoundException ex) {
      throw new InvalidConfigException(
          "Error instantiating filter builder class: " + filterBuilderClass, ex);
    }
  }

  public String getName() {
    return name;
  }

  public boolean isEnabledForCohorts() {
    return isEnabledForCohorts;
  }

  public boolean isEnabledForDataFeatureSets() {
    return isEnabledForDataFeatureSets;
  }
}
