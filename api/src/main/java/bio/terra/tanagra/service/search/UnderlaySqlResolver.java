package bio.terra.tanagra.service.search;

/** Resolves logical entity model expressions to backing SQL constructs. */
public interface UnderlaySqlResolver {
  /** Resolve an {@link Attribute} as an SQL string expression. */
  String resolve(Attribute attribute);

  // TODO resolve entities and relationships.
}
