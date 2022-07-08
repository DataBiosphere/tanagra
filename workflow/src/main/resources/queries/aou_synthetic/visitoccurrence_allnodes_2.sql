-- PrecomputeCounts all-auxiliary-nodes input query for visits. This must match the entityMapping for the visit occurrence entity in the aou_synthetic underlay.
SELECT
  vo.visit_concept_id AS node,
  vo.person_id AS what_to_count
FROM  `broad-tanagra-dev.aou_synthetic_SR2019q4r4.visit_occurrence` vo
;
