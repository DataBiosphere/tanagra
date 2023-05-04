package bio.terra.tanagra.service;

import bio.terra.tanagra.app.configuration.FeatureConfiguration;
import bio.terra.tanagra.db.ConceptSetDao;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceIdCollection;
import bio.terra.tanagra.service.artifact.ConceptSet;
import bio.terra.tanagra.service.artifact.Criteria;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConceptSetService {
  private final ConceptSetDao conceptSetDao;
  private final FeatureConfiguration featureConfiguration;
  private final UnderlaysService underlaysService;
  private final StudyService studyService;

  @Autowired
  public ConceptSetService(
      ConceptSetDao conceptSetDao,
      FeatureConfiguration featureConfiguration,
      UnderlaysService underlaysService,
      StudyService studyService) {
    this.conceptSetDao = conceptSetDao;
    this.featureConfiguration = featureConfiguration;
    this.underlaysService = underlaysService;
    this.studyService = studyService;
  }

  public ConceptSet createConceptSet(
      String studyId, ConceptSet.Builder conceptSetBuilder, String userEmail) {
    featureConfiguration.artifactStorageEnabledCheck();

    // Make sure underlay name, study id, and entity are valid.
    studyService.getStudy(studyId);
    underlaysService.getEntity(conceptSetBuilder.getUnderlay(), conceptSetBuilder.getEntity());

    conceptSetDao.createConceptSet(
        studyId, conceptSetBuilder.createdBy(userEmail).lastModifiedBy(userEmail).build());
    return conceptSetDao.getConceptSet(conceptSetBuilder.getId());
  }

  public void deleteConceptSet(String studyId, String conceptSetId) {
    featureConfiguration.artifactStorageEnabledCheck();
    conceptSetDao.deleteConceptSet(conceptSetId);
  }

  public List<ConceptSet> listConceptSets(
      ResourceIdCollection authorizedConceptSetIds, String studyId, int offset, int limit) {
    featureConfiguration.artifactStorageEnabledCheck();
    if (authorizedConceptSetIds.isAllResourceIds()) {
      return conceptSetDao.getAllConceptSets(studyId, offset, limit);
    } else {
      return conceptSetDao.getConceptSetsMatchingList(
          authorizedConceptSetIds.getResourceIds().stream()
              .map(ResourceId::getId)
              .collect(Collectors.toSet()),
          offset,
          limit);
    }
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
      @Nullable String entity,
      @Nullable List<Criteria> criteria) {
    featureConfiguration.artifactStorageEnabledCheck();

    // Make sure entity name is valid.
    if (entity != null) {
      ConceptSet conceptSet = conceptSetDao.getConceptSet(conceptSetId);
      underlaysService.getEntity(conceptSet.getUnderlay(), entity);
    }
    conceptSetDao.updateConceptSet(
        conceptSetId, userEmail, displayName, description, entity, criteria);
    return conceptSetDao.getConceptSet(conceptSetId);
  }
}
