package bio.terra.tanagra.service.query;

import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR_RATING;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.tanagra.service.search.AttributeVariable;
import bio.terra.tanagra.service.search.DataType;
import bio.terra.tanagra.service.search.EntityVariable;
import bio.terra.tanagra.service.search.Expression;
import bio.terra.tanagra.service.search.Filter;
import bio.terra.tanagra.service.search.Variable;
import bio.terra.tanagra.testing.BaseSpringUnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("nautical")
public class QueryServiceTest extends BaseSpringUnitTest {
  @Autowired private QueryService queryService;

  @Test
  void generatePrimaryKeySql() {
    assertEquals(
        "SELECT s.s_id FROM `my-project-id.nautical`.sailors AS s WHERE s.rating = 42",
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
}
