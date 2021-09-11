package bio.terra.tanagra.service.search;

import bio.terra.tanagra.model.DataType;
import bio.terra.tanagra.service.search.Expression.AttributeExpression;
import bio.terra.tanagra.service.search.Expression.Literal;
import bio.terra.tanagra.service.search.Selection.Count;
import bio.terra.tanagra.service.search.Selection.PrimaryKey;
import bio.terra.tanagra.service.search.Selection.SelectExpression;
import bio.terra.tanagra.service.underlay.Underlay;
import com.google.common.annotations.VisibleForTesting;

/** Visitor classes for inferring the {@link DataType} of different part of query expressions. */
final class DataTypeVisitor {
  private DataTypeVisitor() {}

  /** A {@link Selection.Visitor} for finding the {@link DataType} of an {@link Selection}. */
  @VisibleForTesting
  public static class SelectionVisitor implements Selection.Visitor<DataType> {
    private final Underlay underlay;

    SelectionVisitor(Underlay underlay) {
      this.underlay = underlay;
    }

    @Override
    public DataType selectExpression(SelectExpression selectExpression) {
      return selectExpression.expression().accept(new ExpressionVisitor());
    }

    @Override
    public DataType count(Count count) {
      return DataType.INT64;
    }

    @Override
    public DataType primaryKey(PrimaryKey primaryKey) {
      return underlay.primaryKeys().get(primaryKey.entityVariable().entity()).dataType();
    }
  }

  /** An {@link Expression.Visitor} for finding the {@link DataType} of an {@link Expression}. */
  public static class ExpressionVisitor implements Expression.Visitor<DataType> {
    @Override
    public DataType visitLiteral(Literal literal) {
      return literal.dataType();
    }

    @Override
    public DataType visitAttribute(AttributeExpression attributeExpression) {
      return attributeExpression.attributeVariable().attribute().dataType();
    }
  }
}
