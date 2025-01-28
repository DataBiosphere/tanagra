package bio.terra.tanagra.underlay.visualization;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.Collectors;

public class VisualizationData {
  private final ImmutableList<VisualizationDataKey> keys;
  private final ImmutableList<VisualizationDataValue> values;

  public VisualizationData(List<VisualizationDataKey> keys, List<VisualizationDataValue> values) {
    this.keys = ImmutableList.copyOf(keys);
    this.values = ImmutableList.copyOf(values);
  }

  public List<VisualizationDataKey> getKeys() {
    return keys.stream().collect(Collectors.toUnmodifiableList());
  }

  public List<VisualizationDataValue> getValues() {
    return values.stream().collect(Collectors.toUnmodifiableList());
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
