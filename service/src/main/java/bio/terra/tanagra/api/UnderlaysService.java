package bio.terra.tanagra.api;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.app.configuration.UnderlayConfiguration;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.utils.FileIO;
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

  /** Retrieves a list of all underlays. */
  public List<Underlay> getUnderlays() {
    return underlaysMap.values().stream().collect(Collectors.toUnmodifiableList());
  }

  /** Retrieves a list of underlays by name. */
  public List<Underlay> getUnderlays(List<String> names) {
    return underlaysMap.values().stream()
        .filter(underlay -> names.contains(underlay.getName()))
        .collect(Collectors.toUnmodifiableList());
  }

  /** Retrieves an underlay by name. */
  public Underlay getUnderlay(String name) {
    if (!underlaysMap.containsKey(name)) {
      throw new NotFoundException("Underlay not found: " + name);
    }
    return underlaysMap.get(name);
  }

  /** Retrieves an entity by name for an underlay. */
  public Entity getEntity(String underlayName, String entityName) {
    Underlay underlay = getUnderlay(underlayName);
    if (!underlay.getEntities().containsKey(entityName)) {
      throw new NotFoundException("Entity not found: " + underlayName + ", " + entityName);
    }
    return underlay.getEntity(entityName);
  }
}
