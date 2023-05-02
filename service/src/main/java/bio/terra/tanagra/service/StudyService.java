package bio.terra.tanagra.service;

import bio.terra.tanagra.app.configuration.FeatureConfiguration;
import bio.terra.tanagra.db.StudyDao;
import bio.terra.tanagra.service.artifact.Study;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.lang3.RandomStringUtils;
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
  public Study createStudy(Study.Builder studyBuilder) {
    featureConfiguration.artifactStorageEnabledCheck();

    // Generate a random 10-character alphanumeric string for the new study ID.
    String newStudyId = RandomStringUtils.randomAlphanumeric(10);

    studyDao.createStudy(studyBuilder.studyId(newStudyId).build());
    return studyDao.getStudy(newStudyId);
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
  public Study updateStudy(String id, @Nullable String displayName, @Nullable String description) {
    featureConfiguration.artifactStorageEnabledCheck();
    studyDao.updateStudy(id, displayName, description);
    return studyDao.getStudy(id);
  }

  /**
   * Update an existing study's properties.
   *
   * @param id study ID
   * @param properties list of keys in properties
   */
  public Study updateStudyProperties(String id, Map<String, String> properties) {
    featureConfiguration.artifactStorageEnabledCheck();
    studyDao.updateStudyProperties(id, properties);
    return studyDao.getStudy(id);
  }

  /**
   * Update an existing study's properties.
   *
   * @param id study ID
   * @param propertyKeys list of keys in properties
   */
  public Study deleteStudyProperties(String id, List<String> propertyKeys) {
    featureConfiguration.artifactStorageEnabledCheck();
    studyDao.deleteStudyProperties(id, propertyKeys);
    return studyDao.getStudy(id);
  }
}
