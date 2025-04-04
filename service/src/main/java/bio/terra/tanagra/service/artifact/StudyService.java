package bio.terra.tanagra.service.artifact;

import bio.terra.common.exception.MissingRequiredFieldException;
import bio.terra.tanagra.db.StudyDao;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.artifact.model.ActivityLog;
import bio.terra.tanagra.service.artifact.model.Study;
import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StudyService {
  private final StudyDao studyDao;
  private final ActivityLogService activityLogService;

  @Autowired
  public StudyService(StudyDao studyDao, ActivityLogService activityLogService) {
    this.studyDao = studyDao;
    this.activityLogService = activityLogService;
  }

  /** Create a new study. */
  public Study createStudy(Study.Builder studyBuilder, String userEmail) {
    studyDao.createStudy(studyBuilder.createdBy(userEmail).build());
    Study study = studyDao.getStudy(studyBuilder.getId());
    activityLogService.logStudy(ActivityLog.Type.CREATE_STUDY, userEmail, study);
    return study;
  }

  /** Delete an existing study by ID. */
  public void deleteStudy(String id, String userEmail) {
    Study study = studyDao.getStudy(id);
    studyDao.deleteStudy(id);
    activityLogService.logStudy(ActivityLog.Type.DELETE_STUDY, userEmail, study);
  }

  public List<Study> listStudies(ResourceCollection authorizedIds, int offset, int limit) {
    return listStudies(authorizedIds, offset, limit, false, null);
  }

  public List<Study> listStudies(
      ResourceCollection authorizedIds,
      int offset,
      int limit,
      boolean includeDeleted,
      @Nullable Study.Builder studyFilter) {
    if (authorizedIds.isAllResources()) {
      return studyDao.getAllStudies(offset, limit, includeDeleted, studyFilter);
    } else if (authorizedIds.isEmpty()) {
      // If the incoming list is empty, the caller does not have permission to see any
      // studies, so we return an empty list.
      return Collections.emptyList();
    } else {
      return studyDao.getStudiesMatchingList(
          authorizedIds.getResources().stream()
              .map(ResourceId::getStudy)
              .collect(Collectors.toSet()),
          offset,
          limit,
          includeDeleted,
          studyFilter);
    }
  }

  /** Retrieves an existing study by ID. */
  public Study getStudy(String id) {
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
      @Nullable String description,
      @Nullable Map<String, String> properties) {
    if (displayName == null && description == null) {
      throw new MissingRequiredFieldException("Study name or description must be not null.");
    }
    studyDao.updateStudy(id, lastModifiedBy, displayName, description, properties);
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
    studyDao.deleteStudyProperties(id, lastModifiedBy, propertyKeys);
    return studyDao.getStudy(id);
  }

  @VisibleForTesting
  public void clearAllStudies() {
    studyDao.deleteAllStudies();
  }
}
