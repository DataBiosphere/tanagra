package bio.terra.tanagra.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.query.EntityQueryRunner;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.api.query.list.ListQueryResult;
import bio.terra.tanagra.query.OrderByDirection;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.serialization.SZService;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("requires-cloud-access")
@Tag("broad-underlays")
public class ListQueryPaginationTest {
  private static final String SERVICE_CONFIG_NAME = "cmssynpuf_broad";
  private Underlay underlay;

  @BeforeEach
  void setup() {
    SZService szService = ConfigReader.deserializeService(SERVICE_CONFIG_NAME);
    SZUnderlay szUnderlay = ConfigReader.deserializeUnderlay(szService.underlay);
    underlay = Underlay.fromConfig(szService.bigQuery, szUnderlay);
  }

  @Test
  void noPagination() {
    Entity primaryEntity = underlay.getPrimaryEntity();

    // Select and order by the id attribute.
    AttributeField idAttributeField =
        new AttributeField(underlay, primaryEntity, primaryEntity.getIdAttribute(), false, false);
    ListQueryRequest listQueryRequest =
        new ListQueryRequest(
            underlay,
            primaryEntity,
            List.of(idAttributeField),
            null,
            List.of(new ListQueryRequest.OrderBy(idAttributeField, OrderByDirection.DESCENDING)),
            10,
            null,
            null);
    ListQueryResult listQueryResult =
        EntityQueryRunner.run(listQueryRequest, underlay.getQueryExecutor());

    assertNotNull(listQueryResult.getSql());
    assertEquals(10, listQueryResult.getListInstances().size());
    assertNull(listQueryResult.getPageMarker());
  }

  @Test
  void withPagination() {
    Entity primaryEntity = underlay.getPrimaryEntity();

    // Select and order by the id attribute.
    AttributeField idAttributeField =
        new AttributeField(underlay, primaryEntity, primaryEntity.getIdAttribute(), false, false);
    ListQueryRequest listQueryRequest1 =
        new ListQueryRequest(
            underlay,
            primaryEntity,
            List.of(idAttributeField),
            null,
            List.of(new ListQueryRequest.OrderBy(idAttributeField, OrderByDirection.DESCENDING)),
            10,
            null,
            3);

    // First query request gets the first page of results.
    ListQueryResult listQueryResult1 =
        EntityQueryRunner.run(listQueryRequest1, underlay.getQueryExecutor());

    assertNotNull(listQueryResult1.getSql());
    assertEquals(3, listQueryResult1.getListInstances().size());
    assertNotNull(listQueryResult1.getPageMarker());
    assertNotNull(listQueryResult1.getPageMarker().getPageToken());

    // Second query request gets the second and final page of results.
    ListQueryRequest listQueryRequest2 =
        new ListQueryRequest(
            underlay,
            primaryEntity,
            List.of(idAttributeField),
            null,
            List.of(new ListQueryRequest.OrderBy(idAttributeField, OrderByDirection.DESCENDING)),
            10,
            listQueryResult1.getPageMarker(),
            7);
    ListQueryResult listQueryResult2 =
        EntityQueryRunner.run(listQueryRequest2, underlay.getQueryExecutor());

    assertNotNull(listQueryResult2.getSql());
    assertEquals(7, listQueryResult2.getListInstances().size());
    assertNull(listQueryResult2.getPageMarker());
  }
}
