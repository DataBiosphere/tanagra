package bio.terra.tanagra.service;

import bio.terra.tanagra.app.configuration.FeatureConfiguration;
import bio.terra.tanagra.db.ConceptSetDao;
import bio.terra.tanagra.service.artifact.ConceptSet;
import bio.terra.tanagra.service.artifact.Criteria;
import java.util.HashSet;
import java.util.List;
import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConceptSetService {
  private final ConceptSetDao conceptSetDao;
  private final FeatureConfiguration featureConfiguration;

  @Autowired
  public ConceptSetService(ConceptSetDao conceptSetDao, FeatureConfiguration featureConfiguration) {
    this.conceptSetDao = conceptSetDao;
    this.featureConfiguration = featureConfiguration;
  }

  /** Create a new concept set. */
  public void createConceptSet(ConceptSet conceptSet) {
    featureConfiguration.artifactStorageEnabledCheck();
    conceptSetDao.createConceptSet(conceptSet);
  }

  /** Delete an existing concept set by ID. */
  public void deleteConceptSet(String studyId, String conceptSetId) {
    featureConfiguration.artifactStorageEnabledCheck();
    conceptSetDao.deleteConceptSet(studyId, conceptSetId);
  }

  /** Retrieves a list of all concept sets for a study. */
  public List<ConceptSet> getAllConceptSets(String studyId, int offset, int limit) {
    featureConfiguration.artifactStorageEnabledCheck();
    return conceptSetDao.getAllConceptSets(studyId, offset, limit);
  }

  /** Retrieves a list of concept sets by ID. */
  public List<ConceptSet> getConceptSets(
      String studyId, List<String> conceptSetIds, int offset, int limit) {
    featureConfiguration.artifactStorageEnabledCheck();
    return conceptSetDao.getConceptSetsMatchingList(
        studyId, new HashSet<>(conceptSetIds), offset, limit);
  }

  /** Retrieves a concept set by ID. */
  public ConceptSet getConceptSet(String studyId, String conceptSetId) {
    featureConfiguration.artifactStorageEnabledCheck();
    return conceptSetDao.getConceptSetOrThrow(studyId, conceptSetId);
  }

  /**
   * Update an existing concept set. Currently, can change the concept set's display name,
   * description, or criteria.
   *
   * @param studyId study ID
   * @param conceptSetId concept set ID
   * @param displayName name to change - may be null
   * @param description description to change - may be null
   * @param criteria criteria to change - may be null
   */
  @SuppressWarnings("PMD.UseObjectForClearerAPI")
  public ConceptSet updateConceptSet(
      String studyId,
      String conceptSetId,
      @Nullable String entityName,
      @Nullable String displayName,
      @Nullable String description,
      @Nullable Criteria criteria) {
    featureConfiguration.artifactStorageEnabledCheck();
    conceptSetDao.updateConceptSet(
        studyId, conceptSetId, entityName, displayName, description, criteria);
    return conceptSetDao.getConceptSetOrThrow(studyId, conceptSetId);
  }
}
