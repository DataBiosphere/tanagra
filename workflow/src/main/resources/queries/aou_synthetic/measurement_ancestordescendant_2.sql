-- PrecomputeCounts ancestor-descendant-relationships input query for measurements. This must match the entityMapping for the measurement entity in the aou_synthetic underlay.
SELECT
  mad.ancestor AS ancestor,
  mad.descendant AS descendant
FROM  `broad-tanagra-dev.aou_synthetic_SR2019q4r4_indexes.measurement_ancestor_descendant_2` mad
;
