package bio.terra.tanagra.underlay;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.app.configuration.UnderlayConfiguration;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.indexing.FileIO;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Service to handle underlay operations. */
@Component
public class UnderlaysService {
  private final Map<String, Underlay> underlaysMap;

  @Autowired
  public UnderlaysService(UnderlayConfiguration underlayConfiguration) {
    // read in underlays from resource files
    Map<String, Underlay> underlaysMapBuilder = new HashMap<>();
    FileIO.setToReadResourceFiles();
    for (String underlayFile : underlayConfiguration.getUnderlayFiles()) {
      Path resourceConfigPath = Path.of("config").resolve(underlayFile);
      FileIO.setInputParentDir(resourceConfigPath.getParent());
      try {
        Underlay underlay = Underlay.fromJSON(resourceConfigPath.getFileName().toString());
        underlaysMapBuilder.put(underlay.getName(), underlay);
      } catch (IOException ioEx) {
        throw new SystemException(
            "Error reading underlay file from resources: " + resourceConfigPath, ioEx);
      }
    }
    this.underlaysMap = underlaysMapBuilder;
  }

  public List<Underlay> getUnderlays() {
    return underlaysMap.values().stream().collect(Collectors.toUnmodifiableList());
  }

  public Underlay getUnderlay(String name) {
    if (!underlaysMap.containsKey(name)) {
      throw new NotFoundException("Underlay not found: " + name);
    }
    return underlaysMap.get(name);
  }

  public Entity getEntity(String underlayName, String entityName) {
    Underlay underlay = getUnderlay(underlayName);
    if (!underlay.getEntities().containsKey(entityName)) {
      throw new NotFoundException("Entity not found: " + underlayName + ", " + entityName);
    }
    return underlay.getEntity(entityName);
  }
}
