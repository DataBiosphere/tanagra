package bio.terra.tanagra.service;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.app.configuration.UnderlayConfiguration;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.serialization.SZService;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import bio.terra.tanagra.utils.FileIO;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Service to handle underlay operations. */
@Component
public class UnderlayService {
  private final Map<String, Underlay> underlaysMap;

  @Autowired
  public UnderlayService(UnderlayConfiguration underlayConfiguration) {
    // Read in underlays from resource files.
    Map<String, Underlay> underlaysMapBuilder = new HashMap<>();
    FileIO.setToReadResourceFiles();
    for (String serviceConfig : underlayConfiguration.getFiles()) {
      SZService szService = ConfigReader.deserializeService(serviceConfig);
      SZUnderlay szUnderlay = ConfigReader.deserializeUnderlay(szService.underlay);
      Underlay underlay = Underlay.fromConfig(szService.bigQuery, szUnderlay);
      underlaysMapBuilder.put(underlay.getName(), underlay);
    }
    this.underlaysMap = underlaysMapBuilder;
  }

  public List<Underlay> listUnderlays(ResourceCollection authorizedIds) {
    if (authorizedIds.isAllResources()) {
      return underlaysMap.values().stream().collect(Collectors.toUnmodifiableList());
    } else {
      // If the incoming list is empty, the caller does not have permission to see any
      // underlays, so we return an empty list.
      if (authorizedIds.isEmpty()) {
        return Collections.emptyList();
      }
      List<String> authorizedNames =
          authorizedIds.getResources().stream()
              .map(ResourceId::getUnderlay)
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
}
