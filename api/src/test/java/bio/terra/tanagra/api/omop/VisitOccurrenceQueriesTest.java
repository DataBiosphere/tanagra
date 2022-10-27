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

public abstract class VisitOccurrenceQueriesTest extends BaseQueryTest {
  @Test
  void outpatient() throws IOException {
    Entity visitEntity = underlaysService.getEntity(getUnderlayName(), "visit");
    Entity personEntity = underlaysService.getEntity(getUnderlayName(), "person");
    Relationship visitOccurrenceRelationship = getEntity().getRelationship(visitEntity);
    Relationship personOccurrenceRelationship = getEntity().getRelationship(personEntity);

    // filter for "visit" entity instances that have concept_id=9202
    // i.e. the visit "Outpatient Visit"
    AttributeFilter outpatient =
        new AttributeFilter(visitEntity.getAttribute("id"), EQUALS, new Literal(9_202L));

    // filter for "visit_occurrence" entity instances that are related to "visit" entity
    // instances that have concept_id=9202
    // i.e. give me all the visit occurrences of "Outpatient Visit"
    RelationshipFilter occurrencesOfOutpatient =
        new RelationshipFilter(getEntity(), visitOccurrenceRelationship, outpatient);

    // filter for "person" entity instances that are related to "visit_occurrence" entity
    // instances that are related to "visit" entity instances that have concept_id=9202
    // i.e. give me all the people with visit occurrences of "Outpatient Visit"
    RelationshipFilter peopleWhoHadOutpatient =
        new RelationshipFilter(personEntity, personOccurrenceRelationship, occurrencesOfOutpatient);

    // filter for "visit_occurrence" entity instances that are related to "person" entity
    // instances that are related to "visit_occurrence" entity instances that are related to
    // "visit" entity instances that have concept_id=9202
    // i.e. give me all the visit occurrence rows for people with "Outpatient Visit"
    RelationshipFilter occurrencesOfPeopleWhoHadOutpatient =
        new RelationshipFilter(getEntity(), personOccurrenceRelationship, peopleWhoHadOutpatient);

    EntityQueryRequest entityQueryRequest =
        new EntityQueryRequest.Builder()
            .entity(getEntity())
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(getEntity().getAttributes())
            .filter(occurrencesOfPeopleWhoHadOutpatient)
            .limit(DEFAULT_LIMIT)
            .build();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        querysService.buildInstancesQuery(entityQueryRequest).getSql(),
        "sql/" + getSqlDirectoryName() + "/visitoccurrence-longLegCast.sql");
  }

  @Override
  protected String getEntityName() {
    return "visit_occurrence";
  }
}
