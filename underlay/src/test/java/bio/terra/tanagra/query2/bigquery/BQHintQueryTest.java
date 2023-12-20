package bio.terra.tanagra.query2.bigquery;

import bio.terra.tanagra.api.query.hint.HintQueryRequest;
import bio.terra.tanagra.api.query.hint.HintQueryResult;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class BQHintQueryTest extends BQRunnerTest {
  @Override
  protected String getServiceConfigName() {
    return "aouSR2019q4r4_broad";
  }

  @Test
  void entityLevelHint() throws IOException {
    Entity hintedEntity = underlay.getPrimaryEntity();
    HintQueryResult hintQueryResult =
        BQQueryRunner.run(new HintQueryRequest(underlay, hintedEntity, null, null, null, true));

    TablePointer eldhTable =
        underlay
            .getIndexSchema()
            .getEntityLevelDisplayHints(hintedEntity.getName())
            .getTablePointer();
    assertSqlMatchesWithTableNameOnly("entityLevelHint", hintQueryResult.getSql(), eldhTable);
  }

  @Test
  void instanceLevelHint() throws IOException {
    CriteriaOccurrence criteriaOccurrence =
        (CriteriaOccurrence) underlay.getEntityGroup("measurementLoincPerson");
    Entity hintedEntity = criteriaOccurrence.getOccurrenceEntities().get(0);
    Entity relatedEntity = criteriaOccurrence.getCriteriaEntity();
    HintQueryResult hintQueryResult =
        BQQueryRunner.run(
            new HintQueryRequest(
                underlay,
                hintedEntity,
                relatedEntity,
                new Literal(46_272_910L),
                criteriaOccurrence,
                true));

    TablePointer eldhTable =
        underlay
            .getIndexSchema()
            .getInstanceLevelDisplayHints(
                criteriaOccurrence.getName(), hintedEntity.getName(), relatedEntity.getName())
            .getTablePointer();
    assertSqlMatchesWithTableNameOnly("instanceLevelHint", hintQueryResult.getSql(), eldhTable);
  }
}
