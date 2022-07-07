-- PrecomputeCounts all-auxiliary-nodes input query for conditions. This must match the entityMapping for the condition occurrence entity in the aou_synthetic underlay.
SELECT
  co.condition_concept_id AS node,
  co.person_id AS secondary
FROM  `broad-tanagra-dev.aou_synthetic_SR2019q4r4.condition_occurrence` co
;
