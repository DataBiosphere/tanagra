package bio.terra.tanagra.artifact;

import bio.terra.tanagra.app.configuration.FeatureConfiguration;
import bio.terra.tanagra.db.StudyDao;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
  public void createStudy(Study study) {
    featureConfiguration.artifactStorageEnabledCheck();
    studyDao.createStudy(study);
  }

  /** Delete an existing study by ID. */
  public void deleteStudy(UUID uuid) {
    featureConfiguration.artifactStorageEnabledCheck();
    studyDao.deleteStudy(uuid);
  }

  /** Retrieves a list of all studies. */
  public List<Study> getAllStudies(int offset, int limit) {
    featureConfiguration.artifactStorageEnabledCheck();
    return studyDao.getAllStudies(offset, limit);
  }

  /** Retrieves a list of existing studies by ID. */
  public List<Study> getStudies(List<UUID> uuids, int offset, int limit) {
    featureConfiguration.artifactStorageEnabledCheck();
    return studyDao.getStudiesMatchingList(new HashSet<>(uuids), offset, limit);
  }

  /** Retrieves an existing study by ID. */
  public Study getStudy(UUID uuid) {
    featureConfiguration.artifactStorageEnabledCheck();
    return studyDao.getStudy(uuid);
  }

  /**
   * Update an existing study. Currently, can change the study's display name or description.
   *
   * @param uuid study ID
   * @param displayName name to change - may be null
   * @param description description to change - may be null
   */
  public Study updateStudy(UUID uuid, @Nullable String displayName, @Nullable String description) {
    featureConfiguration.artifactStorageEnabledCheck();
    studyDao.updateStudy(uuid, displayName, description);
    return studyDao.getStudy(uuid);
  }

  /**
   * Update an existing study's properties.
   *
   * @param uuid study ID
   * @param properties list of keys in properties
   */
  public Study updateStudyProperties(UUID uuid, Map<String, String> properties) {
    featureConfiguration.artifactStorageEnabledCheck();
    studyDao.updateStudyProperties(uuid, properties);
    return studyDao.getStudy(uuid);
  }

  /**
   * Update an existing study's properties.
   *
   * @param uuid study ID
   * @param propertyKeys list of keys in properties
   */
  public Study deleteStudyProperties(UUID uuid, List<String> propertyKeys) {
    featureConfiguration.artifactStorageEnabledCheck();
    studyDao.deleteStudyProperties(uuid, propertyKeys);
    return studyDao.getStudy(uuid);
  }
}
