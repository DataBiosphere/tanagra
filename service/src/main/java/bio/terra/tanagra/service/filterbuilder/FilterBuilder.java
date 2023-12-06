package bio.terra.tanagra.service.filterbuilder;

import bio.terra.tanagra.generated.model.ApiFilter;
import bio.terra.tanagra.service.filterbuilder.impl.Attribute;
import java.util.function.Supplier;

public abstract class FilterBuilder {
  public enum Type {
    ATTRIBUTE(() -> new Attribute());
    private Supplier<FilterBuilder> createNewInstanceFn;

    Type(Supplier<FilterBuilder> createNewInstanceFn) {
      this.createNewInstanceFn = createNewInstanceFn;
    }

    public FilterBuilder createNewInstance() {
      return createNewInstanceFn.get();
    }
  }

  protected FilterBuilder() {}

  public void initialize() {}

  protected int getCurrentVersion() {
    return 0;
  }

  protected abstract ApiFilter buildFilter(FilterBuilderInput input);

  protected FilterBuilderInput upgradeInput(FilterBuilderInput inputFromOlderVersion) {
    throw new UnsupportedOperationException(
        "No upgrade defined from version "
            + inputFromOlderVersion.getPluginVersion()
            + " to version "
            + getCurrentVersion());
  }

  public ApiFilter upgradeVersionAndBuildFilter(FilterBuilderInput input) {
    if (input.getPluginVersion() == getCurrentVersion()) {
      return buildFilter(input);
    } else if (input.getPluginVersion() < getCurrentVersion()) {
      return buildFilter(upgradeInput(input));
    } else {
      throw new IllegalArgumentException(
          "Plugin version "
              + input.getPluginVersion()
              + " is greater than the current version "
              + getCurrentVersion());
    }
  }
}
