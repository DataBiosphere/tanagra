-- BuildPathsForHierarchy all-nodes input query for visits. This must match the entityMapping for the visit entity in the aou_synthetic underlay.
SELECT
  c.concept_id AS node
FROM  `broad-tanagra-dev.aou_synthetic_SR2019q4r4.concept` c
WHERE c.domain_id = 'Visit'
AND c.standard_concept = 'S'
;
