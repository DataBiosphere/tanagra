package bio.terra.tanagra.underlay.visualization;

import java.util.ArrayList;
import java.util.List;

public class Visualization {
  private final String name;
  private final String title;
  private final List<VisualizationData> vizDataList = new ArrayList<>();

  public Visualization(String name, String title, List<VisualizationData> vizDataList) {
    this.name = name;
    this.title = title;

    if (vizDataList != null) {
      this.vizDataList.addAll(vizDataList);
    }
  }

  public void addVizData(VisualizationData vizData) {
    vizDataList.add(vizData);
  }

  public String getName() {
    return name;
  }

  public String getTitle() {
    return title;
  }

  public List<VisualizationData> getVizDataList() {
    return vizDataList;
  }
}
