-- BuildPathsForHierarchy all-nodes input query for devices. This must match the entityMapping for the device entity in the aou_synthetic underlay.
SELECT
  c.concept_id AS node
FROM  `broad-tanagra-dev.aou_synthetic_SR2019q4r4.concept` c
WHERE c.domain_id = 'Device'
AND c.standard_concept = 'S'
;
