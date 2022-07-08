-- PrecomputeCounts all-auxiliary-nodes input query for measurements. This must match the entityMapping for the measurement occurrence entity in the aou_synthetic underlay.
SELECT
  m.measurement_concept_id AS node,
  m.person_id AS what_to_count
FROM  `broad-tanagra-dev.aou_synthetic_SR2019q4r4.measurement` m
;
