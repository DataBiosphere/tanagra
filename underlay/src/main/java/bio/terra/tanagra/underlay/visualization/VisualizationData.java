package bio.terra.tanagra.underlay.visualization;

import java.util.ArrayList;
import java.util.List;

public class VisualizationData {
  private final List<VisualizationDataKey> keys = new ArrayList<>();
  private final List<VisualizationDataValue> values = new ArrayList<>();

  public VisualizationData(List<VisualizationDataKey> keys, List<VisualizationDataValue> values) {
    if (keys != null) {
      this.keys.addAll(keys);
    }

    if (values != null) {
      this.values.addAll(values);
    }
  }

  public void addKey(VisualizationDataKey dataKey) {
    keys.add(dataKey);
  }

  public void addValue(VisualizationDataValue dataValue) {
    values.add(dataValue);
  }

  public List<VisualizationDataKey> getKeys() {
    return keys;
  }

  public List<VisualizationDataValue> getValues() {
    return values;
  }

  public boolean isForKey(String dataKey) {
    return keys.stream()
            .map(VisualizationDataKey::name)
            .filter(k -> k.equals(dataKey))
            .distinct()
            .count()
        == 1;
  }

  public record VisualizationDataKey(String name, Integer numericId, String stringId) {}

  public record VisualizationDataValue(int numeric, List<Integer> quartiles) {}
}
