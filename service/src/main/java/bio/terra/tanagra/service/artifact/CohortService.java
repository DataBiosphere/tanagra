package bio.terra.tanagra.service.artifact;

import static bio.terra.tanagra.service.artifact.model.Study.MAX_DISPLAY_NAME_LENGTH;

import bio.terra.common.exception.BadRequestException;
import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.query.count.CountQueryRequest;
import bio.terra.tanagra.api.query.count.CountQueryResult;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.api.query.list.ListQueryResult;
import bio.terra.tanagra.api.query.list.OrderBy;
import bio.terra.tanagra.api.shared.OrderByDirection;
import bio.terra.tanagra.db.ArtifactsDao;
import bio.terra.tanagra.db.CohortDao;
import bio.terra.tanagra.service.UnderlayService;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.artifact.model.ActivityLog;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.CohortRevision;
import bio.terra.tanagra.service.filter.FilterBuilderService;
import bio.terra.tanagra.underlay.Underlay;
import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CohortService {
  private static final Logger LOGGER = LoggerFactory.getLogger(CohortService.class);

  private final CohortDao cohortDao;
  private final UnderlayService underlayService;
  private final StudyService studyService;
  private final ArtifactsDao artifactsDao;
  private final FilterBuilderService filterBuilderService;
  private final ActivityLogService activityLogService;

  @Autowired
  public CohortService(
      CohortDao cohortDao,
      UnderlayService underlayService,
      StudyService studyService,
      ArtifactsDao artifactsDao,
      FilterBuilderService filterBuilderService,
      ActivityLogService activityLogService) {
    this.cohortDao = cohortDao;
    this.underlayService = underlayService;
    this.studyService = studyService;
    this.artifactsDao = artifactsDao;
    this.filterBuilderService = filterBuilderService;
    this.activityLogService = activityLogService;
  }

  /** Create a cohort and its first revision without any criteria. */
  public Cohort createCohort(String studyId, Cohort.Builder cohortBuilder) {
    return createCohort(studyId, cohortBuilder, Collections.emptyList());
  }

  /** Create a cohort and its first revision. */
  public Cohort createCohort(
      String studyId,
      Cohort.Builder cohortBuilder,
      List<CohortRevision.CriteriaGroupSection> sections) {
    // Make sure underlay name and study id are valid.
    underlayService.getUnderlay(cohortBuilder.getUnderlay());
    studyService.getStudy(studyId);

    // Create the first revision.
    CohortRevision firstRevision =
        CohortRevision.builder()
            .sections(sections)
            .setIsMostRecent(true)
            .setIsEditable(true)
            .createdBy(cohortBuilder.getCreatedBy())
            .build();
    cohortBuilder.addRevision(firstRevision);

    cohortDao.createCohort(studyId, cohortBuilder.build());
    Cohort cohort = cohortDao.getCohort(cohortBuilder.getId());
    activityLogService.logCohort(
        ActivityLog.Type.CREATE_COHORT, cohort.getCreatedBy(), studyId, cohort);
    return cohort;
  }

  /** Delete a cohort and all its revisions. */
  public void deleteCohort(String studyId, String cohortId, String userEmail) {
    Cohort cohort = cohortDao.getCohort(cohortId);
    cohortDao.deleteCohort(cohortId);
    activityLogService.logCohort(ActivityLog.Type.DELETE_COHORT, userEmail, studyId, cohort);
  }

  /** List cohorts with their most recent revisions. */
  public List<Cohort> listCohorts(ResourceCollection authorizedCohortIds, int offset, int limit) {
    String studyId = authorizedCohortIds.getParent().getStudy();
    if (authorizedCohortIds.isAllResources()) {
      return cohortDao.getAllCohorts(studyId, offset, limit);
    } else if (authorizedCohortIds.isEmpty()) {
      // If the incoming list is empty, the caller does not have permission to see any
      // cohorts, so we return an empty list.
      return Collections.emptyList();
    } else {
      return cohortDao.getCohortsMatchingList(
          authorizedCohortIds.getResources().stream()
              .map(ResourceId::getCohort)
              .collect(Collectors.toSet()),
          offset,
          limit);
    }
  }

  /** Retrieve a cohort with its most recent revision. */
  public Cohort getCohort(String studyId, String cohortId) {
    return cohortDao.getCohort(cohortId);
  }

  /** Update a cohort's most recent revision. */
  @SuppressWarnings("PMD.UseObjectForClearerAPI")
  public Cohort updateCohort(
      String studyId,
      String cohortId,
      String userEmail,
      @Nullable String displayName,
      @Nullable String description,
      @Nullable List<CohortRevision.CriteriaGroupSection> criteriaGroupSections) {
    cohortDao.updateCohort(cohortId, userEmail, displayName, description, criteriaGroupSections);
    return cohortDao.getCohort(cohortId);
  }

  /** Clone an existing cohort's revisions, including reviews and annotations */
  public Cohort cloneCohort(
      String studyId,
      String cohortId,
      String userEmail,
      String destinationStudyId,
      @Nullable String displayName,
      @Nullable String description) {
    Cohort original = getCohort(studyId, cohortId);

    int displayNameLen = StringUtils.length(displayName);
    if (displayNameLen > MAX_DISPLAY_NAME_LENGTH) {
      throw new BadRequestException(
          "Cohort name cannot be greater than " + MAX_DISPLAY_NAME_LENGTH + " characters");
    }

    String newDisplayName = displayName;
    if (displayNameLen == 0 && original.getDisplayName() != null) {
      newDisplayName = original.getDisplayName();
      if (studyId.equals(destinationStudyId)
          && newDisplayName.length() < MAX_DISPLAY_NAME_LENGTH + 6) {
        newDisplayName = "(Copy) " + newDisplayName;
      }
    }

    String clonedCohortId =
        artifactsDao.cloneCohort(
            cohortId,
            userEmail,
            destinationStudyId,
            newDisplayName,
            description != null ? description : original.getDescription());
    activityLogService.logCohort(ActivityLog.Type.CLONE_COHORT, userEmail, studyId, original);

    LOGGER.info(
        "Cloned cohort {} in study {} to cohort {} in study {}",
        cohortId,
        studyId,
        clonedCohortId,
        destinationStudyId);

    Cohort clonedCohort = cohortDao.getCohort(clonedCohortId);
    activityLogService.logCohort(
        ActivityLog.Type.CREATE_COHORT, userEmail, destinationStudyId, clonedCohort);
    return clonedCohort;
  }

  /**
   * @return the id of the frozen revision just created
   */
  public String createNextRevision(String studyId, String cohortId, String userEmail) {
    Cohort cohort = getCohort(studyId, cohortId);
    Long recordsCount =
        getRecordsCount(
            cohort.getUnderlay(),
            filterBuilderService.buildFilterForCohortRevision(
                cohort.getUnderlay(), cohort.getMostRecentRevision()));
    return cohortDao.createNextRevision(cohortId, null, userEmail, recordsCount);
  }

  /** Build a count query of all primary entity instance ids in the cohort. */
  public long getRecordsCount(String underlayName, EntityFilter entityFilter) {
    Underlay underlay = underlayService.getUnderlay(underlayName);
    CountQueryRequest countQueryRequest =
        new CountQueryRequest(
            underlay,
            underlay.getPrimaryEntity(),
            null,
            List.of(),
            entityFilter,
            OrderByDirection.DESCENDING,
            null,
            null,
            null,
            null,
            false);
    CountQueryResult countQueryResult = underlay.getQueryRunner().run(countQueryRequest);
    LOGGER.debug("getRecordsCount SQL: {}", countQueryResult.getSql());
    if (countQueryResult.getCountInstances().size() > 1) {
      LOGGER.warn(
          "getRecordsCount returned >1 count: {}", countQueryResult.getCountInstances().size());
    }
    return countQueryResult.getCountInstances().get(0).getCount();
  }

  /** Build a query of a random sample of primary entity instance ids in the cohort. */
  public List<Long> getRandomSample(String studyId, String cohortId, int sampleSize) {
    Cohort cohort = getCohort(studyId, cohortId);
    Underlay underlay = underlayService.getUnderlay(cohort.getUnderlay());

    // Build the cohort filter for the primary entity.
    EntityFilter primaryEntityFilter =
        filterBuilderService.buildFilterForCohortRevision(
            cohort.getUnderlay(), cohort.getMostRecentRevision());

    // Build a query of a random sample of primary entity instance ids in the cohort.
    AttributeField idAttributeField =
        new AttributeField(
            underlay,
            underlay.getPrimaryEntity(),
            underlay.getPrimaryEntity().getIdAttribute(),
            false);
    ListQueryRequest listQueryRequest =
        ListQueryRequest.againstIndexData(
            underlay,
            underlay.getPrimaryEntity(),
            List.of(idAttributeField),
            primaryEntityFilter,
            List.of(OrderBy.random()),
            sampleSize,
            null,
            // BQ does not allow paginating through a query that is ordered randomly, unless we
            // manually persist the results in a temp table (i.e. BQ does not cache the query
            // results in an anonymous dataset for us).
            sampleSize);
    ListQueryResult listQueryResult = underlay.getQueryRunner().run(listQueryRequest);
    LOGGER.debug("RANDOM SAMPLE primary entity instance ids: {}", listQueryResult.getSql());
    return listQueryResult.getListInstances().stream()
        .map(
            listInstance ->
                listInstance.getEntityFieldValue(idAttributeField).getValue().getInt64Val())
        .collect(Collectors.toList());
  }
}
