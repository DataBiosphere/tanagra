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
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.NaryOperator;
import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.indexing.job.bigquery.WriteEntityLevelDisplayHints;
import bio.terra.tanagra.query.bigquery.translator.BQApiTranslator;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
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

    Map<Long, Long> enumCounts = new HashMap<>(); // enumVal, count
    Long[] ageRange = new Long[2];
    Long[] yearOfBirthRange = new Long[2];

    // verily that all hints for all attributes are returned without a filter
    HintQueryResult hintQueryResult = underlayService.getEntityLevelHints(underlay, entity, null);
    assertNotNull(hintQueryResult.getSql());
    hintQueryResult
        .getHintInstances()
        .forEach(
            hi -> {
              String attrName = hi.getAttribute().getName();
              assertTrue(hintAttributes.containsKey(attrName), attrName); // expected
              assertNull(hintAttributes.get(attrName), attrName); // not seen before

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

              hintAttributes.put(attrName, hi.getAttribute());
            });
    // check that all attrs were seen
    assertFalse(hintAttributes.values().stream().anyMatch(Objects::isNull));

    // Filter where year_of_birth in (1900 - 1932)
    EntityFilter entityFilter =
        new AttributeFilter(
            underlay,
            entity,
            hintAttributes.get("year_of_birth"),
            NaryOperator.BETWEEN,
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

  @Test
  void buildHintSql() {
    Underlay underlay = underlayService.getUnderlay("cmssynpuf");
    String entityName = "person";
    Entity entity = underlay.getEntity(entityName);
    ITEntityMain entityTable = underlay.getIndexSchema().getEntityMain(entityName);
    String bqTableName = entityTable.getTablePointer().render();

    String attrName = "year_of_birth";
    Attribute attribute = entity.getAttribute(attrName);
    EntityFilter entityFilter =
        new AttributeFilter(
            underlay,
            entity,
            attribute,
            NaryOperator.IN,
            List.of(Literal.forInt64(1900L), Literal.forInt64(1932L)));
    BQApiTranslator bqTranslator = new BQApiTranslator();
    SqlParams sqlParams = new SqlParams();
    String bqFilterSql = bqTranslator.translator(entityFilter).buildSql(sqlParams, null);
    String expectedSql =
        String.format(
            "SELECT MIN(%s) AS minVal, MAX(%s) AS maxVal FROM (SELECT %s FROM %s WHERE %s)",
            attrName, attrName, attrName, bqTableName, bqFilterSql);
    String hintSql =
        WriteEntityLevelDisplayHints.buildRangeHintSql(entityTable, attribute, bqFilterSql);
    assertEquals(expectedSql, hintSql);

    attrName = "race";
    attribute = entity.getAttribute(attrName);
    entityFilter =
        new AttributeFilter(
            underlay, entity, attribute, BinaryOperator.EQUALS, Literal.forInt64(8516L));
    bqFilterSql = new BQApiTranslator().translator(entityFilter).buildSql(new SqlParams(), null);
    expectedSql =
        String.format(
            "SELECT %s AS enumVal, T_DISP_%s AS enumDisp, COUNT(*) AS enumCount FROM %s WHERE %s GROUP BY enumVal, enumDisp",
            attrName, attrName, bqTableName, bqFilterSql);
    hintSql =
        WriteEntityLevelDisplayHints.buildEnumHintForValueDisplaySql(
            entityTable, attribute, bqFilterSql);
    assertEquals(expectedSql, hintSql);
  }
}
