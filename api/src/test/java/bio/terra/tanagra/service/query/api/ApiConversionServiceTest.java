package bio.terra.tanagra.service.query.api;

import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.common.exception.BadRequestException;
import bio.terra.tanagra.generated.model.ApiAttributeValue;
import bio.terra.tanagra.generated.model.ApiAttributeVariable;
import bio.terra.tanagra.generated.model.ApiBinaryFilter;
import bio.terra.tanagra.generated.model.ApiBinaryFilterOperator;
import bio.terra.tanagra.generated.model.ApiEntityFilter;
import bio.terra.tanagra.generated.model.ApiFilter;
import bio.terra.tanagra.service.query.EntityFilter;
import bio.terra.tanagra.service.search.AttributeVariable;
import bio.terra.tanagra.service.search.DataType;
import bio.terra.tanagra.service.search.EntityVariable;
import bio.terra.tanagra.service.search.Expression;
import bio.terra.tanagra.service.search.Filter;
import bio.terra.tanagra.service.search.Variable;
import bio.terra.tanagra.testing.BaseSpringUnitTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("nautical")
public class ApiConversionServiceTest extends BaseSpringUnitTest {
  @Autowired private ApiConversionService apiConversionService;

  @Test
  void entityFilter() {
    ApiEntityFilter apiEntityFilter =
        new ApiEntityFilter()
            .entityVariable("s")
            .filter(
                new ApiFilter()
                    .binaryFilter(
                        new ApiBinaryFilter()
                            .attributeVariable(
                                new ApiAttributeVariable().variable("s").name("rating"))
                            .operator(ApiBinaryFilterOperator.EQUALS)
                            .attributeValue(new ApiAttributeValue().int64Val(42L))));

    Variable sVar = Variable.create("s");
    assertEquals(
        EntityFilter.create(
            EntityVariable.create(SAILOR, Variable.create("s")),
            Filter.BinaryFunction.create(
                Expression.AttributeExpression.create(
                    AttributeVariable.create(SAILOR_RATING, sVar)),
                Filter.BinaryFunction.Operator.EQUALS,
                Expression.Literal.create(DataType.INT64, "42"))),
        apiConversionService.convertEntityFilter(
            NAUTICAL_UNDERLAY_NAME, "sailors", apiEntityFilter));
  }

  @Test
  void entityFilterUnknownNamesThrow() {
    BadRequestException bogusUnderlay =
        assertThrows(
            BadRequestException.class,
            () ->
                apiConversionService.convertEntityFilter(
                    "bogus_underlay", "sailors", new ApiEntityFilter()));
    assertThat(bogusUnderlay.getMessage(), Matchers.containsString("No known underlay with name"));
    BadRequestException bogusEntity =
        assertThrows(
            BadRequestException.class,
            () ->
                apiConversionService.convertEntityFilter(
                    NAUTICAL_UNDERLAY_NAME, "bogus_entity", new ApiEntityFilter()));
    assertThat(bogusEntity.getMessage(), Matchers.containsString("No known entity with name"));
  }
}
