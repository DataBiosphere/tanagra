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
  private final UnderlayConfiguration underlayConfiguration;
  private Map<String, Underlay> underlaysMap;

  @Autowired
  public UnderlaysService(UnderlayConfiguration underlayConfiguration) {
    this.underlayConfiguration = underlayConfiguration;
  }

  public List<Underlay> getUnderlays() {
    return getUnderlaysMap().values().stream().collect(Collectors.toUnmodifiableList());
  }

  public Underlay getUnderlay(String name) {
    if (!getUnderlaysMap().containsKey(name)) {
      throw new NotFoundException("Underlay not found: " + name);
    }
    return getUnderlaysMap().get(name);
  }

  private Map<String, Underlay> getUnderlaysMap() {
    // TODO: Only read in the underlay config files once at startup, instead of for every call.
    if (underlaysMap == null) {
      underlaysMap = new HashMap<>();
      // read in underlays from resource files
      FileIO.setToReadResourceFiles();
      for (String underlayFile : underlayConfiguration.getUnderlayFiles()) {
        Path resourceConfigPath = Path.of("config").resolve(underlayFile);
        FileIO.setInputParentDir(resourceConfigPath.getParent());
        try {
          Underlay underlay = Underlay.fromJSON(resourceConfigPath.getFileName().toString());
          underlaysMap.put(underlay.getName(), underlay);
        } catch (IOException ioEx) {
          throw new SystemException(
              "Error reading underlay file from resources: " + resourceConfigPath, ioEx);
        }
      }
    }
    return underlaysMap;
  }
}
