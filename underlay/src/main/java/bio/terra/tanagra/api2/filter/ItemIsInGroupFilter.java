package bio.terra.tanagra.api2.filter;

import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.underlay2.Attribute;
import bio.terra.tanagra.underlay2.entitygroup.GroupItems;
import javax.annotation.Nullable;

public class ItemIsInGroupFilter extends RelationshipFilter {
  public ItemIsInGroupFilter(
      GroupItems groupItems,
      EntityFilter groupEntitySubFilter,
      @Nullable Attribute itemsEntityAttributeCountDistinct,
      @Nullable BinaryFilterVariable.BinaryOperator countOperator,
      @Nullable Integer countValue) {
    super(
        groupItems.getItemsEntity(),
        groupItems.getRelationship(),
        groupEntitySubFilter,
        itemsEntityAttributeCountDistinct,
        countOperator,
        countValue);
  }
}
