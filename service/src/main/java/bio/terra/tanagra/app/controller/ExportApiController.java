package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.QUERY_INSTANCES;
import static bio.terra.tanagra.service.accesscontrol.Action.READ;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.*;

import bio.terra.tanagra.app.auth.SpringAuthentication;
import bio.terra.tanagra.generated.controller.ExportApi;
import bio.terra.tanagra.generated.model.ApiExportModel;
import bio.terra.tanagra.generated.model.ApiExportModelList;
import bio.terra.tanagra.generated.model.ApiExportRequest;
import bio.terra.tanagra.generated.model.ApiExportResult;
import bio.terra.tanagra.service.*;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.export.DataExport;
import bio.terra.tanagra.service.export.ExportRequest;
import bio.terra.tanagra.service.export.ExportResult;
import bio.terra.tanagra.service.instances.EntityQueryRequest;
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
  private final FromApiConversionService fromApiConversionService;

  @Autowired
  public ExportApiController(
      AccessControlService accessControlService,
      DataExportService dataExportService,
      FromApiConversionService fromApiConversionService) {
    this.accessControlService = accessControlService;
    this.dataExportService = dataExportService;
    this.fromApiConversionService = fromApiConversionService;
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
    List<EntityQueryRequest> entityQueryRequests =
        body.getInstanceQuerys().stream()
            .map(
                apiQuery ->
                    fromApiConversionService.fromApiObject(
                        apiQuery.getQuery(), underlayName, apiQuery.getEntity()))
            .collect(Collectors.toList());
    ExportResult result =
        dataExportService.run(studyId, body.getCohorts(), request, entityQueryRequests);
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
