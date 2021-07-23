package bio.terra.tanagra.service.search;

/** Resolves logical entity model expressions to backing SQL constructs. */
public interface UnderlaySqlResolver {
  /** Resolve an {@link Entity} as an SQL table expression. */
  // TODO consider aliasing entities.
  String resolveTable(Entity entity);

  /** Resolve an {@link Attribute} as an SQL string expression. */
  String resolve(Attribute attribute);

  // TODO resolve relationships.
}
