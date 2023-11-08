package bio.terra.tanagra.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import bio.terra.tanagra.api2.field.AttributeField;
import bio.terra.tanagra.api2.query.EntityQueryRunner;
import bio.terra.tanagra.api2.query.list.ListQueryRequest;
import bio.terra.tanagra.api2.query.list.ListQueryResult;
import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.query.OrderByDirection;
import bio.terra.tanagra.underlay2.Underlay;
import bio.terra.tanagra.underlay2.entitymodel.Entity;
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
  private static final String UNDERLAY_NAME = "cmssynpuf";
  @Autowired private UnderlayService underlayService;

  @Test
  void noPagination() {
    Underlay underlay = underlayService.getUnderlay(UNDERLAY_NAME);
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
    Underlay underlay = underlayService.getUnderlay(UNDERLAY_NAME);
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
