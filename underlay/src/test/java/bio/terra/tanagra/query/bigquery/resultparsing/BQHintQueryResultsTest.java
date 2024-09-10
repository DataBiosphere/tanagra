package bio.terra.tanagra.query.bigquery.resultparsing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.api.query.hint.*;
import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.query.bigquery.BQRunnerTest;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import java.util.List;
import org.junit.jupiter.api.Test;

public class BQHintQueryResultsTest extends BQRunnerTest {
  @Override
  protected String getServiceConfigName() {
    return "aouSR2019q4r4_broad";
  }

  @Test
  void entityLevelHints() {
    // Person entity should have range hints for year_of_birth and age (runtime calculated), and an
    // enum value-display hint with 3 values for gender.
    List<HintInstance> hintInstances = checkEntityLevelHints(underlay.getPrimaryEntity());
    assertTrue(
        hintInstances.stream()
            .anyMatch(
                hintInstance ->
                    hintInstance.getAttribute().getName().equals("year_of_birth")
                        && hintInstance.isRangeHint()));
    assertTrue(
        hintInstances.stream()
            .anyMatch(
                hintInstance ->
                    hintInstance.getAttribute().getName().equals("age")
                        && hintInstance.isRangeHint()));
    assertTrue(
        hintInstances.stream()
            .anyMatch(
                hintInstance ->
                    hintInstance.getAttribute().getName().equals("gender")
                        && hintInstance.isEnumHint()
                        && hintInstance.getEnumValueCounts().size() == 3));

    // Brand entity should have an enum string-value hint with 2 values for vocabulary.
    hintInstances = checkEntityLevelHints(underlay.getEntity("brand"));
    assertTrue(
        hintInstances.stream()
            .anyMatch(
                hintInstance ->
                    hintInstance.getAttribute().getName().equals("vocabulary")
                        && hintInstance.isEnumHint()
                        && hintInstance.getEnumValueCounts().size() == 2));
  }

  private List<HintInstance> checkEntityLevelHints(Entity hintedEntity) {
    HintQueryResult hintQueryResult =
        bqQueryRunner.run(new HintQueryRequest(underlay, hintedEntity, null, null, null, false));

    // Make sure we got some results back.
    assertFalse(hintQueryResult.getHintInstances().isEmpty());

    // Check each of the hinted attributes fields.
    hintQueryResult
        .getHintInstances()
        .forEach(
            hintInstance -> {
              Attribute attribute = hintInstance.getAttribute();
              assertTrue(attribute.isComputeDisplayHint());
              if (hintInstance.isRangeHint()) {
                assertTrue(
                    List.of(DataType.INT64, DataType.DOUBLE)
                        .contains(attribute.getRuntimeDataType()));
                assertTrue(hintInstance.getMin() <= hintInstance.getMax());
              } else { // isEnumHint
                assertTrue(
                    (attribute.isValueDisplay()
                            && DataType.INT64.equals(attribute.getRuntimeDataType()))
                        || (attribute.isSimple()
                            && DataType.STRING.equals(attribute.getRuntimeDataType())));
                assertFalse(hintInstance.getEnumValueCounts().isEmpty());
                hintInstance
                    .getEnumValueCounts()
                    .keySet()
                    .forEach(
                        enumValue ->
                            assertTrue(
                                enumValue.getValue().isNull()
                                    || attribute
                                        .getRuntimeDataType()
                                        .equals(enumValue.getValue().getDataType())));
              }
            });
    return hintQueryResult.getHintInstances();
  }

  @Test
  void instanceLevelHint() {
    CriteriaOccurrence criteriaOccurrence =
        (CriteriaOccurrence) underlay.getEntityGroup("measurementLoincPerson");
    Entity hintedEntity = criteriaOccurrence.getOccurrenceEntities().get(0);
    Entity relatedEntity = criteriaOccurrence.getCriteriaEntity();
    HintQueryResult hintQueryResult =
        bqQueryRunner.run(
            new HintQueryRequest(
                underlay,
                hintedEntity,
                relatedEntity,
                Literal.forInt64(46_272_910L),
                criteriaOccurrence,
                false));

    // Make sure we got some results back.
    assertFalse(hintQueryResult.getHintInstances().isEmpty());

    // Check each of the hinted attributes fields.
    hintQueryResult
        .getHintInstances()
        .forEach(
            hintInstance -> {
              Attribute attribute = hintInstance.getAttribute();
              assertTrue(
                  criteriaOccurrence
                      .getAttributesWithInstanceLevelDisplayHints(hintedEntity)
                      .contains(attribute));
              if (hintInstance.isRangeHint()) {
                assertTrue(
                    List.of(DataType.INT64, DataType.DOUBLE)
                        .contains(attribute.getRuntimeDataType()));
                assertTrue(hintInstance.getMin() <= hintInstance.getMax());
              } else { // isEnumHint
                assertTrue(
                    attribute.isValueDisplay()
                        || attribute.getRuntimeDataType().equals(DataType.STRING));
                assertFalse(hintInstance.getEnumValueCounts().isEmpty());
                hintInstance
                    .getEnumValueCounts()
                    .keySet()
                    .forEach(
                        enumValue ->
                            assertTrue(
                                enumValue.getValue().isNull()
                                    || attribute
                                        .getRuntimeDataType()
                                        .equals(enumValue.getValue().getDataType())));
              }
            });
  }
}
