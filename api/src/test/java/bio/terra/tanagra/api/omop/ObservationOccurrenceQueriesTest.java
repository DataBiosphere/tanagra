package bio.terra.tanagra.api.omop;

import static bio.terra.tanagra.query.filtervariable.BinaryFilterVariable.BinaryOperator.EQUALS;

import bio.terra.tanagra.api.BaseQueryTest;
import bio.terra.tanagra.api.EntityQueryRequest;
import bio.terra.tanagra.api.entityfilter.AttributeFilter;
import bio.terra.tanagra.api.entityfilter.RelationshipFilter;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.testing.GeneratedSqlUtils;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Relationship;
import bio.terra.tanagra.underlay.Underlay;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public abstract class ObservationOccurrenceQueriesTest extends BaseQueryTest {
  @Test
  void vaccineRefusal() throws IOException {
    Entity observationEntity = underlaysService.getEntity(getUnderlayName(), "observation");
    Entity personEntity = underlaysService.getEntity(getUnderlayName(), "person");
    Relationship observationOccurrenceRelationship = getEntity().getRelationship(observationEntity);
    Relationship personOccurrenceRelationship = getEntity().getRelationship(personEntity);

    // filter for "observation" entity instances that have concept_id=43531662
    // i.e. the observation "Vaccine refused by patient"
    AttributeFilter vaccineRefusal =
        new AttributeFilter(observationEntity.getAttribute("id"), EQUALS, new Literal(43_531_662L));

    // filter for "observation_occurrence" entity instances that are related to "observation" entity
    // instances that have concept_id=43531662
    // i.e. give me all the observation occurrences of "Vaccine refused by patient"
    RelationshipFilter occurrencesOfVaccineRefusal =
        new RelationshipFilter(getEntity(), observationOccurrenceRelationship, vaccineRefusal);

    // filter for "person" entity instances that are related to "observation_occurrence" entity
    // instances that are related to "observation" entity instances that have concept_id=43531662
    // i.e. give me all the people with observation occurrences of "Vaccine refused by patient"
    RelationshipFilter peopleWhoRefusedVaccine =
        new RelationshipFilter(
            personEntity, personOccurrenceRelationship, occurrencesOfVaccineRefusal);

    // filter for "observation_occurrence" entity instances that are related to "person" entity
    // instances that are related to "observation_occurrence" entity instances that are related to
    // "observation" entity instances that have concept_id=43531662
    // i.e. give me all the observation occurrence rows for people with "Vaccine refused by
    // patient". this set of rows will include non-vaccine observation occurrences, such as blood
    // disorder.
    RelationshipFilter occurrencesOfPeopleWhoRefusedVaccine =
        new RelationshipFilter(getEntity(), personOccurrenceRelationship, peopleWhoRefusedVaccine);

    EntityQueryRequest entityQueryRequest =
        new EntityQueryRequest.Builder()
            .entity(getEntity())
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(getEntity().getAttributes())
            .filter(occurrencesOfPeopleWhoRefusedVaccine)
            .limit(DEFAULT_LIMIT)
            .build();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        querysService.buildInstancesQuery(entityQueryRequest).getSql(),
        "sql/" + getSqlDirectoryName() + "/observationoccurrence-longLegCast.sql");
  }

  @Override
  protected String getEntityName() {
    return "observation_occurrence";
  }
}
