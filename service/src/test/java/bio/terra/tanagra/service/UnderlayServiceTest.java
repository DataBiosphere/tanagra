package bio.terra.tanagra.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.query.hint.HintQueryResult;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.NaryOperator;
import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Main.class)
@SpringBootTest
@ActiveProfiles("test")
public class UnderlayServiceTest {
  @Autowired private UnderlayService underlayService;

  @Test
  void listAllOrSelected() {
    String underlayName = "cmssynpuf";

    // List underlays.
    List<Underlay> allUnderlays =
        underlayService.listUnderlays(
            ResourceCollection.allResourcesAllPermissions(ResourceType.UNDERLAY, null));
    assertEquals(3, allUnderlays.size());

    List<Underlay> oneUnderlay =
        underlayService.listUnderlays(
            ResourceCollection.resourcesSamePermissions(
                Permissions.allActions(ResourceType.UNDERLAY),
                Set.of(ResourceId.forUnderlay(underlayName))));
    assertEquals(1, oneUnderlay.size());
    assertEquals(underlayName, oneUnderlay.get(0).getName());

    // List entities.
    List<Entity> allEntities = underlayService.getUnderlay(underlayName).getEntities();
    assertEquals(16, allEntities.size());
  }

  @Test
  void invalid() {
    // Get an invalid underlay.
    assertThrows(NotFoundException.class, () -> underlayService.getUnderlay("invalid underlay"));

    // Get an invalid entity.
    assertThrows(
        bio.terra.tanagra.exception.NotFoundException.class,
        () -> underlayService.getUnderlay("cmssynpuf").getEntity("invalid entity"));
  }

  @Test
  void entityLevelHints() {
    Underlay underlay = underlayService.getUnderlay("cmssynpuf");
    Entity entity = underlay.getEntity("person");

    Map<String, Attribute> hintAttributes = new HashMap<>();
    hintAttributes.put("age", null);
    hintAttributes.put("ethnicity", null);
    hintAttributes.put("gender", null);
    hintAttributes.put("race", null);
    hintAttributes.put("year_of_birth", null);

    // verily that all hints for all attributes are returned without a filter

    Map<Long, Long> enumCounts = new HashMap<>(); // enumVal, count
    Long[] ageRange = new Long[2];
    Long[] yearOfBirthRange = new Long[2];

    HintQueryResult hintQueryResult = underlayService.getEntityLevelHints(underlay, entity, null);
    assertNotNull(hintQueryResult.getSql());
    hintQueryResult
        .getHintInstances()
        .forEach(
            hi -> {
              String attrName = hi.getAttribute().getName();
              hintAttributes.put(attrName, hi.getAttribute());
              assertTrue(hintAttributes.containsKey(attrName)); // expected
              assertNull(hintAttributes.get(attrName)); // not seen yet

              if ("age".equals(attrName)) { // depends on current_Timestamp
                assertTrue(hi.isRangeHint());
                assertNotEquals(0, hi.getMin());
                assertNotEquals(0, hi.getMax());
                ageRange[0] = (long) hi.getMin();
                ageRange[1] = (long) hi.getMax();

              } else if ("ethnicity".equals(attrName)) {
                assertFalse(hi.isRangeHint());
                assertEquals(2, hi.getEnumValueCounts().size());
                hi.getEnumValueCounts()
                    .forEach((vd, c) -> enumCounts.put(vd.getValue().getInt64Val(), c));

              } else if ("gender".equals(attrName)) {
                assertFalse(hi.isRangeHint());
                assertEquals(2, hi.getEnumValueCounts().size());
                hi.getEnumValueCounts()
                    .forEach((vd, c) -> enumCounts.put(vd.getValue().getInt64Val(), c));

              } else if ("race".equals(attrName)) {
                assertFalse(hi.isRangeHint());
                assertEquals(3, hi.getEnumValueCounts().size());
                hi.getEnumValueCounts()
                    .forEach((vd, c) -> enumCounts.put(vd.getValue().getInt64Val(), c));

              } else if ("year_of_birth".equals(attrName)) {
                assertTrue(hi.isRangeHint());
                assertEquals(1909, hi.getMin());
                assertEquals(1983, hi.getMax());
                yearOfBirthRange[0] = (long) hi.getMin();
                yearOfBirthRange[1] = (long) hi.getMax();
              }
            });
    // check that all attrs were seen
    assertFalse(hintAttributes.values().stream().anyMatch(Objects::nonNull));

    // Filter where year_of_birth in (1930,1932)
    EntityFilter entityFilter =
        new AttributeFilter(
            underlay,
            entity,
            hintAttributes.get("year_of_birth"),
            NaryOperator.IN,
            List.of(Literal.forInt64(1900L), Literal.forInt64(1932L)));
    hintQueryResult = underlayService.getEntityLevelHints(underlay, entity, entityFilter);
    assertEquals(hintAttributes.size(), hintQueryResult.getHintInstances().size());

    hintQueryResult
        .getHintInstances()
        .forEach(
            hi -> {
              String attrName = hi.getAttribute().getName();
              assertTrue(hintAttributes.containsKey(attrName));

              if ("age".equals(attrName)) { // depends on current_Timestamp
                assertNotEquals(0, hi.getMin());
                assertNotEquals(0, hi.getMax());
                assertTrue(hi.getMin() >= ageRange[0]);
                assertTrue(hi.getMax() <= ageRange[1]);

              } else if ("ethnicity".equals(attrName)) {
                assertEquals(2, hi.getEnumValueCounts().size());
                hi.getEnumValueCounts()
                    .forEach(
                        (vd, c) ->
                            assertTrue(
                                c < enumCounts.getOrDefault(vd.getValue().getInt64Val(), 0L)));

              } else if ("gender".equals(attrName)) {
                assertEquals(2, hi.getEnumValueCounts().size());
                hi.getEnumValueCounts()
                    .forEach(
                        (vd, c) ->
                            assertTrue(
                                c < enumCounts.getOrDefault(vd.getValue().getInt64Val(), 0L)));

              } else if ("race".equals(attrName)) {
                assertEquals(3, hi.getEnumValueCounts().size());
                hi.getEnumValueCounts()
                    .forEach(
                        (vd, c) ->
                            assertTrue(
                                c < enumCounts.getOrDefault(vd.getValue().getInt64Val(), 0L)));

              } else if ("year_of_birth".equals(attrName)) {
                assertEquals(Math.max(1900, yearOfBirthRange[0]), hi.getMin());
                assertEquals(Math.min(1932, yearOfBirthRange[1]), hi.getMax());
                assertTrue(hi.getMin() >= yearOfBirthRange[0]);
                assertTrue(hi.getMax() <= yearOfBirthRange[1]);
              }
            });
  }
}
