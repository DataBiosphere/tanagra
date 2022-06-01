package bio.terra.tanagra.service.query.api;

import bio.terra.common.exception.BadRequestException;
import bio.terra.tanagra.generated.model.ApiArrayFilter;
import bio.terra.tanagra.generated.model.ApiArrayFilterOperator;
import bio.terra.tanagra.generated.model.ApiBinaryFilter;
import bio.terra.tanagra.generated.model.ApiBinaryFilterOperator;
import bio.terra.tanagra.generated.model.ApiFilter;
import bio.terra.tanagra.generated.model.ApiRelationshipFilter;
import bio.terra.tanagra.generated.model.ApiTextSearchFilter;
import bio.terra.tanagra.generated.model.ApiUnaryFilter;
import bio.terra.tanagra.generated.model.ApiUnaryFilterOperator;
import bio.terra.tanagra.service.search.Attribute;
import bio.terra.tanagra.service.search.DataType;
import bio.terra.tanagra.service.search.Entity;
import bio.terra.tanagra.service.search.EntityVariable;
import bio.terra.tanagra.service.search.Expression.AttributeExpression;
import bio.terra.tanagra.service.search.Expression.Literal;
import bio.terra.tanagra.service.search.Filter;
import bio.terra.tanagra.service.underlay.Underlay;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

/** Converts API filters to Tanagra search {@link bio.terra.tanagra.service.search.Filter}s. */
class FilterConverter {
  private final Underlay underlay;
  private final ExpressionConverter expressionConverter;

  FilterConverter(Underlay underlay) {
    this.underlay = underlay;
    this.expressionConverter = new ExpressionConverter(underlay);
  }

  /**
   * Converts an {@link ApiFilter} to a {@link Filter}.
   *
   * <p>If the ApiFilter is null, returns a filter that allows everything.
   */
  public Filter convert(@Nullable ApiFilter apiFilter, VariableScope scope) {
    if (apiFilter == null) {
      return Filter.NullFilter.INSTANCE;
    }

    if (!ConversionUtils.exactlyOneNonNull(
        apiFilter.getArrayFilter(),
        apiFilter.getUnaryFilter(),
        apiFilter.getBinaryFilter(),
        apiFilter.getRelationshipFilter(),
        apiFilter.getTextSearchFilter())) {
      throw new BadRequestException(
          String.format("Filter must have exactly one non-null field.%n%s", apiFilter.toString()));
    }

    if (apiFilter.getArrayFilter() != null) {
      return convert(apiFilter.getArrayFilter(), scope);
    } else if (apiFilter.getUnaryFilter() != null) {
      return convert(apiFilter.getUnaryFilter(), scope);
    } else if (apiFilter.getBinaryFilter() != null) {
      return convert(apiFilter.getBinaryFilter(), scope);
    } else if (apiFilter.getRelationshipFilter() != null) {
      return convert(apiFilter.getRelationshipFilter(), scope);
    } else if (apiFilter.getTextSearchFilter() != null) {
      return convert(apiFilter.getTextSearchFilter(), scope);
    }
    throw new BadRequestException("Unknown Filter type: " + apiFilter.toString());
  }

  @VisibleForTesting
  Filter.BinaryFunction convert(ApiBinaryFilter apiBinary, VariableScope scope) {
    if (apiBinary.getAttributeVariable() == null) {
      throw new BadRequestException(
          String.format("BinaryFilter.attributeVariable must not be null.%n%s", apiBinary));
    }
    if (apiBinary.getOperator() == null) {
      throw new BadRequestException(
          String.format("BinaryFilter.operator must not be null.%n%s", apiBinary));
    }

    AttributeExpression attributeExpression =
        expressionConverter.convert(apiBinary.getAttributeVariable(), scope);
    Filter.BinaryFunction.Operator operator = convert(apiBinary.getOperator());
    Literal literal = expressionConverter.convert(apiBinary.getAttributeValue());

    checkAttributeOperatorMatch(attributeExpression.attributeVariable().attribute(), operator);

    return Filter.BinaryFunction.create(attributeExpression, operator, literal);
  }

  @VisibleForTesting
  static Filter.BinaryFunction.Operator convert(ApiBinaryFilterOperator apiOperator) {
    switch (apiOperator) {
      case EQUALS:
        return Filter.BinaryFunction.Operator.EQUALS;
      case NOT_EQUALS:
        return Filter.BinaryFunction.Operator.NOT_EQUALS;
      case LESS_THAN:
        return Filter.BinaryFunction.Operator.LESS_THAN;
      case GREATER_THAN:
        return Filter.BinaryFunction.Operator.GREATER_THAN;
      case DESCENDANT_OF_INCLUSIVE:
        return Filter.BinaryFunction.Operator.DESCENDANT_OF_INCLUSIVE;
      case CHILD_OF:
        return Filter.BinaryFunction.Operator.CHILD_OF;
      default:
        throw new BadRequestException("Unknown BinaryFilterOperator: " + apiOperator.toString());
    }
  }

  /** Checks if the operator maybe be used with the attribute or else throws. */
  private void checkAttributeOperatorMatch(
      Attribute attribute, Filter.BinaryFunction.Operator operator) {
    if ((Filter.BinaryFunction.Operator.DESCENDANT_OF_INCLUSIVE.equals(operator)
            || Filter.BinaryFunction.Operator.CHILD_OF.equals(operator))
        && !underlay.hierarchies().containsKey(attribute)) {
      throw new BadRequestException(
          String.format(
              "Unable to use %s operator on attribute [%s.%s] that does not have a hierarchy.",
              operator, attribute.entity().name(), attribute.name()));
    }
  }

  @VisibleForTesting
  Filter.ArrayFunction convert(ApiArrayFilter apiArray, VariableScope scope) {
    if (apiArray.getOperands() == null || apiArray.getOperands().isEmpty()) {
      throw new BadRequestException("ArrayFilter must have at least one operand.");
    }
    if (apiArray.getOperands().stream().anyMatch(Objects::isNull)) {
      throw new BadRequestException("ArrayFilter must only have non-null operands.");
    }
    ImmutableList<Filter> filters =
        apiArray.getOperands().stream()
            .map(operand -> convert(operand, scope))
            .collect(ImmutableList.toImmutableList());
    return Filter.ArrayFunction.create(filters, convert(apiArray.getOperator()));
  }

  @VisibleForTesting
  static Filter.ArrayFunction.Operator convert(ApiArrayFilterOperator apiOperator) {
    switch (apiOperator) {
      case AND:
        return Filter.ArrayFunction.Operator.AND;
      case OR:
        return Filter.ArrayFunction.Operator.OR;
      default:
        throw new BadRequestException("Unknown ArrayFilterOperator: " + apiOperator.toString());
    }
  }

  @VisibleForTesting
  Filter.UnaryFunction convert(ApiUnaryFilter apiUnary, VariableScope scope) {
    if (apiUnary.getOperands() == null || apiUnary.getOperands().isEmpty()) {
      throw new BadRequestException("UnaryFilter must have at least one operand.");
    }
    if (apiUnary.getOperands().stream().anyMatch(Objects::isNull)) {
      throw new BadRequestException("UnaryFilter must only have non-null operands.");
    }
    ImmutableList<Filter> filters =
        apiUnary.getOperands().stream()
            .map(operand -> convert(operand, scope))
            .collect(ImmutableList.toImmutableList());
    return Filter.UnaryFunction.create(filters, convert(apiUnary.getOperator()));
  }

  @VisibleForTesting
  static Filter.UnaryFunction.Operator convert(ApiUnaryFilterOperator unaryOperator) {
    if (unaryOperator == ApiUnaryFilterOperator.NOT) return Filter.UnaryFunction.Operator.NOT;
    throw new BadRequestException("Unknown ArrayFilterOperator: " + unaryOperator.toString());
  }

  @VisibleForTesting
  Filter.RelationshipFilter convert(ApiRelationshipFilter apiRelationship, VariableScope scope) {
    Optional<EntityVariable> outerEntityVariable = scope.get(apiRelationship.getOuterVariable());
    if (outerEntityVariable.isEmpty()) {
      throw new BadRequestException(
          String.format(
              "Unknown variable '%s' in RelationshipFilter: %s",
              apiRelationship.getOuterVariable(), apiRelationship));
    }
    Entity newEntity = underlay.entities().get(apiRelationship.getNewEntity());
    if (newEntity == null) {
      throw new BadRequestException(
          String.format(
              "Unable to find entity '%s' in underlay '%s'",
              apiRelationship.getNewEntity(), underlay.name()));
    }
    EntityVariable newEntityVariable =
        EntityVariable.create(
            newEntity, ConversionUtils.createAndValidateVariable(apiRelationship.getNewVariable()));

    VariableScope innerScope = new VariableScope(scope);
    innerScope.add(newEntityVariable);
    Filter innerFilter = convert(apiRelationship.getFilter(), innerScope);

    return Filter.RelationshipFilter.builder()
        .outerVariable(outerEntityVariable.get())
        .newVariable(newEntityVariable)
        .filter(innerFilter)
        .build();
  }

  @VisibleForTesting
  Filter.TextSearchFilter convert(ApiTextSearchFilter apiTextSearch, VariableScope scope) {
    Optional<EntityVariable> entityVariable = scope.get(apiTextSearch.getEntityVariable());
    if (entityVariable.isEmpty()) {
      throw new BadRequestException(
          String.format(
              "Unknown variable '%s' in TextSearchFilter: %s",
              apiTextSearch.getEntityVariable(), apiTextSearch));
    }

    Literal term = Literal.create(DataType.STRING, apiTextSearch.getTerm());

    return Filter.TextSearchFilter.create(entityVariable.get(), term);
  }
}
