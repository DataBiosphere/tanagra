package bio.terra.tanagra.service.query;

import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT_ENGINE;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT_ENGINE_ID;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT_ENGINE_NAME;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR_NAME;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR_RATING;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.tanagra.service.search.AttributeVariable;
import bio.terra.tanagra.service.search.DataType;
import bio.terra.tanagra.service.search.EntityVariable;
import bio.terra.tanagra.service.search.Expression;
import bio.terra.tanagra.service.search.Filter;
import bio.terra.tanagra.service.search.Variable;
import bio.terra.tanagra.testing.BaseSpringUnitTest;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("nautical")
public class QueryServiceTest extends BaseSpringUnitTest {
  @Autowired private QueryService queryService;

  @Test
  void generatePrimaryKeySql() {
    assertEquals(
        "SELECT s.s_id AS primary_key FROM `my-project-id.nautical`.sailors AS s "
            + "WHERE s.rating = 42",
        queryService.generatePrimaryKeySql(
            EntityFilter.builder()
                .primaryEntity(EntityVariable.create(SAILOR, Variable.create("s")))
                .filter(
                    Filter.BinaryFunction.create(
                        Expression.AttributeExpression.create(
                            AttributeVariable.create(SAILOR_RATING, Variable.create("s"))),
                        Filter.BinaryFunction.Operator.EQUALS,
                        Expression.Literal.create(DataType.INT64, "42")))
                .build()));
  }

  @Test
  void generateSqlEntityDataset() {
    assertEquals(
        "SELECT s.rating AS rating, s.s_name AS name "
            + "FROM `my-project-id.nautical`.sailors AS s WHERE s.rating = 42",
        queryService.generateSql(
            EntityDataset.builder()
                .primaryEntity(EntityVariable.create(SAILOR, Variable.create("s")))
                .selectedAttributes(ImmutableList.of(SAILOR_RATING, SAILOR_NAME))
                .filter(
                    Filter.BinaryFunction.create(
                        Expression.AttributeExpression.create(
                            AttributeVariable.create(SAILOR_RATING, Variable.create("s"))),
                        Filter.BinaryFunction.Operator.EQUALS,
                        Expression.Literal.create(DataType.INT64, "42")))
                .build()));
  }

  @Test
  void generateSqlEntityDatasetWithTableFilter() {
    assertEquals(
        "SELECT boatengine.bp_id AS id, boatengine.bp_name AS name "
            + "FROM (SELECT * FROM `my-project-id.nautical`.boat_parts WHERE bp_type = 'engine') AS boatengine "
            + "WHERE TRUE",
        queryService.generateSql(
            EntityDataset.builder()
                .primaryEntity(EntityVariable.create(BOAT_ENGINE, Variable.create("boatengine")))
                .selectedAttributes(ImmutableList.of(BOAT_ENGINE_ID, BOAT_ENGINE_NAME))
                .filter(Filter.NullFilter.INSTANCE)
                .build()));
  }
}
