package bio.terra.tanagra.api2.filter;

import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.FunctionFilterVariable;
import bio.terra.tanagra.underlay2.Underlay;
import bio.terra.tanagra.underlay2.entitymodel.Attribute;
import bio.terra.tanagra.underlay2.entitymodel.Entity;
import bio.terra.tanagra.underlay2.indextable.ITEntityMain;
import com.google.common.collect.ImmutableList;
import java.util.List;

public class AttributeFilter extends EntityFilter {
  private final ITEntityMain indexTable;
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
    this.attribute = attribute;
    this.operator = null;
    this.functionTemplate = functionTemplate;
    this.values = ImmutableList.copyOf(values);
  }

  @Override
  public FilterVariable getFilterVariable(
      TableVariable entityTableVar, List<TableVariable> tableVars) {
    FieldVariable valueFieldVar =
        indexTable
            .getAttributeValueField(attribute.getName())
            .buildVariable(entityTableVar, tableVars);
    return functionTemplate == null
        ? new BinaryFilterVariable(valueFieldVar, operator, values.get(0))
        : new FunctionFilterVariable(
            functionTemplate, valueFieldVar, values.toArray(new Literal[0]));
  }
}
