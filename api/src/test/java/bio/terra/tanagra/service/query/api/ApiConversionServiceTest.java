package bio.terra.tanagra.service.query.api;

import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.NAUTICAL_UNDERLAY_NAME;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR_NAME;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR_RATING;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.generated.model.ApiAttributeValue;
import bio.terra.tanagra.generated.model.ApiAttributeVariable;
import bio.terra.tanagra.generated.model.ApiBinaryFilter;
import bio.terra.tanagra.generated.model.ApiBinaryFilterOperator;
import bio.terra.tanagra.generated.model.ApiEntityDataset;
import bio.terra.tanagra.generated.model.ApiEntityFilter;
import bio.terra.tanagra.generated.model.ApiFilter;
import bio.terra.tanagra.generated.model.ApiOrderByDirection;
import bio.terra.tanagra.service.query.EntityDataset;
import bio.terra.tanagra.service.query.EntityFilter;
import bio.terra.tanagra.service.search.AttributeVariable;
import bio.terra.tanagra.service.search.DataType;
import bio.terra.tanagra.service.search.EntityVariable;
import bio.terra.tanagra.service.search.Expression;
import bio.terra.tanagra.service.search.Filter;
import bio.terra.tanagra.service.search.OrderByDirection;
import bio.terra.tanagra.service.search.Variable;
import bio.terra.tanagra.testing.BaseSpringUnitTest;
import com.google.common.collect.ImmutableList;
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
            EntityFilter.builder()
                    .primaryEntity(EntityVariable.create(SAILOR, Variable.create("s")))
                    .filter(
                            Filter.BinaryFunction.create(
                                    Expression.AttributeExpression.create(
                                            AttributeVariable.create(SAILOR_RATING, sVar)),
                                    Filter.BinaryFunction.Operator.EQUALS,
                                    Expression.Literal.create(DataType.INT64, "42")))
                    .build(),
            apiConversionService.convertEntityFilter(
                    NAUTICAL_UNDERLAY_NAME, "sailors", apiEntityFilter));
  }

  @Test
  void entityFilterUnknownNamesThrow() {
    NotFoundException bogusUnderlay =
            assertThrows(
                    NotFoundException.class,
                    () ->
                            apiConversionService.convertEntityFilter(
                                    "bogus_underlay", "sailors", new ApiEntityFilter()));
    assertThat(bogusUnderlay.getMessage(), Matchers.containsString("No known underlay with name"));
    NotFoundException bogusEntity =
            assertThrows(
                    NotFoundException.class,
                    () ->
                            apiConversionService.convertEntityFilter(
                                    NAUTICAL_UNDERLAY_NAME, "bogus_entity", new ApiEntityFilter()));
    assertThat(bogusEntity.getMessage(), Matchers.containsString("No known entity with name"));
  }

  @Test
  void entityDataset() {
    ApiEntityDataset apiEntityDataset =
            new ApiEntityDataset()
                    .entityVariable("s")
                    .selectedAttributes(ImmutableList.of("name", "rating"))
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
            EntityDataset.builder()
                    .primaryEntity(EntityVariable.create(SAILOR, Variable.create("s")))
                    .selectedAttributes(ImmutableList.of(SAILOR_NAME, SAILOR_RATING))
                    .filter(
                            Filter.BinaryFunction.create(
                                    Expression.AttributeExpression.create(
                                            AttributeVariable.create(SAILOR_RATING, sVar)),
                                    Filter.BinaryFunction.Operator.EQUALS,
                                    Expression.Literal.create(DataType.INT64, "42")))
                    .build(),
            apiConversionService.convertEntityDataset(
                    NAUTICAL_UNDERLAY_NAME, "sailors", apiEntityDataset));
  }

  @Test
  void entityDatasetWithOrderBy() {
    ApiEntityDataset apiEntityDataset =
            new ApiEntityDataset()
                    .entityVariable("s")
                    .selectedAttributes(ImmutableList.of("name", "rating"))
                    .orderByAttribute("name")
                    .orderByDirection(ApiOrderByDirection.DESC)
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
            EntityDataset.builder()
                    .primaryEntity(EntityVariable.create(SAILOR, Variable.create("s")))
                    .selectedAttributes(ImmutableList.of(SAILOR_NAME, SAILOR_RATING))
                    .orderByAttribute(SAILOR_NAME)
                    .orderByDirection(OrderByDirection.DESC)
                    .filter(
                            Filter.BinaryFunction.create(
                                    Expression.AttributeExpression.create(
                                            AttributeVariable.create(SAILOR_RATING, sVar)),
                                    Filter.BinaryFunction.Operator.EQUALS,
                                    Expression.Literal.create(DataType.INT64, "42")))
                    .build(),
            apiConversionService.convertEntityDataset(
                    NAUTICAL_UNDERLAY_NAME, "sailors", apiEntityDataset));
  }

  @Test
  void entityDatasetUnknownNamesThrow() {
    NotFoundException bogusUnderlay =
            assertThrows(
                    NotFoundException.class,
                    () ->
                            apiConversionService.convertEntityDataset(
                                    "bogus_underlay", "sailors", new ApiEntityDataset()));
    assertThat(bogusUnderlay.getMessage(), Matchers.containsString("No known underlay with name"));
    NotFoundException bogusEntity =
            assertThrows(
                    NotFoundException.class,
                    () ->
                            apiConversionService.convertEntityDataset(
                                    NAUTICAL_UNDERLAY_NAME, "bogus_entity", new ApiEntityDataset()));
    assertThat(bogusEntity.getMessage(), Matchers.containsString("No known entity with name"));
  }

  @Test
  void entityDatasetNegativeLimitThrows() {
    ApiEntityDataset apiEntityDataset =
            new ApiEntityDataset()
                    .entityVariable("s")
                    .selectedAttributes(ImmutableList.of("name", "rating"))
                    .limit(-1)
                    .filter(
                            new ApiFilter()
                                    .binaryFilter(
                                            new ApiBinaryFilter()
                                                    .attributeVariable(
                                                            new ApiAttributeVariable().variable("s").name("rating"))
                                                    .operator(ApiBinaryFilterOperator.EQUALS)
                                                    .attributeValue(new ApiAttributeValue().int64Val(42L))));

    IllegalArgumentException invalidLimitException =
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            apiConversionService.convertEntityDataset(
                                    NAUTICAL_UNDERLAY_NAME, "sailors", apiEntityDataset));
    assertThat(invalidLimitException.getMessage(), Matchers.containsString("The provided limit"));
  }

  @Test
  void entityDatasetWithLimit() {
    ApiEntityDataset apiEntityDataset =
            new ApiEntityDataset()
                    .entityVariable("s")
                    .selectedAttributes(ImmutableList.of("name", "rating"))
                    .limit(1)
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
            EntityDataset.builder()
                    .primaryEntity(EntityVariable.create(SAILOR, Variable.create("s")))
                    .selectedAttributes(ImmutableList.of(SAILOR_NAME, SAILOR_RATING))
                    .limit(1)
                    .filter(
                            Filter.BinaryFunction.create(
                                    Expression.AttributeExpression.create(
                                            AttributeVariable.create(SAILOR_RATING, sVar)),
                                    Filter.BinaryFunction.Operator.EQUALS,
                                    Expression.Literal.create(DataType.INT64, "42")))
                    .build(),
            apiConversionService.convertEntityDataset(
                    NAUTICAL_UNDERLAY_NAME, "sailors", apiEntityDataset));
  }
}
