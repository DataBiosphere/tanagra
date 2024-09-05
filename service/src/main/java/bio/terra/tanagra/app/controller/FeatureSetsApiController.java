package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.CREATE_FEATURE_SET;
import static bio.terra.tanagra.service.accesscontrol.Action.DELETE;
import static bio.terra.tanagra.service.accesscontrol.Action.READ;
import static bio.terra.tanagra.service.accesscontrol.Action.UPDATE;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.FEATURE_SET;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.STUDY;

import bio.terra.tanagra.app.authentication.SpringAuthentication;
import bio.terra.tanagra.app.controller.objmapping.FromApiUtils;
import bio.terra.tanagra.app.controller.objmapping.ToApiUtils;
import bio.terra.tanagra.generated.controller.FeatureSetsApi;
import bio.terra.tanagra.generated.model.ApiEntityOutput;
import bio.terra.tanagra.generated.model.ApiFeatureSet;
import bio.terra.tanagra.generated.model.ApiFeatureSetCreateInfo;
import bio.terra.tanagra.generated.model.ApiFeatureSetList;
import bio.terra.tanagra.generated.model.ApiFeatureSetUpdateInfo;
import bio.terra.tanagra.service.accesscontrol.AccessControlService;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.artifact.FeatureSetService;
import bio.terra.tanagra.service.artifact.model.Criteria;
import bio.terra.tanagra.service.artifact.model.FeatureSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class FeatureSetsApiController implements FeatureSetsApi {
  private final FeatureSetService featureSetService;
  private final AccessControlService accessControlService;

  @Autowired
  public FeatureSetsApiController(
      FeatureSetService featureSetService, AccessControlService accessControlService) {
    this.featureSetService = featureSetService;
    this.accessControlService = accessControlService;
  }

  @Override
  public ResponseEntity<ApiFeatureSet> createFeatureSet(
      String studyId, ApiFeatureSetCreateInfo body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(STUDY, CREATE_FEATURE_SET),
        ResourceId.forStudy(studyId));
    FeatureSet createdFeatureSet =
        featureSetService.createFeatureSet(
            studyId,
            FeatureSet.builder()
                .displayName(body.getDisplayName())
                .description(body.getDescription())
                .underlay(body.getUnderlayName()),
            SpringAuthentication.getCurrentUser().getEmail());
    return ResponseEntity.ok(FeatureSetsApiController.toApiObject(createdFeatureSet));
  }

  @Override
  public ResponseEntity<Void> deleteFeatureSet(String studyId, String featureSetId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(FEATURE_SET, DELETE),
        ResourceId.forFeatureSet(studyId, featureSetId));
    featureSetService.deleteFeatureSet(studyId, featureSetId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<ApiFeatureSet> getFeatureSet(String studyId, String featureSetId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(FEATURE_SET, READ),
        ResourceId.forFeatureSet(studyId, featureSetId));
    return ResponseEntity.ok(toApiObject(featureSetService.getFeatureSet(studyId, featureSetId)));
  }

  @Override
  public ResponseEntity<ApiFeatureSetList> listFeatureSets(
      String studyId, Integer offset, Integer limit) {
    ResourceCollection authorizedFeatureSetIds =
        accessControlService.listAuthorizedResources(
            SpringAuthentication.getCurrentUser(),
            Permissions.forActions(FEATURE_SET, READ),
            ResourceId.forStudy(studyId),
            offset,
            limit);
    ApiFeatureSetList apiFeatureSets = new ApiFeatureSetList();
    featureSetService
        .listFeatureSets(authorizedFeatureSetIds, offset, limit)
        .forEach(featureSet -> apiFeatureSets.add(toApiObject(featureSet)));
    return ResponseEntity.ok(apiFeatureSets);
  }

  @Override
  public ResponseEntity<ApiFeatureSet> updateFeatureSet(
      String studyId, String featureSetId, ApiFeatureSetUpdateInfo body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(FEATURE_SET, UPDATE),
        ResourceId.forFeatureSet(studyId, featureSetId));
    List<Criteria> criteria =
        body.getCriteria() == null
            ? null
            : body.getCriteria().stream()
                .map(FromApiUtils::fromApiObject)
                .collect(Collectors.toList());

    Map<String, List<String>> outputAttributesPerEntity =
        body.getEntityOutputs() == null
            ? null
            : body.getEntityOutputs().stream()
                .collect(
                    Collectors.toMap(
                        ApiEntityOutput::getEntity, ApiEntityOutput::getExcludeAttributes));
    FeatureSet updatedFeatureSet =
        featureSetService.updateFeatureSet(
            studyId,
            featureSetId,
            SpringAuthentication.getCurrentUser().getEmail(),
            body.getDisplayName(),
            body.getDescription(),
            criteria,
            outputAttributesPerEntity);
    return ResponseEntity.ok(toApiObject(updatedFeatureSet));
  }

  private static ApiFeatureSet toApiObject(FeatureSet featureSet) {
    return new ApiFeatureSet()
        .id(featureSet.getId())
        .underlayName(featureSet.getUnderlay())
        .displayName(featureSet.getDisplayNameOrDefault())
        .description(featureSet.getDescription())
        .created(featureSet.getCreated())
        .createdBy(featureSet.getCreatedBy())
        .lastModified(featureSet.getLastModified())
        .criteria(
            featureSet.getCriteria() == null
                ? null
                : featureSet.getCriteria().stream()
                    .map(ToApiUtils::toApiObject)
                    .collect(Collectors.toList()))
        .entityOutputs(
            featureSet.getExcludeOutputAttributesPerEntity() == null
                ? null
                : featureSet.getExcludeOutputAttributesPerEntity().entrySet().stream()
                    .map(
                        entry ->
                            new ApiEntityOutput()
                                .entity(entry.getKey())
                                .excludeAttributes(entry.getValue()))
                    .collect(Collectors.toList()));
  }
}
