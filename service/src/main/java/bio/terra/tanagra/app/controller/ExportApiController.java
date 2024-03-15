package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.QUERY_INSTANCES;
import static bio.terra.tanagra.service.accesscontrol.Action.READ;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.COHORT;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.CONCEPT_SET;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.UNDERLAY;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.field.ValueDisplayField;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.api.query.list.ListQueryResult;
import bio.terra.tanagra.app.authentication.SpringAuthentication;
import bio.terra.tanagra.app.controller.objmapping.FromApiUtils;
import bio.terra.tanagra.app.controller.objmapping.ToApiUtils;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.filterbuilder.EntityOutput;
import bio.terra.tanagra.generated.controller.ExportApi;
import bio.terra.tanagra.generated.model.ApiExportModel;
import bio.terra.tanagra.generated.model.ApiExportModelList;
import bio.terra.tanagra.generated.model.ApiExportPreviewRequest;
import bio.terra.tanagra.generated.model.ApiExportRequest;
import bio.terra.tanagra.generated.model.ApiExportResult;
import bio.terra.tanagra.generated.model.ApiInstanceListResult;
import bio.terra.tanagra.service.FilterBuilderService;
import bio.terra.tanagra.service.UnderlayService;
import bio.terra.tanagra.service.accesscontrol.AccessControlService;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.artifact.CohortService;
import bio.terra.tanagra.service.artifact.ConceptSetService;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.ConceptSet;
import bio.terra.tanagra.service.export.DataExport;
import bio.terra.tanagra.service.export.DataExportService;
import bio.terra.tanagra.service.export.ExportRequest;
import bio.terra.tanagra.service.export.ExportResult;
import bio.terra.tanagra.underlay.Underlay;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class ExportApiController implements ExportApi {
  private final AccessControlService accessControlService;
  private final DataExportService dataExportService;
  private final CohortService cohortService;
  private final ConceptSetService conceptSetService;
  private final UnderlayService underlayService;
  private final FilterBuilderService filterBuilderService;

  @Autowired
  public ExportApiController(
      AccessControlService accessControlService,
      DataExportService dataExportService,
      CohortService cohortService,
      ConceptSetService conceptSetService,
      UnderlayService underlayService,
      FilterBuilderService filterBuilderService) {
    this.accessControlService = accessControlService;
    this.dataExportService = dataExportService;
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
    Map<String, Pair<String, DataExport>> exportImpls = dataExportService.getModels(underlayName);
    ApiExportModelList apiExportImpls = new ApiExportModelList();
    exportImpls.entrySet().stream()
        .forEach(
            ei ->
                apiExportImpls.add(
                    toApiObject(ei.getKey(), ei.getValue().getKey(), ei.getValue().getValue())));
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
    List<EntityOutput> entityOutputs =
        filterBuilderService.buildOutputsForExport(cohorts, conceptSets);
    EntityOutput previewEntityOutput =
        entityOutputs.stream()
            .filter(entityOutput -> entityName.equals(entityOutput.getEntity().getName()))
            .findAny()
            .orElseThrow(
                () ->
                    new InvalidQueryException(
                        "Preview entity is not included in the entity outputs for the selected concept sets."));

    // Build the attribute fields to select.
    Underlay underlay = underlayService.getUnderlay(underlayName);
    List<ValueDisplayField> attributeFields = new ArrayList<>();
    previewEntityOutput.getAttributes().stream()
        .forEach(
            attribute ->
                attributeFields.add(
                    new AttributeField(
                        underlay, previewEntityOutput.getEntity(), attribute, false, false)));

    // Run the list query and map the results back to API objects.
    ListQueryRequest listQueryRequest =
        new ListQueryRequest(
            underlay,
            previewEntityOutput.getEntity(),
            attributeFields,
            previewEntityOutput.getDataFeatureFilter(),
            null,
            null,
            null,
            body.getLimit(),
            false);
    ListQueryResult listQueryResult = underlay.getQueryRunner().run(listQueryRequest);
    return ResponseEntity.ok(ToApiUtils.toApiObject(listQueryResult));
  }

  @Override
  public ResponseEntity<ApiExportResult> exportInstancesAndAnnotations(
      String underlayName, ApiExportRequest body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(UNDERLAY, QUERY_INSTANCES),
        ResourceId.forUnderlay(underlayName));
    String studyId = body.getStudy();
    for (String cohortId : body.getCohorts()) {
      accessControlService.throwIfUnauthorized(
          SpringAuthentication.getCurrentUser(),
          Permissions.forActions(COHORT, READ),
          ResourceId.forCohort(studyId, cohortId));
    }
    ExportRequest.Builder request =
        ExportRequest.builder()
            .model(body.getExportModel())
            .inputs(body.getInputs())
            .redirectBackUrl(body.getRedirectBackUrl())
            .includeAnnotations(body.isIncludeAnnotations());
    Underlay underlay = underlayService.getUnderlay(underlayName);
    List<ListQueryRequest> listQueryRequests =
        body.getInstanceQuerys().stream()
            .map(
                apiQuery ->
                    FromApiUtils.fromApiObject(
                        apiQuery.getQuery(), underlay.getEntity(apiQuery.getEntity()), underlay))
            .collect(Collectors.toList());
    ExportResult result =
        dataExportService.run(
            studyId,
            body.getCohorts(),
            request,
            listQueryRequests,
            // TODO: Remove the null handling here once the UI is passing the primary entity filter
            // to the export endpoint.
            body.getPrimaryEntityFilter() == null
                ? null
                : FromApiUtils.fromApiObject(
                    body.getPrimaryEntityFilter(), underlayService.getUnderlay(underlayName)),
            SpringAuthentication.getCurrentUser().getEmail());
    return ResponseEntity.ok(toApiObject(result));
  }

  private ApiExportModel toApiObject(String implName, String displayName, DataExport dataExport) {
    return new ApiExportModel()
        .name(implName)
        .displayName(displayName)
        .description(dataExport.getDescription())
        .inputs(dataExport.describeInputs())
        .outputs(dataExport.describeOutputs());
  }

  private ApiExportResult toApiObject(ExportResult exportResult) {
    return new ApiExportResult()
        .status(ApiExportResult.StatusEnum.valueOf(exportResult.getStatus().name()))
        .outputs(exportResult.getOutputs())
        .redirectAwayUrl(exportResult.getRedirectAwayUrl());
  }
}
