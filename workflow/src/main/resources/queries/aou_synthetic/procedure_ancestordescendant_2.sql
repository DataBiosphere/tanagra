-- PrecomputeCounts ancestor-descendant-relationships input query for procedures. This must match the entityMapping for the procedure entity in the aou_synthetic underlay.
SELECT
  pad.ancestor AS ancestor,
  pad.descendant AS descendant
FROM  `broad-tanagra-dev.aou_synthetic_SR2019q4r4_indexes.procedure_ancestor_descendant_2` pad
;
