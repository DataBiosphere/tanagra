package bio.terra.tanagra.service;

import static org.junit.jupiter.api.Assertions.*;

import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.query.OrderByDirection;
import bio.terra.tanagra.service.instances.EntityQueryOrderBy;
import bio.terra.tanagra.service.instances.EntityQueryRequest;
import bio.terra.tanagra.service.instances.EntityQueryResult;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Underlay;
import java.util.List;
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
public class InstancesPaginationTest {
  private static final String UNDERLAY_NAME = "cms_synpuf";
  @Autowired private UnderlaysService underlaysService;
  @Autowired private QuerysService querysService;

  @Test
  void noPagination() {
    Entity primaryEntity = underlaysService.getUnderlay(UNDERLAY_NAME).getPrimaryEntity();

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

    EntityQueryResult entityQueryResult = querysService.listEntityInstances(entityQueryRequest);

    assertNotNull(entityQueryResult.getSql());
    assertEquals(10, entityQueryResult.getEntityInstances().size());
    assertNull(entityQueryResult.getPageMarker());
  }

  @Test
  void withPagination() {
    Entity primaryEntity = underlaysService.getUnderlay(UNDERLAY_NAME).getPrimaryEntity();

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
    EntityQueryResult entityQueryResult1 = querysService.listEntityInstances(entityQueryRequest1);

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
    EntityQueryResult entityQueryResult2 = querysService.listEntityInstances(entityQueryRequest2);

    assertNotNull(entityQueryResult2.getSql());
    assertEquals(7, entityQueryResult2.getEntityInstances().size());
    assertNull(entityQueryResult2.getPageMarker());
  }
}
