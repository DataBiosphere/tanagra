-- PrecomputeCounts all-auxiliary-nodes input query for observations. This must match the entityMapping for the observation occurrence entity in the aou_synthetic underlay.
SELECT
  o.observation_concept_id AS node,
  o.person_id AS what_to_count
FROM  `broad-tanagra-dev.aou_synthetic_SR2019q4r4.observation` o
;
