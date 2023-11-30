package bio.terra.tanagra.underlay;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.underlay.serialization.SZCriteriaOccurrence;
import bio.terra.tanagra.underlay.serialization.SZEntity;
import bio.terra.tanagra.underlay.serialization.SZGroupItems;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import bio.terra.tanagra.utils.JacksonMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class DataMappingSerialization {
  private final SZUnderlay underlay;
  private final ImmutableSet<SZEntity> entities;
  private final ImmutableSet<SZGroupItems> groupItemsEntityGroups;
  private final ImmutableSet<SZCriteriaOccurrence> criteriaOccurrenceEntityGroups;

  public DataMappingSerialization(
      SZUnderlay underlay,
      Set<SZEntity> entities,
      Set<SZGroupItems> groupItemsEntityGroups,
      Set<SZCriteriaOccurrence> criteriaOccurrenceEntityGroups) {
    this.underlay = underlay;
    this.entities = ImmutableSet.copyOf(entities);
    this.groupItemsEntityGroups = ImmutableSet.copyOf(groupItemsEntityGroups);
    this.criteriaOccurrenceEntityGroups = ImmutableSet.copyOf(criteriaOccurrenceEntityGroups);
  }

  public String serializeUnderlay() {
    try {
      return JacksonMapper.serializeJavaObject(underlay);
    } catch (JsonProcessingException jpEx) {
      throw new SystemException("Error serializing SZUnderlay", jpEx);
    }
  }

  public List<String> serializeEntities() {
    return entities.stream()
        .map(
            szEntity -> {
              try {
                return JacksonMapper.serializeJavaObject(szEntity);
              } catch (JsonProcessingException jpEx) {
                throw new SystemException("Error serializing SZEntity", jpEx);
              }
            })
        .collect(Collectors.toList());
  }

  public List<String> serializeGroupItemsEntityGroups() {
    return groupItemsEntityGroups.stream()
        .map(
            szEntity -> {
              try {
                return JacksonMapper.serializeJavaObject(szEntity);
              } catch (JsonProcessingException jpEx) {
                throw new SystemException("Error serializing SZGroupItems", jpEx);
              }
            })
        .collect(Collectors.toList());
  }

  public List<String> serializeCriteriaOccurrenceEntityGroups() {
    return criteriaOccurrenceEntityGroups.stream()
        .map(
            szEntity -> {
              try {
                return JacksonMapper.serializeJavaObject(szEntity);
              } catch (JsonProcessingException jpEx) {
                throw new SystemException("Error serializing SZCriteriaOccurrence", jpEx);
              }
            })
        .collect(Collectors.toList());
  }
}
