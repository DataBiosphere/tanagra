package bio.terra.tanagra.service.utils;

import avro.shaded.com.google.common.base.Preconditions;
import bio.terra.tanagra.generated.model.*;

public final class ValidationUtils {
  private ValidationUtils() {}

  public static void validateApiFilter(ApiFilterV2 filter) {
    // If one RelationshipFilterV2 group_by field is set, all group_by fields must be set.
    if (filter != null && filter.getFilterType() == ApiFilterV2.FilterTypeEnum.RELATIONSHIP) {
      ApiRelationshipFilterV2 relationshipFilter = filter.getFilterUnion().getRelationshipFilter();
      Preconditions.checkState(
          (relationshipFilter.getGroupByCountAttribute() == null
                  && relationshipFilter.getGroupByCountOperator() == null
                  && relationshipFilter.getGroupByCountValue() == null)
              || (relationshipFilter.getGroupByCountAttribute() != null
                  && relationshipFilter.getGroupByCountOperator() != null
                  && relationshipFilter.getGroupByCountValue() != null),
          "If one RelationshipFilterV2 group_by field is set, all group_by fields must be set");
    }
  }
}
