package bio.terra.tanagra.service.artifact;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.query.OrderBy;
import bio.terra.tanagra.api.query.count.CountQueryRequest;
import bio.terra.tanagra.api.query.count.CountQueryResult;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.api.query.list.ListQueryResult;
import bio.terra.tanagra.api.shared.OrderByDirection;
import bio.terra.tanagra.app.configuration.FeatureConfiguration;
import bio.terra.tanagra.db.CohortDao;
import bio.terra.tanagra.service.UnderlayService;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.artifact.model.ActivityLog;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.CohortRevision;
import bio.terra.tanagra.service.filter.FilterBuilderService;
import bio.terra.tanagra.underlay.Underlay;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CohortService {
  private static final Logger LOGGER = LoggerFactory.getLogger(CohortService.class);

  private final CohortDao cohortDao;
  private final FeatureConfiguration featureConfiguration;
  private final UnderlayService underlayService;
  private final StudyService studyService;
  private final FilterBuilderService filterBuilderService;
  private final ActivityLogService activityLogService;

  @Autowired
  public CohortService(
      CohortDao cohortDao,
      FeatureConfiguration featureConfiguration,
      UnderlayService underlayService,
      StudyService studyService,
      FilterBuilderService filterBuilderService,
      ActivityLogService activityLogService) {
    this.cohortDao = cohortDao;
    this.featureConfiguration = featureConfiguration;
    this.underlayService = underlayService;
    this.studyService = studyService;
    this.filterBuilderService = filterBuilderService;
    this.activityLogService = activityLogService;
  }

  /** Create a cohort and its first revision without any criteria. */
  public Cohort createCohort(String studyId, Cohort.Builder cohortBuilder, String userEmail) {
    return createCohort(studyId, cohortBuilder, userEmail, Collections.emptyList());
  }

  /** Create a cohort and its first revision. */
  public Cohort createCohort(
      String studyId,
      Cohort.Builder cohortBuilder,
      String userEmail,
      List<CohortRevision.CriteriaGroupSection> sections) {
    featureConfiguration.artifactStorageEnabledCheck();

    // Make sure underlay name and study id are valid.
    underlayService.getUnderlay(cohortBuilder.getUnderlay());
    studyService.getStudy(studyId);

    // Create the first revision.
    CohortRevision firstRevision =
        CohortRevision.builder()
            .sections(sections)
            .setIsMostRecent(true)
            .setIsEditable(true)
            .createdBy(userEmail)
            .lastModifiedBy(userEmail)
            .build();
    cohortBuilder.addRevision(firstRevision);

    cohortDao.createCohort(
        studyId, cohortBuilder.createdBy(userEmail).lastModifiedBy(userEmail).build());
    Cohort cohort = cohortDao.getCohort(cohortBuilder.getId());
    activityLogService.logCohort(ActivityLog.Type.CREATE_COHORT, userEmail, studyId, cohort);
    return cohort;
  }

  /** Delete a cohort and all its revisions. */
  public void deleteCohort(String studyId, String cohortId, String userEmail) {
    featureConfiguration.artifactStorageEnabledCheck();
    Cohort cohort = cohortDao.getCohort(cohortId);
    cohortDao.deleteCohort(cohortId);
    activityLogService.logCohort(ActivityLog.Type.DELETE_COHORT, userEmail, studyId, cohort);
  }

  /** List cohorts with their most recent revisions. */
  public List<Cohort> listCohorts(ResourceCollection authorizedCohortIds, int offset, int limit) {
    featureConfiguration.artifactStorageEnabledCheck();
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
    featureConfiguration.artifactStorageEnabledCheck();
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
    featureConfiguration.artifactStorageEnabledCheck();
    cohortDao.updateCohort(cohortId, userEmail, displayName, description, criteriaGroupSections);
    return cohortDao.getCohort(cohortId);
  }

  /** @return the id of the frozen revision just created */
  public String createNextRevision(String studyId, String cohortId, String userEmail) {
    Long recordsCount;
    if (featureConfiguration.isBackendFiltersEnabled()) {
      Cohort cohort = getCohort(studyId, cohortId);
      recordsCount =
          getRecordsCount(
              cohort.getUnderlay(),
              filterBuilderService.buildFilterForCohortRevision(
                  cohort.getUnderlay(), cohort.getMostRecentRevision()));
    } else {
      recordsCount = null;
    }
    return cohortDao.createNextRevision(cohortId, null, userEmail, recordsCount);
  }

  /** Build a count query of all primary entity instance ids in the cohort. */
  public long getRecordsCount(String underlayName, EntityFilter entityFilter) {
    Underlay underlay = underlayService.getUnderlay(underlayName);
    CountQueryRequest countQueryRequest =
        new CountQueryRequest(
            underlay,
            underlay.getPrimaryEntity(),
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
  public List<Long> getRandomSample(
      String studyId, String cohortId, int sampleSize, EntityFilter entityFilter) {
    Cohort cohort = getCohort(studyId, cohortId);
    Underlay underlay = underlayService.getUnderlay(cohort.getUnderlay());

    // Build the cohort filter for the primary entity.
    EntityFilter primaryEntityFilter =
        featureConfiguration.isBackendFiltersEnabled()
            ? filterBuilderService.buildFilterForCohortRevision(
                cohort.getUnderlay(), cohort.getMostRecentRevision())
            : entityFilter;

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
