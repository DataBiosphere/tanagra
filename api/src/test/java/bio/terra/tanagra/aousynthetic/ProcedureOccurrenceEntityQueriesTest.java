package bio.terra.tanagra.aousynthetic;

import static bio.terra.tanagra.aousynthetic.UnderlayUtils.ALL_PROCEDURE_OCCURRENCE_ATTRIBUTES;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.PROCEDURE_OCCURRENCE_ENTITY;
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
 * Tests for procedure occurrence entity queries on the AoU synthetic underlay. There is no need to
 * specify an active profile for this test, because we want to test the main application definition.
 */
public class ProcedureOccurrenceEntityQueriesTest extends BaseSpringUnitTest {
  @Autowired private EntityInstancesApiController apiController;

  @Test
  @DisplayName(
      "example dataset builder query: cohort=people who've had a mammogram, concept set=all procedures")
  void generateSqlForProcedureOccurrenceEntitiesRelatedToPeopleWithAProcedure() throws IOException {
    // filter for "procedure" entity instances that have concept_id=4324693
    // i.e. the procedure "Mammography"
    ApiFilter mammography =
        new ApiFilter()
            .binaryFilter(
                new ApiBinaryFilter()
                    .attributeVariable(
                        new ApiAttributeVariable().variable("procedure_alias").name("concept_id"))
                    .operator(ApiBinaryFilterOperator.EQUALS)
                    .attributeValue(new ApiAttributeValue().int64Val(4_324_693L)));

    // filter for "procedure_occurrence" entity instances that are related to "procedure" entity
    // instances that have concept_id=4324693
    // i.e. give me all the procedure occurrences of "Mammography"
    ApiFilter occurrencesOfMammography =
        new ApiFilter()
            .relationshipFilter(
                new ApiRelationshipFilter()
                    .outerVariable("procedure_occurrence_alias1")
                    .newVariable("procedure_alias")
                    .newEntity("procedure")
                    .filter(mammography));

    // filter for "person" entity instances that are related to "procedure_occurrence" entity
    // instances that are related to "procedure" entity instances that have concept_id=4324693
    // i.e. give me all the people with procedure occurrences of "Mammography"
    ApiFilter peopleWhoHadMammography =
        new ApiFilter()
            .relationshipFilter(
                new ApiRelationshipFilter()
                    .outerVariable("person_alias")
                    .newVariable("procedure_occurrence_alias1")
                    .newEntity("procedure_occurrence")
                    .filter(occurrencesOfMammography));

    // filter for "procedure_occurrence" entity instances that are related to "person" entity
    // instances that are related to "procedure_occurrence" entity instances that are related to
    // "procedure" entity instances that have concept_id=4324693
    // i.e. give me all the procedure occurrence rows for people with "Mammography". this
    // set of rows will include non-mammography procedure occurrences, such as knee surgery.
    ApiFilter allProcedureOccurrencesForPeopleWithMammography =
        new ApiFilter()
            .relationshipFilter(
                new ApiRelationshipFilter()
                    .outerVariable("procedure_occurrence_alias2")
                    .newVariable("person_alias")
                    .newEntity("person")
                    .filter(peopleWhoHadMammography));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            PROCEDURE_OCCURRENCE_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("procedure_occurrence_alias2")
                        .selectedAttributes(ALL_PROCEDURE_OCCURRENCE_ATTRIBUTES)
                        .filter(allProcedureOccurrencesForPeopleWithMammography)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql,
        "aousynthetic/procedure-occurrence-entities-related-to-people-with-a-procedure.sql");
  }
}
