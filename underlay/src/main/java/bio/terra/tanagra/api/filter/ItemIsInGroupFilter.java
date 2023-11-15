package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.GroupItems;
import javax.annotation.Nullable;

public class ItemIsInGroupFilter extends RelationshipFilter {
  public ItemIsInGroupFilter(
      Underlay underlay,
      GroupItems groupItems,
      EntityFilter groupEntitySubFilter,
      @Nullable Attribute itemsEntityAttributeCountDistinct,
      @Nullable BinaryFilterVariable.BinaryOperator countOperator,
      @Nullable Integer countValue) {
    super(
        underlay,
        groupItems,
        groupItems.getItemsEntity(),
        groupItems.getGroupItemsRelationship(),
        groupEntitySubFilter,
        itemsEntityAttributeCountDistinct,
        countOperator,
        countValue);
  }
}
