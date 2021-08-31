package bio.terra.tanagra.service.query;

import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.tanagra.service.search.AttributeVariable;
import bio.terra.tanagra.service.search.DataType;
import bio.terra.tanagra.service.search.EntityVariable;
import bio.terra.tanagra.service.search.Expression;
import bio.terra.tanagra.service.search.Filter;
import bio.terra.tanagra.service.search.Variable;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class EntityDatasetTest {

  @Test
  void attributesMustMatchPrimaryEntity() {
    EntityDataset.Builder builder =
        EntityDataset.builder()
            .primaryEntity(EntityVariable.create(SAILOR, Variable.create("s")))
            .selectedAttributes(ImmutableList.of(SAILOR_RATING, SAILOR_NAME))
            .filter(
                Filter.BinaryFunction.create(
                    Expression.AttributeExpression.create(
                        AttributeVariable.create(SAILOR_RATING, Variable.create("s"))),
                    Filter.BinaryFunction.Operator.EQUALS,
                    Expression.Literal.create(DataType.INT64, "42")));
    // All sailor attributes build without issue.
    builder.build();

    // Non-sailor attribute throws.
    assertThrows(
        IllegalArgumentException.class,
        () ->
            builder
                .selectedAttributes(ImmutableList.of(BOAT_NAME, SAILOR_RATING))
                .filter(
                    Filter.BinaryFunction.create(
                        Expression.AttributeExpression.create(
                            AttributeVariable.create(SAILOR_RATING, Variable.create("s"))),
                        Filter.BinaryFunction.Operator.EQUALS,
                        Expression.Literal.create(DataType.INT64, "42")))
                .build());
  }
}
