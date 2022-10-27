package bio.terra.tanagra.api.omop;

import static bio.terra.tanagra.query.filtervariable.BinaryFilterVariable.BinaryOperator.EQUALS;

import bio.terra.tanagra.api.BaseQueriesTest;
import bio.terra.tanagra.api.EntityQueryRequest;
import bio.terra.tanagra.api.entityfilter.AttributeFilter;
import bio.terra.tanagra.api.entityfilter.BooleanAndOrFilter;
import bio.terra.tanagra.api.entityfilter.RelationshipFilter;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable;
import bio.terra.tanagra.testing.GeneratedSqlUtils;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Relationship;
import bio.terra.tanagra.underlay.Underlay;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

public abstract class ConditionOccurrenceQueriesTest extends BaseQueriesTest {
  @Test
  void diabetes() throws IOException {
    allOccurrencesForPrimariesWithACriteria(201_826L, "diabetes"); // "Type 2 diabetes mellitus"
  }

  @Test
  void diabetesAndSepsis() throws IOException {
    Entity conditionEntity = underlaysService.getEntity(getUnderlayName(), "condition");
    Entity personEntity = underlaysService.getEntity(getUnderlayName(), "person");
    Relationship conditionOccurrenceRelationship = getEntity().getRelationship(conditionEntity);
    Relationship personOccurrenceRelationship = getEntity().getRelationship(personEntity);

    // filter for "condition" entity instances that have concept_id=201826
    // i.e. the condition "Type 2 diabetes mellitus"
    AttributeFilter diabetes =
        new AttributeFilter(conditionEntity.getAttribute("id"), EQUALS, new Literal(201_826L));

    // filter for "condition_occurrence" entity instances that are related to "condition" entity
    // instances that have concept_id=201826
    // i.e. give me all the condition occurrences of "Type 2 diabetes mellitus"
    RelationshipFilter occurrencesOfDiabetes =
        new RelationshipFilter(getEntity(), conditionOccurrenceRelationship, diabetes);

    // filter for "person" entity instances that are related to "condition_occurrence" entity
    // instances that are related to "condition" entity instances that have concept_id=201826
    // i.e. give me all the people with condition occurrences of "Type 2 diabetes mellitus"
    RelationshipFilter peopleWhoHadDiabetes =
        new RelationshipFilter(personEntity, personOccurrenceRelationship, occurrencesOfDiabetes);

    // filter for "condition" entity instances that have concept_id=132797
    // i.e. the condition "Sepsis"
    AttributeFilter sepsis =
        new AttributeFilter(conditionEntity.getAttribute("id"), EQUALS, new Literal(132_797L));

    // filter for "condition_occurrence" entity instances that are related to "condition" entity
    // instances that have concept_id=132797
    // i.e. give me all the condition occurrences of "Sepsis"
    RelationshipFilter occurrencesOfSepsis =
        new RelationshipFilter(getEntity(), conditionOccurrenceRelationship, sepsis);

    // filter for "person" entity instances that are related to "condition_occurrence" entity
    // instances that are related to "condition" entity instances that have concept_id=132797
    // i.e. give me all the people with condition occurrences of "Sepsis"
    RelationshipFilter peopleWhoHadSepsis =
        new RelationshipFilter(personEntity, personOccurrenceRelationship, occurrencesOfSepsis);

    // filter for "person" entity instances who had diabetes AND sepsis
    BooleanAndOrFilter peopleWhoHadDiabetesAndSepsis =
        new BooleanAndOrFilter(
            BooleanAndOrFilterVariable.LogicalOperator.AND,
            List.of(peopleWhoHadDiabetes, peopleWhoHadSepsis));

    // filter for "condition occurrence" entity instances that are related to "person" entity
    // instances that are related to "condition_occurrence" entity instances that are related to
    // ("condition" entity instances that have concept_id=439676 AND are related to
    // "condition_occurrence" entity instances that are related to "condition" entity instances that
    // have concept_id=132797)
    // i.e. give me all the condition occurrence rows for people with "Type 2 diabetes mellitus" and
    // "Sepsis". this set of rows will include non-diabetes and non-sepsis condition occurrences,
    // such as covid.
    RelationshipFilter occurrencesOfPeopleWhoHadDiabetesAndSepsis =
        new RelationshipFilter(
            getEntity(), personOccurrenceRelationship, peopleWhoHadDiabetesAndSepsis);

    EntityQueryRequest entityQueryRequest =
        new EntityQueryRequest.Builder()
            .entity(getEntity())
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(getEntity().getAttributes())
            .filter(occurrencesOfPeopleWhoHadDiabetesAndSepsis)
            .limit(DEFAULT_LIMIT)
            .build();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        querysService.buildInstancesQuery(entityQueryRequest).getSql(),
        "sql/" + getSqlDirectoryName() + "/conditionoccurrence-diabetesAndSepsis.sql");
  }

  @Override
  protected String getEntityName() {
    return "condition_occurrence";
  }
}
