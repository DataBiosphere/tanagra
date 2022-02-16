package bio.terra.tanagra.aousynthetic;

import static bio.terra.tanagra.aousynthetic.UnderlayUtils.PERSON_ENTITY;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.PERSON_ETHNICITY_ATTRIBUTE;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.PERSON_GENDER_ATTRIBUTE;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.PERSON_RACE_ATTRIBUTE;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.PERSON_SEXATBIRTH_ATTRIBUTE;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.PERSON_YEAROFBIRTH_ATTRIBUTE;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.UNDERLAY_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.app.controller.EntitiesApiController;
import bio.terra.tanagra.generated.model.ApiAttribute;
import bio.terra.tanagra.generated.model.ApiAttributeFilterHint;
import bio.terra.tanagra.generated.model.ApiEntity;
import bio.terra.tanagra.testing.BaseSpringUnitTest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Tests for the person entity definition on the AoU synthetic underlay. There is no need to specify
 * an active profile for this test, because we want to test the main application definition.
 */
public class PersonEntityDefinitionTest extends BaseSpringUnitTest {
  @Autowired private EntitiesApiController apiController;

  @Test
  @DisplayName("correct UI hints for demographic attributes")
  void uiHintsForDemographicAttributes() {
    // fetch the entity definition
    ResponseEntity<ApiEntity> response = apiController.getEntity(UNDERLAY_NAME, PERSON_ENTITY);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // build a map of the attribute name -> ui hint
    ApiEntity entity = response.getBody();
    List<ApiAttribute> attributes = entity.getAttributes();
    Map<String, ApiAttributeFilterHint> uiHints =
        attributes.stream()
            .collect(
                HashMap::new,
                (hintsMap, attribute) ->
                    hintsMap.put(attribute.getName(), attribute.getAttributeFilterHint()),
                HashMap::putAll);

    assertTrue(uiHints.containsKey(PERSON_YEAROFBIRTH_ATTRIBUTE), "year_of_birth attribute exists");
    assertNotNull(
        uiHints.get(PERSON_YEAROFBIRTH_ATTRIBUTE).getIntegerBoundsHint(),
        "year_of_birth attribute includes integer bounds hint");

    assertTrue(
        uiHints.containsKey(PERSON_ETHNICITY_ATTRIBUTE), "ethnicity_concept_id attribute exists");
    assertNotNull(
        uiHints.get(PERSON_ETHNICITY_ATTRIBUTE).getEnumHint(),
        "ethnicity_concept_id attribute includes enum hint");

    assertTrue(uiHints.containsKey(PERSON_GENDER_ATTRIBUTE), "gender_concept_id attribute exists");
    assertNotNull(
        uiHints.get(PERSON_GENDER_ATTRIBUTE).getEnumHint(),
        "gender_concept_id attribute includes enum hint");

    assertTrue(uiHints.containsKey(PERSON_RACE_ATTRIBUTE), "race_concept_id attribute exists");
    assertNotNull(
        uiHints.get(PERSON_RACE_ATTRIBUTE).getEnumHint(),
        "race_concept_id attribute includes enum hint");

    assertTrue(
        uiHints.containsKey(PERSON_SEXATBIRTH_ATTRIBUTE),
        "sex_at_birth_concept_id attribute exists");
    assertNotNull(
        uiHints.get(PERSON_SEXATBIRTH_ATTRIBUTE).getEnumHint(),
        "sex_at_birth_concept_id attribute includes enum hint");
  }
}
