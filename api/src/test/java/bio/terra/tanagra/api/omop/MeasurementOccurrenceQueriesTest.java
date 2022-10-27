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

public abstract class MeasurementOccurrenceQueriesTest extends BaseQueryTest {
  @Test
  void hematocrit() throws IOException {
    Entity measurementEntity = underlaysService.getEntity(getUnderlayName(), "measurement");
    Entity personEntity = underlaysService.getEntity(getUnderlayName(), "person");
    Relationship measurementOccurrenceRelationship = getEntity().getRelationship(measurementEntity);
    Relationship personOccurrenceRelationship = getEntity().getRelationship(personEntity);

    // filter for "measurement" entity instances that have concept_id=3009542
    // i.e. the measurement "Hematocrit [Volume Fraction] of Blood"
    AttributeFilter hematocrit =
        new AttributeFilter(measurementEntity.getAttribute("id"), EQUALS, new Literal(3_009_542L));

    // filter for "measurement_occurrence" entity instances that are related to "measruement" entity
    // instances that have concept_id=3009542
    // i.e. give me all the measurement occurrences of "Hematocrit [Volume Fraction] of Blood"
    RelationshipFilter occurrencesOfHematocrit =
        new RelationshipFilter(getEntity(), measurementOccurrenceRelationship, hematocrit);

    // filter for "person" entity instances that are related to "measurement_occurrence" entity
    // instances that are related to "measruement" entity instances that have concept_id=3009542
    // i.e. give me all the people with measurement occurrences of "Hematocrit [Volume Fraction] of
    // Blood"
    RelationshipFilter peopleWhoHadAHematocrit =
        new RelationshipFilter(personEntity, personOccurrenceRelationship, occurrencesOfHematocrit);

    // filter for "measurement_occurrence" entity instances that are related to "person" entity
    // instances that are related to "measurement_occurrence" entity instances that are related to
    // "measurement" entity instances that have concept_id=3009542
    // i.e. give me all the measurement occurrence rows for people with "Hematocrit [Volume
    // Fraction] of Blood". this set of rows will include non-hematocrit measurement occurrences,
    // such as glucose test.
    RelationshipFilter occurrencesOfPeopleWhoHadAHematocrit =
        new RelationshipFilter(getEntity(), personOccurrenceRelationship, peopleWhoHadAHematocrit);

    EntityQueryRequest entityQueryRequest =
        new EntityQueryRequest.Builder()
            .entity(getEntity())
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(getEntity().getAttributes())
            .filter(occurrencesOfPeopleWhoHadAHematocrit)
            .limit(DEFAULT_LIMIT)
            .build();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        querysService.buildInstancesQuery(entityQueryRequest).getSql(),
        "sql/" + getSqlDirectoryName() + "/measurementoccurrence-ibuprofen.sql");
  }

  @Override
  protected String getEntityName() {
    return "measurement_occurrence";
  }
}
