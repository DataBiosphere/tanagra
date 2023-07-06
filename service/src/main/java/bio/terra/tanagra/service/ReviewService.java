package bio.terra.tanagra.service;

import bio.terra.tanagra.app.configuration.FeatureConfiguration;
import bio.terra.tanagra.db.ReviewDao;
import bio.terra.tanagra.query.*;
import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable;
import bio.terra.tanagra.query.filtervariable.FunctionFilterVariable;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.service.artifact.AnnotationKey;
import bio.terra.tanagra.service.artifact.AnnotationValue;
import bio.terra.tanagra.service.artifact.Cohort;
import bio.terra.tanagra.service.artifact.Review;
import bio.terra.tanagra.service.instances.*;
import bio.terra.tanagra.service.instances.filter.AttributeFilter;
import bio.terra.tanagra.service.instances.filter.BooleanAndOrFilter;
import bio.terra.tanagra.service.instances.filter.EntityFilter;
import bio.terra.tanagra.underlay.*;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReviewService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ReviewService.class);

  private final CohortService cohortService;
  private final UnderlaysService underlaysService;

  private final AnnotationService annotationService;
  private final QuerysService querysService;
  private final ReviewDao reviewDao;
  private final FeatureConfiguration featureConfiguration;

  @Autowired
  public ReviewService(
      CohortService cohortService,
      UnderlaysService underlaysService,
      AnnotationService annotationService,
      QuerysService querysService,
      ReviewDao reviewDao,
      FeatureConfiguration featureConfiguration) {
    this.cohortService = cohortService;
    this.underlaysService = underlaysService;
    this.annotationService = annotationService;
    this.querysService = querysService;
    this.reviewDao = reviewDao;
    this.featureConfiguration = featureConfiguration;
  }

  /** Create a review and a list of the primary entity instance ids it contains. */
  public Review createReview(
      String studyId,
      String cohortId,
      Review.Builder reviewBuilder,
      String userEmail,
      EntityFilter entityFilter) {
    featureConfiguration.artifactStorageEnabledCheck();

    // Build a query of a random sample of primary entity instance ids in the cohort.
    Cohort cohort = cohortService.getCohort(studyId, cohortId);
    Underlay underlay = underlaysService.getUnderlay(cohort.getUnderlay());
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
            .limit(reviewBuilder.getSize())
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
    QueryResult queryResult = dataPointer.getQueryExecutor().execute(queryRequest);

    return createReviewHelper(studyId, cohortId, reviewBuilder, userEmail, queryResult);
  }

  @VisibleForTesting
  public Review createReviewHelper(
      String studyId,
      String cohortId,
      Review.Builder reviewBuilder,
      String userEmail,
      QueryResult queryResult) {
    featureConfiguration.artifactStorageEnabledCheck();
    if (!queryResult.getRowResults().iterator().hasNext()) {
      throw new IllegalArgumentException("Cannot create a review with an empty query result");
    }
    reviewDao.createReview(
        cohortId,
        reviewBuilder.createdBy(userEmail).lastModifiedBy(userEmail).build(),
        queryResult);
    return reviewDao.getReview(reviewBuilder.getId());
  }

  /** Delete a review and all the primary entity instance ids and annotation values it contains. */
  public void deleteReview(String studyId, String cohortId, String reviewId) {
    featureConfiguration.artifactStorageEnabledCheck();
    reviewDao.deleteReview(reviewId);
  }

  /** List reviews with their cohort revisions. */
  public List<Review> listReviews(ResourceCollection authorizedReviewIds, int offset, int limit) {
    featureConfiguration.artifactStorageEnabledCheck();
    String cohortId = authorizedReviewIds.getParent().getCohort();
    if (authorizedReviewIds.isAllResources()) {
      return reviewDao.getAllReviews(cohortId, offset, limit);
    } else if (authorizedReviewIds.isEmpty()) {
      // If the incoming list is empty, the caller does not have permission to see any
      // reviews, so we return an empty list.
      return Collections.emptyList();
    } else {
      return reviewDao.getReviewsMatchingList(
          authorizedReviewIds.getResources().stream()
              .map(ResourceId::getReview)
              .collect(Collectors.toSet()),
          offset,
          limit);
    }
  }

  /** Retrieve a review with its cohort revision. */
  public Review getReview(String studyId, String cohortId, String reviewId) {
    featureConfiguration.artifactStorageEnabledCheck();
    return reviewDao.getReview(reviewId);
  }

  /** Update a review's metadata. */
  @SuppressWarnings("PMD.UseObjectForClearerAPI")
  public Review updateReview(
      String studyId,
      String cohortId,
      String reviewId,
      String userEmail,
      @Nullable String displayName,
      @Nullable String description) {
    featureConfiguration.artifactStorageEnabledCheck();
    reviewDao.updateReview(reviewId, userEmail, displayName, description);
    return reviewDao.getReview(reviewId);
  }

  @VisibleForTesting
  public List<AnnotationValue> listAnnotationValues(String studyId, String cohortId) {
    return listAnnotationValues(studyId, cohortId, null);
  }

  @VisibleForTesting
  public List<AnnotationValue> listAnnotationValues(
      String studyId, String cohortId, @Nullable String reviewId) {
    featureConfiguration.artifactStorageEnabledCheck();

    int selectedVersion;
    if (reviewId != null) {
      // Look up the cohort revision associated with the specified review.
      Review review = getReview(studyId, cohortId, reviewId);
      selectedVersion = review.getRevision().getVersion();
    } else {
      // No review is specified, so use the most recent cohort revision.
      Cohort cohort = cohortService.getCohort(studyId, cohortId);
      selectedVersion = cohort.getMostRecentRevision().getVersion();
    }
    LOGGER.debug("selectedVersion: {}", selectedVersion);

    // Fetch all the annotation values for this cohort.
    List<AnnotationValue.Builder> allValues =
        annotationService.getAllAnnotationValues(studyId, cohortId);
    LOGGER.debug("allValues.size = {}", allValues.size());

    // Build a map of the values by key and instance id: annotation key id -> list of annotation
    // values
    Map<Pair<String, String>, List<AnnotationValue.Builder>> allValuesMap = new HashMap<>();
    allValues.stream()
        .forEach(
            v -> {
              Pair<String, String> keyAndInstance =
                  Pair.of(v.getAnnotationKeyId(), v.getInstanceId());
              List<AnnotationValue.Builder> valuesForKeyAndInstance =
                  allValuesMap.get(keyAndInstance);
              if (valuesForKeyAndInstance == null) {
                valuesForKeyAndInstance = new ArrayList<>();
                allValuesMap.put(keyAndInstance, valuesForKeyAndInstance);
              }
              valuesForKeyAndInstance.add(v);
            });

    // Filter the values, keeping only the most recent ones for each key-instance pair, and those
    // that belong to the specified revision.
    List<AnnotationValue> filteredValues = new ArrayList<>();
    allValuesMap.entrySet().stream()
        .forEach(
            keyValues -> {
              Pair<String, String> keyAndInstance = keyValues.getKey();
              LOGGER.debug(
                  "Building filtered list of values for annotation key {} and instance id {}",
                  keyAndInstance.getKey(),
                  keyAndInstance.getValue());

              List<AnnotationValue.Builder> allValuesForKeyAndInstance = keyValues.getValue();
              int maxVersionForKeyAndInstance =
                  allValuesForKeyAndInstance.stream()
                      .max(
                          Comparator.comparingInt(
                              AnnotationValue.Builder::getCohortRevisionVersion))
                      .get()
                      .getCohortRevisionVersion();

              List<AnnotationValue> filteredValuesForKey = new ArrayList<>();
              allValuesForKeyAndInstance.stream()
                  .forEach(
                      v -> {
                        boolean isMostRecent =
                            v.getCohortRevisionVersion() == maxVersionForKeyAndInstance;
                        boolean isPartOfSelectedReview =
                            v.getCohortRevisionVersion() == selectedVersion;
                        if (isMostRecent || isPartOfSelectedReview) {
                          filteredValuesForKey.add(
                              v.isMostRecent(isMostRecent)
                                  .isPartOfSelectedReview(isPartOfSelectedReview)
                                  .build());
                        } else {
                          LOGGER.debug(
                              "Filtering out annotation value {} - {} - {} - {} ({}, {})",
                              v.build().getCohortRevisionVersion(),
                              v.build().getInstanceId(),
                              v.build().getAnnotationKeyId(),
                              v.build().getLiteral().getStringVal(),
                              maxVersionForKeyAndInstance,
                              selectedVersion);
                        }
                      });
              filteredValues.addAll(filteredValuesForKey);
            });
    return filteredValues;
  }

  public ReviewQueryResult listReviewInstances(
      String studyId, String cohortId, String reviewId, ReviewQueryRequest reviewQueryRequest) {
    Cohort cohort = cohortService.getCohort(studyId, cohortId);
    Entity primaryEntity = underlaysService.getUnderlay(cohort.getUnderlay()).getPrimaryEntity();

    // Make sure the entity ID attribute is included, so we can match the entity instances to their
    // associated annotations.
    Attribute idAttribute = primaryEntity.getIdAttribute();
    if (!reviewQueryRequest.getAttributes().contains(idAttribute)) {
      reviewQueryRequest.addAttribute(idAttribute);
    }

    // Add a filter on the entity: ID is included in the review.
    EntityFilter entityFilter =
        new AttributeFilter(
            idAttribute,
            FunctionFilterVariable.FunctionTemplate.IN,
            reviewDao.getPrimaryEntityIds(reviewId));
    if (reviewQueryRequest.getEntityFilter() != null) {
      entityFilter =
          new BooleanAndOrFilter(
              BooleanAndOrFilterVariable.LogicalOperator.AND,
              List.of(entityFilter, reviewQueryRequest.getEntityFilter()));
    }

    // Get all the entity instances.
    EntityQueryResult entityQueryResult =
        querysService.listEntityInstances(
            new EntityQueryRequest.Builder()
                .entity(primaryEntity)
                .mappingType(Underlay.MappingType.INDEX)
                .selectAttributes(reviewQueryRequest.getAttributes())
                .selectHierarchyFields(List.of())
                .selectRelationshipFields(List.of())
                .filter(entityFilter)
                .build());

    // Get the annotation values.
    List<AnnotationValue> annotationValues = listAnnotationValues(studyId, cohortId, reviewId);

    // Merge entity instances and annotation values, filtering out any instances that don't match
    // the annotation filter (if specified).
    List<ReviewInstance> reviewInstances = new ArrayList<>();
    entityQueryResult.getEntityInstances().stream()
        .forEach(
            ei -> {
              Literal entityInstanceId = ei.getAttributeValues().get(idAttribute).getValue();

              // TODO: Handle ID data types other than long.
              String entityInstanceIdStr = entityInstanceId.getInt64Val().toString();

              List<AnnotationValue> associatedAnnotationValues =
                  annotationValues.stream()
                      .filter(av -> av.getInstanceId().equals(entityInstanceIdStr))
                      .collect(Collectors.toList());

              if (!reviewQueryRequest.hasAnnotationFilter()
                  || reviewQueryRequest.getAnnotationFilter().isMatch(associatedAnnotationValues)) {
                reviewInstances.add(
                    new ReviewInstance(ei.getAttributeValues(), associatedAnnotationValues));
              }
            });

    // Order by the attributes and annotation values, preserving the list order.
    if (!reviewQueryRequest.getOrderBys().isEmpty()) {
      Comparator<ReviewInstance> comparator = null;
      for (ReviewQueryOrderBy reviewOrderBy : reviewQueryRequest.getOrderBys()) {
        if (comparator == null) {
          comparator = Comparator.comparing(Function.identity(), reviewOrderBy::compare);
        } else {
          comparator = comparator.thenComparing(Function.identity(), reviewOrderBy::compare);
        }
      }
      reviewInstances.sort(comparator);
    }

    // Return only the page of results the user requested.
    boolean hasOffset =
        reviewQueryRequest.getPageMarker() != null
            && reviewQueryRequest.getPageMarker().getOffset() != null;
    int offset = hasOffset ? reviewQueryRequest.getPageMarker().getOffset() : 0;
    boolean hasPageSize = reviewQueryRequest.getPageSize() != null;
    int lastIndexPlusOne =
        hasPageSize
            ? Math.min(offset + reviewQueryRequest.getPageSize(), reviewInstances.size())
            : reviewInstances.size();
    PageMarker nextPageMarker =
        lastIndexPlusOne >= reviewInstances.size() ? null : PageMarker.forOffset(lastIndexPlusOne);

    return new ReviewQueryResult(
        entityQueryResult.getSql(),
        reviewInstances.subList(offset, lastIndexPlusOne),
        nextPageMarker);
  }

  /**
   * Run a breakdown query on all the entity instances that are part of a review. Return the counts
   * and the generated SQL string.
   */
  public EntityCountResult countReviewInstances(
      String studyId, String cohortId, String reviewId, List<String> groupByAttributeNames) {
    Cohort cohort = cohortService.getCohort(studyId, cohortId);
    Entity entity = underlaysService.getUnderlay(cohort.getUnderlay()).getPrimaryEntity();
    List<Attribute> groupByAttributes =
        groupByAttributeNames.stream()
            .map(attrName -> entity.getAttribute(attrName))
            .collect(Collectors.toList());

    EntityFilter entityFilter =
        new AttributeFilter(
            entity.getIdAttribute(),
            FunctionFilterVariable.FunctionTemplate.IN,
            reviewDao.getPrimaryEntityIds(reviewId));
    return querysService.countEntityInstances(
        new EntityCountRequest.Builder()
            .entity(entity)
            .mappingType(Underlay.MappingType.INDEX)
            .attributes(groupByAttributes)
            .filter(entityFilter)
            .build());
  }

  public String buildTsvStringForAnnotationValues(String studyId, String cohortId) {
    // Build the column headers: id column name in source data, then annotation key display names.
    // Sort the annotation keys by display name, so that we get a consistent ordering.
    // e.g. person_id, key1, key2
    Cohort cohort = cohortService.getCohort(studyId, cohortId);
    Underlay underlay = underlaysService.getUnderlay(cohort.getUnderlay());
    String primaryIdSourceColumnName =
        underlay
            .getPrimaryEntity()
            .getIdAttribute()
            .getMapping(Underlay.MappingType.SOURCE)
            .getValue()
            .getColumnName();
    StringBuilder columnHeaders = new StringBuilder(primaryIdSourceColumnName);
    List<AnnotationKey> annotationKeys =
        annotationService
            .listAnnotationKeys(
                ResourceCollection.allResourcesAllPermissions(
                    ResourceType.ANNOTATION_KEY, ResourceId.forCohort(studyId, cohortId)),
                /*offset=*/ 0,
                /*limit=*/ Integer.MAX_VALUE)
            .stream()
            .sorted(Comparator.comparing(AnnotationKey::getDisplayName))
            .collect(Collectors.toList());
    annotationKeys.forEach(
        annotation -> {
          columnHeaders.append(String.format("\t%s", annotation.getDisplayName()));
        });
    StringBuilder fileContents = new StringBuilder(columnHeaders + "\n");

    // Get all the annotation values for the latest revision.
    List<AnnotationValue> annotationValues = listAnnotationValues(studyId, cohortId);

    // Convert the list of annotation values to a TSV-ready table.
    Table<String, String, String> tsvValues = HashBasedTable.create();
    annotationValues.forEach(
        value -> {
          AnnotationKey key =
              annotationService.getAnnotationKey(studyId, cohortId, value.getAnnotationKeyId());
          tsvValues.put(
              value.getInstanceId(), // row
              key.getDisplayName(), // column
              value.getLiteral().toString() // value
              );
        });

    // Convert table of annotation values to String representing TSV file.
    // Sort the instance ids, so that we get a consistent ordering.
    tsvValues.rowKeySet().stream()
        .sorted()
        .forEach(
            instanceId -> {
              StringBuilder row = new StringBuilder(instanceId);
              annotationKeys.forEach(
                  annotationKey -> {
                    String tsvValue =
                        tsvValues.contains(instanceId, annotationKey.getDisplayName())
                            ? tsvValues.get(instanceId, annotationKey.getDisplayName())
                            : "";
                    row.append("\t" + tsvValue);
                  });
              fileContents.append(String.format(row + "\n"));
            });
    return fileContents.toString();
  }
}
