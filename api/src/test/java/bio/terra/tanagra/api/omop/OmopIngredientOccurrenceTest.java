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

public abstract class OmopIngredientOccurrenceTest extends BaseQueryTest {
  @Test
  void longLegCast() throws IOException {
    Entity ingredientEntity = underlaysService.getEntity(getUnderlayName(), "ingredient");
    Entity personEntity = underlaysService.getEntity(getUnderlayName(), "person");
    Relationship ingredientOccurrenceRelationship = getEntity().getRelationship(ingredientEntity);
    Relationship personOccurrenceRelationship = getEntity().getRelationship(personEntity);

    // filter for "ingredient" entity instances that have concept_id=1177480
    // i.e. the ingredient "Ibuprofen"
    AttributeFilter ibuprofen =
        new AttributeFilter(ingredientEntity.getAttribute("id"), EQUALS, new Literal(1_177_480L));

    // filter for "ingredient_occurrence" entity instances that are related to "ingredient" entity
    // instances that have concept_id=1177480
    // i.e. give me all the ingredient occurrences of "Ibuprofen"
    RelationshipFilter occurrencesOfIbuprofen =
        new RelationshipFilter(getEntity(), ingredientOccurrenceRelationship, ibuprofen);

    // filter for "person" entity instances that are related to "ingredient_occurrence" entity
    // instances that are related to "ingredient" entity instances that have concept_id=1177480
    // i.e. give me all the people with ingredient occurrences of "Ibuprofen"
    RelationshipFilter peopleWhoTookIbuprofen =
        new RelationshipFilter(personEntity, personOccurrenceRelationship, occurrencesOfIbuprofen);

    // filter for "ingredient_occurrence" entity instances that are related to "person" entity
    // instances that are related to "ingredient_occurrence" entity instances that are related to
    // "ingredient" entity instances that have concept_id=1177480
    // i.e. give me all the ingredient occurrence rows for people with "Ibuprofen". this set of rows
    // will include non-ibuprofen ingredient occurrences, such as acetaminophen.
    RelationshipFilter occurrencesOfPeopleWhoTookIbuprofen =
        new RelationshipFilter(getEntity(), personOccurrenceRelationship, peopleWhoTookIbuprofen);

    EntityQueryRequest entityQueryRequest =
        new EntityQueryRequest.Builder()
            .entity(getEntity())
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(getEntity().getAttributes())
            .filter(occurrencesOfPeopleWhoTookIbuprofen)
            .limit(DEFAULT_LIMIT)
            .build();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        querysService.buildInstancesQuery(entityQueryRequest).getSql(),
        "sql/" + getSqlDirectoryName() + "/ingredientoccurrence-ibuprofen.sql");
  }

  @Override
  protected String getEntityName() {
    return "ingredient_occurrence";
  }
}
