package bio.terra.tanagra.aousynthetic;

import static bio.terra.tanagra.aousynthetic.UnderlayUtils.ALL_DEVICE_OCCURRENCE_ATTRIBUTES;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.DEVICE_OCCURRENCE_ENTITY;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.UNDERLAY_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.tanagra.app.controller.EntityInstancesApiController;
import bio.terra.tanagra.generated.model.ApiAttributeValue;
import bio.terra.tanagra.generated.model.ApiAttributeVariable;
import bio.terra.tanagra.generated.model.ApiBinaryFilter;
import bio.terra.tanagra.generated.model.ApiBinaryFilterOperator;
import bio.terra.tanagra.generated.model.ApiEntityDataset;
import bio.terra.tanagra.generated.model.ApiFilter;
import bio.terra.tanagra.generated.model.ApiGenerateDatasetSqlQueryRequest;
import bio.terra.tanagra.generated.model.ApiRelationshipFilter;
import bio.terra.tanagra.generated.model.ApiSqlQuery;
import bio.terra.tanagra.testing.BaseSpringUnitTest;
import bio.terra.tanagra.testing.GeneratedSqlUtils;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Tests for device occurrence entity queries on the AoU synthetic underlay. There is no need to
 * specify an active profile for this test, because we want to test the main application definition.
 */
public class DeviceOccurrenceEntityQueriesTest extends BaseSpringUnitTest {
  @Autowired private EntityInstancesApiController apiController;

  @Test
  @DisplayName(
      "example dataset builder query: cohort=people who've used a long leg cast, concept set=all devices")
  void generateSqlForDeviceOccurrenceEntitiesRelatedToPeopleWithADevice() throws IOException {
    // filter for "device" entity instances that have concept_id=4038664
    // i.e. the device "Long leg cast"
    ApiFilter longLegCast =
        new ApiFilter()
            .binaryFilter(
                new ApiBinaryFilter()
                    .attributeVariable(
                        new ApiAttributeVariable().variable("device_alias").name("concept_id"))
                    .operator(ApiBinaryFilterOperator.EQUALS)
                    .attributeValue(new ApiAttributeValue().int64Val(4_038_664L)));

    // filter for "device_occurrence" entity instances that are related to "device" entity
    // instances that have concept_id=4038664
    // i.e. give me all the device occurrences of "Long leg cast"
    ApiFilter occurrencesOfLongLegCast =
        new ApiFilter()
            .relationshipFilter(
                new ApiRelationshipFilter()
                    .outerVariable("device_occurrence_alias1")
                    .newVariable("device_alias")
                    .newEntity("device")
                    .filter(longLegCast));

    // filter for "person" entity instances that are related to "device_occurrence" entity
    // instances that are related to "device" entity instances that have concept_id=4038664
    // i.e. give me all the people with device occurrences of "Long leg cast"
    ApiFilter peopleWhoUsedLongLegCast =
        new ApiFilter()
            .relationshipFilter(
                new ApiRelationshipFilter()
                    .outerVariable("person_alias")
                    .newVariable("device_occurrence_alias1")
                    .newEntity("device_occurrence")
                    .filter(occurrencesOfLongLegCast));

    // filter for "device_occurrence" entity instances that are related to "person" entity
    // instances that are related to "device_occurrence" entity instances that are related to
    // "device" entity instances that have concept_id=4038664
    // i.e. give me all the device occurrence rows for people with "Long leg cast". this set of rows
    // will include non-cast device occurrences, such as catheter.
    ApiFilter allObservationOccurrencesForPeopleWhoRefusedVaccine =
        new ApiFilter()
            .relationshipFilter(
                new ApiRelationshipFilter()
                    .outerVariable("device_occurrence_alias2")
                    .newVariable("person_alias")
                    .newEntity("person")
                    .filter(peopleWhoUsedLongLegCast));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            DEVICE_OCCURRENCE_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("device_occurrence_alias2")
                        .selectedAttributes(ALL_DEVICE_OCCURRENCE_ATTRIBUTES)
                        .filter(allObservationOccurrencesForPeopleWhoRefusedVaccine)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql,
        "aousynthetic/device-occurrence-entities-related-to-people-with-a-device.sql");
  }
}
