package bio.terra.tanagra.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import bio.terra.tanagra.api.query.EntityQueryOrderBy;
import bio.terra.tanagra.api.query.EntityQueryRequest;
import bio.terra.tanagra.api.query.EntityQueryResult;
import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.query.OrderByDirection;
import bio.terra.tanagra.service.query.UnderlayService;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Underlay;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Main.class)
@SpringBootTest
@ActiveProfiles("test")
@Tag("requires-cloud-access")
public class InstancesPaginationTest {
  private static final String UNDERLAY_NAME = "cms_synpuf";
  @Autowired private UnderlayService underlayService;

  @Test
  void noPagination() {
    Entity primaryEntity = underlayService.getUnderlay(UNDERLAY_NAME).getPrimaryEntity();

    EntityQueryRequest entityQueryRequest =
        new EntityQueryRequest.Builder()
            .entity(primaryEntity)
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(List.of(primaryEntity.getIdAttribute()))
            .orderBys(
                List.of(
                    new EntityQueryOrderBy(
                        primaryEntity.getIdAttribute(), OrderByDirection.DESCENDING)))
            .limit(10)
            .build();

    EntityQueryResult entityQueryResult = UnderlayService.listEntityInstances(entityQueryRequest);

    assertNotNull(entityQueryResult.getSql());
    assertEquals(10, entityQueryResult.getEntityInstances().size());
    assertNull(entityQueryResult.getPageMarker());
  }

  @Test
  void withPagination() {
    Entity primaryEntity = underlayService.getUnderlay(UNDERLAY_NAME).getPrimaryEntity();

    // First query request gets the first page of results.
    EntityQueryRequest entityQueryRequest1 =
        new EntityQueryRequest.Builder()
            .entity(primaryEntity)
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(List.of(primaryEntity.getIdAttribute()))
            .orderBys(
                List.of(
                    new EntityQueryOrderBy(
                        primaryEntity.getIdAttribute(), OrderByDirection.DESCENDING)))
            .limit(10)
            .pageSize(3)
            .build();
    EntityQueryResult entityQueryResult1 = UnderlayService.listEntityInstances(entityQueryRequest1);

    assertNotNull(entityQueryResult1.getSql());
    assertEquals(3, entityQueryResult1.getEntityInstances().size());
    assertNotNull(entityQueryResult1.getPageMarker());
    assertNotNull(entityQueryResult1.getPageMarker().getPageToken());

    // Second query request gets the second and final page of results.
    EntityQueryRequest entityQueryRequest2 =
        new EntityQueryRequest.Builder()
            .entity(primaryEntity)
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(List.of(primaryEntity.getIdAttribute()))
            .orderBys(
                List.of(
                    new EntityQueryOrderBy(
                        primaryEntity.getIdAttribute(), OrderByDirection.DESCENDING)))
            .limit(10)
            .pageMarker(entityQueryResult1.getPageMarker())
            .pageSize(7)
            .build();
    EntityQueryResult entityQueryResult2 = UnderlayService.listEntityInstances(entityQueryRequest2);

    assertNotNull(entityQueryResult2.getSql());
    assertEquals(7, entityQueryResult2.getEntityInstances().size());
    assertNull(entityQueryResult2.getPageMarker());
  }
}
