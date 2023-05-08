package bio.terra.tanagra.service;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.app.configuration.UnderlayConfiguration;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceIdCollection;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.utils.FileIO;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
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
    for (String underlayFile : underlayConfiguration.getFiles()) {
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

  public List<Underlay> listUnderlays(ResourceIdCollection authorizedIds) {
    if (authorizedIds.isAllResourceIds()) {
      return underlaysMap.values().stream().collect(Collectors.toUnmodifiableList());
    } else {
      // If the incoming list is empty, the caller does not have permission to see any
      // underlays, so we return an empty list.
      if (authorizedIds.isEmpty()) {
        return Collections.emptyList();
      }
      List<String> authorizedNames =
          authorizedIds.getResourceIds().stream()
              .map(ResourceId::getId)
              .collect(Collectors.toList());
      return underlaysMap.values().stream()
          .filter(underlay -> authorizedNames.contains(underlay.getName()))
          .collect(Collectors.toUnmodifiableList());
    }
  }

  public Underlay getUnderlay(String name) {
    if (!underlaysMap.containsKey(name)) {
      throw new NotFoundException("Underlay not found: " + name);
    }
    return underlaysMap.get(name);
  }

  public List<Entity> listEntities(String underlayName) {
    return getUnderlay(underlayName).getEntities().values().stream().collect(Collectors.toList());
  }

  public Entity getEntity(String underlayName, String entityName) {
    Underlay underlay = getUnderlay(underlayName);
    if (!underlay.getEntities().containsKey(entityName)) {
      throw new NotFoundException("Entity not found: " + underlayName + ", " + entityName);
    }
    return underlay.getEntity(entityName);
  }
}
