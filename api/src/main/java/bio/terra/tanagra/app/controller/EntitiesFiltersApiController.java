package bio.terra.tanagra.app.controller;

import bio.terra.tanagra.generated.controller.EntitiesFiltersApi;
import bio.terra.tanagra.generated.model.ApiEntityFilter;
import bio.terra.tanagra.generated.model.ApiSqlQuery;
import bio.terra.tanagra.service.query.EntityFilter;
import bio.terra.tanagra.service.query.QueryService;
import bio.terra.tanagra.service.query.api.ApiConversionService;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

/** An {@link EntitiesFiltersApi} controller for serving entity filter queries. */
@Controller
public class EntitiesFiltersApiController implements EntitiesFiltersApi {
  private final ApiConversionService apiConversionService;
  private final QueryService queryService;

  @Autowired
  public EntitiesFiltersApiController(
      ApiConversionService apiConversionService, QueryService queryService) {
    this.apiConversionService = apiConversionService;
    this.queryService = queryService;
  }

  @Override
  public ResponseEntity<ApiSqlQuery> generateSqlQuery(
      String underlayName, String entityName, @Valid ApiEntityFilter body) {
    // TODO authorization check.
    EntityFilter entityFilter =
        apiConversionService.convertEntityFilter(underlayName, entityName, body);
    String sql = queryService.generatePrimaryKeySql(entityFilter);
    return ResponseEntity.ok(new ApiSqlQuery().query(sql));
  }
}
