package bio.terra.tanagra.service.query;

import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT_NAME;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR_NAME;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR_RATING;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.tanagra.service.search.EntityVariable;
import bio.terra.tanagra.service.search.Filter;
import bio.terra.tanagra.service.search.Variable;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class EntityCountsTest {

  @Test
  void attributesMustMatchPrimaryEntity() {
    EntityCounts.Builder builder =
        EntityCounts.builder()
            .primaryEntity(EntityVariable.create(SAILOR, Variable.create("s")))
            .groupByAttributes(ImmutableList.of(SAILOR_RATING))
            .additionalSelectedAttributes(ImmutableList.of(SAILOR_NAME))
            .filter(Filter.NullFilter.INSTANCE);
    // all sailor attributes build without issue.
    builder.build();

    // non-sailor attribute throws, both for group by and selected
    assertThrows(
        IllegalArgumentException.class,
        () -> builder.groupByAttributes(ImmutableList.of(BOAT_NAME)).build());
    assertThrows(
        IllegalArgumentException.class,
        () -> builder.additionalSelectedAttributes(ImmutableList.of(BOAT_NAME)).build());
  }

  @Test
  void countAllRows() {
    EntityCounts.builder()
        .primaryEntity(EntityVariable.create(SAILOR, Variable.create("s")))
        .filter(Filter.NullFilter.INSTANCE)
        .build();
  }
}
