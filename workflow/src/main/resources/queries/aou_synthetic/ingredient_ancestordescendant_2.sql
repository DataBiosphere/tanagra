-- PrecomputeCounts ancestor-descendant-relationships input query for ingredients. This must match the entityMapping for the ingredient entity in the aou_synthetic underlay.
SELECT
  iad.ancestor AS ancestor,
  iad.descendant AS descendant
FROM  `broad-tanagra-dev.aou_synthetic_SR2019q4r4_indexes.ingredient_ancestor_descendant_2` iad
;
