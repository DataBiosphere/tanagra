package bio.terra.tanagra.api.entityfilter;

import bio.terra.tanagra.api.EntityFilter;
import bio.terra.tanagra.query.BinaryFilterVariable;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.AttributeMapping;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityMapping;
import bio.terra.tanagra.underlay.Literal;
import bio.terra.tanagra.underlay.TableFilter;
import java.util.List;

public class AttributeFilter extends EntityFilter {
  private final Attribute attribute;
  private final TableFilter.BinaryOperator operator;
  private final Literal value;

  public AttributeFilter(
      Entity entity,
      EntityMapping entityMapping,
      Attribute attribute,
      TableFilter.BinaryOperator operator,
      Literal value) {
    super(entity, entityMapping);
    this.attribute = attribute;
    this.operator = operator;
    this.value = value;
  }

  @Override
  public FilterVariable getFilterVariable(
      TableVariable entityTableVar, List<TableVariable> tableVars) {
    AttributeMapping attributeMapping = getEntityMapping().getAttributeMapping(attribute.getName());
    FieldVariable valueFieldVar =
        attributeMapping.getValue().buildVariable(entityTableVar, tableVars);
    return new BinaryFilterVariable(valueFieldVar, operator, value);
  }
}
