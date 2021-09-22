package bio.terra.tanagra.service.query.api;

import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT_TYPE_ID;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.RESERVATION;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.RESERVATION_DAY;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR_NAME;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR_RATING;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.loadNauticalUnderlay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.common.exception.BadRequestException;
import bio.terra.tanagra.generated.model.ApiArrayFilter;
import bio.terra.tanagra.generated.model.ApiArrayFilterOperator;
import bio.terra.tanagra.generated.model.ApiAttributeValue;
import bio.terra.tanagra.generated.model.ApiAttributeVariable;
import bio.terra.tanagra.generated.model.ApiBinaryFilter;
import bio.terra.tanagra.generated.model.ApiBinaryFilterOperator;
import bio.terra.tanagra.generated.model.ApiFilter;
import bio.terra.tanagra.generated.model.ApiRelationshipFilter;
import bio.terra.tanagra.service.search.AttributeVariable;
import bio.terra.tanagra.service.search.DataType;
import bio.terra.tanagra.service.search.EntityVariable;
import bio.terra.tanagra.service.search.Expression.AttributeExpression;
import bio.terra.tanagra.service.search.Expression.Literal;
import bio.terra.tanagra.service.search.Filter;
import bio.terra.tanagra.service.search.Filter.ArrayFunction;
import bio.terra.tanagra.service.search.Filter.BinaryFunction;
import bio.terra.tanagra.service.search.Filter.BinaryFunction.Operator;
import bio.terra.tanagra.service.search.Variable;
import bio.terra.tanagra.service.underlay.Underlay;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class FilterConverterTest {
  private static final Underlay NAUTICAL_UNDERLAY = loadNauticalUnderlay();
  private static final EntityVariable S_SAILOR =
      EntityVariable.create(SAILOR, Variable.create("s"));

  @Test
  void convertBinary() {
    VariableScope scope = new VariableScope().add(S_SAILOR);
    FilterConverter converter = new FilterConverter(NAUTICAL_UNDERLAY);

    ApiBinaryFilter apiBinary =
        new ApiBinaryFilter()
            .attributeVariable(new ApiAttributeVariable().variable("s").name("name"))
            .operator(ApiBinaryFilterOperator.EQUALS)
            .attributeValue(new ApiAttributeValue().stringVal("Jane"));
    BinaryFunction expectedFilter =
        BinaryFunction.create(
            AttributeExpression.create(AttributeVariable.create(SAILOR_NAME, S_SAILOR.variable())),
            Operator.EQUALS,
            Literal.create(DataType.STRING, "Jane"));
    assertEquals(expectedFilter, converter.convert(apiBinary, scope));
    assertEquals(expectedFilter, converter.convert(new ApiFilter().binaryFilter(apiBinary), scope));
  }

  @Test
  void convertBinaryDescendantOfOperator() {
    EntityVariable bBoat = EntityVariable.create(BOAT, Variable.create("b"));
    VariableScope scope = new VariableScope().add(bBoat);
    FilterConverter converter = new FilterConverter(NAUTICAL_UNDERLAY);

    ApiBinaryFilter apiBinary =
        new ApiBinaryFilter()
            .attributeVariable(new ApiAttributeVariable().variable("b").name("type_id"))
            .operator(ApiBinaryFilterOperator.DESCENDANT_OF)
            .attributeValue(new ApiAttributeValue().int64Val(12L));
    BinaryFunction expectedFilter =
        BinaryFunction.create(
            AttributeExpression.create(AttributeVariable.create(BOAT_TYPE_ID, bBoat.variable())),
            Operator.DESCENDANT_OF,
            Literal.create(DataType.INT64, "12"));
    // DESCENDANT_OF allowed with hierarchical attribute.
    assertEquals(expectedFilter, converter.convert(apiBinary, scope));
    // DESCENDANT_OF throws with non-hierarchical attribute.
    assertThrows(
        BadRequestException.class,
        () ->
            converter.convert(
                new ApiBinaryFilter()
                    .attributeVariable(new ApiAttributeVariable().variable("b").name("type_name"))
                    .operator(ApiBinaryFilterOperator.DESCENDANT_OF)
                    .attributeValue(new ApiAttributeValue().stringVal("foo")),
                scope));
  }

  @Test
  void convertBinaryOperator() {
    assertEquals(
        Filter.BinaryFunction.Operator.EQUALS,
        FilterConverter.convert(ApiBinaryFilterOperator.EQUALS));
    assertEquals(
        Filter.BinaryFunction.Operator.LESS_THAN,
        FilterConverter.convert(ApiBinaryFilterOperator.LESS_THAN));
    assertEquals(
        Filter.BinaryFunction.Operator.DESCENDANT_OF,
        FilterConverter.convert(ApiBinaryFilterOperator.DESCENDANT_OF));
  }

  @Test
  void convertArray() {
    VariableScope scope = new VariableScope().add(S_SAILOR);
    FilterConverter converter = new FilterConverter(NAUTICAL_UNDERLAY);

    ApiArrayFilter apiArray =
        new ApiArrayFilter()
            .addOperandsItem(
                new ApiFilter()
                    .binaryFilter(
                        new ApiBinaryFilter()
                            .attributeVariable(
                                new ApiAttributeVariable().variable("s").name("name"))
                            .operator(ApiBinaryFilterOperator.EQUALS)
                            .attributeValue(new ApiAttributeValue().stringVal("Jane"))))
            .addOperandsItem(
                new ApiFilter()
                    .binaryFilter(
                        new ApiBinaryFilter()
                            .attributeVariable(
                                new ApiAttributeVariable().variable("s").name("rating"))
                            .operator(ApiBinaryFilterOperator.EQUALS)
                            .attributeValue(new ApiAttributeValue().int64Val(47L))))
            .operator(ApiArrayFilterOperator.AND);
    ArrayFunction expectedFilter =
        ArrayFunction.create(
            ImmutableList.of(
                BinaryFunction.create(
                    AttributeExpression.create(
                        AttributeVariable.create(SAILOR_NAME, S_SAILOR.variable())),
                    Operator.EQUALS,
                    Literal.create(DataType.STRING, "Jane")),
                BinaryFunction.create(
                    AttributeExpression.create(
                        AttributeVariable.create(SAILOR_RATING, S_SAILOR.variable())),
                    Operator.EQUALS,
                    Literal.create(DataType.INT64, "47"))),
            ArrayFunction.Operator.AND);
    assertEquals(expectedFilter, converter.convert(apiArray, scope));
    assertEquals(expectedFilter, converter.convert(new ApiFilter().arrayFilter(apiArray), scope));
  }

  @Test
  public void convertArrayBadRequestsThrow() {
    FilterConverter converter = new FilterConverter(NAUTICAL_UNDERLAY);
    VariableScope scope = new VariableScope();

    // No operands.
    assertThrows(
        BadRequestException.class,
        () -> converter.convert(new ApiArrayFilter().operator(ApiArrayFilterOperator.AND), scope));
    // Null operand.
    List<ApiFilter> nullOperands = new ArrayList<>();
    nullOperands.add(null);
    assertThrows(
        BadRequestException.class,
        () ->
            converter.convert(
                new ApiArrayFilter().operator(ApiArrayFilterOperator.AND).operands(nullOperands),
                scope));
  }

  @Test
  void convertArrayOperator() {
    assertEquals(
        Filter.ArrayFunction.Operator.AND, FilterConverter.convert(ApiArrayFilterOperator.AND));
    assertEquals(
        Filter.ArrayFunction.Operator.OR, FilterConverter.convert(ApiArrayFilterOperator.OR));
  }

  @Test
  void convertRelationship() {
    FilterConverter converter = new FilterConverter(NAUTICAL_UNDERLAY);
    VariableScope scope = new VariableScope().add(S_SAILOR);

    EntityVariable rReservation = EntityVariable.create(RESERVATION, Variable.create("r"));

    ApiRelationshipFilter apiRelationship =
        new ApiRelationshipFilter()
            .outerVariable("s")
            .newVariable("r")
            .newEntity("reservations")
            .filter(
                new ApiFilter()
                    .binaryFilter(
                        new ApiBinaryFilter()
                            .attributeVariable(new ApiAttributeVariable().variable("r").name("day"))
                            .operator(ApiBinaryFilterOperator.EQUALS)
                            .attributeValue(new ApiAttributeValue().stringVal("Tuesday"))));

    Filter.RelationshipFilter expectedFilter =
        Filter.RelationshipFilter.builder()
            .outerVariable(S_SAILOR)
            .newVariable(rReservation)
            .filter(
                Filter.BinaryFunction.create(
                    AttributeExpression.create(
                        AttributeVariable.create(RESERVATION_DAY, rReservation.variable())),
                    Filter.BinaryFunction.Operator.EQUALS,
                    Literal.create(DataType.STRING, "Tuesday")))
            .build();

    assertEquals(expectedFilter, converter.convert(apiRelationship, scope));
    assertEquals(
        expectedFilter,
        converter.convert(new ApiFilter().relationshipFilter(apiRelationship), scope));
  }

  @Test
  void convertFilterExactlyOneOrElseThrows() {
    FilterConverter converter = new FilterConverter(NAUTICAL_UNDERLAY);
    VariableScope scope = new VariableScope().add(S_SAILOR);

    assertThrows(BadRequestException.class, () -> converter.convert(new ApiFilter(), scope));
    assertThrows(
        BadRequestException.class,
        () ->
            converter.convert(
                new ApiFilter()
                    .binaryFilter(new ApiBinaryFilter())
                    .relationshipFilter(new ApiRelationshipFilter()),
                scope));
  }
}
