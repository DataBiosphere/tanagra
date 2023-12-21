package bio.terra.tanagra.query2.bigquery.resultparsing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.api.query.hint.HintQueryRequest;
import bio.terra.tanagra.api.query.hint.HintQueryResult;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query2.bigquery.BQRunnerTest;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import java.util.List;
import org.junit.jupiter.api.Test;

public class BqHintQueryResultsTest extends BQRunnerTest {
  @Override
  protected String getServiceConfigName() {
    return "aouSR2019q4r4_broad";
  }

  @Test
  void entityLevelHint() {
    Entity hintedEntity = underlay.getPrimaryEntity();
    HintQueryResult hintQueryResult =
        bqQueryRunner.run(new HintQueryRequest(underlay, hintedEntity, null, null, null, false));

    // Make sure we got some results back.
    assertFalse(hintQueryResult.getHintInstances().isEmpty());

    // Check each of the hinted attributes fields.
    hintQueryResult.getHintInstances().stream()
        .forEach(
            hintInstance -> {
              Attribute attribute = hintInstance.getAttribute();
              assertTrue(attribute.isComputeDisplayHint());
              if (hintInstance.isRangeHint()) {
                assertTrue(
                    List.of(Literal.DataType.INT64, Literal.DataType.DOUBLE)
                        .contains(attribute.getRuntimeDataType()));
                assertTrue(hintInstance.getMin() <= hintInstance.getMax());
              } else { // isEnumHint
                assertTrue(
                    attribute.isValueDisplay()
                        || attribute.getRuntimeDataType().equals(Literal.DataType.STRING));
                assertFalse(hintInstance.getEnumValueCounts().isEmpty());
                hintInstance.getEnumValueCounts().keySet().stream()
                    .forEach(
                        enumValue ->
                            assertEquals(
                                attribute.getRuntimeDataType(),
                                enumValue.getValue().getDataType()));
              }
            });
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
                new Literal(46_272_910L),
                criteriaOccurrence,
                false));

    // Make sure we got some results back.
    assertFalse(hintQueryResult.getHintInstances().isEmpty());

    // Check each of the hinted attributes fields.
    hintQueryResult.getHintInstances().stream()
        .forEach(
            hintInstance -> {
              Attribute attribute = hintInstance.getAttribute();
              assertTrue(
                  criteriaOccurrence
                      .getAttributesWithInstanceLevelDisplayHints(hintedEntity)
                      .contains(attribute));
              if (hintInstance.isRangeHint()) {
                assertTrue(
                    List.of(Literal.DataType.INT64, Literal.DataType.DOUBLE)
                        .contains(attribute.getRuntimeDataType()));
                assertTrue(hintInstance.getMin() <= hintInstance.getMax());
              } else { // isEnumHint
                assertTrue(
                    attribute.isValueDisplay()
                        || attribute.getRuntimeDataType().equals(Literal.DataType.STRING));
                assertFalse(hintInstance.getEnumValueCounts().isEmpty());
                hintInstance.getEnumValueCounts().keySet().stream()
                    .forEach(
                        enumValue ->
                            assertEquals(
                                attribute.getRuntimeDataType(),
                                enumValue.getValue().getDataType()));
              }
            });
  }
}
