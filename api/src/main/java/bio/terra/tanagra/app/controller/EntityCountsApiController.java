package bio.terra.tanagra.app.controller;

import bio.terra.tanagra.generated.controller.EntityCountsApi;
import bio.terra.tanagra.generated.model.ApiGenerateCountsSqlQueryRequest;
import bio.terra.tanagra.generated.model.ApiSearchEntityCountsRequest;
import bio.terra.tanagra.generated.model.ApiSearchEntityCountsResponse;
import bio.terra.tanagra.generated.model.ApiSqlQuery;
import bio.terra.tanagra.service.databaseaccess.QueryResult;
import bio.terra.tanagra.service.query.EntityCounts;
import bio.terra.tanagra.service.query.QueryService;
import bio.terra.tanagra.service.query.api.ApiConversionService;
import bio.terra.tanagra.service.query.api.QueryResultConverter;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

/** An {@link EntityCountsApi} controller for operating on entity counts. */
@Controller
public class EntityCountsApiController implements EntityCountsApi {
  private final ApiConversionService apiConversionService;
  private final QueryService queryService;

  @Autowired
  public EntityCountsApiController(
      ApiConversionService apiConversionService, QueryService queryService) {
    this.apiConversionService = apiConversionService;
    this.queryService = queryService;
  }

  @Override
  public ResponseEntity<ApiSqlQuery> generateCountsSqlQuery(
      String underlayName, String entityName, @Valid ApiGenerateCountsSqlQueryRequest body) {
    // TODO authorization check.
    EntityCounts entityCounts =
        apiConversionService.convertEntityCounts(underlayName, entityName, body.getEntityCounts());
    String sql = queryService.generateSql(entityCounts);
    return ResponseEntity.ok(new ApiSqlQuery().query(sql));
  }

  @Override
  public ResponseEntity<ApiSearchEntityCountsResponse> searchEntityCounts(
      String underlayName, String entityName, ApiSearchEntityCountsRequest body) {
    // TODO authorization check.
    EntityCounts entityCounts =
        apiConversionService.convertEntityCounts(underlayName, entityName, body.getEntityCounts());
    QueryResult queryResult = queryService.retrieveResults(entityCounts);
    return ResponseEntity.ok(
        new ApiSearchEntityCountsResponse()
            .counts(QueryResultConverter.convertToEntityCounts(queryResult)));
  }
}
