package bio.terra.tanagra.api.cmssynpuf;

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

public class DeviceOccurrenceTest extends BaseQueryTest {
  @Test
  void longLegCast() throws IOException {
    Entity deviceEntity = underlaysService.getEntity(getUnderlayName(), "device");
    Entity personEntity = underlaysService.getEntity(getUnderlayName(), "person");
    Relationship deviceOccurrenceRelationship = getEntity().getRelationship(deviceEntity);
    Relationship personOccurrenceRelationship = getEntity().getRelationship(personEntity);

    // filter for "device" entity instances that have concept_id=4038664
    // i.e. the device "Long leg cast"
    AttributeFilter longLegCast =
        new AttributeFilter(deviceEntity.getAttribute("id"), EQUALS, new Literal(4_038_664L));

    // filter for "device_occurrence" entity instances that are related to "device" entity
    // instances that have concept_id=4038664
    // i.e. give me all the device occurrences of "Long leg cast"
    RelationshipFilter occurrencesOfLongLegCast =
        new RelationshipFilter(getEntity(), deviceOccurrenceRelationship, longLegCast);

    // filter for "person" entity instances that are related to "device_occurrence" entity
    // instances that are related to "device" entity instances that have concept_id=4038664
    // i.e. give me all the people with device occurrences of "Long leg cast"
    RelationshipFilter peopleWhoUsedLongLegCast =
        new RelationshipFilter(
            personEntity, personOccurrenceRelationship, occurrencesOfLongLegCast);

    // filter for "device_occurrence" entity instances that are related to "person" entity
    // instances that are related to "device_occurrence" entity instances that are related to
    // "device" entity instances that have concept_id=4038664
    // i.e. give me all the device occurrence rows for people with "Long leg cast". this set of rows
    // will include non-cast device occurrences, such as catheter.
    RelationshipFilter occurrencesOfPeopleWhoUsedLongLegCast =
        new RelationshipFilter(getEntity(), personOccurrenceRelationship, peopleWhoUsedLongLegCast);

    EntityQueryRequest entityQueryRequest =
        new EntityQueryRequest.Builder()
            .entity(getEntity())
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(getEntity().getAttributes())
            .filter(occurrencesOfPeopleWhoUsedLongLegCast)
            .limit(DEFAULT_LIMIT)
            .build();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        querysService.buildInstancesQuery(entityQueryRequest).getSql(),
        "sql/cmssynpuf/deviceoccurrence-longLegCast.sql");
  }

  @Override
  protected String getUnderlayName() {
    return "cms_synpuf";
  }

  @Override
  protected String getEntityName() {
    return "device_occurrence";
  }
}
