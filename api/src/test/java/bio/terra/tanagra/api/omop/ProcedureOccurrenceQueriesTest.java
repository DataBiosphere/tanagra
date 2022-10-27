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

public abstract class ProcedureOccurrenceQueriesTest extends BaseQueryTest {
  @Test
  void mammogram() throws IOException {
    Entity procedureEntity = underlaysService.getEntity(getUnderlayName(), "procedure");
    Entity personEntity = underlaysService.getEntity(getUnderlayName(), "person");
    Relationship procedureOccurrenceRelationship = getEntity().getRelationship(procedureEntity);
    Relationship personOccurrenceRelationship = getEntity().getRelationship(personEntity);

    // filter for "procedure" entity instances that have concept_id=4324693
    // i.e. the procedure "Mammography"
    AttributeFilter mammogram =
        new AttributeFilter(procedureEntity.getAttribute("id"), EQUALS, new Literal(4_324_693L));

    // filter for "procedure_occurrence" entity instances that are related to "procedure" entity
    // instances that have concept_id=4324693
    // i.e. give me all the procedure occurrences of "Mammography"
    RelationshipFilter occurrencesOfMammogram =
        new RelationshipFilter(getEntity(), procedureOccurrenceRelationship, mammogram);

    // filter for "person" entity instances that are related to "procedure_occurrence" entity
    // instances that are related to "procedure" entity instances that have concept_id=4324693
    // i.e. give me all the people with procedure occurrences of "Mammography"
    RelationshipFilter peopleWhoHadAMammogram =
        new RelationshipFilter(personEntity, personOccurrenceRelationship, occurrencesOfMammogram);

    // filter for "procedure_occurrence" entity instances that are related to "person" entity
    // instances that are related to "procedure_occurrence" entity instances that are related to
    // "procedure" entity instances that have concept_id=4324693
    // i.e. give me all the procedure occurrence rows for people with "Mammography". this
    // set of rows will include non-mammography procedure occurrences, such as knee surgery.
    RelationshipFilter occurrencesOfPeopleWhoHadAMammogram =
        new RelationshipFilter(getEntity(), personOccurrenceRelationship, peopleWhoHadAMammogram);

    EntityQueryRequest entityQueryRequest =
        new EntityQueryRequest.Builder()
            .entity(getEntity())
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(getEntity().getAttributes())
            .filter(occurrencesOfPeopleWhoHadAMammogram)
            .limit(DEFAULT_LIMIT)
            .build();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        querysService.buildInstancesQuery(entityQueryRequest).getSql(),
        "sql/" + getSqlDirectoryName() + "/procedureoccurrence-ibuprofen.sql");
  }

  @Override
  protected String getEntityName() {
    return "procedure_occurrence";
  }
}
