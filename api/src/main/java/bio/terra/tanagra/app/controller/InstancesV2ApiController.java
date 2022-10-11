package bio.terra.tanagra.app.controller;

import bio.terra.tanagra.generated.controller.InstancesV2Api;
import bio.terra.tanagra.generated.model.ApiInstanceListV2;
import bio.terra.tanagra.generated.model.ApiInstanceV2;
import bio.terra.tanagra.generated.model.ApiQueryV2;
import bio.terra.tanagra.query.OrderByDirection;
import bio.terra.tanagra.query.QueryRequest;
import bio.terra.tanagra.query.QuerysService;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.UnderlaysService;
import bio.terra.tanagra.underlay.ValueDisplay;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class InstancesV2ApiController implements InstancesV2Api {
  private final UnderlaysService underlaysService;
  private final QuerysService querysService;

  @Autowired
  public InstancesV2ApiController(UnderlaysService underlaysService, QuerysService querysService) {
    this.underlaysService = underlaysService;
    this.querysService = querysService;
  }

  @Override
  public ResponseEntity<ApiInstanceListV2> queryInstances(
      String underlayName, String entityName, ApiQueryV2 body) {
    Entity entity = underlaysService.getEntity(underlayName, entityName);
    List<Attribute> selectAttributes =
        body.getIncludeAttributes().stream()
            .map(attrName -> querysService.getAttribute(entity, attrName))
            .collect(Collectors.toList());
    List<Attribute> orderByAttributes =
        body.getOrderBy().getAttributes().stream()
            .map(attrName -> querysService.getAttribute(entity, attrName))
            .collect(Collectors.toList());
    OrderByDirection orderByDirection =
        OrderByDirection.valueOf(body.getOrderBy().getDirection().name());
    QueryRequest queryRequest =
        querysService.buildInstancesQuery(
            entity, selectAttributes, orderByAttributes, orderByDirection, body.getLimit());

    return ResponseEntity.ok(
        toApiObject(
            querysService.runInstancesQuery(entity, selectAttributes, queryRequest),
            queryRequest.getSql()));
  }

  private ApiInstanceListV2 toApiObject(List<Map<String, ValueDisplay>> queryResults, String sql) {
    return new ApiInstanceListV2()
        .entities(
            queryResults.stream().map(result -> toApiObject(result)).collect(Collectors.toList()))
        .sql(sql);
  }

  private ApiInstanceV2 toApiObject(Map<String, ValueDisplay> result) {
    ApiInstanceV2 instance = new ApiInstanceV2();
    for (Map.Entry<String, ValueDisplay> selectedProperty : result.entrySet()) {
      instance.put(
          selectedProperty.getKey(), ApiConversionUtils.toApiObject(selectedProperty.getValue()));
    }
    return instance;
  }
}
