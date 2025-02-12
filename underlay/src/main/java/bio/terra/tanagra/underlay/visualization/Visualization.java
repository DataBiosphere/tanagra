package bio.terra.tanagra.underlay.visualization;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.Collectors;

public class Visualization {
  private final String name;
  private final String title;
  private final ImmutableList<VisualizationData> vizDataList;

  public Visualization(String name, String title, List<VisualizationData> vizDataList) {
    this.name = name;
    this.title = title;
    this.vizDataList = ImmutableList.copyOf(vizDataList);
  }

  public String getName() {
    return name;
  }

  public String getTitle() {
    return title;
  }

  public List<VisualizationData> getVizDataList() {
    return vizDataList.stream().collect(Collectors.toUnmodifiableList());
  }
}
