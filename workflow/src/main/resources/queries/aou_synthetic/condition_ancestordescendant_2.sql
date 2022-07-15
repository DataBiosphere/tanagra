-- PrecomputeCounts ancestor-descendant-relationships input query for conditions. This must match the entityMapping for the condition entity in the aou_synthetic underlay.
SELECT
  cad.ancestor AS ancestor,
  cad.descendant AS descendant
FROM  `broad-tanagra-dev.aou_synthetic_SR2019q4r4_indexes.condition_ancestor_descendant_2` cad
;
