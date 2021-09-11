package bio.terra.tanagra.service.query.api;

import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR_RATING;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.loadNauticalUnderlay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.common.exception.BadRequestException;
import bio.terra.tanagra.generated.model.ApiAttributeValue;
import bio.terra.tanagra.generated.model.ApiAttributeVariable;
import bio.terra.tanagra.model.DataType;
import bio.terra.tanagra.service.search.AttributeVariable;
import bio.terra.tanagra.service.search.EntityVariable;
import bio.terra.tanagra.service.search.Expression;
import bio.terra.tanagra.service.search.Variable;
import bio.terra.tanagra.service.underlay.Underlay;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class ExpressionConverterTest {
  private static final Underlay NAUTICAL_UNDERLAY = loadNauticalUnderlay();
  private static final EntityVariable S_SAILOR =
      EntityVariable.create(SAILOR, Variable.create("s"));

  @Test
  void convertAttributeValue() {
    ExpressionConverter converter = new ExpressionConverter(NAUTICAL_UNDERLAY);
    assertEquals(
        Expression.Literal.create(DataType.INT64, "42"),
        converter.convert(new ApiAttributeValue().int64Val(42L)));
    assertEquals(
        Expression.Literal.create(DataType.STRING, "foo"),
        converter.convert(new ApiAttributeValue().stringVal("foo")));
    assertThrows(
        BadRequestException.class, () -> converter.convert(new ApiAttributeValue().boolVal(true)));
  }

  @Test
  void convertAttributeValueExactlyOneOrElseThrows() {
    ExpressionConverter converter = new ExpressionConverter(NAUTICAL_UNDERLAY);
    assertThrows(BadRequestException.class, () -> converter.convert(new ApiAttributeValue()));
    assertThrows(
        BadRequestException.class,
        () -> converter.convert(new ApiAttributeValue().int64Val(42L).stringVal("foo")));
  }

  @Test
  void convertAttributeVariable() {
    ExpressionConverter converter = new ExpressionConverter(NAUTICAL_UNDERLAY);
    VariableScope scope = new VariableScope().add(S_SAILOR);

    ApiAttributeVariable apiSRating = new ApiAttributeVariable().variable("s").name("rating");

    assertEquals(
        Expression.AttributeExpression.create(
            AttributeVariable.create(SAILOR_RATING, S_SAILOR.variable())),
        converter.convert(apiSRating, scope));

    // No matching variable
    assertThrows(
        BadRequestException.class, () -> converter.convert(apiSRating, new VariableScope()));
    // No matching attribute
    assertThrows(
        BadRequestException.class,
        () -> converter.convert(new ApiAttributeVariable().variable("s").name("foo"), scope));
  }
}
