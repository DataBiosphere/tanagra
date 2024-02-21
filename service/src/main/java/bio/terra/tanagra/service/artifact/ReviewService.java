package bio.terra.tanagra.service.artifact;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.field.ValueDisplayField;
import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.query.PageMarker;
import bio.terra.tanagra.api.query.count.CountQueryRequest;
import bio.terra.tanagra.api.query.count.CountQueryResult;
import bio.terra.tanagra.api.query.list.ListInstance;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.api.query.list.ListQueryResult;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.NaryOperator;
import bio.terra.tanagra.api.shared.ValueDisplay;
import bio.terra.tanagra.app.configuration.FeatureConfiguration;
import bio.terra.tanagra.db.ReviewDao;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.service.UnderlayService;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.service.artifact.model.ActivityLog;
import bio.terra.tanagra.service.artifact.model.AnnotationKey;
import bio.terra.tanagra.service.artifact.model.AnnotationValue;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.Review;
import bio.terra.tanagra.service.artifact.model.Study;
import bio.terra.tanagra.service.artifact.reviewquery.ReviewInstance;
import bio.terra.tanagra.service.artifact.reviewquery.ReviewQueryOrderBy;
import bio.terra.tanagra.service.artifact.reviewquery.ReviewQueryRequest;
import bio.terra.tanagra.service.artifact.reviewquery.ReviewQueryResult;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReviewService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ReviewService.class);
  public static final int MAX_REVIEW_SIZE = 10_000;

  private final CohortService cohortService;
  private final UnderlayService underlayService;

  private final AnnotationService annotationService;
  private final ReviewDao reviewDao;
  private final FeatureConfiguration featureConfiguration;
  private final ActivityLogService activityLogService;

  @Autowired
  public ReviewService(
      CohortService cohortService,
      UnderlayService underlayService,
      AnnotationService annotationService,
      ReviewDao reviewDao,
      FeatureConfiguration featureConfiguration,
      ActivityLogService activityLogService) {
    this.cohortService = cohortService;
    this.underlayService = underlayService;
    this.annotationService = annotationService;
    this.reviewDao = reviewDao;
    this.featureConfiguration = featureConfiguration;
    this.activityLogService = activityLogService;
  }

  /** Create a review and a list of the primary entity instance ids it contains. */
  public Review createReview(
      String studyId,
      String cohortId,
      Review.Builder reviewBuilder,
      String userEmail,
      EntityFilter entityFilter) {
    featureConfiguration.artifactStorageEnabledCheck();
    if (reviewBuilder.getSize() > MAX_REVIEW_SIZE) {
      throw new InvalidQueryException(
          "Review size " + reviewBuilder.getSize() + " exceeds maximum allowed " + MAX_REVIEW_SIZE);
    }
    List<Long> randomSampleQueryResult =
        cohortService.getRandomSample(studyId, cohortId, entityFilter, reviewBuilder.getSize());
    long cohortRecordsCount = cohortService.getRecordsCount(studyId, cohortId, entityFilter);
    LOGGER.info("Created review with {} primary entity ids", randomSampleQueryResult.size());
    return createReviewHelper(
        studyId, cohortId, reviewBuilder, userEmail, randomSampleQueryResult, cohortRecordsCount);
  }

  @VisibleForTesting
  public Review createReviewHelper(
      String studyId,
      String cohortId,
      Review.Builder reviewBuilder,
      String userEmail,
      List<Long> primaryEntityIds,
      long cohortRecordsCount) {
    featureConfiguration.artifactStorageEnabledCheck();
    if (primaryEntityIds.isEmpty()) {
      throw new IllegalArgumentException("Cannot create a review with an empty query result");
    }
    reviewDao.createReview(
        cohortId,
        reviewBuilder.createdBy(userEmail).lastModifiedBy(userEmail).build(),
        primaryEntityIds,
        cohortRecordsCount);
    Review review = reviewDao.getReview(reviewBuilder.getId());
    activityLogService.logReview(
        ActivityLog.Type.CREATE_REVIEW, userEmail, studyId, cohortId, review);
    return review;
  }

  /** Delete a review and all the primary entity instance ids and annotation values it contains. */
  public void deleteReview(String studyId, String cohortId, String reviewId, String userEmail) {
    featureConfiguration.artifactStorageEnabledCheck();
    Review review = reviewDao.getReview(reviewId);
    reviewDao.deleteReview(reviewId);
    activityLogService.logReview(
        ActivityLog.Type.DELETE_REVIEW, userEmail, studyId, cohortId, review);
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
    Underlay underlay = underlayService.getUnderlay(cohort.getUnderlay());
    Entity primaryEntity = underlay.getPrimaryEntity();

    // Make sure the entity ID attribute is included, so we can match the entity instances to their
    // associated annotations.
    Attribute idAttribute = primaryEntity.getIdAttribute();
    if (!reviewQueryRequest.getAttributes().contains(idAttribute)) {
      reviewQueryRequest.addAttribute(idAttribute);
    }

    // Add a filter on the entity: ID is included in the review.
    Map<Literal, Integer> primaryEntityIdsToStableIndex =
        reviewDao.getPrimaryEntityIdsToStableIndex(reviewId);
    EntityFilter entityFilter =
        new AttributeFilter(
            underlay,
            primaryEntity,
            idAttribute,
            NaryOperator.IN,
            primaryEntityIdsToStableIndex.keySet().stream().collect(Collectors.toList()));
    if (reviewQueryRequest.getEntityFilter() != null) {
      entityFilter =
          new BooleanAndOrFilter(
              BooleanAndOrFilter.LogicalOperator.AND,
              List.of(entityFilter, reviewQueryRequest.getEntityFilter()));
    }

    // Get all the primary entity instances. Paginate through all the results.
    List<ValueDisplayField> attributeFields = new ArrayList<>();
    reviewQueryRequest.getAttributes().stream()
        .forEach(
            attribute ->
                attributeFields.add(
                    new AttributeField(underlay, primaryEntity, attribute, false, false)));
    ListQueryRequest listQueryRequest =
        new ListQueryRequest(
            underlay,
            primaryEntity,
            attributeFields,
            entityFilter,
            null,
            null,
            null,
            MAX_REVIEW_SIZE,
            false);
    ListQueryResult listQueryResult = underlay.getQueryRunner().run(listQueryRequest);
    List<ListInstance> listInstances = new ArrayList<>();
    listQueryResult.getListInstances().stream()
        .forEach(listInstance -> listInstances.add(listInstance));
    while (listQueryResult.getPageMarker() != null) {
      // Using the MAX_REVIEW_SIZE as the page size should mean we get all results back in a single
      // page, but that's not guaranteed, so paginate here just in case.
      listQueryRequest =
          new ListQueryRequest(
              underlay,
              primaryEntity,
              attributeFields,
              entityFilter,
              null,
              null,
              listQueryResult.getPageMarker(),
              MAX_REVIEW_SIZE,
              false);
      listQueryResult = underlay.getQueryRunner().run(listQueryRequest);
      listQueryResult.getListInstances().stream()
          .forEach(listInstance -> listInstances.add(listInstance));
    }

    // Get the annotation values.
    List<AnnotationValue> annotationValues = listAnnotationValues(studyId, cohortId, reviewId);

    // Merge entity instances and annotation values, filtering out any instances that don't match
    // the annotation filter (if specified).
    List<ReviewInstance> reviewInstances = new ArrayList<>();
    listInstances.stream()
        .forEach(
            listInstance -> {
              Map<Attribute, ValueDisplay> attributeValues = new HashMap<>();
              listInstance.getEntityFieldValues().entrySet().stream()
                  .forEach(
                      entry -> {
                        ValueDisplayField field = entry.getKey();
                        ValueDisplay value = entry.getValue();
                        if (field instanceof AttributeField) {
                          attributeValues.put(((AttributeField) field).getAttribute(), value);
                        }
                      });
              Literal idAttributeValue =
                  attributeValues.get(primaryEntity.getIdAttribute()).getValue();
              String idAttributeValueStr = idAttributeValue.getInt64Val().toString();

              List<AnnotationValue> associatedAnnotationValues =
                  annotationValues.stream()
                      .filter(av -> av.getInstanceId().equals(idAttributeValueStr))
                      .collect(Collectors.toList());

              if (!reviewQueryRequest.hasAnnotationFilter()
                  || reviewQueryRequest.getAnnotationFilter().isMatch(associatedAnnotationValues)) {
                reviewInstances.add(
                    new ReviewInstance(
                        primaryEntityIdsToStableIndex.get(idAttributeValue),
                        attributeValues,
                        associatedAnnotationValues));
              }
            });

    if (reviewQueryRequest.getOrderBys().isEmpty()) {
      // Order by the stable index, ascending.
      reviewInstances.sort(Comparator.comparing(ReviewInstance::getStableIndex));
    } else {
      // Order by the attributes and annotation values, preserving the list order. Then order by the
      // stable index, ascending.
      Comparator<ReviewInstance> comparator = null;
      for (ReviewQueryOrderBy reviewOrderBy : reviewQueryRequest.getOrderBys()) {
        if (comparator == null) {
          comparator = Comparator.comparing(Function.identity(), reviewOrderBy::compare);
        } else {
          comparator = comparator.thenComparing(Function.identity(), reviewOrderBy::compare);
        }
      }
      reviewInstances.sort(comparator.thenComparing(ReviewInstance::getStableIndex));
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
        listQueryResult.getSql(),
        reviewInstances.subList(offset, lastIndexPlusOne),
        nextPageMarker);
  }

  /**
   * Run a breakdown query on all the entity instances that are part of a review. Return the counts
   * and the generated SQL string.
   */
  public CountQueryResult countReviewInstances(
      String studyId, String cohortId, String reviewId, List<String> groupByAttributeNames) {
    Cohort cohort = cohortService.getCohort(studyId, cohortId);
    Underlay underlay = underlayService.getUnderlay(cohort.getUnderlay());
    Entity entity = underlay.getPrimaryEntity();
    List<ValueDisplayField> groupByAttributeFields =
        groupByAttributeNames.stream()
            .map(
                attrName ->
                    new AttributeField(
                        underlay, entity, entity.getAttribute(attrName), true, false))
            .collect(Collectors.toList());

    EntityFilter entityFilter =
        new AttributeFilter(
            underlay,
            entity,
            entity.getIdAttribute(),
            NaryOperator.IN,
            reviewDao.getPrimaryEntityIdsToStableIndex(reviewId).keySet().stream()
                .collect(Collectors.toList()));
    CountQueryRequest countQueryRequest =
        new CountQueryRequest(
            underlay, entity, groupByAttributeFields, entityFilter, null, null, null, false);
    return underlay.getQueryRunner().run(countQueryRequest);
  }

  /**
   * Build a CSV string, with annotation keys as the header and values as the body. Return null if
   * there are no annotation values.
   */
  public @Nullable String buildCsvStringForAnnotationValues(Study study, Cohort cohort) {
    // Get all the annotation values for the latest revision.
    List<AnnotationValue> annotationValues = listAnnotationValues(study.getId(), cohort.getId());
    if (annotationValues.isEmpty()) {
      return null; // Don't generate a file with no data.
    }

    // Build the column headers: id column name in source data, then annotation key display names.
    // Sort the annotation keys by display name, so that we get a consistent ordering.
    // e.g. person_id, key1, key2
    Underlay underlay = underlayService.getUnderlay(cohort.getUnderlay());
    String primaryIdSourceColumnName =
        underlay
            .getSourceSchema()
            .getEntityAttributes(underlay.getPrimaryEntity().getName())
            .getAttributeValueColumnSchemas()
            .get(underlay.getPrimaryEntity().getIdAttribute().getName())
            .getColumnName();
    StringBuilder columnHeaders =
        new StringBuilder(StringEscapeUtils.escapeCsv(primaryIdSourceColumnName));
    List<AnnotationKey> annotationKeys =
        annotationService
            .listAnnotationKeys(
                ResourceCollection.allResourcesAllPermissions(
                    ResourceType.ANNOTATION_KEY,
                    ResourceId.forCohort(study.getId(), cohort.getId())),
                /*offset=*/ 0,
                /*limit=*/ Integer.MAX_VALUE)
            .stream()
            .sorted(Comparator.comparing(AnnotationKey::getDisplayName))
            .collect(Collectors.toList());
    annotationKeys.forEach(
        annotation ->
            columnHeaders.append(
                String.format(",%s", StringEscapeUtils.escapeCsv(annotation.getDisplayName()))));
    StringBuilder fileContents = new StringBuilder(columnHeaders + "\n");

    // Convert the list of annotation values to a CSV-ready table.
    Table<String, String, String> csvValues = HashBasedTable.create();
    annotationValues.forEach(
        value -> {
          AnnotationKey key =
              annotationService.getAnnotationKey(
                  study.getId(), cohort.getId(), value.getAnnotationKeyId());
          csvValues.put(
              value.getInstanceId(), // row
              key.getDisplayName(), // column
              value.getLiteral().toString() // value
              );
        });

    // Convert table of annotation values to String representing CSV file contents.
    // Sort the instance ids, so that we get a consistent ordering.
    csvValues.rowKeySet().stream()
        .sorted()
        .forEach(
            instanceId -> {
              StringBuilder row = new StringBuilder(StringEscapeUtils.escapeCsv(instanceId));
              annotationKeys.forEach(
                  annotationKey -> {
                    String csvValue =
                        csvValues.contains(instanceId, annotationKey.getDisplayName())
                            ? csvValues.get(instanceId, annotationKey.getDisplayName())
                            : "";
                    row.append(String.format(",%s", StringEscapeUtils.escapeCsv(csvValue)));
                  });
              fileContents.append(String.format(row + "\n"));
            });
    return fileContents.toString();
  }
}
