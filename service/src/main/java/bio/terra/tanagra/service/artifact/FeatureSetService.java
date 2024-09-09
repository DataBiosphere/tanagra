package bio.terra.tanagra.service.artifact;

import bio.terra.tanagra.db.FeatureSetDao;
import bio.terra.tanagra.service.UnderlayService;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.artifact.model.Criteria;
import bio.terra.tanagra.service.artifact.model.FeatureSet;
import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FeatureSetService {
  private final FeatureSetDao featureSetDao;
  private final UnderlayService underlayService;
  private final StudyService studyService;

  @Autowired
  public FeatureSetService(
      FeatureSetDao featureSetDao, UnderlayService underlayService, StudyService studyService) {
    this.featureSetDao = featureSetDao;
    this.underlayService = underlayService;
    this.studyService = studyService;
  }

  public FeatureSet createFeatureSet(
      String studyId, FeatureSet.Builder featureSetBuilder, String userEmail) {
    // Make sure study and underlay are valid.
    studyService.getStudy(studyId);
    underlayService.getUnderlay(featureSetBuilder.getUnderlay());

    // TODO: Put this validation back once the UI config overhaul is complete.
    //    // Make sure any entity-attribute pairs are valid.
    //    if (featureSetBuilder.getExcludeOutputAttributesPerEntity() != null) {
    //      featureSetBuilder.getExcludeOutputAttributesPerEntity().entrySet().stream()
    //          .forEach(
    //              entry -> {
    //                Entity entity = underlay.getEntity(entry.getKey());
    //                entry.getValue().stream().forEach(attrName -> entity.getAttribute(attrName));
    //              });
    //    }

    featureSetDao.createFeatureSet(
        studyId, featureSetBuilder.createdBy(userEmail).lastModifiedBy(userEmail).build());
    return featureSetDao.getFeatureSet(featureSetBuilder.getId());
  }

  public void deleteFeatureSet(String studyId, String featureSetId) {
    featureSetDao.deleteFeatureSet(featureSetId);
  }

  public List<FeatureSet> listFeatureSets(
      ResourceCollection authorizedFeatureSetIds, int offset, int limit) {
    String studyId = authorizedFeatureSetIds.getParent().getStudy();
    if (authorizedFeatureSetIds.isAllResources()) {
      return featureSetDao.getAllFeatureSets(studyId, offset, limit);
    } else if (authorizedFeatureSetIds.isEmpty()) {
      // If the incoming list is empty, the caller does not have permission to see any
      // feature sets, so we return an empty list.
      return Collections.emptyList();
    } else {
      return featureSetDao.getFeatureSetsMatchingList(
          authorizedFeatureSetIds.getResources().stream()
              .map(ResourceId::getFeatureSet)
              .collect(Collectors.toSet()),
          offset,
          limit);
    }
  }

  public FeatureSet getFeatureSet(String studyId, String featureSetId) {
    return featureSetDao.getFeatureSet(featureSetId);
  }

  @SuppressWarnings("PMD.UseObjectForClearerAPI")
  public FeatureSet updateFeatureSet(
      String studyId,
      String featureSetId,
      String userEmail,
      @Nullable String displayName,
      @Nullable String description,
      @Nullable List<Criteria> criteria,
      @Nullable Map<String, List<String>> outputAttributesPerEntity) {
    // TODO: Put this validation back once the UI config overhaul is complete.
    //    // Make sure any entity-attribute pairs are valid.
    //    if (outputAttributesPerEntity != null) {
    //      FeatureSet existingFeatureSet = featureSetDao.getFeatureSet(featureSetId);
    //      Underlay underlay = underlayService.getUnderlay(existingFeatureSet.getUnderlay());
    //      outputAttributesPerEntity.entrySet().stream()
    //          .forEach(
    //              entry -> {
    //                Entity entity = underlay.getEntity(entry.getKey());
    //                entry.getValue().stream().forEach(attrName -> entity.getAttribute(attrName));
    //              });
    //    }

    featureSetDao.updateFeatureSet(
        featureSetId, userEmail, displayName, description, criteria, outputAttributesPerEntity);
    return featureSetDao.getFeatureSet(featureSetId);
  }

  public FeatureSet cloneFeatureSet(
      String studyId,
      String featureSetId,
      String userEmail,
      @Nullable String displayName,
      @Nullable String description) {
    FeatureSet original = getFeatureSet(studyId, featureSetId);
    FeatureSet.Builder featureSetBuilder =
        FeatureSet.builder()
            .underlay(original.getUnderlay())
            .displayName(
                displayName != null ? displayName : "Copy of: " + original.getDisplayName())
            .description(
                description != null ? description : "Copy of: " + original.getDescription())
            .createdBy(userEmail)
            .lastModifiedBy(userEmail)
            // Shallow copy criteria and attributes: they are written to DB and fetched for return
            // Any ids are used in conjunction with concept_set_id as primary key
            .criteria(original.getCriteria())
            .excludeOutputAttributesPerEntity(original.getExcludeOutputAttributesPerEntity());

    featureSetDao.createFeatureSet(studyId, featureSetBuilder.build());
    return featureSetDao.getFeatureSet(featureSetBuilder.getId());
  }
}
