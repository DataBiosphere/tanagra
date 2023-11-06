package bio.terra.tanagra.service.artifact;

import bio.terra.tanagra.api.query.EntityCountRequest;
import bio.terra.tanagra.api.query.EntityCountResult;
import bio.terra.tanagra.api.query.filter.EntityFilter;
import bio.terra.tanagra.app.configuration.FeatureConfiguration;
import bio.terra.tanagra.db.CohortDao;
import bio.terra.tanagra.query.ColumnHeaderSchema;
import bio.terra.tanagra.query.DataPointer;
import bio.terra.tanagra.query.OrderByVariable;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.QueryRequest;
import bio.terra.tanagra.query.QueryResult;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.artifact.model.ActivityLog;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.CohortRevision;
import bio.terra.tanagra.service.query.UnderlayService;
import bio.terra.tanagra.underlay.AttributeMapping;
import bio.terra.tanagra.underlay.Underlay;
import com.google.common.collect.Lists;
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
  private final ActivityLogService activityLogService;

  @Autowired
  public CohortService(
      CohortDao cohortDao,
      FeatureConfiguration featureConfiguration,
      UnderlayService underlayService,
      StudyService studyService,
      ActivityLogService activityLogService) {
    this.cohortDao = cohortDao;
    this.featureConfiguration = featureConfiguration;
    this.underlayService = underlayService;
    this.studyService = studyService;
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
  public String createNextRevision(
      String studyId, String cohortId, String userEmail, EntityFilter entityFilter) {
    Long recordsCount =
        entityFilter == null ? null : getRecordsCount(studyId, cohortId, entityFilter);
    return cohortDao.createNextRevision(cohortId, null, userEmail, recordsCount);
  }

  public long getRecordsCount(String studyId, String cohortId, EntityFilter entityFilter) {
    return getRecordsCount(getCohort(studyId, cohortId).getUnderlay(), entityFilter);
  }

  /** Build a count query of all primary entity instance ids in the cohort. */
  public long getRecordsCount(String underlayName, EntityFilter entityFilter) {
    Underlay underlay = underlayService.getUnderlay(underlayName);
    EntityCountResult entityCountResult =
        underlayService.countEntityInstances(
            new EntityCountRequest.Builder()
                .entity(underlay.getPrimaryEntity())
                .mappingType(Underlay.MappingType.INDEX)
                .attributes(List.of())
                .filter(entityFilter)
                .build());
    LOGGER.debug("getRecordsCount SQL: {}", entityCountResult.getSql());
    if (entityCountResult.getEntityCounts().size() > 1) {
      LOGGER.warn(
          "getRecordsCount returned >1 count: {}", entityCountResult.getEntityCounts().size());
    }
    return entityCountResult.getEntityCounts().get(0).getCount();
  }

  /** Build a query of a random sample of primary entity instance ids in the cohort. */
  public QueryResult getRandomSample(
      String studyId, String cohortId, EntityFilter entityFilter, int sampleSize) {
    // Build a query of a random sample of primary entity instance ids in the cohort.
    Cohort cohort = getCohort(studyId, cohortId);
    Underlay underlay = underlayService.getUnderlay(cohort.getUnderlay());
    TableVariable entityTableVar =
        TableVariable.forPrimary(
            underlay.getPrimaryEntity().getMapping(Underlay.MappingType.INDEX).getTablePointer());
    List<TableVariable> tableVars = Lists.newArrayList(entityTableVar);
    AttributeMapping idAttributeMapping =
        underlay.getPrimaryEntity().getIdAttribute().getMapping(Underlay.MappingType.INDEX);
    Query query =
        new Query.Builder()
            .select(idAttributeMapping.buildFieldVariables(entityTableVar, tableVars))
            .tables(tableVars)
            .where(entityFilter.getFilterVariable(entityTableVar, tableVars))
            .orderBy(List.of(OrderByVariable.forRandom()))
            .limit(sampleSize)
            .build();
    QueryRequest queryRequest =
        new QueryRequest(
            query.renderSQL(), new ColumnHeaderSchema(idAttributeMapping.buildColumnSchemas()));
    LOGGER.debug("RANDOM SAMPLE primary entity instance ids: {}", queryRequest.getSql());

    // Run the query and get an iterator to its results.
    DataPointer dataPointer =
        underlay
            .getPrimaryEntity()
            .getMapping(Underlay.MappingType.INDEX)
            .getTablePointer()
            .getDataPointer();
    return dataPointer.getQueryExecutor().execute(queryRequest);
  }
}
