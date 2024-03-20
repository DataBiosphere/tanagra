package bio.terra.tanagra.service.export;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.query.count.CountQueryResult;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.app.configuration.ExportConfiguration;
import bio.terra.tanagra.app.configuration.ExportConfiguration.PerModel;
import bio.terra.tanagra.app.configuration.FeatureConfiguration;
import bio.terra.tanagra.filterbuilder.EntityOutput;
import bio.terra.tanagra.service.FilterBuilderService;
import bio.terra.tanagra.service.UnderlayService;
import bio.terra.tanagra.service.artifact.ActivityLogService;
import bio.terra.tanagra.service.artifact.CohortService;
import bio.terra.tanagra.service.artifact.ReviewService;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.utils.RandomNumberGenerator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DataExportService {
  private final FeatureConfiguration featureConfiguration;
  private final ExportConfiguration.Shared shared;
  private final Map<String, DataExport> modelToImpl = new HashMap<>();
  private final Map<String, ExportConfiguration.PerModel> modelToConfig = new HashMap<>();
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
      this.modelToImpl.put(modelName, dataExportImplInstance);
      this.modelToConfig.put(modelName, perModelConfig);
    }
    this.underlayService = underlayService;
    this.filterBuilderService = filterBuilderService;
    this.cohortService = cohortService;
    this.reviewService = reviewService;
    this.activityLogService = activityLogService;
    this.randomNumberGenerator = randomNumberGenerator;
  }

  /** Return a map of model name -> (display name, implementation class instance). */
  public Map<String, Pair<String, DataExport>> getModels(String underlay) {
    // TODO: Allow configuring the list of implementations per underlay.
    return modelToImpl.keySet().stream()
        .collect(
            Collectors.toMap(
                Function.identity(),
                implName -> {
                  DataExport impl = modelToImpl.get(implName);
                  String displayName = modelToConfig.get(implName).getDisplayName();
                  if (displayName == null || displayName.isEmpty()) {
                    displayName = impl.getDefaultDisplayName();
                  }
                  return Pair.of(displayName, impl);
                }));
  }

  public ExportResult run(
      ExportRequest request,
      List<ListQueryRequest> frontendListQueryRequests,
      EntityFilter frontendPrimaryEntityFilter) {
    // Make the current cohort revisions un-editable, and create the next version.
    Map<String, String> cohortToRevisionIdMap = new HashMap<>();
    request.getCohorts().stream()
        .forEach(
            cohort -> {
              String revisionId =
                  cohortService.createNextRevision(
                      request.getStudy().getId(), cohort.getId(), request.getUserEmail());
              cohortToRevisionIdMap.put(cohort.getId(), revisionId);
            });

    // Build the helper object that implementation classes can use. This object contains utility
    // methods on the specific cohorts and concept sets specified in the request.
    List<EntityOutput> entityOutputs;
    EntityFilter primaryEntityFilter;
    if (featureConfiguration.isBackendFiltersEnabled()) {
      entityOutputs =
          filterBuilderService.buildOutputsForExport(
              request.getCohorts(), request.getConceptSets());
      primaryEntityFilter =
          filterBuilderService.buildFilterForCohortRevisions(
              request.getUnderlay().getName(),
              request.getCohorts().stream()
                  .map(Cohort::getMostRecentRevision)
                  .collect(Collectors.toList()));
    } else {
      entityOutputs =
          frontendListQueryRequests.stream()
              .map(
                  listQueryRequest -> {
                    List<Attribute> attributes = new ArrayList<>();
                    listQueryRequest.getSelectFields().stream()
                        .filter(selectField -> selectField instanceof AttributeField)
                        .forEach(
                            selectField ->
                                attributes.add(((AttributeField) selectField).getAttribute()));
                    if (listQueryRequest.getFilter() == null) {
                      return EntityOutput.unfiltered(listQueryRequest.getEntity(), attributes);
                    } else {
                      return EntityOutput.filtered(
                          listQueryRequest.getEntity(), listQueryRequest.getFilter(), attributes);
                    }
                  })
              .collect(Collectors.toList());
      primaryEntityFilter = frontendPrimaryEntityFilter;
    }
    DataExportHelper helper =
        new DataExportHelper(
            featureConfiguration.getMaxChildThreads(),
            shared,
            randomNumberGenerator,
            reviewService,
            request,
            entityOutputs,
            primaryEntityFilter);

    // Get the implementation class instance for the requested data export model.
    DataExport impl = modelToImpl.get(request.getModel());
    ExportResult exportResult;
    try {
      exportResult = impl.run(request, helper);
    } catch (Exception ex) {
      exportResult = ExportResult.forError(ExportError.forException(ex));
    }

    // Calculate the number of primary entity instances that were included in this export request.
    CountQueryResult countQueryResult =
        underlayService.runCountQuery(
            request.getUnderlay(),
            request.getUnderlay().getPrimaryEntity(),
            List.of(),
            primaryEntityFilter,
            null,
            null);
    long numInstances = countQueryResult.getCountInstances().get(0).getCount();

    // Log the export.
    activityLogService.logExport(
        request.getModel(),
        numInstances,
        request.getUserEmail(),
        request.getStudy().getId(),
        cohortToRevisionIdMap);
    return exportResult;
  }
}
