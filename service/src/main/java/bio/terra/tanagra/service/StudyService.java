package bio.terra.tanagra.service;

import bio.terra.tanagra.app.configuration.FeatureConfiguration;
import bio.terra.tanagra.db.StudyDao;
import bio.terra.tanagra.service.model.Study;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StudyService {
  private final StudyDao studyDao;
  private final FeatureConfiguration featureConfiguration;

  @Autowired
  public StudyService(StudyDao studyDao, FeatureConfiguration featureConfiguration) {
    this.studyDao = studyDao;
    this.featureConfiguration = featureConfiguration;
  }

  /** Create a new study. */
  public Study createStudy(Study.Builder studyBuilder, String userEmail) {
    featureConfiguration.artifactStorageEnabledCheck();
    studyDao.createStudy(studyBuilder.createdBy(userEmail).lastModifiedBy(userEmail).build());
    return studyDao.getStudy(studyBuilder.getId());
  }

  /** Delete an existing study by ID. */
  public void deleteStudy(String id) {
    featureConfiguration.artifactStorageEnabledCheck();
    studyDao.deleteStudy(id);
  }

  /** Retrieves a list of all studies. */
  public List<Study> getAllStudies(int offset, int limit) {
    featureConfiguration.artifactStorageEnabledCheck();
    return studyDao.getAllStudies(offset, limit);
  }

  /** Retrieves a list of existing studies by ID. */
  public List<Study> getStudies(List<String> ids, int offset, int limit) {
    featureConfiguration.artifactStorageEnabledCheck();
    // If the incoming list is empty, the caller does not have permission to see any
    // studies, so we return an empty list.
    if (ids.isEmpty()) {
      return Collections.emptyList();
    }
    return studyDao.getStudiesMatchingList(new HashSet<>(ids), offset, limit);
  }

  /** Retrieves an existing study by ID. */
  public Study getStudy(String id) {
    featureConfiguration.artifactStorageEnabledCheck();
    return studyDao.getStudy(id);
  }

  /**
   * Update an existing study. Currently, can change the study's display name or description.
   *
   * @param id study ID
   * @param displayName name to change - may be null
   * @param description description to change - may be null
   */
  public Study updateStudy(
      String id,
      String lastModifiedBy,
      @Nullable String displayName,
      @Nullable String description) {
    featureConfiguration.artifactStorageEnabledCheck();
    studyDao.updateStudy(id, lastModifiedBy, displayName, description);
    return studyDao.getStudy(id);
  }

  /**
   * Update an existing study's properties.
   *
   * @param id study ID
   * @param properties list of keys in properties
   */
  public Study updateStudyProperties(
      String id, String lastModifiedBy, Map<String, String> properties) {
    featureConfiguration.artifactStorageEnabledCheck();
    studyDao.updateStudyProperties(id, lastModifiedBy, properties);
    return studyDao.getStudy(id);
  }

  /**
   * Update an existing study's properties.
   *
   * @param id study ID
   * @param propertyKeys list of keys in properties
   */
  public Study deleteStudyProperties(String id, String lastModifiedBy, List<String> propertyKeys) {
    featureConfiguration.artifactStorageEnabledCheck();
    studyDao.deleteStudyProperties(id, lastModifiedBy, propertyKeys);
    return studyDao.getStudy(id);
  }
}
