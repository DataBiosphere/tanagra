package bio.terra.tanagra.service.filterbuilder;

import bio.terra.tanagra.generated.model.ApiLiteral;
import bio.terra.tanagra.generated.model.ApiLiteralList;
import bio.terra.tanagra.generated.model.ApiPluginData;

public class FilterBuilderInput {
  private final int pluginVersion;
  private final ApiPluginData pluginData;

  public FilterBuilderInput(int pluginVersion, ApiPluginData pluginData) {
    this.pluginVersion = pluginVersion;
    this.pluginData = pluginData;
  }

  public int getPluginVersion() {
    return pluginVersion;
  }

  public ApiLiteralList getPluginData(String key) {
    return pluginData.get(key);
  }

  public ApiLiteral getPluginDataSingleLiteral(String key) {
    return getPluginData(key).get(0);
  }

  public String getPluginDataSingleString(String key) {
    return getPluginDataSingleLiteral(key).getValueUnion().getStringVal();
  }
}
