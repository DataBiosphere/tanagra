package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.QUERY_INSTANCES;
import static bio.terra.tanagra.service.accesscontrol.Action.READ;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.COHORT;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.CONCEPT_SET;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.UNDERLAY;

import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.api.query.list.ListQueryResult;
import bio.terra.tanagra.app.authentication.SpringAuthentication;
import bio.terra.tanagra.app.controller.objmapping.FromApiUtils;
import bio.terra.tanagra.app.controller.objmapping.ToApiUtils;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.generated.controller.ExportApi;
import bio.terra.tanagra.generated.model.ApiEntityOutputPreview;
import bio.terra.tanagra.generated.model.ApiEntityOutputPreviewCriteria;
import bio.terra.tanagra.generated.model.ApiEntityOutputPreviewList;
import bio.terra.tanagra.generated.model.ApiExportLinkResult;
import bio.terra.tanagra.generated.model.ApiExportModel;
import bio.terra.tanagra.generated.model.ApiExportModelList;
import bio.terra.tanagra.generated.model.ApiExportPreviewRequest;
import bio.terra.tanagra.generated.model.ApiExportRequest;
import bio.terra.tanagra.generated.model.ApiExportResult;
import bio.terra.tanagra.generated.model.ApiInstanceListResult;
import bio.terra.tanagra.service.UnderlayService;
import bio.terra.tanagra.service.accesscontrol.AccessControlService;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.artifact.CohortService;
import bio.terra.tanagra.service.artifact.ConceptSetService;
import bio.terra.tanagra.service.artifact.StudyService;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.ConceptSet;
import bio.terra.tanagra.service.artifact.model.Study;
import bio.terra.tanagra.service.export.DataExportModel;
import bio.terra.tanagra.service.export.DataExportService;
import bio.terra.tanagra.service.export.ExportFileResult;
import bio.terra.tanagra.service.export.ExportRequest;
import bio.terra.tanagra.service.export.ExportResult;
import bio.terra.tanagra.service.filter.EntityOutputPreview;
import bio.terra.tanagra.service.filter.FilterBuilderService;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.utils.SqlFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class ExportApiController implements ExportApi {
  private final AccessControlService accessControlService;
  private final DataExportService dataExportService;
  private final StudyService studyService;
  private final CohortService cohortService;
  private final ConceptSetService conceptSetService;
  private final UnderlayService underlayService;
  private final FilterBuilderService filterBuilderService;

  @Autowired
  public ExportApiController(
      AccessControlService accessControlService,
      DataExportService dataExportService,
      StudyService studyService,
      CohortService cohortService,
      ConceptSetService conceptSetService,
      UnderlayService underlayService,
      FilterBuilderService filterBuilderService) {
    this.accessControlService = accessControlService;
    this.dataExportService = dataExportService;
    this.studyService = studyService;
    this.cohortService = cohortService;
    this.conceptSetService = conceptSetService;
    this.underlayService = underlayService;
    this.filterBuilderService = filterBuilderService;
  }

  @Override
  public ResponseEntity<ApiExportModelList> listExportModels(String underlayName) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(UNDERLAY, READ),
        ResourceId.forUnderlay(underlayName));
    // Get a map of implementation name -> (display name, class instance).
    List<DataExportModel> exportModels = dataExportService.getModels(underlayName);
    ApiExportModelList apiExportImpls = new ApiExportModelList();
    exportModels.stream().forEach(em -> apiExportImpls.add(toApiObject(em)));
    return ResponseEntity.ok(apiExportImpls);
  }

  @Override
  public ResponseEntity<ApiInstanceListResult> previewEntityExport(
      String underlayName, String entityName, ApiExportPreviewRequest body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(UNDERLAY, QUERY_INSTANCES),
        ResourceId.forUnderlay(underlayName));
    for (String cohortId : body.getCohorts()) {
      accessControlService.throwIfUnauthorized(
          SpringAuthentication.getCurrentUser(),
          Permissions.forActions(COHORT, READ),
          ResourceId.forCohort(body.getStudy(), cohortId));
    }
    for (String conceptSetId : body.getConceptSets()) {
      accessControlService.throwIfUnauthorized(
          SpringAuthentication.getCurrentUser(),
          Permissions.forActions(CONCEPT_SET, READ),
          ResourceId.forConceptSet(body.getStudy(), conceptSetId));
    }

    // Build the entity outputs.
    List<Cohort> cohorts =
        body.getCohorts().stream()
            .map(cohortId -> cohortService.getCohort(body.getStudy(), cohortId))
            .collect(Collectors.toList());
    List<ConceptSet> conceptSets =
        body.getConceptSets().stream()
            .map(conceptSetId -> conceptSetService.getConceptSet(body.getStudy(), conceptSetId))
            .collect(Collectors.toList());
    List<EntityOutputPreview> entityOutputPreviews =
        filterBuilderService.buildOutputPreviewsForExport(cohorts, conceptSets);
    EntityOutputPreview entityOutputPreview =
        entityOutputPreviews.stream()
            .filter(eop -> entityName.equals(eop.getEntityOutput().getEntity().getName()))
            .findAny()
            .orElseThrow(
                () ->
                    new InvalidQueryException(
                        "Preview entity is not included in the entity output previews for the selected concept sets."));

    // Run the list query and map the results back to API objects.
    Underlay underlay = underlayService.getUnderlay(underlayName);
    ListQueryRequest listQueryRequest =
        ListQueryRequest.againstIndexData(
            underlay,
            entityOutputPreview.getEntityOutput().getEntity(),
            entityOutputPreview.getSelectedFields(),
            entityOutputPreview.getEntityOutput().getDataFeatureFilter(),
            null,
            null,
            null,
            body.getLimit());
    ListQueryResult listQueryResult = underlay.getQueryRunner().run(listQueryRequest);
    return ResponseEntity.ok(ToApiUtils.toApiObject(listQueryResult));
  }

  @Override
  public ResponseEntity<ApiEntityOutputPreviewList> previewEntityOutputs(
      String underlayName, ApiExportPreviewRequest body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(UNDERLAY, QUERY_INSTANCES),
        ResourceId.forUnderlay(underlayName));
    for (String conceptSetId : body.getConceptSets()) {
      accessControlService.throwIfUnauthorized(
          SpringAuthentication.getCurrentUser(),
          Permissions.forActions(CONCEPT_SET, READ),
          ResourceId.forConceptSet(body.getStudy(), conceptSetId));
    }

    // Build the entity output previews.
    List<Cohort> cohorts =
        body.getCohorts().stream()
            .map(cohortId -> cohortService.getCohort(body.getStudy(), cohortId))
            .collect(Collectors.toList());
    List<ConceptSet> conceptSets =
        body.getConceptSets().stream()
            .map(conceptSetId -> conceptSetService.getConceptSet(body.getStudy(), conceptSetId))
            .collect(Collectors.toList());
    List<EntityOutputPreview> entityOutputPreviews =
        filterBuilderService.buildOutputPreviewsForExport(cohorts, conceptSets);

    // Build the index and source sql for each entity output.
    Underlay underlay = underlayService.getUnderlay(underlayName);
    Map<EntityOutputPreview, String> indexSqlForEntityOutputs = new HashMap<>();
    Map<EntityOutputPreview, String> sourceSqlForEntityOutputs = new HashMap<>();
    entityOutputPreviews
        .parallelStream()
        .forEach(
            entityOutputPreview -> {
              if (entityOutputPreview.getEntityOutput().getEntity().supportsSourceQueries()) {
                ListQueryRequest sourceListQueryRequest =
                    ListQueryRequest.dryRunAgainstSourceData(
                        underlay,
                        entityOutputPreview.getEntityOutput().getEntity(),
                        entityOutputPreview.getSelectedFields(),
                        entityOutputPreview.getEntityOutput().getDataFeatureFilter());
                ListQueryResult sourceListQueryResult =
                    underlay.getQueryRunner().run(sourceListQueryRequest);
                sourceSqlForEntityOutputs.put(
                    entityOutputPreview, sourceListQueryResult.getSqlNoParams());
              } else {
                ListQueryRequest indexListQueryRequest =
                    ListQueryRequest.dryRunAgainstIndexData(
                        underlay,
                        entityOutputPreview.getEntityOutput().getEntity(),
                        entityOutputPreview.getSelectedFields(),
                        entityOutputPreview.getEntityOutput().getDataFeatureFilter(),
                        null,
                        null);
                ListQueryResult indexListQueryResult =
                    underlay.getQueryRunner().run(indexListQueryRequest);
                indexSqlForEntityOutputs.put(
                    entityOutputPreview, indexListQueryResult.getSqlNoParams());
              }
            });

    ApiEntityOutputPreviewList apiEntityOutputs = new ApiEntityOutputPreviewList();
    entityOutputPreviews.stream()
        .forEach(
            entityOutputPreview -> {
              ApiEntityOutputPreview apiEntityOutput =
                  new ApiEntityOutputPreview()
                      .entity(entityOutputPreview.getEntityOutput().getEntity().getName())
                      .includedAttributes(
                          entityOutputPreview.getEntityOutput().getAttributes().stream()
                              .map(Attribute::getName)
                              .collect(Collectors.toList()))
                      .criteria(
                          entityOutputPreview.getAttributedCriteria().stream()
                              .map(
                                  conceptSetAndCriteria ->
                                      new ApiEntityOutputPreviewCriteria()
                                          .conceptSetId(conceptSetAndCriteria.getLeft().getId())
                                          .criteriaId(conceptSetAndCriteria.getRight().getId()))
                              .collect(Collectors.toList()))
                      .indexSql(
                          SqlFormatter.format(indexSqlForEntityOutputs.get(entityOutputPreview)))
                      .sourceSql(
                          SqlFormatter.format(sourceSqlForEntityOutputs.get(entityOutputPreview)));
              apiEntityOutputs.addEntityOutputsItem(apiEntityOutput);
            });
    return ResponseEntity.ok(apiEntityOutputs);
  }

  @Override
  public ResponseEntity<ApiExportResult> exportInstancesAndAnnotations(
      String underlayName, ApiExportRequest body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(UNDERLAY, QUERY_INSTANCES),
        ResourceId.forUnderlay(underlayName));
    for (String cohortId : body.getCohorts()) {
      accessControlService.throwIfUnauthorized(
          SpringAuthentication.getCurrentUser(),
          Permissions.forActions(COHORT, READ),
          ResourceId.forCohort(body.getStudy(), cohortId));
    }
    List<String> conceptSetIds = new ArrayList<>();
    if (body.getConceptSets() != null) {
      conceptSetIds.addAll(body.getConceptSets());
    }
    for (String conceptSetId : conceptSetIds) {
      accessControlService.throwIfUnauthorized(
          SpringAuthentication.getCurrentUser(),
          Permissions.forActions(CONCEPT_SET, READ),
          ResourceId.forConceptSet(body.getStudy(), conceptSetId));
    }

    Underlay underlay = underlayService.getUnderlay(underlayName);
    Study study = studyService.getStudy(body.getStudy());
    List<Cohort> cohorts =
        body.getCohorts().stream()
            .map(cohortId -> cohortService.getCohort(body.getStudy(), cohortId))
            .collect(Collectors.toList());
    List<ConceptSet> conceptSets =
        conceptSetIds.stream()
            .map(conceptSetId -> conceptSetService.getConceptSet(body.getStudy(), conceptSetId))
            .collect(Collectors.toList());

    List<ListQueryRequest> listQueryRequests =
        body.getInstanceQuerys().stream()
            .map(
                apiQuery ->
                    FromApiUtils.fromApiObject(
                        apiQuery.getQuery(), underlay.getEntity(apiQuery.getEntity()), underlay))
            .collect(Collectors.toList());
    EntityFilter primaryEntityFilter;
    if (body.getPrimaryEntityFilter() != null) {
      primaryEntityFilter =
          FromApiUtils.fromApiObject(
              body.getPrimaryEntityFilter(), underlayService.getUnderlay(underlayName));
    } else {
      Optional<ListQueryRequest> primaryEntityListQueryRequest =
          listQueryRequests.stream()
              .filter(listQueryRequest -> listQueryRequest.getEntity().isPrimary())
              .findFirst();
      if (primaryEntityListQueryRequest.isPresent()) {
        primaryEntityFilter = primaryEntityListQueryRequest.get().getFilter();
      } else {
        primaryEntityFilter = null;
      }
    }

    ExportRequest exportRequest =
        new ExportRequest(
            body.getExportModel(),
            body.getInputs(),
            body.getRedirectBackUrl(),
            body.isIncludeAnnotations(),
            SpringAuthentication.getCurrentUser().getEmail(),
            underlay,
            study,
            cohorts,
            conceptSets);
    ExportResult exportResult =
        dataExportService.run(exportRequest, listQueryRequests, primaryEntityFilter);
    return ResponseEntity.ok(toApiObject(exportResult));
  }

  private static ApiExportModel toApiObject(DataExportModel exportModel) {
    return new ApiExportModel()
        .name(exportModel.getName())
        .displayName(exportModel.getDisplayName())
        .description(exportModel.getImpl().getDescription())
        .numPrimaryEntityCap(exportModel.getConfig().getNumPrimaryEntityCap())
        .inputs(exportModel.getImpl().describeInputs())
        .outputs(exportModel.getImpl().describeOutputs());
  }

  private static ApiExportResult toApiObject(ExportResult exportResult) {
    return new ApiExportResult()
        .status(
            exportResult.isSuccessful()
                ? ApiExportResult.StatusEnum.SUCCEEDED
                : ApiExportResult.StatusEnum.FAILED)
        .outputs(exportResult.getOutputs())
        .links(
            exportResult.getFileResults().stream()
                .map(ExportApiController::toApiObject)
                .collect(Collectors.toList()))
        .redirectAwayUrl(exportResult.getRedirectAwayUrl())
        .error(exportResult.getError() == null ? null : exportResult.getError().getMessage());
  }

  private static ApiExportLinkResult toApiObject(ExportFileResult exportFileResult) {
    return new ApiExportLinkResult()
        .displayName(exportFileResult.getFileDisplayName())
        .url(exportFileResult.getFileUrl())
        .tags(exportFileResult.getTags())
        .message(exportFileResult.getMessage())
        .error(
            exportFileResult.getError() == null ? null : exportFileResult.getError().getMessage());
  }
}
