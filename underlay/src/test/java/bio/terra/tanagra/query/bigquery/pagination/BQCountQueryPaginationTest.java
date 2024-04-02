package bio.terra.tanagra.query.bigquery.pagination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.field.ValueDisplayField;
import bio.terra.tanagra.api.query.count.CountQueryRequest;
import bio.terra.tanagra.api.query.count.CountQueryResult;
import bio.terra.tanagra.api.query.hint.HintInstance;
import bio.terra.tanagra.api.query.hint.HintQueryResult;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.ValueDisplay;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.serialization.SZService;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("requires-cloud-access")
@Tag("broad-underlays")
public class BQCountQueryPaginationTest {
  private static final String SERVICE_CONFIG_NAME = "cmssynpuf_broad";
  private Underlay underlay;

  @BeforeEach
  void setup() {
    ConfigReader configReader = ConfigReader.fromJarResources();
    SZService szService = configReader.readService(SERVICE_CONFIG_NAME);
    SZUnderlay szUnderlay = configReader.readUnderlay(szService.underlay);
    underlay = Underlay.fromConfig(szService.bigQuery, szUnderlay, configReader);
  }

  @Test
  void noPagination() {
    Entity entity = underlay.getPrimaryEntity();
    AttributeField valueDisplayAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("gender"), false);
    AttributeField runtimeCalculatedAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("age"), false);

    List<ValueDisplayField> groupBys = List.of(valueDisplayAttribute, runtimeCalculatedAttribute);
    HintQueryResult entityLevelHints =
        new HintQueryResult(
            "",
            List.of(
                new HintInstance(
                    entity.getAttribute("gender"),
                    Map.of(
                        new ValueDisplay(Literal.forInt64(8_507L), "MALE"),
                        111L,
                        new ValueDisplay(Literal.forInt64(8_532L), "FEMALE"),
                        222L))));
    CountQueryResult countQueryResult =
        underlay
            .getQueryRunner()
            .run(
                new CountQueryRequest(
                    underlay, entity, groupBys, null, null, null, entityLevelHints, false));

    assertNotNull(countQueryResult.getSql());
    assertEquals(152, countQueryResult.getCountInstances().size());
    assertNull(countQueryResult.getPageMarker());
  }

  @Test
  void withPagination() {
    Entity entity = underlay.getPrimaryEntity();
    AttributeField valueDisplayAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("gender"), false);
    AttributeField runtimeCalculatedAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("age"), false);

    List<ValueDisplayField> groupBys = List.of(valueDisplayAttribute, runtimeCalculatedAttribute);
    HintQueryResult entityLevelHints =
        new HintQueryResult(
            "",
            List.of(
                new HintInstance(
                    entity.getAttribute("gender"),
                    Map.of(
                        new ValueDisplay(Literal.forInt64(8_507L), "MALE"),
                        111L,
                        new ValueDisplay(Literal.forInt64(8_532L), "FEMALE"),
                        222L))));

    // First query request gets the first page of results.
    CountQueryResult countQueryResult1 =
        underlay
            .getQueryRunner()
            .run(
                new CountQueryRequest(
                    underlay, entity, groupBys, null, null, 10, entityLevelHints, false));

    assertNotNull(countQueryResult1.getSql());
    assertEquals(10, countQueryResult1.getCountInstances().size());
    assertNotNull(countQueryResult1.getPageMarker());
    assertNotNull(countQueryResult1.getPageMarker().getPageToken());
    assertNotNull(countQueryResult1.getPageMarker().getInstant());

    // Second query request gets the second and final page of results.
    CountQueryResult countQueryResult2 =
        underlay
            .getQueryRunner()
            .run(
                new CountQueryRequest(
                    underlay,
                    entity,
                    groupBys,
                    null,
                    countQueryResult1.getPageMarker(),
                    200,
                    entityLevelHints,
                    false));

    assertNotNull(countQueryResult2.getSql());
    assertEquals(142, countQueryResult2.getCountInstances().size());
    assertNull(countQueryResult2.getPageMarker());
  }
}
