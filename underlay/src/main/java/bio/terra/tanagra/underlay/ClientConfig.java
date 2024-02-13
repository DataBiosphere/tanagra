package bio.terra.tanagra.underlay;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.underlay.serialization.SZCriteriaOccurrence;
import bio.terra.tanagra.underlay.serialization.SZCriteriaSelector;
import bio.terra.tanagra.underlay.serialization.SZEntity;
import bio.terra.tanagra.underlay.serialization.SZGroupItems;
import bio.terra.tanagra.underlay.serialization.SZPrepackagedCriteria;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import bio.terra.tanagra.utils.JacksonMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ClientConfig {
  private final SZUnderlay underlay;
  private final ImmutableSet<SZEntity> entities;
  private final ImmutableSet<SZGroupItems> groupItemsEntityGroups;
  private final ImmutableSet<SZCriteriaOccurrence> criteriaOccurrenceEntityGroups;
  private final ImmutableSet<SZCriteriaSelector> criteriaSelectors;
  private final ImmutableSet<SZPrepackagedCriteria> prepackagedDataFeatures;

  public ClientConfig(
      SZUnderlay underlay,
      Set<SZEntity> entities,
      Set<SZGroupItems> groupItemsEntityGroups,
      Set<SZCriteriaOccurrence> criteriaOccurrenceEntityGroups,
      Set<SZCriteriaSelector> criteriaSelectors,
      Set<SZPrepackagedCriteria> prepackagedDataFeatures) {
    this.underlay = underlay;
    this.entities = ImmutableSet.copyOf(entities);
    this.groupItemsEntityGroups = ImmutableSet.copyOf(groupItemsEntityGroups);
    this.criteriaOccurrenceEntityGroups = ImmutableSet.copyOf(criteriaOccurrenceEntityGroups);
    this.criteriaSelectors = ImmutableSet.copyOf(criteriaSelectors);
    this.prepackagedDataFeatures = ImmutableSet.copyOf(prepackagedDataFeatures);
  }

  public String serializeUnderlay() {
    try {
      return JacksonMapper.serializeJavaObject(underlay);
    } catch (JsonProcessingException jpEx) {
      throw new SystemException("Error serializing SZUnderlay", jpEx);
    }
  }

  public List<String> serializeEntities() {
    return serializeObjects(entities);
  }

  public List<String> serializeGroupItemsEntityGroups() {
    return serializeObjects(groupItemsEntityGroups);
  }

  public List<String> serializeCriteriaOccurrenceEntityGroups() {
    return serializeObjects(criteriaOccurrenceEntityGroups);
  }

  public List<String> serializeCriteriaSelectors() {
    return serializeObjects(criteriaSelectors);
  }

  public List<String> serializePrepackagedDataFeatures() {
    return serializeObjects(prepackagedDataFeatures);
  }

  private static <T> List<String> serializeObjects(Collection<T> objects) {
    return objects.stream()
        .map(
            obj -> {
              try {
                return JacksonMapper.serializeJavaObject(obj);
              } catch (JsonProcessingException jpEx) {
                throw new SystemException("Serialization error", jpEx);
              }
            })
        .collect(Collectors.toList());
  }
}
