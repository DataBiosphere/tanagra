package bio.terra.tanagra.query.bigquery.sqlbuilding;

import static bio.terra.tanagra.UnderlayTestConfigs.AOUSR2019Q4R4;

import bio.terra.tanagra.api.query.hint.HintQueryRequest;
import bio.terra.tanagra.api.query.hint.HintQueryResult;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.query.bigquery.BQRunnerTest;
import bio.terra.tanagra.query.bigquery.BQTable;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class BQHintQueryTest extends BQRunnerTest {
  @Override
  protected String getServiceConfigName() {
    return AOUSR2019Q4R4.fileName();
  }

  @Test
  void entityLevelHint() throws IOException {
    Entity hintedEntity = underlay.getPrimaryEntity();
    HintQueryResult hintQueryResult =
        bqQueryRunner.run(new HintQueryRequest(underlay, hintedEntity, null, null, null, true));

    BQTable eldhTable =
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
        bqQueryRunner.run(
            new HintQueryRequest(
                underlay,
                hintedEntity,
                relatedEntity,
                Literal.forInt64(46_272_910L),
                criteriaOccurrence,
                true));

    BQTable eldhTable =
        underlay
            .getIndexSchema()
            .getInstanceLevelDisplayHints(
                criteriaOccurrence.getName(), hintedEntity.getName(), relatedEntity.getName())
            .getTablePointer();
    assertSqlMatchesWithTableNameOnly("instanceLevelHint", hintQueryResult.getSql(), eldhTable);
  }
}
