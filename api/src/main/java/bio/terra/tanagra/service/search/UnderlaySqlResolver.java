package bio.terra.tanagra.service.search;

import bio.terra.tanagra.service.search.Expression.AttributeExpression;

/** Resolves logical entity model expressions to backing SQL constructs. */
public interface UnderlaySqlResolver {
  /** Resolve an {@link Entity} as an SQL table expression. */
  // TODO consider aliasing entities.
  String resolveTable(Entity entity);

  /** Resolve an {@link AttributeExpression} as an SQL string expression. */
  String resolve(AttributeExpression attribute);

  // TODO resolve relationships.
}
