package bio.terra.tanagra.app.controller;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.common.Paginator;
import bio.terra.tanagra.common.Paginator.Page;
import bio.terra.tanagra.generated.controller.UnderlaysApi;
import bio.terra.tanagra.generated.model.ApiListUnderlaysResponse;
import bio.terra.tanagra.generated.model.ApiUnderlay;
import bio.terra.tanagra.service.underlay.Underlay;
import bio.terra.tanagra.service.underlay.UnderlayService;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

/** An {@link UnderlaysApi} controller for getting metadata about underlays. */
@Controller
public class UnderlaysApiController implements UnderlaysApi {
  private static final int DEFAULT_PAGE_SIZE = 100;

  private final UnderlayService underlayService;

  @Autowired
  public UnderlaysApiController(UnderlayService underlayService) {
    this.underlayService = underlayService;
  }

  @Override
  public ResponseEntity<ApiUnderlay> getUnderlay(String underlayName) {
    // TODO authorization
    Optional<Underlay> underlay = underlayService.getUnderlay(underlayName);
    if (underlay.isEmpty()) {
      throw new NotFoundException(String.format("Underlay '%s' not found.", underlayName));
    }
    return ResponseEntity.ok(convert(underlay.get()));
  }

  @Override
  public ResponseEntity<ApiListUnderlaysResponse> listUnderlays(
      @Min(0) @Valid Integer pageSize, @Valid String pageToken) {
    // TODO authorization
    // Sort underlays by name for a consistent ordering.
    List<Underlay> sortedUnderlays =
        underlayService.getUnderlays().stream()
            .sorted(Comparator.comparing(Underlay::name))
            .collect(Collectors.toList());
    int parsedPageSize = (pageSize == null || pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;
    // There are no parameters to hash for this paginated endpoint.
    Paginator<Underlay> paginator = new Paginator<>(parsedPageSize, /* parameterHash =*/ "");
    Page<Underlay> page = paginator.getPage(sortedUnderlays, pageToken);
    ApiListUnderlaysResponse response =
        new ApiListUnderlaysResponse()
            .underlays(
                page.results().stream()
                    .map(UnderlaysApiController::convert)
                    .collect(Collectors.toList()))
            .nextPageToken(page.nextPageToken());

    return ResponseEntity.ok(response);
  }

  private static ApiUnderlay convert(Underlay underlay) {
    List<String> entityNames =
        underlay.entities().keySet().stream().sorted().collect(Collectors.toList());

    String columns = "[" +
            "{\"key\":\"concept_name\",\"width\":\"100%\",\"title\":\"Concept Name\"}," +
            "{\"key\":\"concept_id\",\"width\":120,\"title\":\"Concept ID\"}," +
            "{\"key\":\"standard_concept\",\"width\":180,\"title\":\"Source/Standard\"}," +
            "{\"key\":\"vocabulary_id\",\"width\":120,\"title\":\"Vocab\"}," +
            "{\"key\":\"concept_code\",\"width\":120,\"title\":\"Code\"}" +
            "],";
    String criteriaConfigs = "[" +
            "{\"type\":\"concept\",\"title\":\"Conditions\",\"defaultName\":\"Contains Conditions Codes\",\"plugin\":{\"columns\":" + columns + "\"entities\":[{\"name\":\"condition\",\"selectable\":true,\"hierarchical\":true}]}}," +
            "{\"type\":\"concept\",\"title\":\"Procedures\",\"defaultName\":\"Contains Procedures Codes\",\"plugin\":{\"columns\":" + columns + "\"entities\":[{\"name\":\"procedure\",\"selectable\":true,\"hierarchical\":true}]}}," +
            "{\"type\":\"concept\",\"title\":\"Observations\",\"defaultName\":\"Contains Observations Codes\",\"plugin\":{\"columns\":" + columns + "\"entities\":[{\"name\":\"observation\",\"selectable\":true}]}}," +
            "{\"type\":\"concept\",\"title\":\"Drugs\",\"defaultName\":\"Contains Drugs Codes\",\"plugin\":{\"columns\":" + columns + "\"entities\":[{\"name\":\"ingredient\",\"selectable\":true,\"hierarchical\":true},{\"name\":\"brand\",\"sourceConcepts\":true,\"attributes\":[\"concept_name\",\"concept_id\",\"standard_concept\",\"concept_code\"],\"listChildren\":{\"entity\":\"ingredient\",\"idPath\":\"relationshipFilter.filter.binaryFilter.attributeValue\",\"filter\":{\"relationshipFilter\":{\"outerVariable\":\"ingredient\",\"newVariable\":\"brand\",\"newEntity\":\"brand\",\"filter\":{\"binaryFilter\":{\"attributeVariable\":{\"variable\":\"brand\",\"name\":\"concept_id\"},\"operator\":\"EQUALS\",\"attributeValue\":{\"int64Val\":0}}}}}}}]}}," +
            "{\"type\":\"attribute\",\"title\":\"Ethnicity\",\"defaultName\":\"Contains Ethnicity Codes\",\"plugin\":{\"attribute\":\"ethnicity_concept_id\"}}," +
            "{\"type\":\"attribute\",\"title\":\"Gender Identity\",\"defaultName\":\"Contains Gender Identity Codes\",\"plugin\":{\"attribute\":\"gender_concept_id\"}}," +
            "{\"type\":\"attribute\",\"title\":\"Race\",\"defaultName\":\"Contains Race Codes\",\"plugin\":{\"attribute\":\"race_concept_id\"}}," +
            "{\"type\":\"attribute\",\"title\":\"Sex Assigned at Birth\",\"defaultName\":\"Contains Sex Assigned at Birth Codes\",\"plugin\":{\"attribute\":\"sex_at_birth_concept_id\"}},{\"type\":\"attribute\",\"title\":\"Year at Birth\",\"defaultName\":\"Contains Year at Birth Values\",\"plugin\":{\"attribute\":\"year_of_birth\"}}" +
            "]";

    return new ApiUnderlay()
        .name(underlay.name())
        .entityNames(entityNames)
        .criteriaConfigs(criteriaConfigs);
  }
}
