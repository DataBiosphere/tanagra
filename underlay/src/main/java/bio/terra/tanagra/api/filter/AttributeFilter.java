package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.FunctionFilterVariable;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import com.google.common.collect.ImmutableList;
import java.util.List;

public class AttributeFilter extends EntityFilter {
  private final ITEntityMain indexTable;
  private final Underlay underlay;
  private final Entity entity;
  private final Attribute attribute;
  private final BinaryFilterVariable.BinaryOperator operator;
  private final FunctionFilterVariable.FunctionTemplate functionTemplate;
  private final ImmutableList<Literal> values;

  public AttributeFilter(
      Underlay underlay,
      Entity entity,
      Attribute attribute,
      BinaryFilterVariable.BinaryOperator operator,
      Literal value) {
    this.indexTable = underlay.getIndexSchema().getEntityMain(entity.getName());
    this.underlay = underlay;
    this.entity = entity;
    this.attribute = attribute;
    this.operator = operator;
    this.functionTemplate = null;
    this.values = ImmutableList.of(value);
  }

  public AttributeFilter(
      Underlay underlay,
      Entity entity,
      Attribute attribute,
      FunctionFilterVariable.FunctionTemplate functionTemplate,
      List<Literal> values) {
    this.indexTable = underlay.getIndexSchema().getEntityMain(entity.getName());
    this.underlay = underlay;
    this.entity = entity;
    this.attribute = attribute;
    this.operator = null;
    this.functionTemplate = functionTemplate;
    this.values = ImmutableList.copyOf(values);
  }

  @Override
  public FilterVariable getFilterVariable(
      TableVariable entityTableVar, List<TableVariable> tableVars) {
    FieldPointer valueField = indexTable.getAttributeValueField(attribute.getName());
    if (attribute.hasRuntimeSqlFunctionWrapper()) {
      valueField =
          valueField
              .toBuilder()
              .runtimeCalculated(true)
              .sqlFunctionWrapper(attribute.getRuntimeSqlFunctionWrapper())
              .build();
    }
    FieldVariable valueFieldVar = valueField.buildVariable(entityTableVar, tableVars);
    return functionTemplate == null
        ? new BinaryFilterVariable(valueFieldVar, operator, values.get(0))
        : new FunctionFilterVariable(
            functionTemplate, valueFieldVar, values.toArray(new Literal[0]));
  }

  public Underlay getUnderlay() {
    return underlay;
  }

  public Attribute getAttribute() {
    return attribute;
  }

  public Entity getEntity() {
    return entity;
  }

  public BinaryFilterVariable.BinaryOperator getOperator() {
    return operator;
  }

  public FunctionFilterVariable.FunctionTemplate getFunctionTemplate() {
    return functionTemplate;
  }

  public ImmutableList<Literal> getValues() {
    return values;
  }

  public boolean hasFunctionTemplate() {
    return functionTemplate != null;
  }
}
