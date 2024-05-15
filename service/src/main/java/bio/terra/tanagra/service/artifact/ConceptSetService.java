package bio.terra.tanagra.service.artifact;

import bio.terra.tanagra.app.configuration.FeatureConfiguration;
import bio.terra.tanagra.db.ConceptSetDao;
import bio.terra.tanagra.service.UnderlayService;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.artifact.model.ConceptSet;
import bio.terra.tanagra.service.artifact.model.Criteria;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConceptSetService {
  private final ConceptSetDao conceptSetDao;
  private final FeatureConfiguration featureConfiguration;
  private final UnderlayService underlayService;
  private final StudyService studyService;

  @Autowired
  public ConceptSetService(
      ConceptSetDao conceptSetDao,
      FeatureConfiguration featureConfiguration,
      UnderlayService underlayService,
      StudyService studyService) {
    this.conceptSetDao = conceptSetDao;
    this.featureConfiguration = featureConfiguration;
    this.underlayService = underlayService;
    this.studyService = studyService;
  }

  public ConceptSet createConceptSet(
      String studyId, ConceptSet.Builder conceptSetBuilder, String userEmail) {
    featureConfiguration.artifactStorageEnabledCheck();

    // Make sure study and underlay are valid.
    studyService.getStudy(studyId);
    underlayService.getUnderlay(conceptSetBuilder.getUnderlay());

    // TODO: Put this validation back once the UI config overhaul is complete.
    //    // Make sure any entity-attribute pairs are valid.
    //    if (conceptSetBuilder.getExcludeOutputAttributesPerEntity() != null) {
    //      conceptSetBuilder.getExcludeOutputAttributesPerEntity().entrySet().stream()
    //          .forEach(
    //              entry -> {
    //                Entity entity = underlay.getEntity(entry.getKey());
    //                entry.getValue().stream().forEach(attrName -> entity.getAttribute(attrName));
    //              });
    //    }

    conceptSetDao.createConceptSet(
        studyId, conceptSetBuilder.createdBy(userEmail).lastModifiedBy(userEmail).build());
    return conceptSetDao.getConceptSet(conceptSetBuilder.getId());
  }

  public void deleteConceptSet(String studyId, String conceptSetId) {
    featureConfiguration.artifactStorageEnabledCheck();
    conceptSetDao.deleteConceptSet(conceptSetId);
  }

  public List<ConceptSet> listConceptSets(
      ResourceCollection authorizedConceptSetIds, int offset, int limit) {
    featureConfiguration.artifactStorageEnabledCheck();
    String studyId = authorizedConceptSetIds.getParent().getStudy();
    if (authorizedConceptSetIds.isEmpty()) {
      // If the incoming list is empty, the caller does not have permission to see any
      // concept sets, so we return an empty list.
      return Collections.emptyList();
    }

    List<ConceptSet> authorizedConceptSets;
    if (authorizedConceptSetIds.isAllResources()) {
      authorizedConceptSets = conceptSetDao.getAllConceptSets(studyId, offset, limit);
    } else {
      authorizedConceptSets =
          conceptSetDao.getConceptSetsMatchingList(
              authorizedConceptSetIds.getResources().stream()
                  .map(ResourceId::getConceptSet)
                  .collect(Collectors.toSet()),
              offset,
              limit);
    }

    // Apply user filter to conceptSet.createdBy.
    return authorizedConceptSets.stream()
        .filter(
            conceptSet ->
                !authorizedConceptSetIds.hasUserFilter()
                    || authorizedConceptSetIds.getUserFilter().equals(conceptSet.getCreatedBy()))
        .collect(Collectors.toList());
  }

  public ConceptSet getConceptSet(String studyId, String conceptSetId) {
    featureConfiguration.artifactStorageEnabledCheck();
    return conceptSetDao.getConceptSet(conceptSetId);
  }

  @SuppressWarnings("PMD.UseObjectForClearerAPI")
  public ConceptSet updateConceptSet(
      String studyId,
      String conceptSetId,
      String userEmail,
      @Nullable String displayName,
      @Nullable String description,
      @Nullable List<Criteria> criteria,
      @Nullable Map<String, List<String>> outputAttributesPerEntity) {
    featureConfiguration.artifactStorageEnabledCheck();

    // TODO: Put this validation back once the UI config overhaul is complete.
    //    // Make sure any entity-attribute pairs are valid.
    //    if (outputAttributesPerEntity != null) {
    //      ConceptSet existingConceptSet = conceptSetDao.getConceptSet(conceptSetId);
    //      Underlay underlay = underlayService.getUnderlay(existingConceptSet.getUnderlay());
    //      outputAttributesPerEntity.entrySet().stream()
    //          .forEach(
    //              entry -> {
    //                Entity entity = underlay.getEntity(entry.getKey());
    //                entry.getValue().stream().forEach(attrName -> entity.getAttribute(attrName));
    //              });
    //    }

    conceptSetDao.updateConceptSet(
        conceptSetId, userEmail, displayName, description, criteria, outputAttributesPerEntity);
    return conceptSetDao.getConceptSet(conceptSetId);
  }
}
