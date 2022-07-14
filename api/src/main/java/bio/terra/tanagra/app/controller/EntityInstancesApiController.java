package bio.terra.tanagra.app.controller;

import bio.terra.tanagra.generated.controller.EntityInstancesApi;
import bio.terra.tanagra.generated.model.ApiGenerateDatasetSqlQueryRequest;
import bio.terra.tanagra.generated.model.ApiSearchEntityInstancesRequest;
import bio.terra.tanagra.generated.model.ApiSearchEntityInstancesResponse;
import bio.terra.tanagra.generated.model.ApiSqlQuery;
import bio.terra.tanagra.service.databaseaccess.QueryResult;
import bio.terra.tanagra.service.query.EntityDataset;
import bio.terra.tanagra.service.query.QueryService;
import bio.terra.tanagra.service.query.api.ApiConversionService;
import bio.terra.tanagra.service.query.api.QueryResultConverter;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

/** An {@link EntityInstancesApi} controller for operating on entity instances. */
@Controller
public class EntityInstancesApiController implements EntityInstancesApi {
  private final ApiConversionService apiConversionService;
  private final QueryService queryService;

  @Autowired
  public EntityInstancesApiController(
      ApiConversionService apiConversionService, QueryService queryService) {
    this.apiConversionService = apiConversionService;
    this.queryService = queryService;
  }

  @Override
  public ResponseEntity<ApiSqlQuery> generateDatasetSqlQuery(
      String underlayName, String entityName, @Valid ApiGenerateDatasetSqlQueryRequest body) {
    // TODO authorization check.
    EntityDataset entityDataset =
        apiConversionService.convertEntityDataset(
            underlayName, entityName, body.getEntityDataset());
    String sql = queryService.generateSql(entityDataset);
    return ResponseEntity.ok(new ApiSqlQuery().query(sql));
  }

  @Override
  public ResponseEntity<ApiSearchEntityInstancesResponse> searchEntityInstances(
      String entityName,
      String underlayName,
      ApiSearchEntityInstancesRequest body,
      Integer pageSize,
      String pageToken) {
    // TODO authorization check.
    EntityDataset entityDataset =
        apiConversionService.convertEntityDataset(
            underlayName, entityName, body.getEntityDataset());
    QueryResult queryResult = queryService.retrieveResults(entityDataset);

    ApiSearchEntityInstancesResponse response =
        new ApiSearchEntityInstancesResponse()
            .instances(QueryResultConverter.convertToEntityInstances(queryResult));
    return ResponseEntity.ok(response);
  }
}
