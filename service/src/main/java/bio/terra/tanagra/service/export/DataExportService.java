package bio.terra.tanagra.service.export;

import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.query.count.CountQueryResult;
import bio.terra.tanagra.api.shared.OrderByDirection;
import bio.terra.tanagra.app.configuration.ExportConfiguration;
import bio.terra.tanagra.app.configuration.ExportConfiguration.PerModel;
import bio.terra.tanagra.app.configuration.FeatureConfiguration;
import bio.terra.tanagra.filterbuilder.EntityOutput;
import bio.terra.tanagra.service.UnderlayService;
import bio.terra.tanagra.service.artifact.ActivityLogService;
import bio.terra.tanagra.service.artifact.CohortService;
import bio.terra.tanagra.service.artifact.ReviewService;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.filter.FilterBuilderService;
import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DataExportService {
  private static final Logger LOGGER = LoggerFactory.getLogger(DataExportService.class);
  private final FeatureConfiguration featureConfiguration;
  private final ExportConfiguration.Shared shared;
  private final Map<String, DataExportModel> nameToModel = new HashMap<>();
  private final UnderlayService underlayService;
  private final FilterBuilderService filterBuilderService;
  private final CohortService cohortService;
  private final ReviewService reviewService;
  private final ActivityLogService activityLogService;
  private final RandomNumberGenerator randomNumberGenerator;

  @Autowired
  @SuppressWarnings("checkstyle:ParameterNumber")
  public DataExportService(
      FeatureConfiguration featureConfiguration,
      ExportConfiguration exportConfiguration,
      UnderlayService underlayService,
      FilterBuilderService filterBuilderService,
      CohortService cohortService,
      ReviewService reviewService,
      ActivityLogService activityLogService,
      RandomNumberGenerator randomNumberGenerator) {
    this.featureConfiguration = featureConfiguration;
    this.shared = exportConfiguration.getShared();
    for (PerModel perModelConfig : exportConfiguration.getModels()) {
      DataExport dataExportImplInstance = perModelConfig.getType().createNewInstance();
      dataExportImplInstance.initialize(
          DeploymentConfig.fromApplicationConfig(shared, perModelConfig));

      // If no model name is defined, default it to the type name.
      String modelName = perModelConfig.getName();
      if (modelName == null || modelName.isEmpty()) {
        modelName = perModelConfig.getType().name();
      }
      this.nameToModel.put(
          modelName, new DataExportModel(modelName, dataExportImplInstance, perModelConfig));
    }
    this.underlayService = underlayService;
    this.filterBuilderService = filterBuilderService;
    this.cohortService = cohortService;
    this.reviewService = reviewService;
    this.activityLogService = activityLogService;
    this.randomNumberGenerator = randomNumberGenerator;
  }

  /**
   * @return List of models, each of which includes the unique name, implementation class instance,
   *     and per-model config.
   */
  public List<DataExportModel> getModels(String underlay) {
    // TODO: Allow configuring the list of implementations per underlay.
    return new ArrayList<>(nameToModel.values());
  }

  public ExportResult run(ExportRequest request) {
    // Make the current cohort revisions un-editable, and create the next version.
    Map<String, String> cohortToRevisionIdMap = new HashMap<>();
    request
        .getCohorts()
        .forEach(
            cohort -> {
              String revisionId =
                  cohortService.createNextRevision(
                      request.getStudy().getId(), cohort.getId(), request.getUserEmail());
              cohortToRevisionIdMap.put(cohort.getId(), revisionId);
            });

    // Build the helper object that implementation classes can use. This object contains utility
    // methods on the specific cohorts and feature sets specified in the request.
    DataExportHelper helper = buildHelper(request);

    // Calculate the number of primary entity instances that are included in this export request.
    CountQueryResult countQueryResult =
        underlayService.runCountQuery(
            request.getUnderlay(),
            request.getUnderlay().getPrimaryEntity(),
            null,
            List.of(),
            helper.getPrimaryEntityFilter(),
            OrderByDirection.DESCENDING,
            null,
            null,
            null);
    long numPrimaryEntityInstances = countQueryResult.getCountInstances().get(0).getCount();
    LOGGER.info("Exporting {} primary entity instances", numPrimaryEntityInstances);

    // Enforce the maximum primary entity cap, if defined.
    DataExportModel model = nameToModel.get(request.getModel());
    if (model.getConfig().hasNumPrimaryEntityCap()
        && numPrimaryEntityInstances > model.getConfig().getNumPrimaryEntityCap()) {
      return ExportResult.forError(
          ExportError.forMessage(
              "Maximum number of primary entity instances ("
                  + model.getConfig().getNumPrimaryEntityCap()
                  + ") allowed for this model exceeded: "
                  + numPrimaryEntityInstances,
              false));
    }

    // Get the implementation class instance for the requested data export model.
    DataExport impl = model.getImpl();
    ExportResult exportResult;
    try {
      exportResult = impl.run(request, helper);
    } catch (Exception ex) {
      LOGGER.error("Error running data export model", ex);
      exportResult = ExportResult.forError(ExportError.forException(ex));
    }

    // Log the export.
    activityLogService.logExport(
        request.getModel(),
        numPrimaryEntityInstances,
        request.getUserEmail(),
        request.getStudy().getId(),
        cohortToRevisionIdMap);
    return exportResult;
  }

  @VisibleForTesting
  public DataExportHelper buildHelper(ExportRequest request) {
    // Build the helper object that implementation classes can use. This object contains utility
    // methods on the specific cohorts and feature sets specified in the request.
    List<EntityOutput> entityOutputs =
        filterBuilderService.buildOutputsForExport(request.getCohorts(), request.getFeatureSets());
    EntityFilter primaryEntityFilter =
        filterBuilderService.buildFilterForCohortRevisions(
            request.getUnderlay().getName(),
            request.getCohorts().stream().map(Cohort::getMostRecentRevision).toList());

    return new DataExportHelper(
        featureConfiguration.getMaxChildThreads(),
        shared,
        randomNumberGenerator,
        reviewService,
        request,
        entityOutputs,
        primaryEntityFilter);
  }
}
