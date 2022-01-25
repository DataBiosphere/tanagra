package bio.terra.tanagra.service.underlay;

/**
 * Possible operators to use when comparing a column to a value in a binary column filter on a table
 * in an underlay.
 */
public enum BinaryColumnFilterOperator {
  EQUALS,
  NOT_EQUALS,
  LESS_THAN,
  GREATER_THAN
}
