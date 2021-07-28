package bio.terra.tanagra.service.search;

import bio.terra.tanagra.service.search.Expression.AttributeExpression;

/** Resolves logical entity model expressions to backing SQL constructs. */
public interface UnderlaySqlResolver {
  /** Resolve an {@link EntityVariable} as an SQL table clause. */
  String resolveTable(EntityVariable entity);

  /** Resolve an {@link AttributeExpression} as an SQL expression. */
  String resolve(AttributeVariable attribute);
}
